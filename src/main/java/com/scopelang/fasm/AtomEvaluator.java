package com.scopelang.fasm;

import org.antlr.v4.runtime.Token;

import com.scopelang.ScopeType;
import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;
import com.scopelang.preprocess.FuncGatherer;

public final class AtomEvaluator {
	private AtomEvaluator() {
	}

	public static ScopeType eval(Codeblock cb, AtomContext ctx) {
		if (ctx.literals() != null) {
			// Handle literals
			return evalLiteral(cb, ctx.literals());
		} else if (ctx.Identifier() != null && ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle invoke
			String name = ctx.Identifier().getText();
			cb.addInvoke(name, ctx.arguments().expr(), cb.generator.locationOf(ctx.start));
			return FuncGatherer.returnTypeOf(name);
		} else if (ctx.Identifier() != null) {
			// Handle variables
			String name = ctx.Identifier().getText();
			return evalVariable(cb, name, ctx.Identifier().getSymbol());
		} else {
			Utils.error("Unhandled atom node.", "This is probably not your fault.");
			cb.errored = true;
			return ScopeType.VOID;
		}
	}

	private static ScopeType evalVariable(Codeblock cb, String name, Token symbol) {
		if (!cb.varExists(name)) {
			String closest = Utils.closestMatch(name, cb.allVarNames().stream());

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
			return ScopeType.VOID;
		}

		cb.varGet(name);
		return cb.varType(name);
	}

	private static ScopeType evalLiteral(Codeblock cb, LiteralsContext ctx) {
		if (ctx.StringLiteral() != null) {
			// Get the string literal ID from the token process
			String str = ctx.StringLiteral().getText();

			// If empty, return nullptr
			if (str.equals("\"\"")) {
				cb.add("mov rdi, 0");
				cb.add("mov rsi, 0");
				return ScopeType.STRING;
			}

			int index = cb.generator.tokenProcessor.extactedStrings.get(str);

			String name = "s_" + cb.generator.md5 + "_" + index;
			cb.add("lea rdi, [" + name + "]");
			cb.add("mov rsi, " + name + ".size");
			return ScopeType.STRING;
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
			cb.add("mov rsi, 0");
			return ScopeType.INT;
		} else if (ctx.BooleanLiteral() != null) {
			if (ctx.BooleanLiteral().getText().equals("true")) {
				cb.add("mov rdi, 1");
			} else {
				cb.add("mov rdi, 0");
			}
			cb.add("mov rsi, 0");

			return ScopeType.BOOL;
		} else {
			Utils.error("Unhandled literal node.", "This is probably not your fault.");
			cb.errored = true;
			return ScopeType.VOID;
		}
	}
}
