package com.scopelang;

import java.io.File;
import java.util.ArrayList;

import com.scopelang.metadata.FasmAnalyzer;

public final class ImportManager {
	private static ArrayList<String> importedFiles = new ArrayList<>();

	private ImportManager() {
	}

	public static void reset() {
		importedFiles.clear();
	}

	public static void add(String filePath) {
		// Check if a compiled version exists
		String libPath = filePath + ".inc";
		if (!new File(libPath).exists()) {
			// If not, compile it (imports will be added)
			try {
				Scope.generateAsm(filePath, true);
			} catch (Exception e) {
				Utils.error("Could not generate imported file `" + filePath + "`.");
				e.printStackTrace();
				Utils.forceExit();
				return;
			}
		} else {
			// If so, read metadata and import manually
			FasmAnalyzer analyzer = new FasmAnalyzer(libPath);
			for (var data : analyzer.imports) {
				add(data.file);
			}
		}

		// Add it to the list
		importedFiles.add(filePath);
	}

	public static String[] getAll() {
		return importedFiles.toArray(new String[0]);
	}
}