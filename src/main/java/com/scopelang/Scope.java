package com.scopelang;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Scope {
	public static void main(String[] args) throws Exception {
		CharStream stream = CharStreams.fromStream(System.in);

		// create a lexer that feeds off of input CharStream
		ScopeLexer lexer = new ScopeLexer(stream);

		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);

		// create a parser that feeds off the tokens buffer
		ScopeParser parser = new ScopeParser(tokens);

		ParseTree tree = parser.init(); // begin parsing at init rule
		System.out.println(tree.toStringTree(parser)); // print LISP-style tree
	}
}
