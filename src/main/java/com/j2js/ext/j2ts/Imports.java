package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.Set;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class Imports implements ExtInvocation<Set<String>> {

	@Override
	public void invoke(PrintStream ps, Set<String> input, ExtChain ch) {
		input.forEach(i -> ch.invoke("import", ps, i));
		ch.next(ps, input);
	}
}
