package com.j2js.ext;

import java.io.PrintStream;

public interface Extension<T> {

	boolean canProcess(T node);

	void visit(T node, PrintStream out, ExtEvent event);

}
