package com.scopelang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.scopelang.error.ErrorHandler;
import com.scopelang.fasm.FasmGenerator;
import com.scopelang.project.ScopeXml;

public final class Scope {
	public static File workingDir = null;
	public static File cacheDir = null;
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
			// Set the working directory from the -d flag
			String dir = cmd.getOptionValue("dir");
			if (dir != null) {
				workingDir = new File(dir);
			} else {
				workingDir = new File(System.getProperty("user.dir"));
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
				return;
			}

			// Read .scope.xml
			projXml = new ScopeXml(xmlFile);

			// Create .cache folder (if it doesn't exist and the mode has one)
			if (projXml.mode.equals("project")) {
				cacheDir = new File(workingDir, ".cache");
				try {
					Files.createDirectories(cacheDir.toPath());
				} catch (IOException e) {
					Utils.error("Unable to create cache folder.");
					if (!Utils.disableLog) {
						e.printStackTrace();
					}
					return;
				}
			}

			String mode = cmd.getArgs()[0];
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
							"Try using `build` instead.");
						return;
					}

					build(projXml.mainFile, true);
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
		genAsm(mainFile, asm, false);

		// Delete old exe (if exists)
		File exe = new File(workingDir, baseName + ".out");
		exe.delete();

		// Convert ASM to executable
		String exeName = exe.getAbsolutePath();
		Utils.log("Compiling executable to `" + exeName + "`.");
		Utils.runCmdAndWait("fasm", asm.getAbsolutePath(), exeName);
		Utils.runCmdAndWait("chmod", "+x", exeName);

		// Run (if asked)
		if (run) {
			int exitCode = Utils.runCmdAndWait(true, exeName);

			// Print warning if the exit code is not zero
			if (exitCode != 0) {
				Utils.log("Compiled program exited with non-zero exit code: " + exitCode);
			}
		}
	}

	private static void buildLibrary(String name) {
		// Compile all files
		for (File f : workingDir.listFiles()) {
			if (f.getName().endsWith(".scope")) {
				String baseName = FilenameUtils.getBaseName(f.getPath());
				genAsm(f, new File(workingDir, baseName + ".scopelib"), true);
			}
		}

		// Package into a zip
		try {
			compressFolder(workingDir.getPath(), new File(workingDir, name + ".zip").getPath());
		} catch (Exception e) {
			Utils.error("Could not package library into a zip.");
			if (!Utils.disableLog) {
				e.printStackTrace();
			}
		}
	}

	// https://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
	private static void compressFolder(String sourceDir, String outputFile) throws IOException {
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile))) {
			compressDirectoryToZipFile((new File(sourceDir)).toURI(), new File(sourceDir), zipOutputStream);
		}
	}

	private static void compressDirectoryToZipFile(URI basePath, File dir, ZipOutputStream out) throws IOException {
		List<File> fileList = Files.list(Paths.get(dir.getAbsolutePath())).map(Path::toFile)
			.collect(Collectors.toList());
		for (File file : fileList) {
			// Skip all non-important files
			var n = file.getName();
			if (!n.endsWith(".scope") && !n.endsWith(".scopelib") && !n.equals(".scope.xml")) {
				continue;
			}

			if (file.isDirectory()) {
				compressDirectoryToZipFile(basePath, file, out);
			} else {
				out.putNextEntry(new ZipEntry(basePath.relativize(file.toURI()).getPath()));
				try (FileInputStream in = new FileInputStream(file)) {
					IOUtils.copy(in, out);
				}
			}
		}
	}
}