package com.j2js.ext;

import java.io.PrintStream;

public interface ExtChain {

	public void next(PrintStream ps, Object input);

}
