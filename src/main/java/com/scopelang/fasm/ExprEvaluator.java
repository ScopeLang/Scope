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
		new OperatorInfo("==", ScopeType.STR, ScopeType.STR, cb -> {
			cb.add("call streq");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("+", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("add rdi, rsi");
			return ScopeType.INT;
		}),
		new OperatorInfo("-", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("sub rdi, rsi");
			return ScopeType.INT;
		}),
		new OperatorInfo("*", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("imul rdi, rsi");
			return ScopeType.INT;
		}),
		new OperatorInfo("/", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("mov rax, rdi");
			cb.add("cqo");
			cb.add("idiv rsi");
			cb.add("mov rdi, rax");
			return ScopeType.INT;
		}),
		new OperatorInfo("%", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("mov rax, rdi");
			cb.add("cqo");
			cb.add("idiv rsi");
			cb.add("mov rdi, rdx");
			return ScopeType.INT;
		}),
		new OperatorInfo("-n", ScopeType.INT, null, cb -> {
			cb.add("neg rdi");
			return ScopeType.INT;
		}),
		new OperatorInfo("+", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("addsd xmm0, xmm1");
			cb.add("movq rdi, xmm0");
			return ScopeType.DEC;
		}),
		new OperatorInfo("-", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("subsd xmm0, xmm1");
			cb.add("movq rdi, xmm0");
			return ScopeType.DEC;
		}),
		new OperatorInfo("*", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("mulsd xmm0, xmm1");
			cb.add("movq rdi, xmm0");
			return ScopeType.DEC;
		}),
		new OperatorInfo("/", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("divsd xmm0, xmm1");
			cb.add("movq rdi, xmm0");
			return ScopeType.DEC;
		}),
		new OperatorInfo("-n", ScopeType.DEC, null, cb -> {
			cb.add("xorpd xmm0, xmm0");
			cb.add("movq xmm1, rdi");
			cb.add("subsd xmm0, xmm1");
			cb.add("movq rdi, xmm0");
			return ScopeType.DEC;
		}),
		new OperatorInfo("[]", ScopeType.STR, ScopeType.INT, cb -> {
			cb.add("add rdi, rsi");
			cb.add("mov al, BYTE [rdi + 16]");
			cb.add("mov rdi, QWORD [curpkg]");
			cb.add("mov QWORD [rdi], 1");
			cb.add("mov BYTE [rdi + 16], al");
			cb.add("add QWORD [curpkg], 17");
			return ScopeType.STR;
		}),
		new OperatorInfo("==", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rsi");
			cb.add("sete al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("!=", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rsi");
			cb.add("setne al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo(">", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rsi");
			cb.add("setg al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("<", ScopeType.INT, ScopeType.INT, cb -> {
			cb.add("cmp rdi, rsi");
			cb.add("setl al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo(">", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("comisd xmm0, xmm1");
			cb.add("seta al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("<", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("comisd xmm1, xmm0");
			cb.add("seta al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo(">=", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("comisd xmm0, xmm1");
			cb.add("setnb al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("<=", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("comisd xmm1, xmm0");
			cb.add("setnb al");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("==", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("ucomisd xmm0, xmm1");
			cb.add("setnp al");
			cb.add("mov rdx, 0");
			cb.add("cmovne rax, rdx");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("!=", ScopeType.DEC, ScopeType.DEC, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("movq xmm1, rsi");
			cb.add("ucomisd xmm0, xmm1");
			cb.add("setp al");
			cb.add("mov rdx, 1");
			cb.add("cmovne rax, rdx");
			cb.add("movzx rdi, al");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("->", ScopeType.INT, ScopeType.DEC, cb -> {
			cb.add("mov QWORD [fptmp], rdi");
			cb.add("cvtsi2sd xmm0, QWORD [fptmp]");
			cb.add("movq rdi, xmm0");
			return ScopeType.DEC;
		}),
		new OperatorInfo("->", ScopeType.DEC, ScopeType.INT, cb -> {
			cb.add("movq xmm0, rdi");
			cb.add("cvttsd2si rdi, xmm0");
			return ScopeType.INT;
		}),
		new OperatorInfo("&", ScopeType.BOOL, ScopeType.BOOL, cb -> {
			cb.add("and rdi, rsi");
			return ScopeType.BOOL;
		}),
		new OperatorInfo("|", ScopeType.BOOL, ScopeType.BOOL, cb -> {
			cb.add("or rdi, rsi");
			return ScopeType.BOOL;
		}),
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
		} else if (ctx.arrayInit() != null) {
			// Handle array initializers
			var type = ScopeType.fromTypeNameCtx(ctx.arrayInit().typeName());

			if (!type.name.equals("array")) {
				Utils.error("Attempted to initialize array on non-array type.",
					"Make `" + type.toString() + "` and array by adding `[]` at the end.");
				cb.errored = true;
				return null;
			}

			int byteLength = ctx.arrayInit().arguments().expr().size() * 8;
			cb.add("push rcx");
			cb.add("mov rcx, QWORD [curpkg]");
			cb.add("push rcx");
			cb.add("mov QWORD [rcx], " + byteLength);
			cb.add("add rcx, 16");

			for (var expr : ctx.arrayInit().arguments().expr()) {
				var elemType = eval(cb, expr);

				// Check if the type is correct
				if (!elemType.equals(type.generics[0])) {
					Utils.error("Array element does not match array type.",
						"Try changing the element type to a `" + type.generics[0].toString() + "`.");
					cb.errored = true;
					return null;
				}

				cb.add("mov QWORD [rcx], rdi");
				cb.add("add rcx, 8");
			}

			cb.add("add QWORD [curpkg], " + (byteLength + 16));
			cb.add("pop rdi");
			cb.add("pop rcx");

			return type;
		} else if (ctx.objectInit() != null) {
			// Handle object initializers
			var type = ScopeType.fromTypeNameCtx(ctx.objectInit().typeName());

			if (!type.name.equals("array")) {
				Utils.error("Object initializers can only be used on arrays for now.");
				cb.errored = true;
				return null;
			}

			var exprType = eval(cb, ctx.objectInit().arguments().expr(0));

			if (!exprType.equals(ScopeType.INT)) {
				Utils.error("Object initializers can only have one int for now.");
				cb.errored = true;
				return null;
			}

			cb.add("mov rsi, rdi");
			cb.add("mov rdi, QWORD [curpkg]");
			cb.add("imul rsi, 8");
			cb.add("mov QWORD [rdi], rsi");
			cb.add("add rsi, 16");
			cb.add("add QWORD [curpkg], rsi");

			return type;
		} else {
			// Handle operators
			var ret = evalOperator(cb, ctx);
			if (ret != null) {
				return ret;
			}
		}

		if (!cb.errored) {
			Utils.error("Unhandled expression node.", "This is probably not your fault.");
			cb.errored = true;
		}

		return null;
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
		} else if (ctx.GreaterThanEqual() != null) {
			opType = ">=";
		} else if (ctx.LessThanEqual() != null) {
			opType = "<=";
		} else if (ctx.Cast() != null) {
			opType = "->";
		} else if (ctx.And() != null) {
			opType = "&";
		} else if (ctx.Or() != null) {
			opType = "|";
		} else if (ctx.Access() != null) {
			// Temporary
			var left = eval(cb, ctx.expr(0));
			if (left.equals(ScopeType.STR) && ctx.Identifier().getText().equals("length")) {
				cb.add("mov rdi, QWORD [rdi]");
				return ScopeType.INT;
			} else if (left.name.equals("array") && ctx.Identifier().getText().equals("length")) {
				cb.add("mov rdi, QWORD [rdi]");
				cb.add("lea rax, [rdi + 7]");
				cb.add("test rdi, rdi");
				cb.add("cmovs rdi, rax");
				cb.add("sar rdi, 3");
				return ScopeType.INT;
			}

			Utils.error("Can only use `.` with `length` on strings and arrays for now.");
			return null;
		}

		// Get right (if not unary)
		ScopeType right = null;
		if (!opType.equals("-n") && !opType.equals("->")) {
			right = eval(cb, ctx.expr(1));
			cb.add("push rdi");

			// Discard if error
			if (right == null) {
				return null;
			}
		}

		// If cast, get the right type
		if (opType.equals("->")) {
			right = ScopeType.fromTypeNameCtx(ctx.typeName());
		}

		// Get left (only if unary)
		var left = eval(cb, ctx.expr(0));

		// Discard if error
		if (left == null) {
			return null;
		}

		// Pop if not unary
		if (!opType.equals("-n") && !opType.equals("->")) {
			cb.add("pop rsi");
		}

		// Temporary for array
		if (left.name.equals("array") && right.equals(ScopeType.INT)) {
			cb.add("imul rsi, 8");
			cb.add("add rsi, rdi");
			cb.add("mov rdi, QWORD [rsi + 16]");
			return left.generics[0];
		}

		// Use the operator
		var type = useOperator(opType, left, right, cb);
		if (type != null) {
			return type;
		}

		Utils.error(cb.locationOf(ctx.start),
			"No operator `" + opType + "` that has the arguments `" + left + "` and `" + right + "`.");
		cb.errored = true;
		return null;
	}

	public static ScopeType useOperator(String name, ScopeType left, ScopeType right, Codeblock cb) {
		for (var op : operators) {
			if (!op.operator.equals(name)) {
				continue;
			}

			if ((left == null) != (op.left == null) ||
				(right == null) != (op.right == null)) {

				continue;
			}

			if ((left != null && !left.equals(op.left)) ||
				(right != null && !right.equals(op.right))) {

				continue;
			}

			return op.action.action(cb);
		}

		return null;
	}
}