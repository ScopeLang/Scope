package com.scopelang;

import com.scopelang.ScopeParser.TypeNameContext;

public class ScopeType {
	public static final ScopeType VOID = new ScopeType("void");
	public static final ScopeType STR = new ScopeType("str");
	public static final ScopeType INT = new ScopeType("int");
	public static final ScopeType DEC = new ScopeType("dec");
	public static final ScopeType BOOL = new ScopeType("bool");

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

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ScopeType)) {
			return false;
		}

		ScopeType other = (ScopeType) obj;
		return name.equals(other.name);
	}

	public static ScopeType fromTypeNameCtx(TypeNameContext ctx) {
		if (ctx.StrType() != null) {
			return ScopeType.STR;
		} else if (ctx.IntType() != null) {
			return ScopeType.INT;
		} else if (ctx.BoolType() != null) {
			return ScopeType.BOOL;
		} else if (ctx.DecType() != null) {
			return ScopeType.DEC;
		}

		return ScopeType.VOID;
	}

	public static ScopeType parseFromString(String str) {
		return new ScopeType(str);
	}
}