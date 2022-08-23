package com.scopelang;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FasmGenerator {
	private String fileName;

	private PrintWriter writer;

	private int indent = 0;

	public FasmGenerator(String fileName) {
		this.fileName = fileName;

		try {
			writer = new PrintWriter(fileName);
		} catch (IOException e) {
			System.out.println("Could not generate file.");
			e.printStackTrace();
		}
	}

	public void generate(Preprocessor preprocessor) {
		// Header
		write("format ELF64 executable 3");
		writeEmpty();
		write("segment readable executable");
		write("entry _main");
		writeEmpty();

		// Standard procedures
		write("_exit:");
		indent();
		write("mov rdi, rax");
		write("mov rax, 60");
		write("syscall");
		write("ret");
		unindent();
		writeEmpty();
		write("_print:");
		indent();
		write("mov rsi, rax");
		write("mov rdi, 1");
		write("mov rax, 1");
		write("syscall");
		write("ret");
		unindent();
		writeEmpty();

		// Code
		write("_main:");
		indent();
		write("mov rax, 0");
		write("call _exit");
		unindent();
		writeEmpty();

		// Strings
		write("segment readable writable");
		writeEmpty();
		for (int i = 0; i < preprocessor.extactedStrings.size(); i++) {
			String name = "str" + i;

			String bytes = "";
			for (byte b : preprocessor.extactedStrings.get(i).getBytes(StandardCharsets.UTF_8)) {
				bytes += (int) b + ", ";
			}
			bytes = bytes.substring(0, bytes.length() - 2);

			write(name + " db " + bytes);
			write(name + ".len = $ - " + name);
		}

		finish();
	}

	private void indent() {
		indent++;
	}

	private void unindent() {
		indent--;
	}

	private void write(String str) {
		for (int i = 0; i < indent; i++) {
			writer.print("\t");
		}

		writer.println(str);
	}

	private void writeEmpty() {
		writer.println();
	}

	private void finish() {
		System.out.println("Finished writing assembly to `" + fileName + "`.");

		writer.flush();
		writer.close();
	}
}
