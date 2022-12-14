package com.scopelang.fasm;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;

import com.scopelang.*;
import com.scopelang.ScopeParser.*;
import com.scopelang.project.CompileTask;
import com.scopelang.project.CompileTask.Mode;

public class FasmGenerator extends ScopeBaseListener {
	public FilePair sourceFile;
	private PrintWriter writer;
	private boolean libraryMode;

	private Modules modules;
	public boolean errored = false;
	public String md5 = null;

	private Identifier namespace = null;
	public HashSet<Identifier> usings = new HashSet<>();

	public Codeblock codeblock = null;

	private boolean mainFound = false;
	private String segRead = "";
	private boolean isFuncVoid = false;
	private boolean returnFound = false;

	public FasmGenerator(FilePair sourceFile, File fileName, Modules modules, boolean libraryMode) {
		this.sourceFile = sourceFile;
		this.modules = modules;
		this.libraryMode = libraryMode;

		try {
			writer = new PrintWriter(fileName);
			md5 = Utils.hashOf(sourceFile.toFile());
		} catch (IOException e) {
			Utils.error("Could not generate file.");
			e.printStackTrace();
			Utils.forceExit();
		}
	}

	public void insertHeader() {
		String date = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a").format(LocalDateTime.now());
		String filePath = sourceFile.file.getPath();

		write("; Generated at " + date);
		write("");

		if (libraryMode) {
			write(";@FILE,LIB," + md5 + "," + filePath);
			writeImportMeta();
			write("");
			write(";@SEG_CODE");
			writeObjects();
			return;
		}

		write(";@FILE,ELF64," + md5 + "," + filePath);
		try {
			// Split header
			InputStream in = getClass().getResourceAsStream("files/headers/ELF64.inc");
			String[] file = IOUtils.toString(in, StandardCharsets.UTF_8).split(System.lineSeparator());

			// Remove comments and write
			for (String str : file) {
				int commentIndex = str.indexOf(";");
				if (commentIndex == -1) {
					write(str);
				} else {
					String s = str.substring(0, commentIndex);
					if (s.length() > 0) {
						write(s);
					}
				}
			}

			// Write padding
			write("");
		} catch (IOException e) {
			Utils.error("Could not insert header.");
			e.printStackTrace();
			Utils.forceExit();
		}

		// Keep in mind we return if we are in library mode
		writeObjects();
		writeImports();
	}

	public void finishGen() {
		if (!libraryMode) {
			write("segment readable");
		} else {
			write(";@SEG_READ");
		}
		write("");
		writeStrings();
		writeConsts();

		if (!mainFound && !libraryMode) {
			Utils.error("A `main` function was not found.",
				"Try adding a `main` function like so:",
				"",
				"func void main() {",
				"\tprint(\"Hello, World!\");",
				"}",
				"",
				"If this a library, add the `-l` flag.");
			errored = true;
		} else if (mainFound && libraryMode) {
			Utils.error("A `main` function cannot be declared in library mode.",
				"Try remove the `-l` flag to compile as a normal program.");
			errored = true;
		}

		finish();

		if (errored) {
			Utils.forceExit();
		}
	}

	private void writeImportMeta() {
		for (var filePair : modules.importManager.getAll()) {
			var hash = Utils.hashOf(filePair.toFile());
			write(";@IMPORT," + hash + "," + filePair.file.getPath());
		}
	}

	private void writeImports() {
		for (var source : modules.globalImports) {
			var file = CompileTask.convertSourceToCompiled(source, Mode.IMPORT).toFile();

			String text = Utils.readFile(file);

			// Append to constants
			segRead += text.substring(text.indexOf(";@SEG_READ") + 10, text.length()).trim() + "\n";

			// Get the section of code
			int start = text.indexOf(";@SEG_CODE") + 10;
			int end = text.indexOf(";@SEG_READ");
			text = text.substring(start, end);

			write(text);
		}
	}

	private void writeStrings() {
		if (!libraryMode) {
			write("s_empty dq 0, 0");
		}

		for (var entry : modules.tokenProcessor.extactedStrings.entrySet()) {
			String name = "s_" + md5 + "_" + entry.getValue();
			String str = Utils.processLiteral(entry.getKey());

			String bytes = "";
			var byteArr = str.getBytes(StandardCharsets.UTF_8);
			for (byte b : byteArr) {
				bytes += (int) b + ", ";
			}
			bytes = bytes.substring(0, bytes.length() - 2);

			write(name + " dq " + byteArr.length + ", 0");
			write("\tdb " + bytes);
		}

		write(segRead);
	}

