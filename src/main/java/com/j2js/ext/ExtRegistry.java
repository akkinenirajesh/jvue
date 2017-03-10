package com.j2js.ext;

import java.util.HashMap;
import java.util.Map;

import com.j2js.assembly.Project;
import com.j2js.ext.j2ts.J2TSExtRegistry;
import com.j2js.ts.J2TSCompiler;
import com.j2js.ts.TypeScriptGenerator;

public class ExtRegistry {

	private static ExtRegistry INS;

	private Map<String, ExtInvocationList> points = new HashMap<>();

	private ExtRegistry() {
	}

	public static ExtRegistry get() {
		if (INS == null) {
			INS = new ExtRegistry();
			J2TSExtRegistry.register();
		}
		return INS;
	}

	public <I> void add(String point, ExtInvocation<?> invoke) {
		add(point, invoke, 0);
	}

	public <I> void add(String point, ExtInvocation<?> invoke, int order) {
		ExtInvocationList list = points.get(point);
		if (list == null) {
			list = new ExtInvocationList();
			points.put(point, list);
		}
		list.add(invoke, order);
	}

	public static ExtInvoker createInvoker(Project project, J2TSCompiler compiler, TypeScriptGenerator generator) {
		return new ExtInvoker(get().points, project, compiler, generator);
	}
}
