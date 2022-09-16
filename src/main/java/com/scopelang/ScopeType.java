package com.scopelang;

import com.scopelang.ScopeParser.TypeNameContext;

public enum ScopeType {
	VOID, STRING, INT;

	public static ScopeType fromTypeNameCtx(TypeNameContext ctx) {
		if (ctx.StringType() != null) {
			return STRING;
		} else if (ctx.IntType() != null) {
			return INT;
		}

		return VOID;
	}
}