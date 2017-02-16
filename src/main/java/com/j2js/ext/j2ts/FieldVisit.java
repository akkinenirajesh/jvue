package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.List;

import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ts.VisitorInput;

public class FieldVisit implements ExtInvocation<VisitorInput<List<VariableDeclaration>>> {

	@Override
	public void invoke(PrintStream ps, VisitorInput<List<VariableDeclaration>> input, ExtChain ch) {
		input.getInput()
				.forEach(m -> ExtRegistry.get().invoke("field.visit", ps, new VisitorInput<>(m, input.getGenerator())));
	}

}
