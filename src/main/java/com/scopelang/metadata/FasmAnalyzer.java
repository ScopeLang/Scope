package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import com.scopelang.*;
import com.scopelang.preprocess.*;

public class FasmAnalyzer {
	public static class ImportMeta {
		public FilePair file;
		public String md5;

		public ImportMeta(FilePair file, String md5) {
			this.file = file;
			this.md5 = md5;
		}

		@Override
		public boolean equals(Object obj) {
			return file.equals(((ImportMeta) obj).file)
				&& md5.equals(((ImportMeta) obj).md5);
		}

		@Override
		public int hashCode() {
			return Objects.hash(file, md5);
		}
	}

	private FilePair sourceFile;
	private String text;

	public ArrayList<ImportMeta> imports = new ArrayList<>();
	public String type = null;
	public String hash = null;
	public String source = null;
	public HashMap<Identifier, FuncInfo> functions = new HashMap<>();
	public HashMap<Identifier, ScopeType> constants = new HashMap<>();

	public FasmAnalyzer(FilePair sourceFile) {
		this.sourceFile = sourceFile;
		text = Utils.readFile(sourceFile.toFile());

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
			var path = text.substring(i, j);
			var file = new FilePair(sourceFile.root,
				new File(path), sourceFile.type);

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

		// Analyze constants
		for (int i = text.indexOf(";@CONST"); i != -1; i = text.indexOf(";@CONST", i + 1)) {
			// Skip over ";@CONST" and the ","
			i += 8;

			// Get the index of the next ","
			int j = text.indexOf(",", i);

			// Get the name
			String name = text.substring(i, j);
			i = j + 1;

			// Get the index of the newline
			j = text.indexOf("\n", i);

			// Get the type
			var type = ScopeType.parseFromString(text.substring(i, j));

			// Add
			constants.put(new Identifier(name), type);
		}
	}
}