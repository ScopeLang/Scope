package com.scopelang.project;

import java.nio.file.Path;
import java.util.ArrayList;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FilenameUtils;

import com.scopelang.*;
import com.scopelang.error.ErrorHandler;
import com.scopelang.fasm.FasmGenerator;
import com.scopelang.metadata.*;
import com.scopelang.preprocess.*;

import java.io.File;

public class CompileTask {
	public static enum Mode {
		MAIN, IMPORT, LIBRARY
	}

	public File root;
	public File source;
	public Mode mode;

	public File output;

	/**
	 * @param root
	 *            The root directory.
	 * @param source
	 *            The source file relative to the root.
	 * @param mode
	 *            The compile mode.
	 */
	public CompileTask(File root, File source, Mode mode) {
		this.root = root;
		this.source = source;
		this.mode = mode;
		output = convertSourceToCompiled(root, source, mode);
	}

	public Path pathRelativeToRoot(Path path) {
		if (path.getParent() == null) {
			return path;
		}

		Path base = root.toPath();
		return base.relativize(path);
	}

	public void run(ScopeXml xml) {
		File file = new File(root, source.getPath());

		Modules modules = new Modules(this);
		var errorHandler = new ErrorHandler(file);

		// Preprocess
		modules.funcGatherer = new FuncGatherer();
		modules.importManager = new ImportManager(modules);
		modules.preprocessor = new Preprocessor(file);

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Lex
		CharStream inputStream = CharStreams.fromString(modules.preprocessor.get());
		modules.lexer = new ScopeLexer(inputStream);
		modules.lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
		modules.lexer.addErrorListener(errorHandler);

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Token process
		CommonTokenStream stream = new CommonTokenStream(modules.lexer);
		modules.tokenProcessor = new TokenProcessor(file, stream, modules);

		var allImports = modules.importManager.getAll();
		modules.globalImports.addAll(allImports);
		analyzeImports(allImports, modules, xml);

		// Parse
		modules.parser = new ScopeParser(stream);
		modules.parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
		modules.parser.addErrorListener(errorHandler);
		ParseTree tree = modules.parser.program();

		if (errorHandler.errored) {
			Utils.forceExit();
		}

		// Gather info
		ParseTreeWalker.DEFAULT.walk(modules.funcGatherer, tree);

		// Generate
		modules.generator = new FasmGenerator(file, output, modules, mode != Mode.MAIN);
		modules.generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(modules.generator, tree);
		modules.generator.finishGen();

		// Log
		Utils.log("Generated and cached `" +
			pathRelativeToRoot(output.toPath()).toString() + "`.");
	}

	private void analyzeImports(ArrayList<File> imports, Modules modules, ScopeXml xml) {
		for (var file : imports) {
			var asm = convertSourceToCompiled(root, file, Mode.IMPORT);

			if (!asm.exists()) {
				Utils.log(asm.getName() + " doesn't exist. Generating.");
				var relative = pathRelativeToRoot(file.toPath());
				var task = new CompileTask(root, relative.toFile(), Mode.IMPORT);
				task.run(xml);
			}

			// Merge everything

			var analyzer = new FasmAnalyzer(root, asm);

			for (var func : analyzer.functions.entrySet()) {
				modules.funcGatherer.addLibFunc(func.getKey(), func.getValue());
			}

			var newImports = new ArrayList<File>();
			for (var importMeta : analyzer.imports) {
				var relative = pathRelativeToRoot(importMeta.file.toPath()).toFile();
				if (modules.globalImports.contains(relative)) {
					continue;
				}

				newImports.add(relative);
				modules.globalImports.add(relative);
			}
			analyzeImports(newImports, modules, xml);
		}
	}

	public static File convertSourceToCompiled(File root, File source, Mode mode) {
		if (mode == Mode.LIBRARY) {
			String name = FilenameUtils.removeExtension(source.getPath());
			return new File(root, name + ".scopelib");
		} else {
			String ext = mode == Mode.MAIN ? ".scopeasm" : ".scopelib";

			String name = FilenameUtils.removeExtension(source.getPath());
			return new File(new File(root, ".cache"), name + ext);
		}
	}
}