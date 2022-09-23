package com.scopelang;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.scopelang.error.ErrorHandler;
import com.scopelang.fasm.FasmGenerator;
import com.scopelang.preprocess.FuncGatherer;
import com.scopelang.preprocess.Preprocessor;
import com.scopelang.preprocess.TokenProcessor;
import com.scopelang.project.ScopeXml;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

public final class Scope {
	public static File workingDir = null;
	public static File cacheDir = null;
	public static File libDir = null;
	public static ScopeXml projXml = null;

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
			if (dir != null) {
				workingDir = new File(dir);
			} else {
				workingDir = new File(System.getProperty("user.dir"));
			}

			// Check for scope.xml
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

			// Read scope.xml
			projXml = new ScopeXml(xmlFile);

			// Create .cache and .lib folder (if it doesn't exist and the mode has one)
			if (projXml.mode.equals("project")) {
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
					return;
				}
			} else if (projXml.mode.equals("library")) {
				cacheDir = workingDir;
			}

			// Solve libs
			if (!mode.equals("clean")) {
				projXml.solveLibraries();
			}

			// Build/run or whatever
			switch (mode) {
				case "build":
					if (projXml.mode.equals("project")) {
						build(projXml.mainFile, false);
					} else if (projXml.mode.equals("library")) {
						buildLibrary(projXml.name);
					}
					break;
				case "run":
					if (!projXml.mode.equals("project")) {
						Utils.error("You can only use the `run` option on projects!",
							"Try using `build` or `test` instead.");
						return;
					}

					build(projXml.mainFile, true);
					break;
				case "test":
					if (!projXml.mode.equals("library")) {
						Utils.error("You can only use the `test` option on libraries!",
							"Try using `build` or `run` instead.");
						return;
					}

					// Run then delete
					var exe = build(projXml.testFile, true);
					exe.delete();

					// Delete scopeasm
					String baseName = FilenameUtils.getBaseName(projXml.testFile.getPath());
					File asm = new File(cacheDir, baseName + ".scopeasm");
					asm.delete();

					break;
				case "clean":
					if (projXml.mode.equals("library")) {
						// Delete all scopelib files
						for (File f : workingDir.listFiles()) {
							if (f.getName().endsWith(".scopelib")) {
								f.delete();
							}
						}
					} else {
						try {
							// Delete .cache folder
							FileUtils.deleteDirectory(cacheDir);
							FileUtils.deleteDirectory(libDir);
						} catch (IOException e) {
							Utils.error("Could not delete cache folder.");
							if (!Utils.disableLog) {
								e.printStackTrace();
							}
							return;
						}
					}
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

	public static void genAsm(File sourceFile, File outputFile, boolean libraryMode) {
		var errorHandler = new ErrorHandler(sourceFile);

		// Preprocess
		Preprocessor preprocessor = new Preprocessor(sourceFile);

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Lex
		CharStream inputStream = CharStreams.fromString(preprocessor.get());
		ScopeLexer lexer = new ScopeLexer(inputStream);
		lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
		lexer.addErrorListener(errorHandler);

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Token process
		CommonTokenStream stream = new CommonTokenStream(lexer);
		TokenProcessor tokenProcessor = new TokenProcessor(sourceFile, stream);

		// Parse
		ScopeParser parser = new ScopeParser(stream);
		parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
		parser.addErrorListener(errorHandler);
		ParseTree tree = parser.program();

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Gather info
		FuncGatherer funcGatherer = new FuncGatherer();
		ParseTreeWalker.DEFAULT.walk(funcGatherer, tree);

		// Generate
		FasmGenerator generator = new FasmGenerator(sourceFile, outputFile, tokenProcessor,
			preprocessor, funcGatherer, libraryMode);
		generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(generator, tree);
		generator.finishGen();

		// Log
		Utils.log("Generated and cached `" + Utils.pathRelativeToWorkingDir(outputFile.toPath()).toString() + "`.");
	}

	private static File build(File mainFile, boolean run) {
		// Generate ASM
		String baseName = FilenameUtils.getBaseName(mainFile.getPath());
		File asm = new File(cacheDir, baseName + ".scopeasm");
		genAsm(mainFile, asm, false);

		// Delete old exe (if exists)
		File exe = new File(workingDir, baseName + ".out");
		exe.delete();

		// Convert ASM to executable
		String exeName = exe.getAbsolutePath();
		Utils.log("Compiling executable to `" + exeName + "`.");
		Utils.runCmdAndWait("fasm", asm.getAbsolutePath(), exeName);
		Utils.runCmdAndWait("chmod", "+x", exeName);

		// Check if executable exists
		if (!exe.exists()) {
			Utils.error("FASM could not compile the assembly output!",
				"Use the `-f` option for details.");
			return exe;
		}

		// Run (if asked)
		if (run) {
			int exitCode = Utils.runCmdAndWait(true, exeName);

			// Print warning if the exit code is not zero
			if (exitCode != 0) {
				Utils.log("Compiled program exited with non-zero exit code: " + exitCode);
			}
		}

		return exe;
	}

	private static void buildLibrary(String name) {
		// Compile all files
		try {
			var sourceFiles = Files.walk(workingDir.toPath())
				.filter(Files::isRegularFile).map(i -> i.toFile());
			for (File f : sourceFiles.toArray(File[]::new)) {
				// Skip test file
				if (f.equals(projXml.testFile)) {
					continue;
				}
				
				if (f.getName().endsWith(".scope")) {
					genAsm(f, new File(f.getPath() + "lib"), true);
				}
			}
		} catch (Exception e) {
			Utils.error("Could not locate source files.",
				"Use `-f` for more info.");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
		}

		// Delete old zip file
		var zipFile = new File(workingDir, name + ".zip");
		zipFile.delete();

		// Package into a zip
		try {
			// Get all valid files
			var files = Files.find(Paths.get(workingDir.toURI()), Integer.MAX_VALUE,
				(path, fileAttr) -> {
					var n = path.getFileName().toString();

					// Skip non-files
					if (!fileAttr.isRegularFile()) {
						return false;
					}

					// Skip test file
					if (path.toAbsolutePath().toString().equals(projXml.testFile.getAbsolutePath())) {
						return false;
					}

					if (n.endsWith(".scope") || n.endsWith(".scopelib")) {
						return true;
					}

					if (n.equals("scope.xml") || n.equalsIgnoreCase("LICENSE")) {
						return true;
					}

					return false;
				}).map(i -> i.toFile());

			// Zip it up
			try (var z = new ZipFile(zipFile)) {
				// Add each file relative to workspace
				for (var file : files.toArray(File[]::new)) {
					Path path = Utils.pathRelativeToWorkingDir(file.toPath());

					ZipParameters zipParameters = new ZipParameters();
					zipParameters.setFileNameInZip(path.toString());

					z.addFile(file, zipParameters);
				}
			}
		} catch (Exception e) {
			Utils.error("Could not package library into a zip.",
				"Use `-f` for more info.");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
		}
	}
}