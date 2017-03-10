package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.TypeContext;

public class ClassDeclaration implements ExtInvocation<TypeContext> {

	@Override
	public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {

		if (!ch.getProject().getSettings().singleFile) {
			ch.invoke("imports", ps, input.getImports());
		}

		ch.invoke("class.name", ps, input.getType());

		ch.invoke("class.extends", ps, input.getType());

		ch.invoke("class.implements", ps, input.getType());

		ps.println(" {");

		ch.invoke("class.body", ps, input);

		ps.print("}");

		ch.next(ps, input);

		if (input.getStaticMethods().containsKey("<clinit>")) {
			ps.println();
			ps.print(input.getType().getUnQualifiedName());
			ps.println(".staticBlock();");
		}
	}

}
