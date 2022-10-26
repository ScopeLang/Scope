package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;

import com.scopelang.*;
import com.scopelang.FilePair.RootType;
import com.scopelang.project.ScopeXml;

public class ImportManager {
	private Modules modules;

	private ArrayList<FilePair> importedFiles = new ArrayList<>();

	public ImportManager(Modules modules) {
		this.modules = modules;
	}

	public void addRaw(String name, ScopeXml xml) {
		String real = name + ".scope";

		if (real.contains(":")) {
			String libName = real.substring(0, real.indexOf(":"));
			String fileName = real.substring(real.indexOf(":") + 1, real.length());
			var libInfo = xml.libraryInfoByName(libName);

			if (libInfo == null) {
				if (libName.equals("stdlib")) {
					Utils.error("`stdlib` isn't added to the project.",
						"Try adding this to your `scope.xml`",
						"<library type=\"github\">ScopeLang/stdlib</library>");
					Utils.forceExit();
				} else {
					Utils.error("Library with name `" + libName + "` isn't added to the project.",
						"Try adding one of the following to your `scope.xml`",
						"<library type=\"github\">AuthorNameHere/" + libName + "</library>",
						"<library type=\"remote\">https://example.com/link-to/" + libName + "</library>",
						"<library>path/to/" + libName + "</library>");
					Utils.forceExit();
				}
			}

			File file = new File(new File(modules.task.source.root, libInfo.path), fileName);
			if (!file.exists()) {
				Utils.error("File `" + real + "` does not exist in `" + libName + "`.",
					"Are you getting the file name right?");
				Utils.forceExit();
				return;
			}

			File root = new File(modules.task.source.root, libInfo.path);
			importedFiles.add(new FilePair(root, fileName, RootType.LIBRARY));
		} else {
			File file = new File(modules.task.source.root, real);
			if (!file.exists()) {
				Utils.error("File `" + real + "` does not exist.",
					"`" + name + "` is imported. Imports are always relative to `scope.xml`");
				Utils.forceExit();
				return;
			}
			importedFiles.add(new FilePair(modules.task.source.root, real, RootType.NORMAL));
		}
	}

	public void add(FilePair file) {
		importedFiles.add(file);
	}

	public ArrayList<FilePair> getAll() {
		return new ArrayList<>(importedFiles);
	}
}