package com.scopelang;

public final class Utils {
	private Utils() {

	}

	public static String processLiteral(String str) {
		return str.substring(1, str.length() - 1);
	}
}