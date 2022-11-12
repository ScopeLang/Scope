package com.scopelang.project;

import java.util.ArrayList;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.io.FilenameUtils;

import com.scopelang.*;
import com.scopelang.FilePair.RootType;
import com.scopelang.error.ErrorHandler;
import com.scopelang.fasm.FasmGenerator;
import com.scopelang.metadata.*;
import com.scopelang.preprocess.*;

import java.io.File;

public class CompileTask {
	public static enum Mode {
		MAIN, IMPORT, LIBRARY
	}

	public FilePair source;
	public FilePair output;
	public Mode mode;

	public CompileTask(FilePair source, Mode mode) {
		this.source = source;
		this.mode = mode;
		output = convertSourceToCompiled(source, mode);
	}

	public void run(ScopeXml xml) {
		File file = source.toFile();

		Modules modules = new Modules(this);
		var errorHandler = new ErrorHandler(file);

		// Preprocess
		modules.funcGatherer = new FuncGatherer();
		modules.importManager = new ImportManager(modules);
		modules.preprocessor = new Preprocessor(file);

		// Add `stdlib:Core` automatically (if stdlib is included)
		if (xml.libraryInfoByName("stdlib") != null) {
			modules.importManager.addRaw("stdlib:Core", xml);
		}

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
		modules.tokenProcessor = new TokenProcessor(file, stream, xml, modules);

		modules.globalImports.addAll(modules.importManager.getAll());
		analyzeImports(modules.importManager.getAll(), modules, xml);

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
		modules.generator = new FasmGenerator(source, output.toFile(),
			modules, mode != Mode.MAIN);
		modules.generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(modules.generator, tree);
		modules.generator.finishGen();

		// Log
		Utils.log("Generated and cached `" + output.toFile().getPath() + "`.");
	}

	private void analyzeImports(ArrayList<FilePair> imports, Modules modules, ScopeXml xml) {
		for (var file : imports) {
			var mode = xml.mode.equals("library") ? Mode.LIBRARY : Mode.IMPORT;

			var asm = convertSourceToCompiled(file, mode);
			boolean regen = false;

			FasmAnalyzer analyzer = null;

			if (!asm.toFile().exists()) {
				Utils.log("`" + asm.toFile().getName() + "` doesn't exist. Generating.");
				regen = true;
			} else {
				// Check for changes
				var md5 = Utils.hashOf(file.toFile());
				analyzer = new FasmAnalyzer(asm);
				if (!md5.equals(analyzer.hash)) {
					// Regen if so
					Utils.log("Changed detected in `" + asm.toFile().getName() +
						"`. Re-generating.");
					analyzer = null;
					regen = true;
				}
			}

			if (regen) {
				var task = new CompileTask(file, mode);
				task.run(xml);
			}

			// Merge everything

			if (analyzer == null) {
				analyzer = new FasmAnalyzer(asm);
			}

			for (var func : analyzer.functions.entrySet()) {
				modules.funcGatherer.addLibFunc(func.getKey(), func.getValue());
			}

			var newImports = new ArrayList<FilePair>();
			for (var importMeta : analyzer.imports) {
				if (modules.globalImports.contains(importMeta.file)) {
					continue;
				}

				newImports.add(importMeta.file);
				modules.globalImports.add(importMeta.file);
			}
			analyzeImports(newImports, modules, xml);
		}
	}

	public static FilePair convertSourceToCompiled(FilePair file, Mode mode) {
		if (file.type == RootType.LIBRARY) {
			mode = Mode.LIBRARY;
		}

		if (mode == Mode.LIBRARY) {
			String name = FilenameUtils.removeExtension(file.file.getPath());
			return new FilePair(file.root, name + ".scopelib", RootType.LIBRARY);
		} else {
			String ext = mode == Mode.MAIN ? ".scopeasm" : ".scopelib";

			String name = FilenameUtils.removeExtension(file.file.getPath());
			return new FilePair(new File(file.root, ".cache"), name + ext, RootType.CACHE);
		}
	}
}