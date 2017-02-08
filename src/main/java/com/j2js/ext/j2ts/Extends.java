package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.TSHelper;

public class Extends implements ExtInvocation<String> {

	@Override
	public void invoke(PrintStream ps, String input, ExtChain ch) {
		ps.print(" extends ");
		ps.print(TSHelper.getSimpleName(input));
		ch.next(ps, input);
	}

}
