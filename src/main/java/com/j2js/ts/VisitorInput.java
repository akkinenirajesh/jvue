package com.j2js.ts;

public class VisitorInput<T> {

	private T input;
	private TypeScriptGenerator generator;

	public VisitorInput(T input, TypeScriptGenerator generator) {
		this.input = input;
		this.generator = generator;
	}

	public T getInput() {
		return input;
	}

	public TypeScriptGenerator getGenerator() {
		return generator;
	}
}