	private void writeConsts() {
		for (var constant : modules.constGatherer.getAllValues()) {
			String name = "c_" + constant.getKey().get();
			write(";@CONST," + constant.getKey().get() + "," +
				constant.getValue().type.toString());
			write(name + " " + constant.getValue().output);
		}
	}

	private void writeObjects() {
		for (var object : modules.objectGatherer.getAllValues()) {
			write(";@OBJ_START," + object.getKey().get());

			for (int i = 0; i < object.getValue().fields.size(); i++) {
				write("\t;@OBJ_FIELD," + object.getValue().fields.get(i) +
					"," + object.getValue().fieldTypes.get(i));
			}

			write(";@OBJ_END");
			write("");
		}
	}

	public void write(String str) {
		if (str.isEmpty()) {
			writer.println();
			return;
		}

		writer.println(str);
	}

	private void finish() {
		writer.flush();
		writer.close();
	}

	@Override
	public void enterCodeblock(CodeblockContext ctx) {
		if (codeblock != null) {
			codeblock.increaseScope();
		}
	}

	@Override
	public void exitCodeblock(CodeblockContext ctx) {
		if (codeblock != null) {
			codeblock.decreaseScope();
		}
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		// Check if main is in a namespace
		if (ctx.Identifier().getText().equals("main") && namespace != null) {
			Utils.error(modules.locationOf(ctx.start),
				"The `main` function cannot be in a namespace.",
				"Remove the `namespace` statement in this file.");
			errored = true;
			return;
		}

		Identifier ident = new Identifier(namespace, ctx.Identifier().getText());
		var returnType = ScopeType.fromTypeNameCtx(modules,
			ctx.typeName());

		// Generate metadata
		String meta = ";@FUNC," + ident.get() + "," + returnType;
		for (var param : ctx.parameters().parameter()) {
			meta += "," + ScopeType.fromTypeNameCtx(modules,
				param.typeName());
		}

		// Write function header
		write(meta);
		write("f_" + ident.get() + ":");
		if (ident.equalsStr("main")) {
			mainFound = true;
			write("\tcall init");
		}

		codeblock = new Codeblock(modules);

		// Error if there are too many parameters
		var params = ctx.parameters().parameter();
		if (params.size() > Utils.ARG_REGS.length) {
			Utils.error(modules.locationOf(ctx.start),
				"A function cannot have more than " + Utils.ARG_REGS.length + " parmeters.",
				"Having more than " + Utils.ARG_REGS.length + " parmeters is unreadable. Consider refactoring.");
			errored = true;
		}

		// Set arguments as local variables
		for (int i = 0; i < params.size(); i++) {
			var param = params.get(i);
			var name = param.Identifier().getText();

			// Check for duplicate params
			if (codeblock.varExists(name)) {
				Utils.error(modules.locationOf(param.Identifier().getSymbol()),
					"Parameter `" + name + "` was already defined in the function contract.",
					"Try to keep parameter names concise and readable.");
				errored = true;
				continue;
			}

			// Set variable
			var type = ScopeType.fromTypeNameCtx(modules,
				param.typeName());
			codeblock.appendArgument(name, Utils.ARG_REGS[i], type);
		}

		isFuncVoid = returnType.isVoid();

		// If it is a void func, returns are implicit
		returnFound = isFuncVoid;
	}

	@Override
	public void exitFunction(FunctionContext ctx) {
		// Handle error
		if (codeblock.errored) {
			errored = true;
			write("\t; This codeblock errored. Skipped write.");
			codeblock = null;
			return;
		}

		// Handle return error
		if (!returnFound) {
			Utils.error(modules.locationOf(ctx.start),
				"A non-void function has no return statement.",
				"Change the function return type to void or add a return statement like so:",
				"ret <my-value-here>;");
			errored = true;
		}

		// Implicitly add return statement (for void only)
		if (ctx.typeName().primitiveType().VoidType() != null) {
			String ident = ctx.Identifier().getText();
			codeblock.startReturn();
			if (ident.equals("main")) {
				codeblock.add("mov rdi, 0");
				codeblock.add("call exit");
			}
			codeblock.endReturn();
		}

		// Write in the codeblock
		write(codeblock.toString());
		codeblock = null;
	}

