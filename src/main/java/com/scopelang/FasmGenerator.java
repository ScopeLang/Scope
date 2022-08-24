package com.scopelang;

import java.io.*;
import java.nio.charset.StandardCharsets;

import com.scopelang.ScopeParser.InvokeContext;

public class FasmGenerator extends ScopeBaseListener {
	private String fileName;
	private Preprocessor preprocessor;

	private PrintWriter writer;

	private int indent = 0;

	public FasmGenerator(String fileName, Preprocessor preprocessor) {
		this.fileName = fileName;
		this.preprocessor = preprocessor;

		try {
			writer = new PrintWriter(fileName);
		} catch (IOException e) {
			System.out.println("Could not generate file.");
			e.printStackTrace();
		}
	}

	public void genHeader() {
		// Header
		write("format ELF64 executable 3");
		write("");
		write("struc db [data] {");
		write("common");
		indent++;
		write(". db data");
		write(".size = $ - .");
		indent--;
		write("}");
		write("");
		write("segment readable executable");
		write("entry _main");
		write("");

		// Standard procedures
		write("_exit:");
		indent++;
		write("mov rdi, rax");
		write("mov rax, 60");
		write("syscall");
		write("ret");
		indent--;
		write("");
		write("_print:");
		indent++;
		write("mov rsi, rax");
		write("mov rdi, 1");
		write("mov rax, 1");
		write("syscall");
		write("ret");
		indent--;
		write("");

		// Code start
		write("_main:");
		indent++;
	}

	public void finishGen() {
		// Code end
		write("mov rax, 0");
		write("call _exit");
		indent--;
		write("");

		// Data
		write("segment readable writable");
		write("");
		writeStrings();

		finish();
	}

	private void writeStrings() {
		for (int i = 0; i < preprocessor.extactedStrings.size(); i++) {
			String name = "str" + i;

			String bytes = "";
			for (byte b : preprocessor.extactedStrings.get(i).getBytes(StandardCharsets.UTF_8)) {
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
		System.out.println("Finished writing assembly to `" + fileName + "`.");

		writer.flush();
		writer.close();
	}

	@Override
	public void exitInvoke(InvokeContext ctx) {
		String ident = ctx.IDENT().getText();
		if (ident.equals("print")) {
			write("lea rax, [str0]");
			write("mov rdx, str0.size");
			write("call _print");
		}
	}
}