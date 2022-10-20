package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.scopelang.*;
import com.scopelang.preprocess.*;

public class FasmAnalyzer {
	public static class ImportMeta {
		public File file;
		public String md5;

		public ImportMeta(File file, String md5) {
			this.file = file;
			this.md5 = md5;
		}
	}

	private File root;
	private String text;

	public ArrayList<ImportMeta> imports = new ArrayList<>();
	public String type = null;
	public String hash = null;
	public String source = null;
	public HashMap<Identifier, FuncInfo> functions = new HashMap<>();

	public FasmAnalyzer(File root, File file) {
		this.root = root;
		text = Utils.readFile(file);

		analyze();
	}

	private void analyze() {
		// All metadata is expected to be PROPERLY WRITTEN
		// any tampering with the metadata can cause ERRORS

		// Analyze file type
		int fileIndex = text.indexOf(";@FILE");
		if (fileIndex == -1) {
			Utils.error("Could not find file metadata.");
			Utils.forceExit();
			return;
		}
		fileIndex += 7;

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
			File file = new File(root, text.substring(i, j));

			// Add
			imports.add(new ImportMeta(file, md5));
		}

		// Analyze functions
		for (int i = text.indexOf(";@FUNC"); i != -1; i = text.indexOf(";@FUNC", i + 1)) {
			// Skip over ";@FUNC" and the ","
			i += 7;

			// Get the data
			int end = text.indexOf("\n", i);
			String[] data = text.substring(i, end).split(",");

			// Get args
			ArrayList<ScopeType> args = new ArrayList<>();
			for (int j = 2; j < data.length; j++) {
				args.add(ScopeType.parseFromString(data[j]));
			}

			// Add function to gatherer
			var funcInfo = new FuncInfo(ScopeType.parseFromString(data[1]),
				args.toArray(ScopeType[]::new));
			functions.put(new Identifier(data[0]), funcInfo);
		}
	}
}