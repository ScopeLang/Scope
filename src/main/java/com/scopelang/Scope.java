package com.scopelang;

import java.io.File;
import java.nio.file.Files;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.scopelang.error.ErrorHandler;
import com.scopelang.fasm.FasmGenerator;
import com.scopelang.project.ScopeXml;

public final class Scope {
	public static File workingDir = null;
	public static File cacheDir = null;

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
			Utils.disableLog = !cmd.hasOption("full");

			if (cmd.hasOption("help") || cmd.getArgs().length != 1) {
				// Forced "help" message to appear
				throw new Exception();
			} else {
				// Set the working directory from the -d flag
				String dir = cmd.getOptionValue("dir");
				if (dir != null) {
					workingDir = new File(dir);
				}

				// Check for .scope.xml
				File xmlFile = new File(workingDir, ".scope.xml");
				if (!xmlFile.exists()) {
					Utils.error("`.scope.xml` does not exist!",
						"The `.scope.xml` file stores information about your scope project.",
						"",
						"As an example, create a file named `.scope.xml` and fill it with:",
						"<scope>",
						"\t<mode>project</mode>",
						"\t<main>HelloWorld.scope</main>",
						"</scope>",
						"",
						"Then, create a file named `HelloWorld.scope` in the same folder and put",
						"your scope code in it.");

					Utils.forceExit();
					return;
				}

				// Read .scope.xml
				ScopeXml xml = new ScopeXml(xmlFile);

				// Create .cache folder (if it doesn't exist)
				cacheDir = new File(workingDir, ".cache");
				Files.createDirectories(cacheDir.toPath());

				String mode = cmd.getArgs()[0];
				switch (mode) {
					case "build":
						build(xml.mainFile, false);
						break;
					case "run":
						build(xml.mainFile, true);
						break;
					case "clean":
						// Delete .cache folder
						FileUtils.deleteDirectory(cacheDir);
						break;
					default:
						throw new Exception();
				}
			}
		} catch (Exception e) {
			formatter.printHelp("scope <mode>", options);
			System.out.println("\nmodes:");
			System.out.println(" build   Builds the project.");
			System.out.println(" run     Builds then runs the project.");
			System.out.println(" clean   Deletes all cache files.");
		}
	}

	public static void cacheAsm(File sourceFile, File outputFile, boolean libraryMode) {
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

		// Generate
		FasmGenerator generator = new FasmGenerator(sourceFile, outputFile, tokenProcessor, preprocessor, libraryMode);
		generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(generator, tree);
		generator.finishGen();

		// Log
		Utils.log("Generated and cached `" + Utils.pathRelativeToWorkingDir(outputFile.toPath()).toString() + "`.");
	}

	private static void build(File mainFile, boolean run) {
		// Generate ASM
		String baseName = FilenameUtils.getBaseName(mainFile.getPath());
		File asm = new File(cacheDir, baseName + ".scopeasm");
		cacheAsm(mainFile, asm, false);

		// Convert ASM to executable
		String exeName = new File(workingDir, baseName + ".out").getAbsolutePath();
		Utils.log("Compiling executable to `" + exeName + "`.");
		Utils.runCmdAndWait("fasm", asm.getAbsolutePath(), exeName);
		Utils.runCmdAndWait("chmod", "+x", exeName);

		// Run (if asked)
		if (run) {
			Utils.runCmdAndWait(true, exeName);
		}
	}
}