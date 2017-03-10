package com.j2js.ext;

import java.io.PrintStream;

import com.j2js.assembly.Project;
import com.j2js.ts.J2TSCompiler;
import com.j2js.ts.TypeScriptGenerator;

public interface ExtChain {

	public void next(PrintStream ps, Object input);

	public void invoke(String point, PrintStream ps, Object input);

	public Project getProject();

	public J2TSCompiler getCompiler();

	public TypeScriptGenerator getGenerator();

}
