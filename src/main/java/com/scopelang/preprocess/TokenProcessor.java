package com.scopelang.preprocess;

import java.io.File;
import java.util.HashMap;

import org.antlr.v4.runtime.*;

import com.scopelang.Modules;
import com.scopelang.ScopeLexer;
import com.scopelang.Utils;
import com.scopelang.error.ErrorLoc;
import com.scopelang.project.ScopeXml;

public class TokenProcessor {
	private File sourceFile;
	private CommonTokenStream stream;
	private Modules modules;

	public boolean errored = false;
	public HashMap<String, Integer> extactedStrings;

	public TokenProcessor(File sourceFile, CommonTokenStream tokenStream, ScopeXml xml, Modules modules) {
		this.sourceFile = sourceFile;
		stream = tokenStream;
		this.modules = modules;

		extactedStrings = new HashMap<String, Integer>();

		tokenProcess(xml);

		if (errored) {
			Utils.forceExit();
		}
	}

	public ErrorLoc locationOf(Token token) {
		return new ErrorLoc(sourceFile, token.getLine(), token.getCharPositionInLine() + 1);
	}

	private void tokenProcess(ScopeXml xml) {
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
				inImport = false;
				if (token.getType() == ScopeLexer.StringLiteral) {
					// Get and check file name
					String fileName = token.getText().substring(1, token.getText().length() - 1);
					if (fileName.contains(".")) {
						Utils.error("Imported files cannot have a `.` in them!",
							"All import statments are relative to the project `scope.xml` and do not",
							"require the `.scope` extension.");
						errored = true;
						continue;
					}

					// Add the file to libraries
					modules.importManager.addRaw(fileName, xml);
				} else {
					Utils.error("Expected string literal after `import`.",
						"Import statments look like this:",
						"import \"stdlib:Core\";");
					errored = true;
				}
			}
		}
	}
}
