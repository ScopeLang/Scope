package com.scopelang.fasm;

import com.scopelang.ScopeParser.ExprContext;
import com.scopelang.ScopeType;
import com.scopelang.Utils;

public final class ExprEvaluator {
	public abstract interface IOperatorAction {
		public abstract ScopeType action(Codeblock cb);
	}

	public static class OperatorInfo {
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
		new OperatorInfo("-n", ScopeType.INT, ScopeType.VOID, cb -> {
			cb.add("neg rdi");
			return ScopeType.INT;
		}),
		new OperatorInfo("+", ScopeType.DEC, ScopeType.DEC, cb -> {
			writeFloatingPoint(cb, "fadd");
			return ScopeType.DEC;
		}),
		new OperatorInfo("-", ScopeType.DEC, ScopeType.DEC, cb -> {
			writeFloatingPoint(cb, "fsub");
			return ScopeType.DEC;
		}),
		new OperatorInfo("*", ScopeType.DEC, ScopeType.DEC, cb -> {
			writeFloatingPoint(cb, "fmul");
			return ScopeType.DEC;
		}),
		new OperatorInfo("/", ScopeType.DEC, ScopeType.DEC, cb -> {
			writeFloatingPoint(cb, "fdiv");
			return ScopeType.DEC;
		}),
		new OperatorInfo("-n", ScopeType.DEC, ScopeType.VOID, cb -> {
			cb.add("mov QWORD [fptmp], rdi");
			cb.add("fld QWORD [fptmp]");
			cb.add("fchs");
			cb.add("fstp QWORD [fptmp]");
			cb.add("mov rdi, QWORD [fptmp]");
			return ScopeType.DEC;
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
		}),
		new OperatorInfo(">", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("mov QWORD [fptmp], rdi");
			cb.add("fld QWORD [fptmp]");
			cb.add("mov QWORD [fptmp], rdx");
			cb.add("fld QWORD [fptmp]");
			cb.add("fcomip st1");
			cb.add("fstp QWORD [fptmp]");
			cb.add("mov rdi, QWORD [fptmp]");
			cb.add("setb al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("<", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("mov QWORD [fptmp], rdi");
			cb.add("fld QWORD [fptmp]");
			cb.add("mov QWORD [fptmp], rdx");
			cb.add("fld QWORD [fptmp]");
			cb.add("fcomip st1");
			cb.add("fstp QWORD [fptmp]");
			cb.add("mov rdi, QWORD [fptmp]");
			cb.add("seta al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("->", ScopeType.INT, ScopeType.DEC, cb -> {
			cb.add("mov QWORD [fptmp], rdi");
			cb.add("fild QWORD [fptmp]");
			cb.add("fstp QWORD [fptmp]");
			cb.add("mov rdi, QWORD [fptmp]");
			return ScopeType.DEC;
		}),
		new OperatorInfo("->", ScopeType.DEC, ScopeType.INT, cb -> {
			cb.add("mov QWORD [fptmp], rdi");
			cb.add("fld QWORD [fptmp]");
			cb.add("fistp QWORD [fptmp]");
			cb.add("mov rdi, QWORD [fptmp]");
			return ScopeType.INT;
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
			if (ctx.expr().size() == 2) {
				opType = "-";
			} else {
				opType = "-n";
			}
		} else if (ctx.Equals() != null) {
			opType = "==";
		} else if (ctx.NotEquals() != null) {
			opType = "!=";
		} else if (ctx.GreaterThan() != null) {
			opType = ">";
		} else if (ctx.LessThan() != null) {
			opType = "<";
		} else if (ctx.Cast() != null) {
			opType = "->";
		}

		// Get right (if not unary)
		var right = ScopeType.VOID;
		if (!opType.equals("-n") && !opType.equals("->")) {
			right = eval(cb, ctx.expr(1));
			cb.add("push rdi, rsi");
		}

		// If cast, get the right type
		if (opType.equals("->")) {
			right = ScopeType.fromTypeNameCtx(ctx.typeName());
		}

		// Get left (only if unary)
		var left = eval(cb, ctx.expr(0));

		// Pop if not unary
		if (!opType.equals("-n") && !opType.equals("->")) {
			cb.add("pop rcx, rdx");
		}

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

	private static void writeFloatingPoint(Codeblock cb, String inst) {
		cb.add("mov QWORD [fptmp], rdi");
		cb.add("fld QWORD [fptmp]");
		cb.add("mov QWORD [fptmp], rdx");
		cb.add(inst + " QWORD [fptmp]");
		cb.add("fstp QWORD [fptmp]");
		cb.add("mov rdi, QWORD [fptmp]");
	}
}