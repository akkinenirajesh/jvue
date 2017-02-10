package com.j2js.ext.j2ts;

import java.io.PrintStream;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class DummyConstructorParams implements ExtInvocation<Integer> {

	@Override
	public void invoke(PrintStream ps, Integer input, ExtChain ch) {
		for (int i = 1; i <= input; i++) {
			if (i != 1) {
				ps.print(", ");
			}
			ps.print("_p" + i);
			ps.print("?:any");
		}
	}

}
