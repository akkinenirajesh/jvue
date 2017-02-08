package com.j2js.ext;

import java.io.PrintStream;

public interface ExtInvocation<T> {

	public void invoke(PrintStream ps, T input, ExtChain ch);

}
