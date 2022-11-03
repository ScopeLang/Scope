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
			return cb.modules.funcGatherer.returnTypeOf(name);
		} else if (ctx.typeName() != null && ctx.LeftBracket() != null && ctx.RightBracket() != null) {
			// Handle array init
			ExprEvaluator.eval(cb, ctx.expr());
			cb.add("mov rax, QWORD [curpkg]");
			cb.add("mov QWORD [rax], rdi");
			cb.add("imul rdi, 8");
			cb.add("add rdi, 8");
			cb.add("add QWORD [curpkg], rdi");
			return new ScopeType("array", new ScopeType[] {
				ScopeType.fromTypeNameCtx(ctx.typeName())
			});
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
				Utils.error(cb.locationOf(symbol),
					"Variable with name `" + name + "` doesn't exist.",
					"Did you mean `" + closest + "`?");
			} else {
				Utils.error(cb.locationOf(symbol),
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
				return ScopeType.STR;
			}

			int index = cb.modules.tokenProcessor.extactedStrings.get(str);

			String name = "s_" + cb.modules.generator.md5 + "_" + index;
			cb.add("lea rdi, [" + name + "]");
			cb.add("mov rsi, " + name + ".size");
			return ScopeType.STR;
		} else if (ctx.IntegerLiteral() != null) {
			String strValue = ctx.IntegerLiteral().getText();

			// Check for overflow
			try {
				Long.parseLong(strValue);
			} catch (Exception e) {
				Utils.error(cb.locationOf(ctx.start),
					"Integer literal value must be between -9,223,372,036,854,775,808 and 9,223,372,036,854,775,807.",
					"Try using a `long` for bigger values.");
				cb.errored = true;
			}

			cb.add("mov rdi, QWORD " + strValue);
			cb.add("mov rsi, 0");
			return ScopeType.INT;
		} else if (ctx.DecimalLiteral() != null) {
			String strValue = ctx.DecimalLiteral().getText();
			if (strValue.startsWith(".")) {
				strValue = "0" + strValue;
			}

			cb.add("mov rdi, QWORD " + strValue);
			cb.add("mov rsi, 0");
			return ScopeType.DEC;
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
