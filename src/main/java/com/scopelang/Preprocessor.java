package com.scopelang;

import java.util.ArrayList;
import org.antlr.v4.runtime.*;

public class Preprocessor {
	private ScopeLexer lexer;

	public ArrayList<String> extactedStrings;

	public Preprocessor(ScopeLexer lexer) {
		this.lexer = lexer;

		extactedStrings = new ArrayList<String>();
	}

	public CommonTokenStream build() {
		Token token;
		do {
			token = lexer.nextToken();

			if (token.getType() == ScopeLexer.STRING) {
				String rawStr = token.getText();
				extactedStrings.add(rawStr.substring(1, rawStr.length() - 1));
			}
		} while (token.getType() != ScopeLexer.EOF);

		return new CommonTokenStream(lexer);
	}
}
