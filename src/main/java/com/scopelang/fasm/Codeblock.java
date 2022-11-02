package com.scopelang.fasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

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

	public boolean errored = false;

	public Modules modules;

	private int currentScope = 0;
	private int localVariableNext = 0;
	private HashMap<String, VariableInfo> localVariables = new HashMap<>();

	private int labelNext = 0;
	private Stack<String> labelStack = new Stack<>();

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
			for (var namespace : modules.generator.usings) {
				fullIdent = new Identifier(namespace, ident);
				if (modules.funcGatherer.exists(fullIdent)) {
					break;
				} else {
					fullIdent = null;
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

	public void startReturn() {
		add("pop rax");
		add("mov QWORD [vlist], rax");
		add("pop rax");
		add("mov QWORD [vlist_end], rax");
	}

	public void endReturn() {
		add("ret");
	}

	public boolean varExists(String name) {
		return localVariables.containsKey(name);
	}

	public boolean varExistsOrError(String name, ParserRuleContext ctx) {
		if (!varExists(name)) {
			Utils.error(modules.generator.locationOf(ctx.start),
				"Variable `" + name + "` was not defined yet in this scope.");
			modules.generator.errored = true;
			return false;
		}

		return true;
	}

	public boolean varNotExistsOrError(String name, ParserRuleContext ctx) {
		if (varExists(name)) {
			Utils.error(modules.generator.locationOf(ctx.start),
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
		add("vlist_getptr rdi, " + id);
		add("vlist_getsize rsi, " + id);
	}

	public Set<String> allVarNames() {
		return localVariables.keySet();
	}

	public void appendArgument(String name, String register, ScopeType type) {
		add("vlist_load " + register);
		varCreate(name, type);
	}

	public String pushLabelName() {
		String name = "l" + labelNext++;
		labelStack.push(name);
		return name;
	}

	public String popLabelName() {
		if (labelStack.empty()) {
			Utils.error("Unrecoverable label stack error.",
				"This should go away once above errors are fixed.");
			Utils.forceExit();
			return null;
		}

		return labelStack.pop();
	}

	public void increaseScope() {
		currentScope++;
	}

	public void decreaseScope() {
		currentScope--;
		localVariables.entrySet().removeIf(kv -> kv.getValue().scope > currentScope);
	}

	public ErrorLoc locationOf(Token token) {
		return modules.generator.locationOf(token);
	}

	@Override
	public String toString() {
		// Save old vlist values
		write("\tpush QWORD [vlist_end]");
		write("\tpush QWORD [vlist]");

		// Move the vlist reference frame
		write("\tmov rax, QWORD [vlist_end]");
		write("\tmov QWORD [vlist], rax");
		write("\tadd QWORD [vlist_end], " + localVariableNext * 16);

		// Write the actual code
		for (var instruction : instructions) {
			write(instruction);
		}

		return output;
	}
}