package com.scopelang.fasm;

import com.scopelang.Identifier;
import com.scopelang.ScopeType;
import com.scopelang.Utils;
import com.scopelang.ScopeParser.*;
import com.scopelang.error.ErrorLoc;

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
			name = cb.addInvoke(name, ctx.arguments().expr(),
				cb.modules.locationOf(ctx.start));

			if (name == null) {
				return null;
			} else {
				var returnType = cb.modules.funcGatherer.returnTypeOf(name);

				if (returnType.isVoid()) {
					Utils.error(cb.modules.locationOf(ctx.start),
						"Attempted to get the return value of a void function.",
						"Void functions don't return any value and therefore cannot be used in an expression.");
					cb.errored = true;
					return null;
				}

				return returnType;
			}
		} else if (ctx.fullIdent() != null) {
			// Handle variables and consts
			var errorLoc = cb.modules.locationOf(ctx.fullIdent().start);
			var ident = new Identifier(ctx.fullIdent());
			if (ident.isSimple()) {
				return evalVariable(cb, ident, errorLoc);
			} else {
				return evalConst(cb, ident, errorLoc);
			}
		} else {
			Utils.error("Unhandled atom node.", "This is probably not your fault.");
			cb.errored = true;
			return null;
		}
	}

	private static ScopeType evalVariable(Codeblock cb, Identifier name, ErrorLoc errorLoc) {
		var strName = name.toString();
		if (!cb.varExists(strName)) {
			return evalConst(cb, name, errorLoc);
		}

		cb.varGet(strName);
		return cb.varType(strName);
	}

	private static ScopeType evalConst(Codeblock cb, Identifier name, ErrorLoc errorLoc) {
		var fullIdent = name;
		if (!cb.modules.constGatherer.exists(name)) {
			fullIdent = null;
			for (var namespace : cb.modules.generator.usings) {
				var newIdent = new Identifier(namespace, name);
				if (cb.modules.constGatherer.exists(newIdent)) {
					fullIdent = newIdent;
					break;
				}
			}
		}

		if (fullIdent == null) {
			String strName = name.toString();
			String closest = Utils.closestMatch(strName, cb.allVarNames().stream());

			if (closest != null) {
				Utils.error(errorLoc,
					"Variable or constant with name `" + strName + "` doesn't exist.",
					"Did you mean `" + closest + "`?");
			} else {
				Utils.error(errorLoc,
					"Variable with name `" + strName + "` doesn't exist.",
					"You can define a variable like so:",
					"string " + name + " = \"Test\";");
			}

			cb.errored = true;
			return null;
		}

		cb.add("mov rdi, QWORD [c_" + fullIdent.get() + "]");
		return cb.modules.constGatherer.typeOf(fullIdent);
	}

	private static ScopeType evalLiteral(Codeblock cb, LiteralsContext ctx) {
		var output = LiteralEvaluator.evalLiteral(cb.modules.tokenProcessor, ctx);
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
