package com.scopelang;

import com.scopelang.ScopeParser.FullIdentContext;

public class Identifier {
	private String rawName;

	public Identifier(FullIdentContext ctx) {
		rawName = ctx.Identifier(0).getText();
		for (int i = 1; i < ctx.Identifier().size(); i++) {
			rawName += "$" + ctx.Identifier(i);
		}
	}

	public Identifier(String rawName) {
		this.rawName = rawName;
	}

	public Identifier(Identifier root, String name) {
		if (root == null) {
			rawName = name;
		} else {
			rawName = root.get() + "$" + name;
		}
	}

	public Identifier(Identifier namespace, Identifier ident) {
		this.rawName = namespace.get() + "$" + ident.get();
	}

	public String get() {
		return rawName;
	}

	public boolean isSimple() {
		return !rawName.contains("$");
	}

	@Override
	public String toString() {
		return toReadable(rawName);
	}

	@Override
	public boolean equals(Object obj) {
		return rawName.equals(((Identifier) obj).rawName);
	}

	@Override
	public int hashCode() {
		return rawName.hashCode();
	}

	public boolean equalsStr(String str) {
		return rawName.equals(str);
	}

	public static String toReadable(String str) {
		return str.replace("$", "::");
	}
}
