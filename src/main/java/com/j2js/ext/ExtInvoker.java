package com.j2js.ext;

import java.io.PrintStream;
import java.util.Map;

import com.j2js.assembly.Project;
import com.j2js.ts.J2TSCompiler;
import com.j2js.ts.TypeScriptGenerator;

public class ExtInvoker {

	private Map<String, ExtInvocationList> points;
	private Project project;
	private J2TSCompiler compiler;
	private TypeScriptGenerator generator;

	public ExtInvoker(Map<String, ExtInvocationList> points, Project project, J2TSCompiler compiler,
			TypeScriptGenerator generator) {
		this.points = points;
		this.project = project;
		this.compiler = compiler;
		this.generator = generator;
	}

	public void invoke(String point, PrintStream ps, Object input) {
		ExtInvocationList list = points.get(point);
		if (list == null) {
			return;
		}
		ExtChain ch = new ExtChainImpl(list);
		ch.next(ps, input);
	}

	public class ExtChainImpl implements ExtChain {

		private ExtInvocationList list;
		private int ind;

		public ExtChainImpl(ExtInvocationList list) {
			this.list = list;
		}

		@Override
		public void next(PrintStream ps, Object input) {
			if (ind == list.size()) {
				return;
			}
			ExtInvocation inv = list.get(ind++);
			inv.invoke(ps, input, this);
		}

		@Override
		public Project getProject() {
			return project;
		}

		@Override
		public J2TSCompiler getCompiler() {
			return compiler;
		}

		@Override
		public TypeScriptGenerator getGenerator() {
			return generator;
		}

		@Override
		public void invoke(String point, PrintStream ps, Object input) {
			ExtInvoker.this.invoke(point, ps, input);
		}
	}
}
