// Copyright 2011 The j2js Authors. All Rights Reserved.
//
// This file is part of j2js.
//
// j2js is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// j2js is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with j2js. If not, see <http://www.gnu.org/licenses/>.

package com.j2js;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.j2js.assembly.Project;
import com.j2js.ext.ExtensibleGenerator;
import com.j2js.ext.ExtensionsProvider;
import com.j2js.ext.ExtensionsRegistry;
import com.j2js.visitors.Generator;

/**
 * The cross-compiler translates Java class files into JavaScript code and
 * assembles all reachable code into assemblies.
 * <p>
 * For details please refer to the <a href="../../../compile.html">plugin
 * description</a>.
 * 
 * @author Wolfgang Kuehn
 */
public class J2JSCompiler {

	public static int errorCount = 0;

	public Writer writer;

	private File basedir;

	public boolean needCache;

	private File cacheFile;

	List<com.j2js.Assembly> assemblies = new ArrayList<Assembly>();

	private List<File> classpath = new ArrayList<File>();

	public ClassLoader classLoader;

	public FileManager fileManager;

	public boolean optimize = true;

	public boolean failOnError = true;

	private boolean compression = true;

	private String singleEntryPoint;

	private String targetPlatform;

	public int reductionLevel = 5;

	private int junkSizeInKiloBytes = Integer.MAX_VALUE;

	private boolean generateLineNumbers = false;

	public int compileCount = 0;

	public Generator generator;

	private Log logger;

	private ExtensionsProvider provider;

	// Begin main
	public static void main(String argv[]) throws Exception {
		if (argv == null || argv.length != 4) {
			StringBuffer sb = new StringBuffer();
			sb.append("Usage: java ");
			sb.append(J2JSCompiler.class.getName());
			sb.append(" <basedir> <classpathElements> <entryPointClassName> <targetLocation>");
			System.out.print(sb.toString());
			return;
		}

		File basedir = new File(argv[0]);
		String classpathElements = argv[1];
		String entryPointClassName = argv[2];

		J2JSCompiler compiler = new J2JSCompiler();
		compiler.setBasedir(basedir);
		compiler.addClasspathElements(classpathElements);

		Assembly assembly = new Assembly(compiler);
		assembly.setEntryPointClassName(entryPointClassName);
		assembly.setTargetLocation(new File(basedir, argv[3]));
		compiler.addAssembly(assembly);
		compiler.setGenerateLineNumbers(true);
		compiler.execute();
	}
	// End main

	/**
	 * Create new compiler with the current directory as basedir.
	 */
	public J2JSCompiler() {
		setBasedir(new File(System.getProperty("user.dir")));
		setTargetPlatform("javascript");
		addClasspathElements("j2js-libs/j2js-jre.jar;j2js-libs/j2js-agent.jar");
		setBasedir(new File("."));
		provider = new ExtensionsProvider();
	}

	public ExtensionsRegistry getExtensionsRegistry() {
		return provider;
	}

	public void execute() throws Exception {
		if (logger == null) {
			setLogger(new Log());
		}

		Project.clearSingleton();

		for (Assembly assembly : assemblies) {
			execute(assembly);
		}
	}

	private boolean isMavenExecution() {
		return System.getProperty("localRepository") != null;
	}

	public void execute(Assembly assembly) throws Exception {
		logger.info("Entry point is " + assembly.getEntryPointClassName() + "#main(java.lang.String[])void");

		if (classpath == null) {
			throw new RuntimeException("Field classPath must be set");
		}

		if (assembly.getEntryPointClassName() == null) {
			throw new RuntimeException("Field assembly.entryPointClassName must be set");
		}

		if (cacheFile == null && needCache) {
			setCacheFile(new File(basedir, "target/j2js.cache"));
		}

		// if (assembly.getTargetLocation() == null) {
		// throw new RuntimeException("Field assembly.targetLocation must be
		// set");
		// assembly.setTargetLocation(new File(basedir,
		// "target/classes/assemblies/" +
		// assembly.getEntryPointClassName().replaceAll("\\.", "/")));
		// }

		logger.info("Creating assembly " + assembly.getTargetLocation());

		fileManager = new FileManager(classpath, classLoader);
		Project project = Project.createSingleton(getCacheFile(), this);
		assembly.setProject(project);
		generator = createGenerator(project);

		errorCount = 0;

		assembly.addEntryPoint(assembly.getEntryPointClassName() + "#main(java.lang.String[])void");

		for (String memberSignature : assembly.entryPoints) {
			assembly.taint(memberSignature);
		}

		long startTime = System.currentTimeMillis();

		// Used by the JavaScript JVM. The static code analyser would miss
		// these.

		String[] signatures = Utils.getProperty("j2js.preTaintedSignatures").split(";");
		for (int i = 0; i < signatures.length; i++) {
			assembly.taint(signatures[i]);
		}

		if (getSingleEntryPoint() != null) {
			assembly.processSingle(project.getSignature(getSingleEntryPoint()));
		} else {
			assembly.processTainted();
		}

		int methodCount;
		try {
			methodCount = assembly.createAssembly();

			if (getCacheFile() != null) {
				Project.write(this);
			}
		} catch (IOException e) {
			throw new Exception("Error while creating assembly", e);
		}

		logger.info(timesName("Compiled|Compiled", compileCount, "class|classes") + ", "
				+ timesName("packed|packed", methodCount, "method|methods") + ".");
		logger.info("Execution time was " + (System.currentTimeMillis() - startTime) + " millis.");

		if (errorCount > 0) {
			logger.error("There " + timesName("was|were", errorCount, "error|errors") + ".");
		}
	}

