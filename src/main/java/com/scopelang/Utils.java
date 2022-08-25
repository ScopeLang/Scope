package com.scopelang;

import org.apache.commons.text.StringEscapeUtils;

public final class Utils {
	public static boolean disableLog = false;

	private Utils() {

	}

	public static String processLiteral(String str) {
		String out = str.substring(1, str.length() - 1);
		out = StringEscapeUtils.unescapeJson(out);
		return out;
	}

	public static Process runCmd(String... cmd) {
		return runCmd(!disableLog, cmd);
	}

	public static Process runCmd(boolean inheritIO, String... cmd) {
		try {
			var builder = new ProcessBuilder(cmd);
			if (inheritIO) {
				builder.inheritIO();
			}
			return builder.start();
		} catch (Exception e) {
			log("Failed to run command.");
			e.printStackTrace();
			return null;
		}
	}

	public static int runCmdAndWait(String... cmd) {
		return runCmdAndWait(!disableLog, cmd);
	}

	public static int runCmdAndWait(boolean inheritIO, String... cmd) {
		Process p = runCmd(inheritIO, cmd);
		try {
			return p.waitFor();
		} catch (Exception e) {
			log("Failed to wait for process.");
			e.printStackTrace();
			return -1;
		}
	}

	public static void log(String str) {
		if (disableLog) {
			return;
		}

		System.out.println(str);
	}
}