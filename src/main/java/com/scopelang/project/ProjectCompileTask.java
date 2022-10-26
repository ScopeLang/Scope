package com.scopelang.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import com.scopelang.FilePair;
import com.scopelang.Utils;
import com.scopelang.FilePair.RootType;
import com.scopelang.project.CompileTask.Mode;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

public class ProjectCompileTask {
	public ScopeXml xml;

	public File workingDir = null;
	public File cacheDir = null;
	public File libDir = null;

	public ProjectCompileTask(File root, ScopeXml xml) {
		this.xml = xml;
		workingDir = root;
	}

	public Path pathRelativeToWorkingDir(Path path) {
		if (!path.startsWith(workingDir.toPath())) {
			return path;
		}

		Path base = workingDir.toPath();
		return base.relativize(path);
	}

	public File run() {
		if (xml.mode.equals("project")) {
			return runProject();
		} else if (xml.mode.equals("library")) {
			return runLibrary();
		} else {
			Utils.error("Unknown project mode `" + xml.mode + "`.");
			return null;
		}
	}

	private File runProject() {
		// Create .cache and .lib folder
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
			Utils.forceExit();
		}

		// Get libraries from web
		xml.solveLibraries(this);

		// Compile main file to FASM
		File main = pathRelativeToWorkingDir(xml.mainFile.toPath()).toFile();
		var source = new FilePair(workingDir, main, RootType.NORMAL);
		CompileTask task = new CompileTask(source, Mode.MAIN);
		task.run(xml);

		// Remove old exe
		File exe = new File(workingDir, FilenameUtils.removeExtension(main.getPath()) + ".out");
		exe.delete();

		// Convert FASM to executable
		String exeName = exe.getAbsolutePath();
		Utils.log("Compiling executable to `" + exeName + "`.");
		Utils.runCmdAndWait("fasm", task.output.toFile().getAbsolutePath(), exeName);
		Utils.runCmdAndWait("chmod", "+x", exeName);

		// Error is FASM failed
		if (!exe.exists()) {
			Utils.error("FASM could not compile the assembly output!",
				"Use the `-f` option for details.");
			Utils.forceExit();
		}

		return exe;
	}

	private File runLibrary() {
		// Compile all files
		try {
			var sourceFiles = Files.walk(workingDir.toPath())
				.filter(Files::isRegularFile).map(i -> i.toFile());
			for (File f : sourceFiles.toArray(File[]::new)) {
				if (f.getName().endsWith(".scope")) {
					File main = pathRelativeToWorkingDir(f.toPath()).toFile();
					var source = new FilePair(workingDir, main, RootType.NORMAL);
					CompileTask task = new CompileTask(source, Mode.LIBRARY);
					task.run(xml);
				}
			}
		} catch (Exception e) {
			Utils.error("Could not locate source files.",
				"Use `-f` for more info.");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
		}

		// Delete old zip file
		var zipFile = new File(workingDir, xml.name + ".zip");
		zipFile.delete();

		// Package into a zip
		try {
			// Get all valid files
			var files = Files.find(workingDir.toPath(), Integer.MAX_VALUE,
				(path, fileAttr) -> {
					var n = path.getFileName().toString();

					// Skip non-files
					if (!fileAttr.isRegularFile()) {
						return false;
					}

					if (n.endsWith(".scope") || n.endsWith(".scopelib")) {
						return true;
					}

					if (n.equals("scope.xml") || n.equalsIgnoreCase("LICENSE")) {
						return true;
					}

					return false;
				}).map(i -> i.toFile());

			// Zip it up
			try (var z = new ZipFile(zipFile)) {
				// Add each
				for (var file : files.toArray(File[]::new)) {
					Path relativePath = workingDir.toPath();
					relativePath = relativePath.relativize(file.toPath());

					ZipParameters zipParameters = new ZipParameters();
					zipParameters.setFileNameInZip(relativePath.toString());

					z.addFile(file, zipParameters);
				}
			}
		} catch (Exception e) {
			Utils.error("Could not package library into a zip.",
				"Use `-f` for more info.");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
		}

		return null;
	}
}