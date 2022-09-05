package com.scopelang;

import java.io.File;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import com.scopelang.error.ErrorHandler;
import com.scopelang.fasm.FasmGenerator;

public final class Scope {
	private Scope() {
	}

	public static void main(String[] args) {
		// Add options
		var options = new Options();

		var helpOpt = new Option("h", "help", false, "Prints this.");
		options.addOption(helpOpt);

		var silentOpt = new Option("s", "silent", false,
			"Won't print anything except for program output if applicable.");
		options.addOption(silentOpt);

		var outputOpt = new Option("o", "output", true, "The output path. The default is `./<inputName>.out`");
		options.addOption(outputOpt);

		var libraryOpt = new Option("l", "library", false,
			"Whether or not the compile the specified program as a library. " +
				"Programs compiled in library mode will output an assembly file and cannot be ran.");
		options.addOption(libraryOpt);

		var runOpt = new Option("r", "run", false, "Whether or not to run the compiled program.");
		options.addOption(runOpt);

		// Parse command line args
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			formatter.printHelp("scope <file>", options);
			return;
		}

		// Run or print help
		if (cmd.hasOption("help") || cmd.getArgs().length != 1) {
			formatter.printHelp("scope <file>", options);
		} else {
			String file = new File(cmd.getArgs()[0]).getAbsolutePath();
			Utils.disableLog = cmd.hasOption("silent");
			if (cmd.hasOption("library")) {
				ImportManager.reset();

				try {
					Utils.log("Use `-s` or `--silent` to prevent output.");
					Utils.log("\n\033[0;32m== Generating ASM ==\033[0m\n");
					generateAsm(file, true);
					Utils.log("");
				} catch (Exception e) {
					Utils.error("Failed to generate file.");
					e.printStackTrace();
					return;
				}
			} else {
				compileFile(file, cmd.getOptionValue("output"),
					cmd.hasOption("run"), cmd.hasOption("delete"));
			}
		}
	}

	public static String generateAsm(String file, boolean libraryMode) throws Exception {
		var errorHandler = new ErrorHandler(file);

		// Preprocess
		Preprocessor preprocessor = new Preprocessor(file);

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
		TokenProcessor tokenProcessor = new TokenProcessor(file, stream);

		// Parse
		ScopeParser parser = new ScopeParser(stream);
		parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
		parser.addErrorListener(errorHandler);
		ParseTree tree = parser.program();

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Generate
		String asmName = file;
		asmName += libraryMode ? ".inc" : ".asm";
		FasmGenerator generator = new FasmGenerator(file, asmName, tokenProcessor, preprocessor, libraryMode);
		generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(generator, tree);
		generator.finishGen();

		return asmName;
	}

	public static void compileFile(String file, String outputName, boolean run, boolean delete) {
		ImportManager.reset();

		// Generate asm
		Utils.log("Use `-s` or `--silent` to prevent output.");
		Utils.log("\n\033[0;32m== Generating ASM ==\033[0m\n");
		String asmName;
		try {
			asmName = generateAsm(file, false);
		} catch (Exception e) {
			Utils.error("Failed to generate file.");
			e.printStackTrace();
			return;
		}

		// Get the name of the output
		if (outputName == null) {
			outputName = FilenameUtils.removeExtension(file) + ".out";
		}

		// Generate executable
		Utils.log("\n\033[0;32m== Compiling ==\033[0m\n");
		Utils.runCmdAndWait("fasm", asmName, outputName);
		Utils.runCmdAndWait("chmod", "+x", outputName);

		// If the file wasn't generated (i.e. FASM errored), exit
		if (!new File(outputName).exists()) {
			Utils.log("");
			Utils.forceExit();
			return;
		}

		Utils.log("Finished compiling to `" + outputName + "`.");

		if (!run) {
			Utils.log("");
			return;
		}

		// Run executable
		Utils.log("\n\033[0;32m== Running ==\033[0m\n");
		long startTime = System.currentTimeMillis();
		int exitCode = Utils.runCmdAndWait(true, outputName);
		long endTime = System.currentTimeMillis();

		// Print stats
		Utils.log("\n\033[0;32m== Stats ==\033[0m\n");
		Utils.log("Exit code: " + exitCode);
		Utils.log("Time: ~" + (endTime - startTime) + "ms");
		Utils.log("");
	}
}