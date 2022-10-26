package com.scopelang;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import com.scopelang.project.ProjectCompileTask;
import com.scopelang.project.ScopeXml;

public final class Scope {
	private Scope() {
	}

	public static void main(String[] args) {
		// Add options
		var options = new Options();

		var helpOpt = new Option("h", "help", false, "Prints this.");
		options.addOption(helpOpt);

		var directoryOpt = new Option("d", "dir", true, "The directory that the project is in.");
		options.addOption(directoryOpt);

		var fullOpt = new Option("f", "full", false, "Shows the full debug output.");
		options.addOption(fullOpt);

		// Parse command line args
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			printHelp(formatter, options);
			return;
		}

		Utils.disableLog = !cmd.hasOption("full");

		if (cmd.hasOption("help") || cmd.getArgs().length != 1) {
			printHelp(formatter, options);
			return;
		} else {
			String mode = cmd.getArgs()[0];

			// Set the working directory from the -d flag
			String dir = cmd.getOptionValue("dir");
			File workingDir;
			if (dir != null) {
				workingDir = new File(dir);
			} else {
				workingDir = new File(System.getProperty("user.dir"));
			}

			// Check and read scope.xml
			File xmlFile = new File(workingDir, "scope.xml");
			if (!xmlFile.exists()) {
				Utils.error("`scope.xml` does not exist!",
					"The `scope.xml` file stores information about your scope project.",
					"",
					"As an example, create a file named `scope.xml` and fill it with:",
					"<scope>",
					"\t<mode>project</mode>",
					"\t<main>HelloWorld.scope</main>",
					"</scope>",
					"",
					"Then, create a file named `HelloWorld.scope` in the same folder and put",
					"your scope code in it.");
				return;
			}
			var projXml = new ScopeXml(xmlFile);

			// If clean mode, clean
			if (mode.equals("clean")) {
				try {
					if (projXml.mode.equals("library")) {
						// Delete all scopelib and scopeasm files
						Files.walk(xmlFile.getParentFile().toPath())
							.filter(Files::isRegularFile).forEach(p -> {
								var f = p.toFile();
								if (f.getName().endsWith(".scopelib") ||
									f.getName().endsWith(".scopeasm") ||
									f.getName().equals(projXml.name + ".zip")) {

									f.delete();
								}
							});
					} else {
						// Delete cache folders
						FileUtils.deleteDirectory(new File(workingDir, ".lib"));
						FileUtils.deleteDirectory(new File(workingDir, ".cache"));
					}
				} catch (IOException e) {
					Utils.error("Could not delete cache folder or lib files.");
					if (!Utils.disableLog) {
						e.printStackTrace();
					}
				}
				return;
			}

			var task = new ProjectCompileTask(xmlFile.getParentFile(), projXml);

			// Build/run or whatever
			switch (mode) {
				case "build":
					task.run();
					break;
				case "run":
					File exe = task.run();
					if (exe == null) {
						Utils.error("An executable was not created in this project mode.",
							"Try using `scope build` next time.");
						break;
					}

					int exitCode = Utils.runCmdAndWait(true, exe.getAbsolutePath());
					if (exitCode != 0) {
						Utils.error("Compiled program exited with non-zero exit code: " + exitCode,
							"A non-zero exit code usually signifies an error.");
					}
					break;
				case "test":
					Utils.error("TODO");
					break;
				default:
					printHelp(formatter, options);
					return;
			}
		}
	}

	private static void printHelp(HelpFormatter formatter, Options options) {
		formatter.printHelp("scope <mode>", options);
		System.out.println("\nmodes:");
		System.out.println(" build   Builds the project.");
		System.out.println(" run     Builds then runs the project.");
		System.out.println(" test    Runs the test file for a library.");
		System.out.println(" clean   Deletes all cache files.");
	}
}