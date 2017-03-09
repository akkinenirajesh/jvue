package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.lang.reflect.Modifier;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.Tuple;
import com.j2js.ts.MethodContext;

public class MethodName implements ExtInvocation<Tuple<String, MethodContext>> {

	@Override
	public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
		ps.print("\t");
		if (Modifier.isPrivate(input.getR().getAccess())) {
			ps.print("private ");
		}
		if (Modifier.isStatic(input.getR().getAccess())) {
			ps.print("static ");
		}
		ps.print(input.getT());
		ch.next(ps, input);
	}

}
