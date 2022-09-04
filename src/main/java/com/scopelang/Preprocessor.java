package com.scopelang;

import java.util.HashMap;

import org.antlr.v4.runtime.*;

import com.scopelang.error.ErrorLoc;

public class Preprocessor {
	private String sourceFile;
	private CommonTokenStream stream;

	public boolean errored = false;
	public HashMap<String, Integer> extactedStrings;

	public Preprocessor(String sourceFile, CommonTokenStream tokenStream) {
		this.sourceFile = sourceFile;
		stream = tokenStream;

		extactedStrings = new HashMap<String, Integer>();

		preprocess();

		if (errored) {
			Utils.forceExit();
		}
	}

	public ErrorLoc locationOf(Token token) {
		return new ErrorLoc(sourceFile, token.getLine(), token.getCharPositionInLine() + 1);
	}

	private void preprocess() {
		stream.fill();
		for (int i = 0; i < stream.size(); i++) {
			Token token = stream.get(i);
			if (token.getType() == ScopeLexer.StringLiteral) {
				String str = token.getText();

				// If empty string
				if (str.equals("\"\"")) {
					Utils.error(locationOf(token), "Empty string literals are not allowed.",
						"Consider using `null` instead.");
					errored = true;
				}

				// If exists, skip
				if (extactedStrings.containsKey(str)) {
					continue;
				}

				int index = extactedStrings.size();
				extactedStrings.put(str, index);
			}
		}
	}
}
