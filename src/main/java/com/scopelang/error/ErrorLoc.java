package com.scopelang.error;

import java.io.File;

public class ErrorLoc {
	public String file;
	public int line;
	public int character;

	public ErrorLoc(String file, int line, int character) {
		this.file = file;
		this.line = line;
		this.character = character;
	}

	public ErrorLoc(File file, int line, int character) {
		this.file = file.getPath();
		this.line = line;
		this.character = character;
	}
}