package com.scopelang.fasm;

import com.scopelang.ScopeType;
import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class ExprEvaluator {
	private ExprEvaluator() {
	}

	public static ScopeType eval(Codeblock cb, ExprContext ctx) {
		if (ctx.atom() != null) {
			// Handle atoms (variables, literals)
			return AtomEvaluator.eval(cb, ctx.atom());
		} else if (ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle parens
			return eval(cb, ctx.expr(0));
		} else {
			// Handle operators
			var ret = evalOperator(cb, ctx);
			if (!ret.isVoid()) {
				return ret;
			}
		}

		if (!cb.errored) {
			Utils.error("Unhandled expression node.", "This is probably not your fault.");
			cb.errored = true;
		}

		return ScopeType.VOID;
	}

	private static ScopeType evalOperator(Codeblock cb, ExprContext ctx) {
		if (ctx.Pow() != null) {
			// return Operator.POW;
		} else if (ctx.Mul() != null) {
			// return Operator.MUL;
		} else if (ctx.Div() != null) {
			// return Operator.DIV;
		} else if (ctx.Add() != null) {
			var a = eval(cb, ctx.expr(1));
			if (a.equals(ScopeType.STRING)) {
				cb.add("push rdi, rsi");
				var b = eval(cb, ctx.expr(0));
				cb.add("pop rcx, rdx");
				cb.add("call concat");

				if (!b.equals(ScopeType.STRING)) {
					Utils.error(cb.generator.locationOf(ctx.start),
						"No operator `+` between `" + a + "` and `" + b + "`.");
					cb.errored = true;
					return ScopeType.VOID;
				}
			}

			return ScopeType.STRING;
		} else if (ctx.Sub() != null) {
			// return Operator.SUB;
		} else if (ctx.Mod() != null) {
			// return Operator.MOD;
		}

		return ScopeType.VOID;
	}
}
