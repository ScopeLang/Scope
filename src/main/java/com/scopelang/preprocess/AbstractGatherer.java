package com.scopelang.preprocess;

import com.scopelang.*;
import com.scopelang.ScopeParser.NamespaceContext;

public abstract class AbstractGatherer extends ScopeBaseListener {
	protected Modules modules = null;
	protected Identifier namespace = null;

	public AbstractGatherer(Modules modules) {
		this.modules = modules;
	}

	@Override
	public void enterNamespace(NamespaceContext ctx) {
		namespace = new Identifier(ctx.fullIdent());
	}
}
