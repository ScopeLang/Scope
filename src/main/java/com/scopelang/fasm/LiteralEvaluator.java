package com.scopelang.fasm;

import com.scopelang.ScopeType;
import com.scopelang.Utils;
import com.scopelang.ScopeParser.LiteralsContext;

public class LiteralEvaluator {
	public static class LiteralOutput {
		public ScopeType type;
		public String output;
		public boolean address;

		public LiteralOutput(ScopeType type, String output, boolean address) {
			this.type = type;
			this.output = output;
			this.address = address;
		}
	}

	public static LiteralOutput evalLiteral(Codeblock cb, LiteralsContext ctx) {
		if (ctx.StringLiteral() != null) {
			// Get the string literal ID from the token process
			String str = ctx.StringLiteral().getText();

			// If empty, return empty string
			if (str.equals("\"\"")) {
				return new LiteralOutput(ScopeType.STR, "[s_empty]", true);
			}

			int index = cb.modules.tokenProcessor.extactedStrings.get(str);

			String name = "s_" + cb.modules.generator.md5 + "_" + index;
			return new LiteralOutput(ScopeType.STR, "[" + name + "]", true);
		} else if (ctx.IntegerLiteral() != null) {
			String strValue = ctx.IntegerLiteral().getText().replaceAll("'", "");

			// Check for overflow
			try {
				Long.parseLong(strValue);
			} catch (Exception e) {
				Utils.error(cb.locationOf(ctx.start),
					"Integer literal value must be between -9,223,372,036,854,775,808 and 9,223,372,036,854,775,807.",
					"Try using a `long` for bigger values.");
				cb.errored = true;
				return null;
			}

			return new LiteralOutput(ScopeType.INT, "QWORD " + strValue, false);
		} else if (ctx.DecimalLiteral() != null) {
			String strValue = ctx.DecimalLiteral().getText();

			// Special cases
			if (strValue.equals("infinity")) {
				return new LiteralOutput(ScopeType.DEC, "QWORD 0x7FF0000000000000", false);
			} else if (strValue.equals("-infinity")) {
				return new LiteralOutput(ScopeType.DEC, "QWORD 0xFFF0000000000000", false);
			} else if (strValue.equals("nan")) {
				return new LiteralOutput(ScopeType.DEC, "QWORD 0xFFF8000000000000", false);
			}

			// Deal with number
			strValue = strValue.replaceAll("'", "");
			if (strValue.startsWith(".")) {
				strValue = "0" + strValue;
			}

			return new LiteralOutput(ScopeType.DEC, "QWORD " + strValue, false);
		} else if (ctx.BooleanLiteral() != null) {
			if (ctx.BooleanLiteral().getText().equals("true")) {
				return new LiteralOutput(ScopeType.BOOL, "QWORD 1", false);
			} else {
				return new LiteralOutput(ScopeType.BOOL, "QWORD 0", false);
			}
		} else {
			Utils.error("Unhandled literal node.", "This is probably not your fault.");
			cb.errored = true;
			return null;
		}
	}
}