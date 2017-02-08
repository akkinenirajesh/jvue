package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.TSHelper;

public class ImportReplace implements ExtInvocation<String> {

	public static Map<String, String> imports = new HashMap<>();

	@Override
	public void invoke(PrintStream ps, String input, ExtChain ch) {
		String val = imports.get(input);
		if (val != null) {
			ch.next(ps, val);
		} else {
			ch.next(ps, TSHelper.getSimpleName(input));
		}
	}

}
