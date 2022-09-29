package com.scopelang.preprocess;

import com.scopelang.ScopeType;

public class FuncInfo {
	ScopeType returnType;
	ScopeType[] argTypes;

	public FuncInfo(ScopeType returnType, ScopeType[] argTypes) {
		this.returnType = returnType;
		this.argTypes = argTypes;
	}
}
