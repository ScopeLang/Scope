package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import com.scopelang.Scope;
import com.scopelang.Utils;

public final class ImportManager {
	private static ArrayList<File> importedFiles = new ArrayList<>();

	private ImportManager() {
	}

	public static void reset() {
		importedFiles.clear();
	}

	private static void cache(File file, File cached) {
		try {
			Scope.genAsm(file, cached, true);
		} catch (Exception e) {
			String relativePath = Utils.pathRelativeToWorkingDir(file.toPath()).toString();
			Utils.error("Could not generate imported file `" + relativePath + "`.");
			e.printStackTrace();
			Utils.forceExit();
			return;
		}
	}

	public static void addLib(String libName, File file) {
		// Get the .scopelib file
		String path = FilenameUtils.removeExtension(file.toPath().toString()) + ".scopelib";
		File libFile = new File(Scope.workingDir, path);

		// See if it exists
		if (!libFile.exists()) {
			Utils.error("The `" + libName + "` library doesn't seem to be compiled.",
				"Build `" + libName + "` using `scope build` before using it.");
			Utils.forceExit();
			return;
		}

		// Add it to the list
		importedFiles.add(file);
	}

	public static void add(File file) {
		// Check if a compiled version exists
		File cached = Utils.convertUncachedLibToCached(file);
		if (!cached.exists()) {
			// If not, compile it (imports will be added)
			cache(file, cached);
		} else {
			// If so, check the md5 and see if the file needs to be updated
			FasmAnalyzer analyzer = new FasmAnalyzer(cached);
			String hash = Utils.hashOf(new File(Scope.workingDir, analyzer.source));
			if (!hash.equals(analyzer.hash)) {
				// If the hashes do not match, recompile
				Utils.log("Changes detected in `" + analyzer.source + "`. Recompiling.");
				cache(file, cached);
			} else {
				// Else, read the metadata and import manually
				for (var data : analyzer.imports) {
					add(data.file);
				}
			}
		}

		// Add it to the list
		importedFiles.add(file);
	}

	public static File[] getAll() {
		return importedFiles.toArray(new File[0]);
	}
}