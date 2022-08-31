package com.scopelang.fasm;

import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class AtomEvaluator {
	private AtomEvaluator() {
	}

	public static void eval(FasmGenerator g, AtomContext ctx) {
		if (ctx.literals() != null) {
			// Handle literals
			evalLiteral(g, ctx.literals());
		} else if (ctx.Identifier() != null) {
			// Handle variables
			int id = g.localVariables.get(ctx.Identifier().getText());
			g.write("vlist_getptr rdi, " + id);
			g.write("vlist_getsize esi, " + id);
		} else {
			Utils.error("Unhandled atom node.", "This is probably not your fault.");
			g.errored = true;
		}
	}

	private static void evalLiteral(FasmGenerator g, LiteralsContext ctx) {
		if (ctx.StringLiteral() != null) {
			String str = ctx.StringLiteral().getText();
			int index = g.preprocessor.extactedStrings.get(str);

			g.write("lea rdi, [c_" + index + "]");
			g.write("mov rsi, c_" + index + ".size");
		} else {
			Utils.error("Unhandled literal node.", "This is probably not your fault.");
			g.errored = true;
		}
	}
}
