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
		} else if (ctx.Identifier() != null && ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle invoke
			String name = ctx.Identifier().getText();
			g.writeInvoke(name, ctx.arguments().expr());
		} else if (ctx.Identifier() != null) {
			// Handle variables
			String name = ctx.Identifier().getText();
			if (!g.localVariables.containsKey(name)) {
				String closest = Utils.closestMatch(name, g.localVariables.keySet().stream());

				if (closest != null) {
					Utils.error(g.locationOf(ctx.Identifier().getSymbol()),
						"Variable with name `" + name + "` doesn't exist.",
						"Did you mean `" + closest + "`?");
				} else {
					Utils.error(g.locationOf(ctx.Identifier().getSymbol()),
						"Variable with name `" + name + "` doesn't exist.",
						"You can defined a variable like so:",
						"string " + name + " = \"Test\";");
				}

				g.errored = true;
				return;
			}

			int id = g.localVariables.get(name);
			g.write("vlist_getptr rdi, " + id);
			g.write("vlist_getsize rsi, " + id);
		} else {
			Utils.error("Unhandled atom node.", "This is probably not your fault.");
			g.errored = true;
		}
	}

	private static void evalLiteral(FasmGenerator g, LiteralsContext ctx) {
		if (ctx.StringLiteral() != null) {
			String str = ctx.StringLiteral().getText();
			int index = g.tokenProcessor.extactedStrings.get(str);

			String name = "s_" + g.md5 + "_" + index;
			g.write("lea rdi, [" + name + "]");
			g.write("mov rsi, " + name + ".size");
		} else {
			Utils.error("Unhandled literal node.", "This is probably not your fault.");
			g.errored = true;
		}
	}
}
