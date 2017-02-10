package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.TypeContext.TSPrintStream;

public class ClassFields implements ExtInvocation<TSPrintStream> {

	@Override
	public void invoke(PrintStream ps, TSPrintStream input, ExtChain ch) {
		input.write(ps);
		ch.next(ps, input);
	}

}
