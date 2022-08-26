package com.scopelang.fasm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.IOUtils;

import com.scopelang.*;
import com.scopelang.ScopeParser.FuncContext;
import com.scopelang.ScopeParser.InvokeContext;

public class FasmGenerator extends ScopeBaseListener {
	private String fileName;
	private Preprocessor preprocessor;
	private PrintWriter writer;

	private int indent = 0;

	private boolean mainFound = false;

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
		// Insert footer
		indent = 0;
		try {
			InputStream in = getClass().getResourceAsStream("GenericFooter.inc");
			write(IOUtils.toString(in, StandardCharsets.UTF_8));
		} catch (IOException e) {
			Utils.log("Could not insert footer.");
			e.printStackTrace();
		}

		writeStrings();

		if (!mainFound) {
			Utils.log("Warning! `main` function not found. FASM will crash!");
		}

		finish();
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

	private void write(String str) {
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
		Utils.log("Finished writing assembly to `" + fileName + "`.");

		writer.flush();
		writer.close();
	}

	@Override
	public void enterFunc(FuncContext ctx) {
		String ident = ctx.Identifier().getText();
		if (ident.equals("main")) {
			mainFound = true;
		}

		write("f_" + ident + ":");
		indent++;
	}

	@Override
	public void exitFunc(FuncContext ctx) {
		// Add program exit if main func, return otherwise
		String ident = ctx.Identifier().getText();
		if (ident.equals("main")) {
			write("mov rax, 0");
			write("call exit");
		} else {
			write("ret");
		}

		indent--;
		write("");
	}

	@Override
	public void exitInvoke(InvokeContext ctx) {
		String ident = ctx.Identifier().getText();
		if (ident.equals("print")) {
			String str = ctx.StringLiteral().getText();
			int index = preprocessor.extactedStrings.get(str);

			write("lea rax, [c_" + index + "]");
			write("mov rdx, c_" + index + ".size");
			write("call print");
		} else {
			write("call f_" + ident);
		}
	}
}