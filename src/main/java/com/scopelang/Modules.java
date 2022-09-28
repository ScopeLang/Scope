package com.scopelang;

import com.scopelang.fasm.FasmGenerator;
import com.scopelang.metadata.ImportManager;
import com.scopelang.preprocess.*;

public class Modules {
	public ImportManager importManager;
	public Preprocessor preprocessor;
	public ScopeLexer lexer;
	public TokenProcessor tokenProcessor;
	public ScopeParser parser;
	public FuncGatherer funcGatherer;
	public FasmGenerator generator;
}
