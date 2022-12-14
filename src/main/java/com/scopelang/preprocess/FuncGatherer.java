package com.scopelang.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import com.scopelang.*;
import com.scopelang.ScopeParser.*;

public class FuncGatherer extends AbstractGatherer {
	private HashMap<Identifier, FuncInfo> functions = new HashMap<>();

	public FuncGatherer(Modules modules) {
		super(modules);
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		Identifier ident = new Identifier(namespace, ctx.Identifier().getText());

		// Skip main (since it isn't callable anyways)
		if (ident.equalsStr("main")) {
			return;
		}

		// Get return type
		var type = ScopeType.fromTypeNameCtx(modules,
			ctx.typeName());

		// Get argument types
		ArrayList<ScopeType> args = new ArrayList<>();
		for (var param : ctx.parameters().parameter()) {
			args.add(ScopeType.fromTypeNameCtx(modules,
				param.typeName()));
		}

		// Error if the function already exists
		if (functions.containsKey(ident)) {
			Utils.error("Multiple instances of the function `" + ident + "` were found.",
				"Try removing one of the instances.");
			Utils.forceExit();
			return;
		}

		functions.put(ident, new FuncInfo(type, args.toArray(ScopeType[]::new)));
	}

	public void addLibFunc(Identifier name, FuncInfo info) {
		functions.put(name, info);
	}

	public boolean exists(Identifier name) {
		return functions.containsKey(name);
	}

	public ScopeType returnTypeOf(Identifier name) {
		return functions.get(name).returnType;
	}

	public ScopeType nthArgOf(Identifier name, int n) {
		return functions.get(name).argTypes[n];
	}

	public int numberOfArgs(Identifier name) {
		return functions.get(name).argTypes.length;
	}

	public Stream<Identifier> allFuncNames() {
		return functions.keySet().stream();
	}

	public Stream<String> allFuncNamesStr() {
		return functions.keySet().stream()
			.map(i -> i.get());
	}
}