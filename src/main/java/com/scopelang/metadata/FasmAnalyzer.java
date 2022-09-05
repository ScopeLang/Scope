package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;

import com.scopelang.Scope;
import com.scopelang.Utils;

public class FasmAnalyzer {
	public class ImportMeta {
		public File file;
		public String md5;

		public ImportMeta(File file, String md5) {
			this.file = file;
			this.md5 = md5;
		}
	}

	private String text;

	public ArrayList<ImportMeta> imports = new ArrayList<>();
	public String type = null;
	public String hash = null;
	public String source = null;

	public FasmAnalyzer(File file) {
		text = Utils.readFile(file);

		analyze();
	}

	private void analyze() {
		// All metadata is expected to be PROPERLY WRITTEN
		// any tampering with the metadata can cause ERRORS

		// Analyze file type
		int fileIndex = text.indexOf(";@FILE") + 7;
		int comma = text.indexOf(",", fileIndex);
		type = text.substring(fileIndex, comma);

		// Analyze file hash
		fileIndex = comma + 1;
		comma = text.indexOf(",", fileIndex);
		hash = text.substring(fileIndex, comma);

		// Analyze file source
		fileIndex = comma + 1;
		comma = text.indexOf("\n", fileIndex);
		source = text.substring(fileIndex, comma);

		// Analyze imports
		for (int i = text.indexOf(";@IMPORT"); i != -1; i = text.indexOf(";@IMPORT", i + 1)) {
			// Skip over ";@IMPORT" and the ","
			i += 9;

			// Get the index of the next ","
			int j = text.indexOf(",", i);

			// Get the md5
			String md5 = text.substring(i, j);
			i = j + 1;

			// Get the index of the newline
			j = text.indexOf("\n", i);

			// Get the file
			File file = new File(Scope.workingDir, text.substring(i, j));

			// Add
			imports.add(new ImportMeta(file, md5));
		}
	}
}