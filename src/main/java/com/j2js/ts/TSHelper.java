package com.j2js.ts;

import java.util.Collection;
import java.util.Iterator;

public class TSHelper {
	public static String getSimpleName(String fullName) {
		return fullName.substring(fullName.lastIndexOf(".") + 1);
	}

	public static String combine(Collection<String> coll, String separator) {
		StringBuilder b = new StringBuilder();
		Iterator<String> iterator = coll.iterator();
		if (iterator.hasNext()) {
			b.append(iterator.next());
			while (iterator.hasNext()) {
				b.append(separator).append(iterator.next());
			}
		}
		return b.toString();
	}
}
