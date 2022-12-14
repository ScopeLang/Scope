package com.scopelang.preprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import com.scopelang.*;
import com.scopelang.ScopeParser.ObjectContext;
import com.scopelang.ScopeParser.ObjectFieldContext;

public class ObjectGatherer extends AbstractGatherer {
	public static class ScopeObject {
		public ArrayList<String> fields = new ArrayList<>();
		public ArrayList<ScopeType> fieldTypes = new ArrayList<>();
	}

	private HashMap<Identifier, ScopeObject> objects = new HashMap<>();
	private Identifier currentObject = null;

	public ObjectGatherer(Modules modules) {
		super(modules);
	}

	@Override
	public void enterObject(ObjectContext ctx) {
		currentObject = new Identifier(namespace, ctx.Identifier().getText());
		objects.put(currentObject, new ScopeObject());
	}

	@Override
	public void enterObjectField(ObjectFieldContext ctx) {
		var object = objects.get(currentObject);
		object.fields.add(ctx.Identifier().getText());
		object.fieldTypes.add(ScopeType.fromTypeNameCtx(modules, ctx.typeName()));
	}

	@Override
	public void exitObject(ObjectContext ctx) {
		currentObject = null;
	}

	public boolean objectExists(Identifier ident) {
		return objects.containsKey(ident);
	}

	public ScopeObject getObjectInfo(Identifier ident) {
		return objects.get(ident);
	}

	public Set<Entry<Identifier, ScopeObject>> getAllValues() {
		return objects.entrySet();
	}

	public void addLibObject(Identifier name, ScopeObject info) {
		objects.put(name, info);
	}
}