package com.scopelang.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.scopelang.ScopeBaseListener;
import com.scopelang.ScopeType;
import com.scopelang.ScopeParser.*;

public class FuncGatherer extends ScopeBaseListener {
	private HashMap<String, FuncInfo> functions = new HashMap<>();

	@Override
	public void enterFunction(FunctionContext ctx) {
		String ident = ctx.Identifier().getText();

		// Skip main (since it isn't callable anyways)
		if (ident.equals("main")) {
			return;
		}

		// Get return type
		var type = ScopeType.fromTypeNameCtx(ctx.typeName());

		// Get argument types
		ArrayList<ScopeType> args = new ArrayList<>();
		for (var param : ctx.parameters().parameter()) {
			args.add(ScopeType.fromTypeNameCtx(param.typeName()));
		}

		functions.put(ident, new FuncInfo(type, args.toArray(ScopeType[]::new)));
	}

	public void addLibFunc(String name, FuncInfo info) {
		functions.put(name, info);
	}

	public boolean exists(String name) {
		return functions.containsKey(name);
	}

	public ScopeType returnTypeOf(String name) {
		return functions.get(name).returnType;
	}

	public ScopeType nthArgOf(String name, int n) {
		return functions.get(name).argTypes[n];
	}

	public int numberOfArgs(String name) {
		return functions.get(name).argTypes.length;
	}

	public Set<String> allFuncNames() {
		return functions.keySet();
	}

	public void merge(FuncGatherer other) {
		for (var kv : other.functions.entrySet()) {
			if (!functions.containsKey(kv.getKey())) {
				functions.put(kv.getKey(), kv.getValue());
			}
		}
	}
}