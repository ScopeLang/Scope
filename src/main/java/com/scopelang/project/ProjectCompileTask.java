package com.scopelang.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import com.scopelang.Utils;

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
		Path base = workingDir.toPath();

		// Make both paths the same type
		if (path.isAbsolute()) {
			base = base.toAbsolutePath();
		}

		return base.relativize(path);
	}

	public File run() {
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
				Utils.forceExit();
			}
		}

		// Get libraries from web
		xml.solveLibraries(this);

		// Compile main file to FASM
		File main = pathRelativeToWorkingDir(xml.mainFile.toPath()).toFile();
		CompileTask task = new CompileTask(workingDir, main, CompileTask.Mode.MAIN);
		task.run(xml);

		// Remove old exe
		File exe = new File(workingDir, FilenameUtils.removeExtension(main.getPath()) + ".out");
		exe.delete();

		// Convert FASM to executable
		String exeName = exe.getAbsolutePath();
		Utils.log("Compiling executable to `" + exeName + "`.");
		Utils.runCmdAndWait("fasm", task.output.getAbsolutePath(), exeName);
		Utils.runCmdAndWait("chmod", "+x", exeName);

		// Error is FASM failed
		if (!exe.exists()) {
			Utils.error("FASM could not compile the assembly output!",
				"Use the `-f` option for details.");
			Utils.forceExit();
		}

		return exe;
	}
}