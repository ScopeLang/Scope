package com.scopelang;

import java.util.HashMap;

import org.antlr.v4.runtime.*;

public class Preprocessor {
	private CommonTokenStream stream;

	public HashMap<String, Integer> extactedStrings;

	public Preprocessor(CommonTokenStream tokenStream) {
		stream = tokenStream;

		extactedStrings = new HashMap<String, Integer>();

		preprocess();
	}

	private void preprocess() {
		stream.fill();
		for (int i = 0; i < stream.size(); i++) {
			Token token = stream.get(i);
			if (token.getType() == ScopeLexer.STRING) {
				String str = token.getText();
				int index = extactedStrings.size();
				extactedStrings.put(str, index);
			}
		}
	}
}
