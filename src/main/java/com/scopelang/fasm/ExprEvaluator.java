package com.scopelang.fasm;

import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class ExprEvaluator {
	private ExprEvaluator() {
	}

	public static void eval(FasmGenerator g, ExprContext ctx, String ptr, String size) {
		if (ctx.literals() != null) {
			evalLiteral(g, ctx.literals(), ptr, size);
		} else {
			Utils.log("Unhandled expression node.");
		}
	}

	private static void evalLiteral(FasmGenerator g, LiteralsContext ctx, String ptr, String size) {
		if (ctx.StringLiteral() != null) {
			String str = ctx.StringLiteral().getText();
			int index = g.preprocessor.extactedStrings.get(str);

			g.write("movref c_" + index + ", " + ptr + ", " + size);
		}
	}
}
