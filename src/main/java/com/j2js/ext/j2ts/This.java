package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class This implements ExtInvocation<Object> {

	@Override
	public void invoke(PrintStream ps, Object input, ExtChain ch) {
		ps.print("this");
	}

}
