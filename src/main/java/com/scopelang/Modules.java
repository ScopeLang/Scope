package com.scopelang;

import java.util.ArrayList;

import org.antlr.v4.runtime.Token;

import com.scopelang.error.ErrorLoc;
import com.scopelang.fasm.FasmGenerator;
import com.scopelang.metadata.ImportManager;
import com.scopelang.preprocess.*;
import com.scopelang.project.CompileTask;

public class Modules {
	public ImportManager importManager;
	public Preprocessor preprocessor;
	public ScopeLexer lexer;
	public TokenProcessor tokenProcessor;
	public ScopeParser parser;
	public FuncGatherer funcGatherer;
	public ConstGatherer constGatherer;
	public FasmGenerator generator;
	public CompileTask task;

	public ArrayList<FilePair> globalImports;

	public Modules(CompileTask task) {
		this.task = task;
		globalImports = new ArrayList<>();
	}

	public ErrorLoc locationOf(Token token) {
		return new ErrorLoc(
			generator.sourceFile.toFile(), token.getLine(),
			token.getCharPositionInLine() + 1);
	}
}
