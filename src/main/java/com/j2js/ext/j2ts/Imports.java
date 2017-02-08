package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.Set;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;

public class Imports implements ExtInvocation<Set<String>> {

	@Override
	public void invoke(PrintStream ps, Set<String> input, ExtChain ch) {
		ExtRegistry r = ExtRegistry.get();
		input.forEach(i -> r.invoke("import", ps, i));
		ch.next(ps, input);
	}
}
