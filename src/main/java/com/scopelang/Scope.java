package com.scopelang;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import com.scopelang.fasm.FasmGenerator;

public class Scope {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: scope <file>");
			return;
		}

		// Lex
		CharStream inputStream = CharStreams.fromFileName(args[0]);
		ScopeLexer lexer = new ScopeLexer(inputStream);

		// Preprocess
		CommonTokenStream stream = new CommonTokenStream(lexer);
		Preprocessor preprocessor = new Preprocessor(stream);

		// Parse
		ScopeParser parser = new ScopeParser(stream);
		ParseTree tree = parser.program();

		// Generate
		FasmGenerator generator = new FasmGenerator(args[0] + ".asm", preprocessor);
		generator.insertHeader();
		ParseTreeWalker.DEFAULT.walk(generator, tree);
		generator.finishGen();
	}
}