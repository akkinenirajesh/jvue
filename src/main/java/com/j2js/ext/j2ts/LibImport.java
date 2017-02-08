package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.Set;

import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.Tuple;
import com.j2js.ts.TSHelper;

public class LibImport implements ExtInvocation<Tuple<String, Set<String>>> {

	@Override
	public void invoke(PrintStream ps, Tuple<String, Set<String>> input, ExtChain ch) {
		ps.print("import ");
		ps.print("{");
		ps.print(TSHelper.combine(input.getR(), ", "));
		ps.print("} from '");
		ps.print(input.getT());
		ps.println("';");
		ch.next(ps, input);
	}

}
