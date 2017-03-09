package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.Set;
import java.util.stream.Collectors;

import com.j2js.assembly.Project;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class EnumImports implements ExtInvocation<Set<String>> {

	@Override
	public void invoke(PrintStream ps, Set<String> input, ExtChain ch) {
		Project p = Project.getSingleton();
		input = input.stream().filter(t -> !p.isEnum(t)).collect(Collectors.toSet());
		ch.next(ps, input);
	}
}
