package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.List;

import com.j2js.dom.MethodDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ts.VisitorInput;

public class MethodsVisit implements ExtInvocation<VisitorInput<List<MethodDeclaration>>> {

	@Override
	public void invoke(PrintStream ps, VisitorInput<List<MethodDeclaration>> input, ExtChain ch) {
		input.getInput().stream().filter(m -> !m.getMethodBinding().getName().startsWith("$SWITCH_TABLE$")).forEach(
				m -> ExtRegistry.get().invoke("method.visit", ps, new VisitorInput<>(m, input.getGenerator())));
		ch.next(ps, input);
	}

}
