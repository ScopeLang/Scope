package com.scopelang;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.*;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;

import com.scopelang.error.ErrorLoc;

public final class Utils {
	public static final String[] ARG_REGS = {
		"rdx",
		"rcx",
		"r8",
		"r9",
		"r10",
		"r11"
	};

	public static boolean disableLog = false;

	private Utils() {
	}

	public static String processLiteral(String str) {
		String out = str.substring(1, str.length() - 1);
		out = StringEscapeUtils.unescapeJson(out);
		return out;
	}

	public static String closestMatch(String v, Stream<String> possibleValues) {
		var compare = new Comparator<String>() {
			@Override
			public int compare(String n1, String n2) {
				var l = LevenshteinDistance.getDefaultInstance();
				int a = l.apply(n1, v);
				int b = l.apply(n2, v);
				return Integer.compare(a, b);
			}
		};

		return possibleValues.min(compare).orElse(null);
	}

	public static String readFile(File file) {
		try {
			return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Utils.error("File `" + file + "` could not be read.", "Does `" + file + "` exist?");
			Utils.forceExit();
			return null;
		}
	}

	public static Path pathRelativeToWorkingDir(Path path) {
		Path base = Scope.workingDir.toPath();
		return base.relativize(path);
	}

	public static File convertUncachedLibToCached(File file) {
		String relative = pathRelativeToWorkingDir(file.toPath()).toString();
		String baseName = FilenameUtils.removeExtension(relative);
		return new File(Scope.cacheDir, baseName + ".scopelib");
	}

	public static String hashOf(File file) {
		try {
			return DigestUtils.md5Hex(new FileInputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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