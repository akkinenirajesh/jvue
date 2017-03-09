package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.dom.TypeDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class ClassExtends implements ExtInvocation<TypeDeclaration> {

	@Override
	public void invoke(PrintStream ps, TypeDeclaration input, ExtChain ch) {
		if (input.hasSuperClass()) {
			ps.print(" extends ");
			ps.print(input.getSuperType().getClassName());
		}

		ch.next(ps, input);
	}

}
