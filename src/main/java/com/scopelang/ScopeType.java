package com.scopelang;

import com.scopelang.ScopeParser.TypeNameContext;

public class ScopeType {
	public String name;

	public ScopeType(String name) {
		this.name = name;
	}

	public boolean isVoid() {
		return name.equals("void");
	}

	@Override
	public String toString() {
		return name;
	}

	public static ScopeType fromTypeNameCtx(TypeNameContext ctx) {
		if (ctx.StringType() != null) {
			return new ScopeType("string");
		} else if (ctx.IntType() != null) {
			return new ScopeType("int");
		}

		return new ScopeType("void");
	}

	public static ScopeType parseFromString(String str) {
		return new ScopeType(str);
	}
}