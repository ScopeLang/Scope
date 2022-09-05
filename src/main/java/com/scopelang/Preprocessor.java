package com.scopelang;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
	private String text;

	public Preprocessor(File file) {
		text = Utils.readFile(file);

		preprocess();
	}

	private void preprocess() {
		// Process import "statements"
		Pattern regex = Pattern.compile("\\b(import)\\b");
		Matcher matcher = regex.matcher(text);
		while (matcher.find()) {
			int i = matcher.end();

			// Look for start of string
			while (text.charAt(i) != '"') {
				if (!Character.isWhitespace(text.charAt(i))) {
					Utils.error("Found non-whitespace character after `import` keyword.");
					Utils.forceExit();
					return;
				}

				i++;
			}

			// Look for the end of the string
			int stringEnd = text.indexOf("\"", i + 1);
			if (stringEnd == -1) {
				Utils.error("Could not find end of string after `import` keyword.");
				Utils.forceExit();
				return;
			}

			// Get the file
			File importedFile = new File(Scope.workingDir, text.substring(i + 1, stringEnd) + ".scope");

			// Go to end
			i = stringEnd;
			i++;

			// Check for semicolon
			if (text.charAt(i) != ';') {
				Utils.error("Expected semi-colon after `import` keyword.");
				Utils.forceExit();
				return;
			}

			// Add to imported files
			ImportManager.add(importedFile);

			// Remove the whole import statement
			text = text.substring(0, matcher.start()) + text.substring(i + 1, text.length());
		}
	}

	public String get() {
		return text;
	}
}