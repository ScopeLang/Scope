package com.scopelang.metadata;

import java.io.File;
import java.util.ArrayList;

import com.scopelang.Modules;

public class ImportManager {
	private Modules modules;

	private ArrayList<File> importedFiles = new ArrayList<>();

	public ImportManager(Modules modules) {
		this.modules = modules;
	}

	public void addRaw(String name) {
		String real = name + ".scope";
	}

	public File[] getAll() {
		return importedFiles.toArray(new File[0]);
	}
}