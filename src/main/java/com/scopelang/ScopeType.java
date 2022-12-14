package com.scopelang;

import java.util.ArrayList;
import java.util.Arrays;

import com.scopelang.ScopeParser.TypeNameContext;

public class ScopeType {
	public static final ScopeType VOID = new ScopeType("void");

	public static final ScopeType STR = new ScopeType("str");
	public static final ScopeType INT = new ScopeType("int");
	public static final ScopeType DEC = new ScopeType("dec");
	public static final ScopeType BOOL = new ScopeType("bool");

	public String name;
	public ScopeType[] generics;

	public ScopeType(String name) {
		this.name = name;
		generics = new ScopeType[0];
	}

	public ScopeType(String name, ScopeType[] generics) {
		this.name = name;
		this.generics = generics;
	}

	public boolean isVoid() {
		return name.equals("void");
	}

	@Override
	public String toString() {
		String output = name;

		if (generics != null && generics.length > 0) {
			output += "<";
			for (int i = 0; i < generics.length; i++) {
				if (i != 0) {
					output += ",";
				}
				output += generics[i].toString();
			}
			output += ">";
		}

		return output;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ScopeType)) {
			return false;
		}

		ScopeType other = (ScopeType) obj;
		return name.equals(other.name) && Arrays.equals(generics, other.generics);
	}

	public static ScopeType fromTypeNameCtx(Modules modules, TypeNameContext ctx) {
		if (ctx.primitiveType() != null) {
			return new ScopeType(ctx.primitiveType().getText());
		} else if (ctx.fullIdent() != null) {
			var objName = new Identifier(ctx.fullIdent());

			if (!modules.objectGatherer.objectExists(objName)) {
				Utils.error("Object with name `" + objName + "` doesn't exist.",
					"You can declare an object like so:",
					"object MyObject {",
					"\tint myVar = 0;",
					"}");
				return null;
			}

			return new ScopeType(objName.get());
		} else {
			return new ScopeType("array", new ScopeType[] {
				fromTypeNameCtx(modules, ctx.typeName())
			});
		}
	}

	public static ScopeType parseFromString(String str) {
		int genericsStart = str.indexOf("<");
		if (genericsStart != -1) {
			ArrayList<ScopeType> types = new ArrayList<>();
			String generics = str.substring(genericsStart, str.length() - 1);

			// Split it by comma (without sub-commas) and get subtypes
			int bracketPair = 0;
			int start = 0;
			for (int i = 0; i < generics.length(); i++) {
				if (generics.charAt(i) == '<') {
					bracketPair++;
				} else if (generics.charAt(i) == '>') {
					bracketPair--;
				} else if (generics.charAt(i) == ',' && bracketPair == 0) {
					String subtype = generics.substring(start, i);
					types.add(parseFromString(subtype));
					start = i;
				}
			}

			return new ScopeType(str.substring(0, genericsStart),
				types.toArray(ScopeType[]::new));
		}

		return new ScopeType(str);
	}
}