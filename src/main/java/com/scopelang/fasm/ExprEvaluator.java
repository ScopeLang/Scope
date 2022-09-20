package com.scopelang.fasm;

import com.scopelang.ScopeType;
import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;

public final class ExprEvaluator {
	private abstract interface IOperatorAction {
		public abstract ScopeType action(Codeblock cb);
	}

	private static class OperatorInfo {
		public String operator;
		public ScopeType left;
		public ScopeType right;
		public IOperatorAction action;

		public OperatorInfo(String operator, ScopeType left, ScopeType right, IOperatorAction action) {
			this.operator = operator;
			this.left = left;
			this.right = right;
			this.action = action;
		}
	}

	public static final OperatorInfo[] operators = {
		new OperatorInfo("+", ScopeType.STR, ScopeType.STR, cb -> {
			cb.add("call concat");
			return ScopeType.STR;
		}),
		new OperatorInfo("+", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("add rdi, rdx");
			return ScopeType.INT;
		}),
		new OperatorInfo("-", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("sub rdi, rdx");
			return ScopeType.INT;
		}),
		new OperatorInfo("*", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("imul rdi, rdx");
			return ScopeType.INT;
		}),
		new OperatorInfo("/", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("mov rax, rdi");
			cb.add("mov rcx, rdx");
			cb.add("cqo");
			cb.add("idiv rcx");
			cb.add("mov rdi, rax");
			return ScopeType.INT;
		}),
		new OperatorInfo("%", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("mov rax, rdi");
			cb.add("mov rcx, rdx");
			cb.add("cqo");
			cb.add("idiv rcx");
			cb.add("mov rdi, rdx");
			return ScopeType.INT;
		}),
		new OperatorInfo("[]", ScopeType.STR, ScopeType.INT, cb -> {
			cb.add("add rdx, rdi");
			cb.add("mov al, BYTE [rdx]");
			cb.add("mov rdi, QWORD [curpkg]");
			cb.add("mov BYTE [rdi], al");
			cb.add("mov rsi, 1");
			cb.add("add QWORD [curpkg], 1");
			return ScopeType.STR;
		}),
		new OperatorInfo("==", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rdx");
			cb.add("sete al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("!=", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rdx");
			cb.add("setne al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo(">", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rdx");
			cb.add("setg al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("<", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rdx");
			cb.add("setl al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		})
	};

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
		String opType = null;
		if (ctx.LeftBracket() != null && ctx.RightBracket() != null) {
			opType = "[]";
		} else if (ctx.Pow() != null) {
			opType = "^";
		} else if (ctx.Mul() != null) {
			opType = "*";
		} else if (ctx.Div() != null) {
			opType = "/";
		} else if (ctx.Mod() != null) {
			opType = "%";
		} else if (ctx.Add() != null) {
			opType = "+";
		} else if (ctx.Sub() != null) {
			opType = "-";
		} else if (ctx.Equals() != null) {
			opType = "==";
		} else if (ctx.NotEquals() != null) {
			opType = "!=";
		} else if (ctx.GreaterThan() != null) {
			opType = ">";
		} else if (ctx.LessThan() != null) {
			opType = "<";
		}

		var right = eval(cb, ctx.expr(1));
		cb.add("push rdi, rsi");
		var left = eval(cb, ctx.expr(0));
		cb.add("pop rcx, rdx");

		for (var op : operators) {
			if (!op.operator.equals(opType)) {
				continue;
			}

			if (!left.equals(op.left) || !right.equals(op.right)) {
				continue;
			}

			return op.action.action(cb);
		}

		Utils.error(cb.generator.locationOf(ctx.start),
			"No operator `" + opType + "` that has the arguments `" + left + "` and `" + right + "`.");
		cb.errored = true;
		return ScopeType.VOID;
	}
}