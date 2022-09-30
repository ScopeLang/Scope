package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import com.scopelang.Identifier;
import com.scopelang.Modules;
import com.scopelang.Scope;
import com.scopelang.Utils;

public class ImportManager {
	private Modules modules;

	private ArrayList<File> importedFiles = new ArrayList<>();

	public ImportManager(Modules modules) {
		this.modules = modules;
	}

	public void reset() {
		importedFiles.clear();
	}

	private Modules cache(File file, File cached) {
		try {
			return Scope.genAsm(file, cached, true);
		} catch (Exception e) {
			String relativePath = Utils.pathRelativeToWorkingDir(file.toPath()).toString();
			Utils.error("Could not generate imported file `" + relativePath + "`.");
			e.printStackTrace();
			Utils.forceExit();
			return null;
		}
	}

	// Adds an external library
	public void addLib(String libName, File libRoot, File file) {
		// Skip if library was already added
		if (importedFiles.contains(file)) {
			return;
		}

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

		// Analyze it
		FasmAnalyzer analyzer = new FasmAnalyzer(libRoot, libFile);

		for (var data : analyzer.imports) {
			addLib(libName, libRoot, data.file);
		}

		// Add library functions
		for (var entry : analyzer.functions.entrySet()) {
			modules.funcGatherer.addLibFunc(new Identifier(entry.getKey()),
				entry.getValue());
		}

		// Add it to the list
		importedFiles.add(file);
	}

	// Adds a project file
	public void add(File file) {
		// Skip if the file was already added
		if (importedFiles.contains(file)) {
			return;
		}

		// Check if a compiled version exists
		File cached = Utils.convertUncachedLibToCached(file);
		if (!cached.exists()) {
			var o = cache(file, cached);
			merge(o);
		} else {
			// If so, check the md5 and see if the file needs to be updated
			FasmAnalyzer analyzer = new FasmAnalyzer(Scope.workingDir, cached);
			String hash = Utils.hashOf(new File(Scope.workingDir, analyzer.source));
			if (!hash.equals(analyzer.hash)) {
				// If the hashes do not match, recompile
				Utils.log("Changes detected in `" + analyzer.source + "`. Recompiling.");
				var o = cache(file, cached);
				merge(o);
			} else {
				// Else, read the metadata and import manually
				for (var data : analyzer.imports) {
					add(data.file);
				}

				// Add library functions
				for (var entry : analyzer.functions.entrySet()) {
					modules.funcGatherer.addLibFunc(
						new Identifier(entry.getKey()), entry.getValue());
				}
			}
		}

		// Add it to the list
		importedFiles.add(file);
	}

	private void merge(Modules other) {
		modules.funcGatherer.merge(other.funcGatherer);
	}

	public File[] getAll() {
		return importedFiles.toArray(new File[0]);
	}
}
// quinn was here