	@Override
	public void exitDeclare(DeclareContext ctx) {
		String ident = ctx.Identifier().getText();

		// Error if the local variable already exists
		if (!codeblock.varNotExistsOrError(ident, ctx)) {
			return;
		}

		// Get value and type
		var exprType = ExprEvaluator.eval(codeblock, ctx.expr());
		var type = ScopeType.fromTypeNameCtx(modules,
			ctx.typeName());

		// Discard if error
		if (exprType == null) {
			return;
		}

		// Check
		if (!exprType.equals(type)) {
			Utils.error(modules.locationOf(ctx.start),
				"The declaration type (`" + type + "`) and expression (`" + exprType + "`) don't match.",
				"Try changing the declaration type to `" + exprType + "`.");
			errored = true;
			return;
		}

		// Create!
		codeblock.varCreate(ident, type);
	}

	@Override
	public void exitAssign(AssignContext ctx) {
		String ident = ctx.Identifier(0).getText();

		// Error if the local variable doesn't exist
		if (!codeblock.varExistsOrError(ident, ctx)) {
			return;
		}

		if (ctx.Access() != null) {
			String access = ctx.Identifier(1).getText();

			// Get type
			var varType = codeblock.varType(ident);
			if (!modules.objectGatherer.objectExists(new Identifier(varType.name))) {
				Utils.error(modules.locationOf(ctx.start),
					"Object type `" + varType.name + "` has no fields.",
					"Are you accessing the wrong variable?");
				errored = true;
				return;
			}

			var objInfo = modules.objectGatherer
				.getObjectInfo(new Identifier(varType.name));

			// Check field name
			if (!objInfo.fields.contains(access)) {
				Utils.error(modules.locationOf(ctx.start),
					"Object type `" + varType.name + "` has no field named `" + access + "`.",
					"Are you accessing the wrong variable?");
				errored = true;
				return;
			}

			int fieldIndex = objInfo.fields.indexOf(access);
			var fieldType = objInfo.fieldTypes.get(fieldIndex);

			// Get value and type
			var exprType = ExprEvaluator.eval(codeblock, ctx.expr(0));
			codeblock.add("push rdi");

			// Check type
			if (!exprType.equals(fieldType)) {
				Utils.error(modules.locationOf(ctx.start),
					"The field type (`" + fieldType + "`) and expression (`" + exprType + "`) don't match.",
					"Are you assigning to the wrong field?");
				errored = true;
				return;
			}

			// Assign to field
			codeblock.varGet(ident);
			codeblock.add("lea rdi, [rdi + " + (16 + fieldIndex * 8) + "]");
			codeblock.add("pop rsi");
			codeblock.add("mov QWORD [rdi], rsi");
		} else if (ctx.LeftBracket().size() > 0) {
			// Get value and type
			var exprType = ExprEvaluator.eval(codeblock, ctx.expr(ctx.LeftBracket().size()));

			// Get type
			var varType = codeblock.varType(ident);
			for (int i = 0; i < ctx.LeftBracket().size(); i++) {
				if (!varType.name.equals("array") || varType.generics == null) {
					Utils.error(modules.locationOf(ctx.start),
						"You can only use the index arrays for now.",
						"Are you assigning to the wrong variable?");
					errored = true;
					return;
				}

				varType = varType.generics[0];
			}

			// Check type
			if (!varType.equals(exprType)) {
				Utils.error(modules.locationOf(ctx.start),
					"The array type type (`" + varType + "`) and expression (`" + exprType
						+ "`) don't match.",
					"Are you assigning to the wrong array?");
				errored = true;
				return;
			}

			codeblock.add("push rdi");

			for (int i = 0; i < ctx.LeftBracket().size(); i++) {
				// Get index
				var indexType = ExprEvaluator.eval(codeblock, ctx.expr(i));

				// Check index type
				if (!indexType.equals(ScopeType.INT)) {
					Utils.error(modules.locationOf(ctx.start),
						"Indices must always be the type of `int`.",
						"Change the value inside of the brackets to an `int`.");
					errored = true;
					return;
				}

				codeblock.add("push rdi");
			}

			// Put into array

			codeblock.varGet(ident);

			for (int i = 0; i < ctx.LeftBracket().size(); i++) {
				codeblock.add("pop rsi");
				codeblock.add("imul rsi, 8");

				if (i >= ctx.LeftBracket().size() - 1) {
					codeblock.add("lea rsi, [rdi + rsi + 16]");
				} else {
					codeblock.add("mov rdi, QWORD [rdi + rsi + 16]");
				}
			}

			codeblock.add("pop rdi");
			codeblock.add("mov QWORD [rsi], rdi");
		} else {
			// Get value and type
			var exprType = ExprEvaluator.eval(codeblock, ctx.expr(0));

			// Check type
			var varType = codeblock.varType(ident);
			if (!exprType.equals(varType)) {
				Utils.error(modules.locationOf(ctx.start),
					"The variable type (`" + varType + "`) and expression (`" + exprType + "`) don't match.",
					"Are you assigning to the wrong variable?");
				errored = true;
				return;
			}

			// Assign!
			codeblock.varAssign(ident);
		}
	}

