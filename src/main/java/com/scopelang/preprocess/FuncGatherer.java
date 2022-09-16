package com.scopelang.preprocess;

import java.util.HashMap;
import java.util.Set;

import com.scopelang.ScopeBaseListener;
import com.scopelang.ScopeType;
import com.scopelang.ScopeParser.*;

public class FuncGatherer extends ScopeBaseListener {
	private static HashMap<String, FuncInfo> functions = new HashMap<>();

	@Override
	public void enterFunction(FunctionContext ctx) {
		String ident = ctx.Identifier().getText();

		// Skip main (since it isn't callable anyways)
		if (ident.equals("main")) {
			return;
		}

		var type = ScopeType.fromTypeNameCtx(ctx.typeName());
		functions.put(ident, new FuncInfo(type));
	}

	public static void addLibFunc(String name, FuncInfo info) {
		functions.put(name, info);
	}

	public static boolean exists(String name) {
		return functions.containsKey(name);
	}

	public static Set<String> allFuncNames() {
		return functions.keySet();
	}
}