package com.scopelang;

import java.util.ArrayList;
import org.antlr.v4.runtime.*;

public class Preprocessor {
	private CommonTokenStream stream;
	private TokenStreamRewriter rewriter;

	public ArrayList<String> extactedStrings;

	public Preprocessor(CommonTokenStream tokenStream) {
		stream = tokenStream;
		rewriter = new TokenStreamRewriter(tokenStream);

		extactedStrings = new ArrayList<String>();
	}

	public TokenStream getStream() {
		stream.fill();
		for (int i = 0; i < stream.size(); i++) {
			Token token = stream.get(i);
			if (token.getType() == ScopeLexer.STRING) {
				String rawStr = token.getText();
				extactedStrings.add(rawStr.substring(1, rawStr.length() - 1));

				// rewriter.replace(i, "str" + (extactedStrings.size() - 1));
			}
		}

		return rewriter.getTokenStream();
	}
}
