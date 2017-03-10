package com.j2js;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class J2JSSettings {

	public int reductionLevel = 5;

	public boolean optimize = true;

	public boolean failOnError = false;

	public boolean compression = false;

	public boolean generateLineNumbers;

	public int compileCount;

	public int errorCount;

	public boolean singleFile = false;

	private File basedir;

	public String fileName = "out";

	public String ext = "ts";

	public Set<String> denyClasses = new HashSet<>();
	public Set<String> denyPkgs = new HashSet<>();

	public Predicate<String> allowClass = s -> !denyClasses.contains(s.split("\\$")[0]);

	public J2JSSettings() {
		this.basedir = new File(".");
		addFilter(s -> {
			for (String p : denyPkgs) {
				if (s.startsWith(p)) {
					return false;
				}
			}
			return true;
		});
	}

	public void setBasedir(File basedir) {
		if (!basedir.exists()) {
			basedir.mkdirs();
		}
		this.basedir = basedir;
	}

	public File getBasedir() {
		return basedir;
	}

	public String getSingleEntryPoint() {
		return null;
	}

	public void addFilter(Predicate<String> filter) {
		allowClass = allowClass.and(filter);
	}
}
