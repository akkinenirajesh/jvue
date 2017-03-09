package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ext.Tuple;
import com.j2js.ts.MethodContext;

public class Method implements ExtInvocation<Tuple<String, MethodContext>> {

	@Override
	public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
		ps.println("");
		ExtRegistry.get().invoke("method.name", ps, input);
		ExtRegistry.get().invoke("method.params", ps, input);
		ExtRegistry.get().invoke("method.return", ps, input);
		ExtRegistry.get().invoke("method.body.start", ps, input);
		ExtRegistry.get().invoke("method.body.end", ps, input);
		ch.next(ps, input);
	}

}