	@Override
	public void exitInvoke(InvokeContext ctx) {
		Identifier ident = new Identifier(ctx.fullIdent());
		if (ident.equalsStr("print")) {
			var t = ExprEvaluator.eval(codeblock, ctx.arguments().expr(0));
			if (!t.equals(ScopeType.STR)) {
				Utils.error(modules.locationOf(ctx.start),
					"Function `print` does not have an appropiate parameter list `" + t + "`.",
					"Try changing the parameter types to `str`.");
				errored = true;
				return;
			}
			codeblock.add("call print");
		} else {
			codeblock.addInvoke(ident, ctx.arguments().expr(), modules.locationOf(ctx.start));
		}
	}

	@Override
	public void exitReturn(ReturnContext ctx) {
		if (isFuncVoid && ctx.expr() != null) {
			Utils.error(modules.locationOf(ctx.start),
				"Attemped to return a value in a void function.",
				"Try changing the return type or removing this statement.");
			errored = true;
			return;
		} else if (!isFuncVoid && ctx.expr() == null) {
			Utils.error(modules.locationOf(ctx.start),
				"Attemped to return without a value in a non-void function.",
				"Try adding an expression after `ret`.");
			errored = true;
			return;
		}

		if (!isFuncVoid) {
			returnFound = true;

			ExprEvaluator.eval(codeblock, ctx.expr());
		}

		codeblock.addReturn();
	}

	@Override
	public void enterBreakpoint(BreakpointContext ctx) {
		codeblock.add("int3");
	}

	@Override
	public void enterIf(IfContext ctx) {
		var label = new Codeblock.LabelInfo();
		label.elseOrEndLabel = codeblock.nextLabelName();
		label.endLabel = label.elseOrEndLabel;

		ExprEvaluator.eval(codeblock, ctx.expr());
		codeblock.add("cmp rdi, 0");
		codeblock.add("je ." + label.elseOrEndLabel);
		codeblock.indent++;

		codeblock.pushLabelInfo(label);
	}

	@Override
	public void enterElse(ElseContext ctx) {
		var label = codeblock.peekLabelInfo();
		label.endLabel = codeblock.nextLabelName();

		codeblock.add("jmp ." + label.endLabel);

		codeblock.indent--;
		codeblock.add("." + label.elseOrEndLabel + ":");
		codeblock.indent++;
	}

	@Override
	public void exitIf(IfContext ctx) {
		codeblock.indent--;
		codeblock.add("." + codeblock.popLabelInfo().endLabel + ":");
	}

	@Override
	public void enterWhile(WhileContext ctx) {
		var label = new Codeblock.LabelInfo();
		label.startLabel = codeblock.nextLabelName();
		label.conditionLabel = codeblock.nextLabelName();
		label.breakLabel = codeblock.nextLabelName();
		label.continueLabel = label.conditionLabel;

		codeblock.add("jmp ." + label.conditionLabel);
		codeblock.add("." + label.startLabel + ":");
		codeblock.indent++;

		codeblock.pushLabelInfo(label);
	}

	@Override
	public void exitWhile(WhileContext ctx) {
		var label = codeblock.popLabelInfo();

		codeblock.indent--;
		codeblock.add("." + label.conditionLabel + ":");
		codeblock.indent++;

		ExprEvaluator.eval(codeblock, ctx.expr());
		codeblock.add("cmp rdi, 1");
		codeblock.add("je ." + label.startLabel);
		codeblock.indent--;
		codeblock.add("." + label.breakLabel + ":");
	}

