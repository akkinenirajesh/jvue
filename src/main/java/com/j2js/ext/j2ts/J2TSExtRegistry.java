package com.j2js.ext.j2ts;

import com.j2js.ext.ExtRegistry;

public class J2TSExtRegistry {

	public static void register() {
		ExtRegistry r = ExtRegistry.get();

		r.add("imports", new Imports());
		r.add("import", new ImportReplace());
		r.add("import", new Import());
		r.add("import.lib", new LibImport());

		r.add("method", new MethodDeclaration());
		r.add("method.name", new MethodName());
		r.add("method.params", new MethodParams());
		r.add("method.body.start", new MethodStart());
		r.add("method.body.end", new MethodEnd());

		r.add("class", new ClassDeclaration());
		r.add("class.name", new ClassName());
		r.add("class.extends", new ClassExtends());
		r.add("class.body", new ClassBody());
	}
}
