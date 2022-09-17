package com.scopelang.fasm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.antlr.v4.runtime.Token;
import org.apache.commons.io.IOUtils;

import com.scopelang.*;
import com.scopelang.ScopeParser.*;
import com.scopelang.error.ErrorLoc;
import com.scopelang.metadata.ImportManager;
import com.scopelang.preprocess.*;

public class FasmGenerator extends ScopeBaseListener {
	private File sourceFile;
	private PrintWriter writer;
	private boolean libraryMode;

	public TokenProcessor tokenProcessor;
	public Preprocessor preprocessor;
	public FuncGatherer funcGatherer;
	public boolean errored = false;
	public String md5 = null;

	public Codeblock codeblock = null;

	private boolean mainFound = false;
	private String stringAppend = "";
	private boolean isFuncVoid = false;
	private boolean returnFound = false;

	public FasmGenerator(File sourceFile, File fileName, TokenProcessor tokenProcessor, Preprocessor preprocessor,
		FuncGatherer funcGatherer, boolean libraryMode) {

		this.sourceFile = sourceFile;
		this.tokenProcessor = tokenProcessor;
		this.preprocessor = preprocessor;
		this.funcGatherer = funcGatherer;
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
		var returnType = ScopeType.fromTypeNameCtx(ctx.typeName());

		// Write function header
		write(";@FUNC," + ident + "," + returnType);
		write("f_" + ident + ":");
		if (ident.equals("main")) {
			mainFound = true;
			write("\tcall init");
		}

		codeblock = new Codeblock(this);

		// Error if there are too many parameters
		var params = ctx.parameters().parameter();
		if (params.size() > Utils.ARG_REGS.length) {
			Utils.error(locationOf(ctx.start),
				"A function cannot have more than " + Utils.ARG_REGS.length + " parmeters.",
				"Having more than " + Utils.ARG_REGS.length + " parmeters is unreadable. Consider refactoring.");
			errored = true;
		}

		// Set arguments as local variables
		for (int i = 0; i < params.size(); i++) {
			var param = params.get(i);
			var name = param.Identifier().getText();

			// Check for duplicate params
			if (codeblock.varExists(name)) {
				Utils.error(locationOf(param.Identifier().getSymbol()),
					"Parameter `" + name + "` was already defined in the function contract.",
					"Try to keep parameter names concise and readable.");
				errored = true;
				continue;
			}

			// Set variable
			var type = ScopeType.fromTypeNameCtx(param.typeName());
			codeblock.appendArgument(name, Utils.ARG_REGS[i], type);
		}

		isFuncVoid = returnType.isVoid();

		// If it is a void func, returns are implicit
		returnFound = isFuncVoid;
	}

	@Override
	public void exitFunction(FunctionContext ctx) {
		// Handle error
		if (codeblock.errored) {
			errored = true;
			write("\t; This codeblock errored. Skipped write.");
			codeblock = null;
			return;
		}

		// Handle return error
		if (!returnFound) {
			Utils.error(locationOf(ctx.start),
				"A non-void function has no return statement.",
				"Change the function return type to void or add a return statement like so:",
				"ret <my-value-here>;");
			errored = true;
		}

		// Implicitly add return statement (for void only)
		if (ctx.typeName().VoidType() != null) {
			String ident = ctx.Identifier().getText();
			codeblock.startReturn();
			if (ident.equals("main")) {
				codeblock.add("mov rdi, 0");
				codeblock.add("call exit");
			}
			codeblock.endReturn();
		}

		// Write in the codeblock
		write(codeblock.toString());
		codeblock = null;
	}

	@Override
	public void exitDeclare(DeclareContext ctx) {
		String ident = ctx.Identifier().getText();

		// Error if the local variable already exists
		if (codeblock.varExists(ident)) {
			Utils.error(locationOf(ctx.start),
				"Variable `" + ident + "` was already defined in this scope.",
				"Try to keep variable names concise and readable.");
			errored = true;
			return;
		}

		// Get value and type
		var exprType = ExprEvaluator.eval(codeblock, ctx.expr());
		var type = ScopeType.fromTypeNameCtx(ctx.typeName());

		// Check
		if (!exprType.equals(type)) {
			Utils.error(locationOf(ctx.start),
				"The declaration type (`" + type + "`) and expression (`" + exprType + "`) don't match.",
				"Try changing the declaration type to `" + exprType + "`.");
			errored = true;
			return;
		}

		// Create!
		codeblock.varCreate(ident, type);
	}

	@Override
	public void exitInvoke(InvokeContext ctx) {
		String ident = ctx.Identifier().getText();
		if (ident.equals("print")) {
			ExprEvaluator.eval(codeblock, ctx.arguments().expr(0));
			codeblock.add("call print");
		} else {
			codeblock.addInvoke(ident, ctx.arguments().expr(), locationOf(ctx.start));
		}
	}

	@Override
	public void exitReturn(ReturnContext ctx) {
		if (isFuncVoid) {
			Utils.error(locationOf(ctx.start),
				"Attemped to return a value in a void function.",
				"Try changing the return type or removing this statement.");
			errored = true;
			return;
		}

		returnFound = true;

		codeblock.startReturn();
		ExprEvaluator.eval(codeblock, ctx.expr());
		codeblock.endReturn();
	}

	@Override
	public void enterBreakpoint(BreakpointContext ctx) {
		codeblock.add("int3");
	}
}