	protected Generator createGenerator(Project project) {
		return new ExtensibleGenerator(provider, project, this);
	}

	private String timesName(String verb, int count, String noun) {
		String[] verbs = verb.split("\\|");
		String[] nouns = noun.split("\\|");
		int index = (count == 1 ? 0 : 1);
		return verbs[index] + " " + count + " " + nouns[index];
	}

	public void setCompression(boolean isCompression) {
		this.compression = isCompression;
	}

	public boolean isCompression() {
		return compression;
	}

	/**
	 * For debugging. Internal use only.
	 */
	public void setSingleEntryPoint(String signature) {
		singleEntryPoint = signature;
	}

	/**
	 * @see #setSingleEntryPoint(String)
	 */
	public String getSingleEntryPoint() {
		return singleEntryPoint;
	}

	/**
	 * Sets one of the target platforms "web" or "javascript".
	 * 
	 * @param targetPlatform
	 *            optional; default is "web"
	 */
	public void setTargetPlatform(String targetPlatform) {
		targetPlatform = targetPlatform.toLowerCase();
		if ("web".equals(targetPlatform) || "javascript".equals(targetPlatform)) {
			this.targetPlatform = targetPlatform;
		} else {
			throw new IllegalArgumentException("Target platform must be web or javascript");
		}
	}

	/**
	 * @see #setTargetPlatform(String)
	 */
	public String getTargetPlatform() {
		return targetPlatform;
	}

	public List<File> getClasspath() {
		return classpath;
	}

	/**
	 * @param classpathElements
	 *            (optional) additional class path elements
	 */
	public void addClasspathElements(List<File> classpathElements) {
		classpath.addAll(classpathElements);
	}

	/**
	 * @param classpathElement
	 *            (optional) additional class path element
	 */
	public void addClasspathElement(File classpathElement) {
		classpath.add(classpathElement);
	}

	/**
	 * Semicolon- or whitespace-separated list of class path elements.
	 * 
	 * @param classPathElements
	 *            (optional) additional class path elements
	 * @see #setClasspathElements(List)
	 */
	public void addClasspathElements(String classPathElements) {
		String[] array = classPathElements.split("(;|,)");
		for (String path : array) {
			path = path.trim();
			if (path.length() > 0) {
				addClasspathElement(Utils.resolve(basedir, path));
			}
		}
	}

	public void setClasspathElements(List<String> classpathElements) {
		for (Object part : classpathElements) {
			addClasspathElements((String) part);
		}
	}

	public void setFailOnError(boolean flag) {
		failOnError = flag;
	}

	public boolean isFailOnError() {
		return failOnError;
	}

	public File getCacheFile() {
		return cacheFile;
	}

	public void setCacheFile(File theCacheFile) {
		cacheFile = theCacheFile;
	}

	public List<com.j2js.Assembly> getAssemblies() {
		return assemblies;
	}

	public void setAssemlies(List<com.j2js.Assembly> assemblies) {
		this.assemblies = assemblies;
	}

	public void setGenerateLineNumbers(boolean theGenerateLineNumbers) {
		generateLineNumbers = theGenerateLineNumbers;
	}

	public boolean isGenerateLineNumbers() {
		return generateLineNumbers;
	}

	public void setJunkSizeInKiloBytes(int junkSizeInKiloBytes) {
		if (junkSizeInKiloBytes < 1) {
			throw new RuntimeException("Junk size must be greater than zero.");
		}
		this.junkSizeInKiloBytes = junkSizeInKiloBytes;
	}

	public int getJunkSizeInKiloBytes() {
		return junkSizeInKiloBytes;
	}

	/**
	 * @return Returns the logger.
	 */
	public Log getLogger() {
		return logger;
	}

	/**
	 * Sets the logger.
	 */
	public void setLogger(Log logger) {
		this.logger = logger;
		Log.logger = logger;
	}

	public void setBasedir(File basedir) {
		this.basedir = basedir;
	}

	public File getBasedir() {
		return basedir;
	}

	public void addAssembly(Assembly assembly) {
		assemblies.add(assembly);
	}

}
