package com.j2js.ext;

import java.util.ArrayList;
import java.util.List;

import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;

public class ExtensionsProvider implements ExtensionsRegistry {

	private List<Extension<TypeDeclaration>> clazz = new ArrayList<>();
	private List<Extension<MethodDeclaration>> methods = new ArrayList<>();
	private List<Extension<VariableDeclaration>> fields = new ArrayList<>();

	public List<Extension<TypeDeclaration>> getClassExtensions() {
		return clazz;
	}

	public List<Extension<MethodDeclaration>> getMethodExtensions() {
		return methods;
	}

	public List<Extension<VariableDeclaration>> getFieldExtensions() {
		return fields;
	}

	@Override
	public void addForClass(Extension<TypeDeclaration> ext) {
		clazz.add(ext);
	}

	@Override
	public void addForMethod(Extension<MethodDeclaration> ext) {
		methods.add(ext);
	}

	@Override
	public void addForField(Extension<VariableDeclaration> ext) {
		fields.add(ext);
	}
}
