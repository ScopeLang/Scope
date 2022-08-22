package com.scopelang;

// import org.antlr.v4.runtime.*;
// import org.antlr.v4.runtime.tree.*;

public class Scope {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: scope <file>");
			return;
		}

		FasmGenerator generator = new FasmGenerator(args[0] + ".asm");
		generator.generate();

		// CharStream stream = CharStreams.fromFileName(args[0]);

		// // create a lexer that feeds off of input CharStream
		// ScopeLexer lexer = new ScopeLexer(stream);

		// // create a buffer of tokens pulled from the lexer
		// CommonTokenStream tokens = new CommonTokenStream(lexer);

		// // create a parser that feeds off the tokens buffer
		// ScopeParser parser = new ScopeParser(tokens);

		// ParseTree tree = parser.program(); // begin parsing at init rule
		// System.out.println(tree.toStringTree(parser)); // print LISP-style tree
	}
}