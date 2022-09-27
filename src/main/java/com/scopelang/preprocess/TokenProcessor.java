package com.scopelang.preprocess;

import java.io.File;
import java.util.HashMap;

import org.antlr.v4.runtime.*;

import com.scopelang.Scope;
import com.scopelang.ScopeLexer;
import com.scopelang.Utils;
import com.scopelang.error.ErrorLoc;
import com.scopelang.metadata.ImportManager;

public class TokenProcessor {
	private File sourceFile;
	private CommonTokenStream stream;
	private ImportManager importManager;

	public boolean errored = false;
	public HashMap<String, Integer> extactedStrings;

	public TokenProcessor(File sourceFile, CommonTokenStream tokenStream, ImportManager importManager) {
		this.sourceFile = sourceFile;
		stream = tokenStream;
		this.importManager = importManager;

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

		boolean inImport = false;
		for (int i = 0; i < stream.size(); i++) {
			Token token = stream.get(i);
			if (!inImport) {
				if (token.getType() == ScopeLexer.StringLiteral) {
					String str = token.getText();

					// If empty string, skip
					if (str.equals("\"\"")) {
						continue;
					}

					// If exists, skip
					if (extactedStrings.containsKey(str)) {
						continue;
					}

					int index = extactedStrings.size();
					extactedStrings.put(str, index);
				} else if (token.getType() == ScopeLexer.ImportKeyword) {
					inImport = true;
				}
			} else {
				if (token.getType() == ScopeLexer.StringLiteral) {
					inImport = false;

					// Get and check file name
					String fileName = token.getText().substring(1, token.getText().length() - 1);
					if (fileName.contains(".")) {
						Utils.error("Imported files cannot have a `.` in them!",
							"All import statments are relative to the project `scope.xml` and do not",
							"require the `.scope` extension.");
						errored = true;
						continue;
					}
					fileName += ".scope";

					// Add the file to libraries
					if (fileName.contains(":")) {
						// Get the library
						String libName = fileName.substring(0, fileName.indexOf(":"));
						var lib = Scope.projXml.libraryInfoByName(libName);

						// Add to ImportManager
						if (lib != null) {
							String libLoc = lib.path;
							File importedFile = new File(libLoc,
								fileName.substring(fileName.indexOf(":") + 1, fileName.length()));
							importManager.addLib(libName, new File(libLoc), importedFile);
						} else {
							Utils.error("Library with name `" + libName + "` was not added to the project.",
								"To import this library, add the following to your `scope.xml`:",
								"<library>" + libName + "</library>");
							errored = true;
							continue;
						}
					} else {
						File importedFile = new File(Scope.workingDir, fileName);
						importManager.add(importedFile);
					}
				}
			}
		}
	}
}
