package com.scopelang.fasm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.io.IOUtils;

import com.scopelang.*;
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
		indent++;
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
	public void exitInvoke(InvokeContext ctx) {
		String ident = ctx.IDENT().getText();
		if (ident.equals("print")) {
			String str = ctx.STRING().getText();
			int index = preprocessor.extactedStrings.get(str);

			write("lea rax, [c_" + index + "]");
			write("mov rdx, c_" + index + ".size");
			write("call print");
		}
	}
}