package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.TSHelper;

public class Import implements ExtInvocation<String> {

	@Override
	public void invoke(PrintStream ps, String input, ExtChain ch) {
		String simpleName = TSHelper.getSimpleName(input);
		ps.print("import ");
		ps.print("{");
		ps.print(simpleName);
		ps.print("} from './");
		ps.print(input.replaceAll("\\.", "/"));
		ps.println("';");
		ch.next(ps, input);
	}

}