	@Override
	public void enterFor(ForContext ctx) {
		String ident = ctx.Identifier().getText();
		var type = ScopeType.fromTypeNameCtx(modules,
			ctx.typeName());

		// Error if the local variable already exists
		if (!codeblock.varNotExistsOrError(ident, ctx)) {
			return;
		}

		// Get the start
		codeblock.increaseScope();
		codeblock.indent++;
		var startType = ExprEvaluator.eval(codeblock, ctx.expr(0));

		// Check type
		if (!startType.equals(type)) {
			Utils.error(modules.locationOf(ctx.start),
				"The variable type (`" + type + "`) and the range type (`" + startType + "`) don't match.");
			errored = true;
			return;
		}

		var label = new Codeblock.LabelInfo();
		label.startLabel = codeblock.nextLabelName();
		label.conditionLabel = codeblock.nextLabelName();
		label.breakLabel = codeblock.nextLabelName();
		label.continueLabel = codeblock.nextLabelName();

		// Add ASM
		codeblock.varCreate(ident, type);
		codeblock.add("jmp ." + label.conditionLabel);
		codeblock.add("." + label.startLabel + ":");
		codeblock.indent++;

		codeblock.pushLabelInfo(label);
	}

	@Override
	public void exitFor(ForContext ctx) {
		String ident = ctx.Identifier().getText();
		var type = ScopeType.fromTypeNameCtx(modules,
			ctx.typeName());
		var label = codeblock.popLabelInfo();

		codeblock.indent--;
		codeblock.add("." + label.continueLabel + ":");
		codeblock.indent++;

		// Set up the plus
		if (type.equals(ScopeType.INT) && ctx.expr().size() <= 2) {
			// Optimized increment for integers
			codeblock.add("vlist_get rdi, " + codeblock.varId(ident));
			codeblock.add("inc rdi");
		} else {
			codeblock.varGet(ident);
			codeblock.add("push rdi");
			if (ctx.expr().size() >= 3) {
				var stepType = ExprEvaluator.eval(codeblock, ctx.expr(2));
				codeblock.add("pop rsi");

				if (!stepType.equals(type)) {
					Utils.error(modules.locationOf(ctx.start),
						"The variable type (`" + type + "`) and the step type (`" + stepType + "`) don't match.");
					errored = true;
					return;
				}
			} else {
				Utils.error(modules.locationOf(ctx.start),
					"For loops where the type is not an `int` must have a step argument.",
					"A step argument looks like this:",
					"for (int i : 0..10 step 5) {",
					"\t...",
					"}");
				errored = true;
				return;
			}

			// Look for the plus operator
			ScopeType result = ExprEvaluator.useOperator("+", type, type, codeblock);

			// Error
			if (result == null) {
				Utils.error(modules.locationOf(ctx.start),
					"No operator `+` that can be used for a for loop that has arguments `" + type + "` and `" + type
						+ "`.");
				errored = true;
				return;
			}
		}

		codeblock.varAssign(ident);
		codeblock.indent--;
		codeblock.add("." + label.conditionLabel + ":");
		codeblock.indent++;

		// Variable
		var endType = ExprEvaluator.eval(codeblock, ctx.expr(1));
		codeblock.add("push rdi");

		// Expr
		codeblock.varGet(ident);
		codeblock.add("pop rsi");

		// Check end type
		if (!endType.equals(type)) {
			Utils.error(modules.locationOf(ctx.start),
				"The variable type (`" + type + "`) and the range type (`" + endType + "`) don't match.");
			errored = true;
			return;
		}

		// Look for the less than operator
		ScopeType result = ExprEvaluator.useOperator("<", type, type, codeblock);

		// Error
		if (result == null) {
			Utils.error(modules.locationOf(ctx.start),
				"No operator `<` that can be used for a for loop that has arguments `" + type + "` and `" + type
					+ "`.");
			errored = true;
			return;
		}

		codeblock.add("cmp rdi, 1");
		codeblock.add("je ." + label.startLabel);
		codeblock.indent--;

		codeblock.add("." + label.breakLabel + ":");
		codeblock.decreaseScope();
		codeblock.indent--;
	}

