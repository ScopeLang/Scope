package com.scopelang.fasm;

import org.antlr.v4.runtime.Token;

import com.scopelang.Identifier;
import com.scopelang.ScopeType;
import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class AtomEvaluator {
	private AtomEvaluator() {
	}

	public static ScopeType eval(Codeblock cb, AtomContext ctx) {
		if (ctx.literals() != null) {
			// Handle literals
			return evalLiteral(cb, ctx.literals());
		} else if (ctx.fullIdent() != null && ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle invoke
			var name = new Identifier(ctx.fullIdent());
			name = cb.addInvoke(name, ctx.arguments().expr(), cb.locationOf(ctx.start));
			if (name == null) {
				return null;
			} else {
				var returnType = cb.modules.funcGatherer.returnTypeOf(name);

				if (returnType.isVoid()) {
					Utils.error(cb.locationOf(ctx.start),
						"Attempted to get the return value of a void function.",
						"Void functions don't return any value and therefore cannot be used in an expression.");
					cb.errored = true;
					return null;
				}

				return returnType;
			}
		} else if (ctx.Identifier() != null) {
			// Handle variables
			String name = ctx.Identifier().getText();
			return evalVariable(cb, name, ctx.Identifier().getSymbol());
		} else {
			Utils.error("Unhandled atom node.", "This is probably not your fault.");
			cb.errored = true;
			return null;
		}
	}

	private static ScopeType evalVariable(Codeblock cb, String name, Token symbol) {
		if (!cb.varExists(name)) {
			String closest = Utils.closestMatch(name, cb.allVarNames().stream());

			if (closest != null) {
				Utils.error(cb.locationOf(symbol),
					"Variable with name `" + name + "` doesn't exist.",
					"Did you mean `" + closest + "`?");
			} else {
				Utils.error(cb.locationOf(symbol),
					"Variable with name `" + name + "` doesn't exist.",
					"You can define a variable like so:",
					"string " + name + " = \"Test\";");
			}

			cb.errored = true;
			return null;
		}

		cb.varGet(name);
		return cb.varType(name);
	}

	private static ScopeType evalLiteral(Codeblock cb, LiteralsContext ctx) {
		var output = LiteralEvaluator.evalLiteral(cb, ctx);
		if (output == null) {
			return null;
		}

		if (output.address) {
			cb.add("lea rdi, " + output.output);
		} else {
			cb.add("mov rdi, " + output.output);
		}
		return output.type;
	}
}
