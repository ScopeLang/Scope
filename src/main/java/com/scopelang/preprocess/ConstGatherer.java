package com.scopelang.preprocess;

import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.scopelang.*;
import com.scopelang.ScopeParser.*;
import com.scopelang.fasm.LiteralEvaluator;
import com.scopelang.fasm.LiteralEvaluator.LiteralOutput;

public class ConstGatherer extends ScopeBaseListener {
	private Modules modules = null;
	private Identifier namespace = null;

	private HashMap<Identifier, ScopeType> constants = new HashMap<>();
	private HashMap<Identifier, LiteralEvaluator.LiteralOutput> constantValues = new HashMap<>();

	public ConstGatherer(Modules modules) {
		this.modules = modules;
	}

	@Override
	public void enterNamespace(NamespaceContext ctx) {
		namespace = new Identifier(ctx.fullIdent());
	}

	@Override
	public void enterConst(ConstContext ctx) {
		Identifier ident = new Identifier(namespace, ctx.Identifier().getText());
		var type = ScopeType.fromTypeNameCtx(ctx.typeName());

		if (constants.containsKey(ident)) {
			Utils.error(modules.locationOf(ctx.start),
				"Multiple instances of the constant `" + ident + "` were found.",
				"Try removing one of the instances.");
			Utils.forceExit();
			return;
		}

		var literal = LiteralEvaluator.evalLiteral(modules.tokenProcessor, ctx.literals());

		// Check type
		if (!type.equals(literal.type)) {
			Utils.error(modules.locationOf(ctx.start),
				"The constant type and value type don't match.",
				"Try changing the constant type to `" + literal.type + "`");
			Utils.forceExit();
			return;
		}

		literal.output = literal.output.replace("QWORD", "dq");
		if (literal.output.startsWith("[")) {
			literal.output = "dq " + literal.output.substring(1, literal.output.length() - 1);
		}

		constantValues.put(ident, literal);
		constants.put(ident, literal.type);
	}

	public boolean exists(Identifier identifier) {
		return constants.containsKey(identifier);
	}

	public ScopeType typeOf(Identifier identifier) {
		return constants.get(identifier);
	}

	public void addLibConst(Identifier identifier, ScopeType scopeType) {
		constants.put(identifier, scopeType);
	}

	public Set<Entry<Identifier, LiteralOutput>> getAllValues() {
		return constantValues.entrySet();
	}
}