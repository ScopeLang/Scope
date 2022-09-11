package com.scopelang;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scopelang.metadata.ImportManager;

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

			// Get the file name
			String fileName = text.substring(i + 1, stringEnd);
			if (fileName.contains(".")) {
				Utils.error("Imported files cannot have a `.` in them!",
					"All import statments are relative to the project `.scope.xml` and do not",
					"require the `.scope` extension.");
				Utils.forceExit();
				return;
			}
			fileName += ".scope";

			// Get the file
			if (fileName.contains(":")) {
				// Get the library
				String libName = fileName.substring(0, fileName.indexOf(":"));
				var lib = Scope.projXml.libraryInfoByName(libName);

				// Add to ImportManager
				if (lib != null) {
					String libLoc = lib.path;
					File importedFile = new File(libLoc,
						fileName.substring(fileName.indexOf(":") + 1, fileName.length()));
					ImportManager.addLib(libName, importedFile);
				} else {
					Utils.error("Library with name `" + libName + "` was not added to the project.",
						"To import this library, add the following to your `.scope.xml`:",
						"<library>" + libName + "</library>");
					Utils.forceExit();
					return;
				}
			} else {
				File importedFile = new File(Scope.workingDir, fileName);
				ImportManager.add(importedFile);
			}

			// Go to end
			i = stringEnd;
			i++;

			// Check for semicolon
			if (text.charAt(i) != ';') {
				Utils.error("Expected semi-colon after `import` keyword.");
				Utils.forceExit();
				return;
			}

			// Remove the whole import statement
			text = text.substring(0, matcher.start()) + text.substring(i + 1, text.length());
		}
	}

	public String get() {
		return text;
	}
}