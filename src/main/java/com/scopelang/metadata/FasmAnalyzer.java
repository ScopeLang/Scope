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

	public FasmAnalyzer(File file) {
		text = Utils.readFile(file);

		analyze();
	}

	private void analyze() {
		for (int i = text.indexOf(";@IMPORT"); i != -1; i = text.indexOf(";@IMPORT", i + 1)) {
			// Skip over ";@IMPORT" and the ","
			i += 9;

			// Get the index of the next ","
			int j = text.indexOf(",", i + 1);

			// Get the md5
			String md5 = text.substring(i, j);
			i = j + 1;

			// Get the index of the newline
			j = text.indexOf("\n", i + 1);

			// Get the file
			File file = new File(Scope.workingDir, text.substring(i, j));

			// Add
			imports.add(new ImportMeta(file, md5));
		}
	}
}