	@Override
	public void exitOpAssign(OpAssignContext ctx) {
		String ident = ctx.Identifier().getText();

		// Error if the local variable doesn't exist
		if (!codeblock.varExistsOrError(ident, ctx)) {
			return;
		}

		// Get operator
		String operator = "";
		if (ctx.AddAssign() != null) {
			operator = "+";
		} else if (ctx.SubAssign() != null) {
			operator = "-";
		} else if (ctx.MulAssign() != null) {
			operator = "*";
		} else if (ctx.DivAssign() != null) {
			operator = "/";
		} else if (ctx.ModAssign() != null) {
			operator = "%";
		} else if (ctx.PowAssign() != null) {
			operator = "^";
		}

		// Expr
		var left = ExprEvaluator.eval(codeblock, ctx.expr());
		codeblock.add("push rdi");

		// Variable
		codeblock.varGet(ident);
		var right = codeblock.varType(ident);
		codeblock.add("pop rsi");

		// Look for the operator
		ScopeType result = ExprEvaluator.useOperator(operator, left, right, codeblock);

		// Error
		if (result == null) {
			Utils.error(modules.locationOf(ctx.start),
				"No operator `" + operator + "` that has the arguments `" + left + "` and `" + right + "`.");
			errored = true;
			return;
		}

		// Check type
		var varType = codeblock.varType(ident);
		if (!result.equals(varType)) {
			Utils.error(modules.locationOf(ctx.start),
				"The variable type (`" + varType + "`) and expression (`" + varType + "`) don't match.",
				"Are you op-assigning to the wrong variable?");
			errored = true;
			return;
		}

		// Assign!
		codeblock.varAssign(ident);
	}

	@Override
	public void enterAssembly(AssemblyContext ctx) {
		codeblock.add(";@ASM_START");
	}

	@Override
	public void exitAssembly(AssemblyContext ctx) {
		// Get the asm and remove the start
		String asm = ctx.AssemblyBlock().getText();
		int firstBracket = asm.indexOf("{");
		asm = asm.substring(firstBracket + 1, asm.length() - 1);

		// Clean up asm
		asm = asm.trim().replace("\t", "").replace("\n", "\n\t");

		// Punch in local variable IDs
		for (var localVar : codeblock.allVarNames()) {
			asm = asm.replace("$" + localVar + "$", Integer.toString(codeblock.varId(localVar)));
		}

		// Add it all
		codeblock.add(asm);
		codeblock.add(";@ASM_END");
	}

	@Override
	public void enterNamespace(NamespaceContext ctx) {
		if (namespace != null) {
			Utils.error(modules.locationOf(ctx.start),
				"You can only have 1 namespace per file.",
				"Try removing this namespace statement.");
			errored = true;
			return;
		}

		namespace = new Identifier(ctx.fullIdent());
		usings.add(namespace);
	}

	@Override
	public void enterUsing(UsingContext ctx) {
		var ident = new Identifier(ctx.fullIdent());

		if (usings.contains(ident)) {
			Utils.error(modules.locationOf(ctx.start),
				"The namespace `" + ident.toString() + "` is already being used!",
				"Try removing this using statement.");
			errored = true;
			return;
		}

		usings.add(ident);
	}

	@Override
	public void enterBreak(BreakContext ctx) {
		int size = ctx.BreakKeyword().size();
		if (ctx.ContinueKeyword() != null) {
			size++;
		}

		var label = codeblock.peekLoopLabelInfo(size - 1);

		if (label == null) {
			Utils.error(modules.locationOf(ctx.start),
				"Attempted to use a break statement outside of a loop.",
				"Try removing this break statement or removing a nested break.");
			codeblock.errored = true;
			return;
		}

		if (ctx.ContinueKeyword() != null) {
			codeblock.add("jmp ." + label.continueLabel);
		} else {
			codeblock.add("jmp ." + label.breakLabel);
		}
	}

	@Override
	public void enterContinue(ContinueContext ctx) {
		var label = codeblock.peekLoopLabelInfo();

		if (label == null) {
			Utils.error(modules.locationOf(ctx.start),
				"Attempted to use a continue statement outside of a loop.",
				"Try removing this continue statement.");
			codeblock.errored = true;
			return;
		}

		codeblock.add("jmp ." + label.continueLabel);
	}
}