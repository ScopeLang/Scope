package com.scopelang.error;

import java.io.File;
import java.util.BitSet;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import com.scopelang.Utils;

public class ErrorHandler implements ANTLRErrorListener {
	private File file;

	public boolean errored = false;

	public ErrorHandler(File file) {
		this.file = file;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
		String msg, RecognitionException e) {

		var loc = new ErrorLoc(file, line, charPositionInLine);

		if (msg.startsWith("missing ';'")) {
			Utils.error(loc, "Missing `;` at end of statement.",
				"Try adding a `;` at the end of line " + (line - 1) + ".");
		} else if (msg.startsWith("mismatched input '")) {
			var unexpected = msg.substring(18, msg.indexOf("'", 18));
			Utils.error(loc, "Unexpected token `" + unexpected + "`.",
				"`" + unexpected + "` is not supposed to be here. Try removing it.");
		} else {
			Utils.error(loc, "Syntax error: " + msg);
		}

		errored = true;
	}

	@Override
	public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
		BitSet ambigAlts, ATNConfigSet configs) {

		Utils.error("Ambiguity detected. Most likely not your fault.");
	}

	@Override
	public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
		BitSet conflictingAlts, ATNConfigSet configs) {

		Utils.error("SLL conflict. Most likely not your fault.");
	}

	@Override
	public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction,
		ATNConfigSet configs) {

		Utils.error("Context sensitivity. Most likely not your fault.");
	}
}
