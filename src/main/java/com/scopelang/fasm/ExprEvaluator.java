package com.scopelang.fasm;

import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class ExprEvaluator {
	private enum Operator {
		POW, MUL, DIV, ADD, SUB, MOD, NONE
	}

	private ExprEvaluator() {
	}

	public static void eval(FasmGenerator g, ExprContext ctx) {
		if (ctx.atom() != null) {
			// Handle atoms (variables, literals)
			AtomEvaluator.eval(g, ctx.atom());
			return;
		} else if (ctx.LeftParen() != null && ctx.RightParen() != null) {
			// Handle parens
			eval(g, ctx.expr(0));
			return;
		} else {
			var op = getOperator(ctx);
			if (op != Operator.NONE) {
				return;
			}
		}

		Utils.error("Unhandled expression node.", "This is probably not your fault.");
		g.errored = true;
	}

	private static Operator getOperator(ExprContext ctx) {
		if (ctx.Pow() != null) {
			return Operator.POW;
		} else if (ctx.Mul() != null) {
			return Operator.MUL;
		} else if (ctx.Div() != null) {
			return Operator.DIV;
		} else if (ctx.Add() != null) {
			return Operator.ADD;
		} else if (ctx.Sub() != null) {
			return Operator.SUB;
		} else if (ctx.Mod() != null) {
			return Operator.MOD;
		}

		return Operator.NONE;
	}
}
