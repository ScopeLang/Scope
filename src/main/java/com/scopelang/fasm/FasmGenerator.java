package com.scopelang.fasm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;

import com.scopelang.*;
import com.scopelang.ScopeParser.*;
import com.scopelang.error.ErrorLoc;
import com.scopelang.metadata.ImportManager;

public class FasmGenerator extends ScopeBaseListener {
	private File sourceFile;
	private PrintWriter writer;
	private boolean libraryMode;

	public TokenProcessor tokenProcessor;
	public Preprocessor preprocessor;
	public int indent = 0;
	public boolean errored = false;
	public String md5 = null;

	public HashMap<String, Integer> localVariables = new HashMap<>();

	private boolean mainFound = false;
	private String stringAppend = "";

	public FasmGenerator(File sourceFile, File fileName, TokenProcessor tokenProcessor, Preprocessor preprocessor,
		boolean libraryMode) {

		this.sourceFile = sourceFile;
		this.tokenProcessor = tokenProcessor;
		this.preprocessor = preprocessor;
		this.libraryMode = libraryMode;

		try {
			writer = new PrintWriter(fileName);
			md5 = Utils.hashOf(sourceFile);
		} catch (IOException e) {
			Utils.error("Could not generate file.");
			e.printStackTrace();
			Utils.forceExit();
		}
	}

	public void insertHeader() {
		String date = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a").format(LocalDateTime.now());
		String filePath = Utils.pathRelativeToWorkingDir(sourceFile.toPath()).toString();

		indent = 0;
		write("; Generated at " + date);
		write("");

		if (libraryMode) {
			write(";@FILE,LIB," + md5 + "," + filePath);
			writeImportMeta();
			write("");
			write(";@SEG_CODE");
			return;
		}

		write(";@FILE,ELF64," + md5 + "," + filePath);
		try {
			// Split header
			InputStream in = getClass().getResourceAsStream("files/headers/ELF64.inc");
			String[] file = IOUtils.toString(in, StandardCharsets.UTF_8).split(System.lineSeparator());

			// Remove comments and write
			for (String str : file) {
				int commentIndex = str.indexOf(";");
				if (commentIndex == -1) {
					write(str);
				} else {
					String s = str.substring(0, commentIndex);
					if (s.length() > 0) {
						write(s);
					}
				}
			}

			// Write padding
			write("");
		} catch (IOException e) {
			Utils.error("Could not insert header.");
			e.printStackTrace();
			Utils.forceExit();
		}

		if (!libraryMode) {
			writeImports();
		}
	}

	public void finishGen() {
		if (!libraryMode) {
			write("segment readable");
		} else {
			write(";@SEG_READ");
		}
		write("");
		writeStrings();

		if (!mainFound && !libraryMode) {
			Utils.error("A `main` function was not found.",
				"Try adding a `main` function like so:",
				"",
				"func void main() {",
				"\tprint(\"Hello, World!\");",
				"}",
				"",
				"If this a library, add the `-l` flag.");
			errored = true;
		} else if (mainFound && libraryMode) {
			Utils.error("A `main` function cannot be declared in library mode.",
				"Try remove the `-l` flag to compile as a normal program.");
			errored = true;
		}

		finish();

		if (errored) {
			Utils.forceExit();
		}
	}

	private void writeImportMeta() {
		for (var file : ImportManager.getAll()) {
			write(";@IMPORT," + Utils.hashOf(file) + "," + Utils.pathRelativeToWorkingDir(file.toPath()).toString());
		}
	}

	private void writeImports() {
		for (var file : ImportManager.getAll()) {
			String text = Utils.readFile(Utils.convertUncachedLibToCached(file));

			// Append to constants
			stringAppend += text.substring(text.indexOf(";@SEG_READ") + 10, text.length()).trim() + "\n";

			// Get the section of code
			int start = text.indexOf(";@SEG_CODE") + 10;
			int end = text.indexOf(";@SEG_READ");
			text = text.substring(start, end);

			// Update function metadata
			for (int i = text.indexOf(";@FUNC"); i != -1; i = text.indexOf(";@FUNC", i + 1)) {
				int j = text.indexOf('\n', i);
				text = text.substring(0, j) + "," + file + text.substring(j, text.length());
			}

			write(text);
		}
	}

	private void writeStrings() {
		for (var entry : tokenProcessor.extactedStrings.entrySet()) {
			String name = "s_" + md5 + "_" + entry.getValue();
			String str = Utils.processLiteral(entry.getKey());

			String bytes = "";
			for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
				bytes += (int) b + ", ";
			}
			bytes = bytes.substring(0, bytes.length() - 2);

			write(";@STR," + str.length());
			write(name + " db " + bytes);
		}

		write(stringAppend);
	}

	public void write(String str) {
		if (str.isEmpty()) {
			writer.println();
			return;
		}

		for (int i = 0; i < indent; i++) {
			writer.print("\t");
		}

		writer.println(str);
	}

	private void finish() {
		writer.flush();
		writer.close();
	}

	public ErrorLoc locationOf(Token token) {
		return new ErrorLoc(sourceFile, token.getLine(), token.getCharPositionInLine() + 1);
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		String ident = ctx.Identifier().getText();

		write(";@FUNC," + ctx.Identifier().getSymbol().getLine());
		write("f_" + ident + ":");
		indent++;

		if (ident.equals("main")) {
			mainFound = true;
			write("call init");
		}

		write("push rbp");
		write("mov rbp, rsp");
		write("push QWORD [vlist]");

		localVariables.clear();
	}

	@Override
	public void exitFunction(FunctionContext ctx) {
		String ident = ctx.Identifier().getText();
		endFunction(ctx.typeName().VoidType() != null, ident.equals("main"));

		indent--;
		write("");
	}

	private void endFunction(boolean isVoid, boolean isMain) {
		if (isVoid) {
			write("pop rax");
			write("mov QWORD [vlist], rax");
		}
		write("pop rbp");

		// Add program exit if main func, return otherwise
		if (isMain) {
			write("mov rdi, 0");
			write("call exit");
		} else {
			write("ret");
		}
	}

	@Override
	public void exitDeclare(DeclareContext ctx) {
		String ident = ctx.Identifier().getText();

		if (localVariables.containsKey(ident)) {
			Utils.error(locationOf(ctx.Identifier().getSymbol()),
				"Variable `" + ident + "` was already defined in this scope.",
				"Try to keep variable names concise and readable.");
			errored = true;
			return;
		}

		localVariables.put(ident, localVariables.size());

		ExprEvaluator.eval(this, ctx.expr());
		write("call vlist_append");
	}

	@Override
	public void exitInvoke(InvokeContext ctx) {
		String ident = ctx.Identifier().getText();
		if (ident.equals("print")) {
			ExprEvaluator.eval(this, ctx.expr());
			write("call print");
		} else if (ident.equals("main")) {
			Utils.error(locationOf(ctx.Identifier().getSymbol()),
				"The `main` function cannot be called manually.",
				"Try moving the contents of main into a different function",
				"and calling that instead like so:",
				"",
				"func void myNewFunc() {",
				"\t// The code that *was* in `main`",
				"}",
				"",
				"func void main() {",
				"\tmyNewFunc();",
				"}");
			errored = true;
		} else {
			write("call f_" + ident);
		}
	}

	@Override
	public void exitReturn(ReturnContext ctx) {
		write("pop rax");
		write("mov QWORD [vlist], rax");
		ExprEvaluator.eval(this, ctx.expr());
		write("call vlist_append");
		endFunction(false, false);
	}
}