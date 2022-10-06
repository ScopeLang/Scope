package com.scopelang.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.scopelang.Utils;

public class ProjectCompileTask {
	public ScopeXml xml;

	public File workingDir = null;
	public File cacheDir = null;
	public File libDir = null;

	public ProjectCompileTask(ScopeXml xml) {
		this.xml = xml;
	}

	public Path pathRelativeToWorkingDir(Path path) {
		Path base = workingDir.toPath();

		// Make both paths the same type
		if (path.isAbsolute()) {
			base = base.toAbsolutePath();
		}

		return base.relativize(path);
	}

	public void run() {
		// Create .cache and .lib folder
		if (xml.mode.equals("project")) {
			cacheDir = new File(workingDir, ".cache");
			libDir = new File(workingDir, ".lib");
			try {
				Files.createDirectories(cacheDir.toPath());
				Files.createDirectories(libDir.toPath());
			} catch (IOException e) {
				Utils.error("Unable to create cache folder.");
				if (!Utils.disableLog) {
					e.printStackTrace();
				}
				return;
			}
		}

		// Get libraries from web
		xml.solveLibraries(this);

		// Compile main file
		// -> compiles libraries recursively
	}
}