package com.scopelang.fasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;

import com.scopelang.*;
import com.scopelang.ScopeParser.ExprContext;
import com.scopelang.error.ErrorLoc;

public class Codeblock {
	public static class VariableInfo {
		public int id;
		public int scope;
		public ScopeType type;

		public VariableInfo(int id, int scope, ScopeType type) {
			this.id = id;
			this.scope = scope;
			this.type = type;
		}
	}

	public static class LabelInfo {
		public String endLabel;
		public String elseOrEndLabel;

		public String startLabel;
		public String conditionLabel;
		public String breakLabel;
		public String continueLabel;

		public LabelInfo() {

		}
	}

	public boolean errored = false;

	public Modules modules;

	private int currentScope = 0;
	private int localVariableNext = 0;
	private HashMap<String, VariableInfo> localVariables = new HashMap<>();

	private int labelNext = 0;
	private Stack<LabelInfo> labelStack = new Stack<>();

	private ArrayList<String> instructions = new ArrayList<>();
	private String output = "";

	public int indent = 1;

	public Codeblock(Modules modules) {
		this.modules = modules;
	}

	private void write(String str) {
		if (str.isEmpty()) {
			output += "\n";
			return;
		}

		output += str + "\n";
	}

	public void add(String instruction) {
		for (int i = 0; i < indent; i++) {
			instruction = "\t" + instruction;
		}

		instructions.add(instruction);
	}

	public Identifier addInvoke(Identifier ident, List<ExprContext> exprs, ErrorLoc loc) {
		var fullIdent = ident;
		if (!modules.funcGatherer.exists(fullIdent)) {
			fullIdent = null;
			for (var namespace : modules.generator.usings) {
				var newIdent = new Identifier(namespace, ident);
				if (modules.funcGatherer.exists(newIdent)) {
					fullIdent = newIdent;
					break;
				}
			}
		}

		// Check for errors
		if (ident.equalsStr("main")) {
			Utils.error(loc,
				"The `main` function cannot be called manually.",
				"Try moving the contents of main into a different function",
				"and calling that instead like so:",
				"",
				"func void myNewFunc() {",
				"\t// The code that *was* in `main`",
				"}",
				"",
				"func void main() {",
				"\tmyNewFunc();",
				"}");
			errored = true;
			return fullIdent;
		} else if (fullIdent == null) {
			String closest = Utils.closestMatch(ident.get(),
				modules.funcGatherer.allFuncNamesStr());

			if (closest != null) {
				Utils.error(loc,
					"Function with name `" + ident + "` doesn't exist!",
					"Did you mean `" + Identifier.toReadable(closest) + "`?");
			} else {
				Utils.error(loc,
					"Function with name `" + ident + "` doesn't exist!",
					"You can define a function like this:",
					"func void " + ident + "() {",
					"\t// Your code here",
					"}");
			}

			errored = true;
			return fullIdent;
		} else if (modules.funcGatherer.numberOfArgs(fullIdent) != exprs.size()) {
			Utils.error(loc,
				"No function `" + fullIdent + "` with " + exprs.size() + " arguments.",
				"Are you missing arguments?");
			errored = true;
			return fullIdent;
		}

		// Push all of the arguments
		for (int i = 0; i < exprs.size(); i++) {
			var t = ExprEvaluator.eval(this, exprs.get(i));
			add("vlist_set " + localVariableNext++);
			add("push rax");

			var expected = modules.funcGatherer.nthArgOf(fullIdent, i);
			if (!expected.equals(t)) {
				Utils.error(loc, "Argument " + (i + 1) + " does not have the correct type of `" + expected + "`.",
					"Try changing the argument type from `" + t + "` to `" + expected + "`.");
				errored = true;
				return fullIdent;
			}
		}

		// And then move them (to prevent conflicts)
		for (int i = exprs.size() - 1; i >= 0; i--) {
			add("pop " + Utils.ARG_REGS[i]);
		}

		add("call f_" + fullIdent.get());
		return fullIdent;
	}

	public void addReturn() {
		startReturn();
		endReturn();
	}

	public void startReturn() {
		add("freturn");
	}

	public void endReturn() {
		add("ret");
	}

	public boolean varExists(String name) {
		return localVariables.containsKey(name);
	}

	public boolean varExistsOrError(String name, ParserRuleContext ctx) {
		if (!varExists(name)) {
			Utils.error(modules.locationOf(ctx.start),
				"Variable `" + name + "` was not defined yet in this scope.");
			modules.generator.errored = true;
			return false;
		}

		return true;
	}

	public boolean varNotExistsOrError(String name, ParserRuleContext ctx) {
		if (varExists(name)) {
			Utils.error(modules.locationOf(ctx.start),
				"Variable `" + name + "` was already defined in this scope.",
				"Try to keep variable names concise and readable.");
			modules.generator.errored = true;
			return false;
		}

		return true;
	}

	public void varCreate(String name, ScopeType type) {
		int id = localVariableNext++;
		localVariables.put(name, new VariableInfo(id, currentScope, type));
		add("vlist_set " + id);
	}

	public void varAssign(String name) {
		int id = localVariables.get(name).id;
		add("vlist_set " + id);
	}

	public int varId(String name) {
		return localVariables.get(name).id;
	}

	public ScopeType varType(String name) {
		return localVariables.get(name).type;
	}

	public void varGet(String name) {
		int id = localVariables.get(name).id;
		add("vlist_get rdi, " + id);
	}

	public Set<String> allVarNames() {
		return localVariables.keySet();
	}

	public void appendArgument(String name, String register, ScopeType type) {
		add("mov rdi, QWORD [" + register + "]");
		varCreate(name, type);
	}

	public String nextLabelName() {
		return "l" + labelNext++;
	}

	public void pushLabelInfo(LabelInfo labelInfo) {
		labelStack.push(labelInfo);
	}

	public LabelInfo popLabelInfo() {
		if (labelStack.empty()) {
			Utils.error("Unrecoverable label stack error.",
				"This should go away once above errors are fixed.");
			Utils.forceExit();
			return null;
		}

		return labelStack.pop();
	}

	public LabelInfo peekLabelInfo() {
		var l = popLabelInfo();
		pushLabelInfo(l);
		return l;
	}

	public LabelInfo peekLoopLabelInfo() {
		return peekLoopLabelInfo(0);
	}

	public LabelInfo peekLoopLabelInfo(int depth) {
		for (int i = labelStack.size() - 1; i >= 0; i--) {
			if (labelStack.get(i).conditionLabel != null) {
				if (depth <= 0) {
					return labelStack.get(i);
				} else {
					depth--;
				}
			}
		}

		return null;
	}

	public void increaseScope() {
		currentScope++;
	}

	public void decreaseScope() {
		currentScope--;
		localVariables.entrySet().removeIf(kv -> kv.getValue().scope > currentScope);
	}

	@Override
	public String toString() {
		write("\tfstart " + localVariableNext * 8);

		// Write the actual code
		for (var instruction : instructions) {
			write(instruction);
		}

		return output;
	}
}