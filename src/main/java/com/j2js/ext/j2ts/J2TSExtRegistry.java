package com.j2js.ext.j2ts;

import com.j2js.ext.ExtRegistry;

public class J2TSExtRegistry {

	public static void register() {
		ExtRegistry r = ExtRegistry.get();
		r.add("extends", new Extends());
		r.add("imports", new Imports());
		r.add("import", new ImportReplace());
		r.add("import", new Import());
		r.add("import.lib", new LibImport());
	}
}
