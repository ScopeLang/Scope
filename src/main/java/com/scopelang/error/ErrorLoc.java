package com.scopelang.error;

public class ErrorLoc {
	public String file;
	public int line;
	public int character;

	public ErrorLoc(String file, int line, int character) {
		this.file = file;
		this.line = line;
		this.character = character;
	}
}