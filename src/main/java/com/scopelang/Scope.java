package com.scopelang;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.apache.commons.cli.*;

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

		var runOpt = new Option("r", "run", false, "Whether or not to run the compiled program.");
		options.addOption(runOpt);

		var deleteOpt = new Option("d", "delete", false,
				"Whether or not to delete the compiled program after running. Does not work if `-r` is not set.");
		options.addOption(deleteOpt);

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
			Utils.disableLog = cmd.hasOption("silent");
			compileFile(cmd.getArgs()[0], cmd.getOptionValue("output"),
					cmd.hasOption("run"), cmd.hasOption("delete"));
		}
	}

	public static String generateAsm(String file) throws Exception {
		// Lex
		CharStream inputStream = CharStreams.fromFileName(file);
		ScopeLexer lexer = new ScopeLexer(inputStream);

		// Preprocess
		CommonTokenStream stream = new CommonTokenStream(lexer);
		Preprocessor preprocessor = new Preprocessor(stream);

		// Parse
		ScopeParser parser = new ScopeParser(stream);
		ParseTree tree = parser.program();

		// Generate
		String asmName = file + ".asm";
		FasmGenerator generator = new FasmGenerator(asmName, preprocessor);
		generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(generator, tree);
		generator.finishGen();

		return asmName;
	}

	public static void compileFile(String file, String outputName, boolean run, boolean delete) {
		// Generate asm
		Utils.log("Use `-s` or `--silent` to prevent output.");
		Utils.log("\n\033[0;32m== Generating ASM ==\033[0m\n");
		String asmName;
		try {
			asmName = generateAsm(file);
		} catch (Exception e) {
			Utils.log("Failed to generate file.");
			e.printStackTrace();
			return;
		}

		// Delete old executable (if exists)
		if (outputName == null) {
			outputName = asmName.split("\\.")[0] + ".out";
		}
		Utils.runCmdAndWait("rm", "-f", outputName);

		// Generate executable
		Utils.log("\n\033[0;32m== Compiling ==\033[0m\n");
		Utils.runCmdAndWait("fasm", asmName, outputName);
		Utils.runCmdAndWait("chmod", "+x", outputName);
		Utils.log("Finished compiling to `" + outputName + "`.");

		if (!run) {
			Utils.log("");
			return;
		}

		// Run executable
		Utils.log("\n\033[0;32m== Running ==\033[0m\n");
		long startTime = System.currentTimeMillis();
		int exitCode = Utils.runCmdAndWait(true, "./" + outputName);
		long endTime = System.currentTimeMillis();

		// Print stats
		Utils.log("\n\033[0;32m== Stats ==\033[0m\n");
		Utils.log("Exit code: " + exitCode);
		Utils.log("Time: ~" + (endTime - startTime) + "ms");
		Utils.log("");

		// Delete
		if (!delete) {
			return;
		}
		Utils.runCmd("rm", "-f", asmName);
		Utils.runCmd("rm", "-f", outputName);
	}
}