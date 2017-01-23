package com.j2js.ext;

import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;

public interface ExtensionsRegistry {

	void addForClass(Extension<TypeDeclaration> ext);

	void addForMethod(Extension<MethodDeclaration> ext);

	void addForField(Extension<VariableDeclaration> ext);
}
