package com.scopelang;

import java.io.File;
import java.util.HashMap;

import org.antlr.v4.runtime.*;

import com.scopelang.error.ErrorLoc;

public class TokenProcessor {
	private File sourceFile;
	private CommonTokenStream stream;

	public boolean errored = false;
	public HashMap<String, Integer> extactedStrings;

	public TokenProcessor(File sourceFile, CommonTokenStream tokenStream) {
		this.sourceFile = sourceFile;
		stream = tokenStream;

		extactedStrings = new HashMap<String, Integer>();

		tokenProcess();

		if (errored) {
			Utils.forceExit();
		}
	}

	public ErrorLoc locationOf(Token token) {
		return new ErrorLoc(sourceFile, token.getLine(), token.getCharPositionInLine() + 1);
	}

	private void tokenProcess() {
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
