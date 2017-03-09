package com.j2js.util;

public class TypeUtils {

	public static String extractClassName(String cls) {
		if (cls.startsWith("[")) {
			return extractClassName(cls.substring(1, cls.length()));
		}
		if (cls.endsWith(";")) {
			return extractClassName(cls.substring(1, cls.length() - 1));
		}
		if (cls.startsWith("L")) {
			return extractClassName(cls.substring(1, cls.length()));
		}
		if (cls.endsWith("[]")) {
			return extractClassName(cls.substring(0, cls.length() - 2));
		}
		return cls;
	}

}
