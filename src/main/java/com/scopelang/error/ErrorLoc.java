package com.scopelang.error;

import java.io.File;

import com.scopelang.Utils;

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
		this.file = Utils.pathRelativeToWorkingDir(file.toPath()).toString();
		this.line = line;
		this.character = character;
	}
}