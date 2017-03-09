package com.j2js;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class J2JSSettings {

	public static int reductionLevel = 5;

	public static boolean optimize = true;

	public static boolean failOnError = false;

	public static boolean compression = false;

	public static boolean generateLineNumbers;

	public static int compileCount;

	public static int errorCount;

	public static boolean singleFile = false;

	public static String ext = "ts";

	public static Set<String> denyClasses = new HashSet<>();
	public static Set<String> denyPkgs = new HashSet<>();

	public static Predicate<String> allowClass = s -> !denyClasses.contains(s.split("\\$")[0]);

	static {
		addFilter(s -> {
			for (String p : J2JSSettings.denyPkgs) {
				if (s.startsWith(p)) {
					return false;
				}
			}
			return true;
		});
	}

	public static String getSingleEntryPoint() {
		return null;
	}

	public static void addFilter(Predicate<String> filter) {
		allowClass = allowClass.and(filter);
	}
}
