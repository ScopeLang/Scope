package com.scopelang;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class Preprocessor {
	private String file;
	private String text;

	public boolean errored = false;
	public ArrayList<String> importedFiles = new ArrayList<>();

	public Preprocessor(String file) {
		this.file = file;
		try {
			var s = new BufferedInputStream(new FileInputStream(file));
			text = IOUtils.toString(s, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Utils.error("File `" + file + "` could not be read.", "Does `" + file + "` exist?");
			errored = true;
		}

		if (errored) {
			Utils.forceExit();
		}

		preprocess();

		if (errored) {
			Utils.forceExit();
		}
	}

	private void preprocess() {
		String path = new File(file).getParentFile().getAbsolutePath() + "/";

		// Process import "statements"
		Pattern regex = Pattern.compile("\\b(import)\\b");
		Matcher matcher = regex.matcher(text);
		while (matcher.find()) {
			int i = matcher.end();

			// Look for start of string
			while (text.charAt(i) != '"') {
				if (!Character.isWhitespace(text.charAt(i))) {
					Utils.error("Found non-whitespace character after `import` keyword.");
					errored = true;
					return;
				}

				i++;
			}

			// Look for the end of the string
			int stringEnd = text.indexOf("\"", i + 1);
			if (stringEnd == -1) {
				Utils.error("Could not find end of string after `import` keyword.");
				errored = true;
				return;
			}

			// Get the file
			String fileName = path + text.substring(i + 1, stringEnd) + ".scope";

			// Go to end
			i = stringEnd;
			i++;

			// Check for semicolon
			if (text.charAt(i) != ';') {
				Utils.error("Expected semi-colon after `import` keyword.");
				errored = true;
				return;
			}

			// Check if a compiled version exists
			if (!new File(fileName + ".inc").exists()) {
				// If not, compile it
				try {
					Scope.generateAsm(fileName, true);
				} catch (Exception e) {
					Utils.error("Could not generate imported file `" + fileName + "`.");
					e.printStackTrace();
					errored = true;
					return;
				}
			}
			importedFiles.add(fileName);

			// Remove the whole import statement
			text = text.substring(0, matcher.start()) + text.substring(i + 1, text.length());
		}
	}

	public String get() {
		return text;
	}
}