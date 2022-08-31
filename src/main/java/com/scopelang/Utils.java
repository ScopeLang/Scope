package com.scopelang;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.text.StringEscapeUtils;

import com.scopelang.error.ErrorLoc;

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

	public static void error(String str, String... extra) {
		System.err.println("\033[1;91mERROR: \033[0;1m" + str + "\033[0m");
		for (String s : extra) {
			System.err.println("\033[1;91m   | \033[0;2m" + s + "\033[0m");
		}
		System.err.println("");
	}

	public static void error(ErrorLoc loc, String str, String... extra) {
		var e = new ArrayList<String>();
		e.add("At: \033[0m" + loc.file + ":" + loc.line + ":" + loc.character);
		if (extra.length > 0) {
			e.add("");
			e.addAll(Arrays.asList(extra));
		}
		error(str, e.toArray(new String[0]));
	}

	public static void forceExit() {
		System.err.println("The program could not finish compiling due to above errors.");
		System.err.println("Force exiting.");
		System.exit(1);
	}
}