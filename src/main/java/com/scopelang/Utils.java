package com.scopelang;

import org.apache.commons.text.StringEscapeUtils;

public final class Utils {
	private Utils() {

	}

	public static String processLiteral(String str) {
		String out = str.substring(1, str.length() - 1);
		out = StringEscapeUtils.unescapeJson(out);
		return out;
	}
}