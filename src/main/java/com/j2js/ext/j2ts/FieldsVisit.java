package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.List;

import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.VisitorInput;

public class FieldsVisit implements ExtInvocation<VisitorInput<List<VariableDeclaration>>> {

	@Override
	public void invoke(PrintStream ps, VisitorInput<List<VariableDeclaration>> input, ExtChain ch) {
		input.getInput().stream().filter(m -> !m.getName().startsWith("$SWITCH_TABLE$"))
				.forEach(m -> ch.invoke("field.visit", ps, new VisitorInput<>(m, input.getGenerator())));
		ch.next(ps, input);
	}

}
