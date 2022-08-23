package com.scopelang;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Scope {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: scope <file>");
			return;
		}

		CharStream stream = CharStreams.fromFileName(args[0]);
		ScopeLexer lexer = new ScopeLexer(stream);
		Preprocessor preprocessor = new Preprocessor(lexer);

		ScopeParser parser = new ScopeParser(preprocessor.build());
		ParseTree tree = parser.program();

		FasmGenerator generator = new FasmGenerator(args[0] + ".asm");
		generator.generate(preprocessor);
	}
}