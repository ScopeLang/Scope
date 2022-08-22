package com.scopelang;

import java.io.*;

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

	public void generate() {
		write("format ELF64 executable 3");
		writeEmpty();
		write("segment readable executable");
		write("entry main");
		writeEmpty();
		write("main:");
		indent();
		write("lea rdi, [msg]");
		write("mov rax, 14");
		write("mov rdx, rax");
		write("mov rsi, rdi");
		write("mov rdi, 1");
		write("mov rax, 1");
		write("syscall");
		write("xor rdi, rdi");
		write("mov rax, 60");
		write("syscall");
		unindent();
		writeEmpty();
		write("segment readable writable");
		writeEmpty();
		write("msg db 'Hello, World!', 10, 0");

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
