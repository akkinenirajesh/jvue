package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.dom.TypeDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class ClassName implements ExtInvocation<TypeDeclaration> {

	@Override
	public void invoke(PrintStream ps, TypeDeclaration input, ExtChain ch) {

		String className = input.getUnQualifiedName();

		ps.print("class ");

		ps.print(className);

		ch.next(ps, input);
	}

}
