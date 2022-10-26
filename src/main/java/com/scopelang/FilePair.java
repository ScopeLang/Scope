package com.scopelang;

import java.io.File;
import java.util.Objects;

public class FilePair {
	public static enum RootType {
		NORMAL, CACHE, LIBRARY
	}

	public File root;
	public File file;
	public RootType type;

	public FilePair(File root, File file, RootType type) {
		this.root = root;
		this.file = file;
		this.type = type;
	}

	public FilePair(String root, String file, RootType type) {
		this.root = new File(root);
		this.file = new File(file);
		this.type = type;
	}

	public FilePair(File root, String file, RootType type) {
		this.root = root;
		this.file = new File(file);
		this.type = type;
	}

	public boolean rootsMatch(FilePair other) {
		return root.equals(other.root) && type.equals(other.type);
	}

	public File toFile() {
		return new File(root, file.getPath());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		var fp = (FilePair) obj;
		return fp.root.equals(root)
			&& fp.file.equals(file)
			&& fp.type.equals(type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(root, file, type);
	}
}