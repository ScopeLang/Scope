package com.scopelang.fasm;

import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class ExprEvaluator {
	private ExprEvaluator() {
	}

	public static void eval(Codeblock cb, ExprContext ctx) {
		if (ctx.atom() != null) {
			// Handle atoms (variables, literals)
			AtomEvaluator.eval(cb, ctx.atom());
			return;
		} else if (ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle parens
			eval(cb, ctx.expr(0));
			return;
		} else {
			// Handle operators
			if (evalOperator(cb, ctx)) {
				return;
			}
		}

		Utils.error("Unhandled expression node.", "This is probably not your fault.");
		cb.errored = true;
	}

	private static boolean evalOperator(Codeblock cb, ExprContext ctx) {
		if (ctx.Pow() != null) {
			// return Operator.POW;
		} else if (ctx.Mul() != null) {
			// return Operator.MUL;
		} else if (ctx.Div() != null) {
			// return Operator.DIV;
		} else if (ctx.Add() != null) {
			eval(cb, ctx.expr(1));
			cb.add("push rdi, rsi");
			eval(cb, ctx.expr(0));
			cb.add("pop rcx, rdx");
			cb.add("call concat");

			return true;
		} else if (ctx.Sub() != null) {
			// return Operator.SUB;
		} else if (ctx.Mod() != null) {
			// return Operator.MOD;
		}

		return false;
	}
}
