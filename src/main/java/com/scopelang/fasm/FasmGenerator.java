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

public class FasmGenerator extends ScopeBaseListener {
	private String sourceFile;
	private String fileName;
	private PrintWriter writer;
	private boolean libraryMode;

	public Preprocessor preprocessor;
	public int indent = 0;
	public boolean errored = false;

	public HashMap<String, Integer> localVariables = new HashMap<>();

	private boolean mainFound = false;

	public FasmGenerator(String sourceFile, String fileName, Preprocessor preprocessor, boolean libraryMode) {
		this.sourceFile = sourceFile;
		this.fileName = fileName;
		this.preprocessor = preprocessor;
		this.libraryMode = libraryMode;

		try {
			writer = new PrintWriter(fileName);
		} catch (IOException e) {
			Utils.error("Could not generate file.");
			e.printStackTrace();
			Utils.forceExit();
		}
	}

	public void insertHeader() {
		String date = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a").format(LocalDateTime.now());

		indent = 0;
		write("; Generated to `" + fileName + "` at " + date);
		write("");

		if (libraryMode) {
			write(";@FILE,LIB," + sourceFile);
			write("");
			return;
		}

		write(";@FILE,ELF64," + sourceFile);
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

	private void writeStrings() {
		for (var entry : preprocessor.extactedStrings.entrySet()) {
			String name = "c_" + entry.getValue();
			String str = Utils.processLiteral(entry.getKey());

			String bytes = "";
			for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
				bytes += (int) b + ", ";
			}
			bytes = bytes.substring(0, bytes.length() - 2);

			write(";@STR," + str.length());
			write(name + " db " + bytes);
		}
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
		if (!errored) {
			Utils.log("Finished writing assembly to `" + fileName + "`.");
		} else {
			Utils.log("Finished writing assembly to `" + fileName + "` (with errors).\n");
		}

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
		write("pop rax");
		write("mov QWORD [vlist], rax");
		write("pop rbp");

		// Add program exit if main func, return otherwise
		String ident = ctx.Identifier().getText();
		if (ident.equals("main")) {
			write("mov rdi, 0");
			write("call exit");
		} else {
			write("ret");
		}

		indent--;
		write("");
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
}