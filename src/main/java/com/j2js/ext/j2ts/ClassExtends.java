package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.dom.TypeDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.TSHelper;

public class ClassExtends implements ExtInvocation<TypeDeclaration> {

	@Override
	public void invoke(PrintStream ps, TypeDeclaration input, ExtChain ch) {
		if (input.hasSuperClass()) {
			ps.print(" extends ");
			ps.print(TSHelper.getSimpleName(input.getSuperType().getClassName()));
		}

		ch.next(ps, input);
	}

}
