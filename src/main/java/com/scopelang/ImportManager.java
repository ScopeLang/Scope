package com.scopelang;

import java.io.File;
import java.util.ArrayList;

import com.scopelang.metadata.FasmAnalyzer;

public final class ImportManager {
	private static ArrayList<File> importedFiles = new ArrayList<>();

	private ImportManager() {
	}

	public static void reset() {
		importedFiles.clear();
	}

	public static void add(File file) {
		// Check if a compiled version exists
		File cached = Utils.convertUncachedLibToCached(file);
		if (!cached.exists()) {
			// If not, compile it (imports will be added)
			try {
				Scope.cacheAsm(file, cached, true);
			} catch (Exception e) {
				String relativePath = Utils.pathRelativeToWorkingDir(file.toPath()).toString();
				Utils.error("Could not generate imported file `" + relativePath + "`.");
				e.printStackTrace();
				Utils.forceExit();
				return;
			}
		} else {
			// If so, read metadata and import manually
			FasmAnalyzer analyzer = new FasmAnalyzer(cached);
			for (var data : analyzer.imports) {
				add(data.file);
			}
		}

		// Add it to the list
		importedFiles.add(file);
	}

	public static File[] getAll() {
		return importedFiles.toArray(new File[0]);
	}
}