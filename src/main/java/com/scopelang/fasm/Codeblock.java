package com.scopelang.fasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.scopelang.*;
import com.scopelang.ScopeParser.ExprContext;

public class Codeblock {
	public class VariableInfo {
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

	private ArrayList<String> instructions = new ArrayList<>();
	private String output = "";
	private int indent = 1;

	public Codeblock(FasmGenerator generator) {
		this.generator = generator;
	}

	private void write(String str) {
		if (str.isEmpty()) {
			output += "\n";
			return;
		}

		for (int i = 0; i < indent; i++) {
			output += "\t";
		}

		output += str + "\n";
	}

	public void add(String instruction) {
		instructions.add(instruction);
	}

	public void addInvoke(String ident, List<ExprContext> exprs) {
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

	public ScopeType varGetType(String name) {
		return localVariables.get(name).type;
	}

	public void varGet(String name) {
		int id = localVariables.get(name).id;
		add("vlist_getptr rdi, " + id);
		add("vlist_getsize rsi, " + id);
	}

	public Set<String> varAllNames() {
		return localVariables.keySet();
	}

	public void appendArgument(String name, String register, ScopeType type) {
		add("vlist_load " + register);
		varCreate(name, type);
	}

	@Override
	public String toString() {
		// Save old vlist values
		write("push QWORD [vlist_end]");
		write("push QWORD [vlist]");

		// Move the vlist reference frame
		write("mov rax, QWORD [vlist_end]");
		write("mov QWORD [vlist], rax");
		write("add QWORD [vlist_end], " + localVariableNext * 16);

		// Write the actual code
		for (var instruction : instructions) {
			write(instruction);
		}

		return output;
	}
}