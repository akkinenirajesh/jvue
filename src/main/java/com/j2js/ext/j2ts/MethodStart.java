package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.Tuple;
import com.j2js.ts.MethodContext;

public class MethodStart implements ExtInvocation<Tuple<String, MethodContext>> {

	@Override
	public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
		ps.println("");
		if (input.getR().getList().size() == 1) {
			String name = input.getT();
			if (name.equals("constructor") && input.getR().getType().hasSuperClass()
					&& !input.getR().getType().getClassName().contains("$")) {
				ps.println("\t\tsuper();");
			}
		}
		input.getR().getBody().write(ps);
		ch.next(ps, input);
	}

}
