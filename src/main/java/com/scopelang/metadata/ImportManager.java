package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;

import com.scopelang.Modules;
import com.scopelang.Utils;
import com.scopelang.project.CompileTask;

public class ImportManager {
	private Modules modules;

	private ArrayList<File> importedFiles = new ArrayList<>();

	public ImportManager(Modules modules) {
		this.modules = modules;
	}

	public void addRaw(String name) {
		String real = name + ".scope";

		if (real.contains(":")) {
			Utils.error("TODO");
			Utils.forceExit();
		} else {
			File file = new File(modules.task.root, real);
			if (!file.exists()) {
				Utils.error("File `" + real + "` does not exist.",
					"`" + name + "` is imported. Imports are always relative to `scope.xml`");
				Utils.forceExit();
				return;
			}
			importedFiles.add(new File(real));
		}
	}

	public File[] getAll() {
		return importedFiles.toArray(new File[0]);
	}

	public File[] getAllAsm() {
		ArrayList<File> files = new ArrayList<>();
		for (var file : importedFiles) {
			files.add(CompileTask.convertSourceToCompiled(
				modules.task.root, file, CompileTask.Mode.IMPORT));
		}
		return files.toArray(new File[0]);
	}
}