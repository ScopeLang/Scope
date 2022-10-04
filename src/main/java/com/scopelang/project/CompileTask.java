package com.scopelang.project;

import java.nio.file.Path;
import java.io.File;

public class CompileTask {
	public File root;
	public File source;

	public CompileTask(File root, File source) {
		this.root = root;
		this.source = pathRelativeToRoot(source.toPath()).toFile();
	}

	public Path pathRelativeToRoot(Path path) {
		Path base = root.toPath();

		// Make both paths the same type
		if (path.isAbsolute()) {
			base = base.toAbsolutePath();
		}

		return base.relativize(path);
	}
}
