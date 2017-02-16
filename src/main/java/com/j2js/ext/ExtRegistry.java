package com.j2js.ext;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import com.j2js.ext.j2ts.J2TSExtRegistry;

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

	public <I> void invoke(String point, PrintStream ps, Object input) {
		ExtInvocationList list = points.get(point);
		if (list == null) {
			return;
		}
		ExtChain ch = new ExtChainImpl(list);
		ch.next(ps, input);
	}

	private class ExtChainImpl implements ExtChain {

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
	}

	private class ExtInvocationList extends OrderedList<ExtInvocation<?>> {

	}
}
