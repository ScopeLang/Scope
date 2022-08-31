package com.scopelang.fasm;

import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class ExprEvaluator {
	private ExprEvaluator() {
	}

	public static void eval(FasmGenerator g, ExprContext ctx) {
		if (ctx.atom() != null) {
			// Handle atoms (variables, literals)
			AtomEvaluator.eval(g, ctx.atom());
		} else if (ctx.start.getText() == "(" && ctx.stop.getText() == ")") {
			// Handle parens
			eval(g, ctx.expr(0));
		} else {
			Utils.error("Unhandled expression node.", "This is probably not your fault.");
			g.errored = true;
		}
	}

	private static void evalOperator(FasmGenerator g, LiteralsContext ctx) {

	}
}
