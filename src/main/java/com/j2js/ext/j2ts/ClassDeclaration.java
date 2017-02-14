package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.stream.Collectors;

import com.j2js.J2JSSettings;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ts.TypeContext;

public class ClassDeclaration implements ExtInvocation<TypeContext> {

	@Override
	public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {

		if (!J2JSSettings.singleFile) {
			ExtRegistry.get().invoke("imports", ps,
					input.getImports().stream().map(i -> i.getClassName()).collect(Collectors.toSet()));
		}

		ExtRegistry.get().invoke("class.name", ps, input.getType());

		ExtRegistry.get().invoke("class.extends", ps, input.getType());

		ExtRegistry.get().invoke("class.implements", ps, input.getType());

		ps.println(" {");

		ExtRegistry.get().invoke("class.body", ps, input);

		ps.print("}");

		ch.next(ps, input);
	}

}
