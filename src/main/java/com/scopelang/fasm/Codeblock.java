package com.scopelang.fasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.scopelang.*;
import com.scopelang.ScopeParser.ExprContext;
import com.scopelang.error.ErrorLoc;
import com.scopelang.preprocess.FuncGatherer;

public class Codeblock {
	public static class VariableInfo {
		public int id;
		public ScopeType type;

		public VariableInfo(int id, ScopeType type) {
			this.id = id;
			this.type = type;
		}
	}

	public boolean errored = false;

	public FasmGenerator generator;

	private int localVariableNext = 0;
	private HashMap<String, VariableInfo> localVariables = new HashMap<>();

	private int labelNext = 0;
	private Stack<String> labelStack = new Stack<>();

	private ArrayList<String> instructions = new ArrayList<>();
	private String output = "";

	public int indent = 1;

	public Codeblock(FasmGenerator generator) {
		this.generator = generator;
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

	public void addInvoke(String ident, List<ExprContext> exprs, ErrorLoc loc) {
		// Check for errors
		if (ident.equals("main")) {
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
			return;
		} else if (!FuncGatherer.exists(ident)) {
			String closest = Utils.closestMatch(ident, FuncGatherer.allFuncNames().stream());

			if (closest != null) {
				Utils.error(loc,
					"Function with name `" + ident + "` doesn't exist!",
					"Did you mean `" + closest + "`?");
			} else {
				Utils.error(loc,
					"Function with name `" + ident + "` doesn't exist!",
					"You can define a function like this:",
					"func void " + ident + "() {",
					"\t// Your code here",
					"}");
			}

			errored = true;
			return;
		}

		// Push all of the arguments
		for (int i = 0; i < exprs.size(); i++) {
			ExprEvaluator.eval(this, exprs.get(i));
			add("vlist_set " + localVariableNext++);
			add("push rax");
		}

		// And then move them (to prevent conflicts)
		for (int i = exprs.size() - 1; i >= 0; i--) {
			add("pop " + Utils.ARG_REGS[i]);
		}

		add("call f_" + ident);
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

	public void varCreate(String name, ScopeType type) {
		int id = localVariableNext++;
		localVariables.put(name, new VariableInfo(id, type));
		add("vlist_set " + id);
	}

	public void varAssign(String name) {
		int id = localVariables.get(name).id;
		add("vlist_set " + id);
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
		return labelStack.pop();
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