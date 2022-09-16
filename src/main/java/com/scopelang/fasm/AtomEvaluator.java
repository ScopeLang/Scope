package com.scopelang.fasm;

import org.antlr.v4.runtime.Token;

import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class AtomEvaluator {
	private AtomEvaluator() {
	}

	public static void eval(Codeblock cb, AtomContext ctx) {
		if (ctx.literals() != null) {
			// Handle literals
			evalLiteral(cb, ctx.literals());
		} else if (ctx.Identifier() != null && ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle invoke
			String name = ctx.Identifier().getText();
			cb.addInvoke(name, ctx.arguments().expr());
		} else if (ctx.Identifier() != null) {
			// Handle variables
			String name = ctx.Identifier().getText();
			evalVariable(cb, name, ctx.Identifier().getSymbol());
		} else {
			Utils.error("Unhandled atom node.", "This is probably not your fault.");
			cb.errored = true;
		}
	}

	private static void evalVariable(Codeblock cb, String name, Token symbol) {
		if (!cb.varExists(name)) {
			String closest = Utils.closestMatch(name, cb.varAllNames().stream());

			if (closest != null) {
				Utils.error(cb.generator.locationOf(symbol),
					"Variable with name `" + name + "` doesn't exist.",
					"Did you mean `" + closest + "`?");
			} else {
				Utils.error(cb.generator.locationOf(symbol),
					"Variable with name `" + name + "` doesn't exist.",
					"You can defined a variable like so:",
					"string " + name + " = \"Test\";");
			}

			cb.errored = true;
			return;
		}

		cb.varGet(name);
	}

	private static void evalLiteral(Codeblock cb, LiteralsContext ctx) {
		if (ctx.StringLiteral() != null) {
			// Get the string literal ID from the token process
			String str = ctx.StringLiteral().getText();
			int index = cb.generator.tokenProcessor.extactedStrings.get(str);

			String name = "s_" + cb.generator.md5 + "_" + index;
			cb.add("lea rdi, [" + name + "]");
			cb.add("mov rsi, " + name + ".size");
		} else if (ctx.IntLiteral() != null) {
			String strValue = ctx.IntLiteral().getText();

			// Check for overflow
			try {
				Integer.parseInt(strValue);
			} catch (Exception e) {
				Utils.error(cb.generator.locationOf(ctx.start),
					"Integer literal value must be between -2147483648 and 2147483647.",
					"Try using a `long` for bigger values.");
				cb.errored = true;
			}

			cb.add("mov rdi, " + strValue);
			cb.add("mov rsi, 4");
		} else {
			Utils.error("Unhandled literal node.", "This is probably not your fault.");
			cb.errored = true;
		}
	}
}
