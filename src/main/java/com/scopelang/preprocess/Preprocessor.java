package com.scopelang.preprocess;

import java.io.File;

import com.scopelang.Utils;

public class Preprocessor {
	private String text;

	public Preprocessor(File file) {
		text = Utils.readFile(file);

		preprocess();
	}

	private void preprocess() {

	}

	public String get() {
		return text;
	}
}