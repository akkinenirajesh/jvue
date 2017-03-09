package com.j2js.ext.j2ts;

import java.io.PrintStream;

import org.apache.bcel.generic.Type;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.Tuple;
import com.j2js.ts.MethodContext;

public class MethodReturn implements ExtInvocation<Tuple<String, MethodContext>> {

	@Override
	public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
		MethodContext c = input.getR();
		if (c.getMethod() != null) {
			Type returnType = c.getMethod().getMethodBinding().getReturnType();
			if (returnType != Type.VOID) {
				ps.print(" :any");
			}
		}

		ch.next(ps, input);

	}

}
