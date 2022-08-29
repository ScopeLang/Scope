package com.scopelang.fasm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

import com.scopelang.*;
import com.scopelang.ScopeParser.*;

public class FasmGenerator extends ScopeBaseListener {
	private String fileName;
	private PrintWriter writer;

	public Preprocessor preprocessor;
	public int indent = 0;

	private boolean mainFound = false;
	private HashMap<String, Integer> localVariables = new HashMap<>();
	private int localVariableMax = 0;

	public FasmGenerator(String fileName, Preprocessor preprocessor) {
		this.fileName = fileName;
		this.preprocessor = preprocessor;

		try {
			writer = new PrintWriter(fileName);
		} catch (IOException e) {
			Utils.log("Could not generate file.");
			e.printStackTrace();
		}
	}

	public void insertHeader() {
		String date = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a").format(LocalDateTime.now());

		indent = 0;
		write("; Generated to `" + fileName + "` at " + date);
		try {
			InputStream in = getClass().getResourceAsStream("GenericHeader.inc");
			write(IOUtils.toString(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			Utils.log("Could not insert header.");
			e.printStackTrace();
		}
	}

	public void finishGen() {
		write("; Constant Data ;");
		write("");
		write("segment readable");
		write("");
		writeStrings();

		if (!mainFound) {
			Utils.log("Warning! `main` function not found. FASM will crash!");
		}

		finish();
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

	private void writeStrings() {
		for (var entry : preprocessor.extactedStrings.entrySet()) {
			String name = "c_" + entry.getValue();
			String str = Utils.processLiteral(entry.getKey());

			String bytes = "";
			for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
				bytes += (int) b + ", ";
			}
			bytes = bytes.substring(0, bytes.length() - 2);

			write(name + " db " + bytes);
		}
	}

	private void finish() {
		Utils.log("Finished writing assembly to `" + fileName + "`.");

		writer.flush();
		writer.close();
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		String ident = ctx.Identifier().getText();

		write("f_" + ident + ":");
		indent++;

		if (ident.equals("main")) {
			mainFound = true;
			write("call init");
		}

		write("push rbp");
		write("mov rbp, rsp");

		localVariables.clear();
		localVariableMax = 0;
	}

	@Override
	public void exitFunction(FunctionContext ctx) {
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

	// @Override
	// public void exitDeclare(DeclareContext ctx) {
	// String ident = ctx.Identifier().getText();
	// localVariableMax += 4;
	// localVariables.put(ident, localVariableMax);
	// }

	@Override
	public void exitInvoke(InvokeContext ctx) {
		String ident = ctx.Identifier().getText();
		if (ident.equals("print")) {
			ExprEvaluator.eval(this, ctx.expr(), "rax", "rdx");
			write("call print");
		} else if (ident.equals("main")) {
			Utils.log("`main` cannot be called! Ignoring.");
		} else {
			write("call f_" + ident);
		}
	}
}