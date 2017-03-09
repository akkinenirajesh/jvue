package com.j2js.ts;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import com.j2js.FileManager;
import com.j2js.J2JSSettings;
import com.j2js.Log;
import com.j2js.Utils;
import com.j2js.assembly.ClassUnit;
import com.j2js.assembly.MemberUnit;
import com.j2js.assembly.Project;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.ext.ExtRegistry;

public class J2TSCompiler {

	private List<String> classes = new ArrayList<>();

	private FileManager fileManager;

	private ClassLoader classLoader;

	private List<File> classpath = new ArrayList<>();

	private Map<String, String> fieldAccessors = new HashMap<>();

	private Map<String, Object> attr = new HashMap<>();

	private File basedir;

	public J2TSCompiler() {
		this.classLoader = getClass().getClassLoader();
		this.basedir = new File(".");
		Log.logger = new Log();
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

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	public void addClass(String cls, boolean process) {
		if (!this.classes.contains(cls)) {
			this.classes.add(cls);
		}
		if (process) {
			Project project = Project.getSingleton();
			process(project, (TypeScriptGenerator) project.generator, cls);
		}

	}

	public void addClass(String cls) {
		this.addClass(cls, false);
	}

	public void execute() throws Exception {
		this.fileManager = new FileManager(classpath, classLoader);
		Project.clearSingleton();
		Project project = Project.createSingleton(null);
		TypeScriptGenerator visitor = new TypeScriptGenerator(this);
		project.generator = visitor;
		project.fileManager = fileManager;
		while (!classes.isEmpty()) {
			String cls = classes.remove(0);
			process(project, visitor, cls);
		}
		visitor.writeToFile();
	}

	private void process(Project project, TypeScriptGenerator visitor, String cls) {
		String[] split = cls.split("#");
		boolean isPartial = false;
		if (split.length > 1) {
			isPartial = true;
		}
		String fqn = split[0];

		if (!J2JSSettings.allowClass.test(fqn)) {
			return;
		}
		ClassUnit unit = project.getOrCreateClassUnit(fqn);
		unit.setPartial(isPartial);
		resolve(unit, visitor);
		if (isPartial) {
			MemberUnit mu = unit.getDeclaredMember(split[1]);
			if (mu != null) {
				resolve(mu, visitor);
			}
		}

	}

	private void resolve(MemberUnit mu, TypeScriptGenerator visitor) {
		if (mu.isResolved())
			return;
		mu.setResolved(true);
		try {
			if (mu.getDeclaringClass().typeDecl == null) {
				System.err.println("Def not found: " + mu);
				return;
			}
			visitor.setStream(mu.getDeclaringClass().typeDecl);
			visitor.incDepth();
			ExtRegistry.get().invoke("method.visit", null, new VisitorInput<MethodDeclaration>(
					mu.getDeclaringClass().typeDecl.getMethodBySignature(mu.toString()), visitor));
		} finally {
			visitor.decDepth();
		}
	}

	private void resolve(ClassUnit clazz, TypeScriptGenerator visitor) {
		if (clazz.isResolved())
			return;

		if (clazz.getName().startsWith("[")) {
			// This is an array type and not a class.
			clazz.setSuperUnit(null);
			// clazz.setTainted();
			clazz.setResolved(true);

			TypeDeclaration typeDecl = new TypeDeclaration(new ObjectType(clazz.getName()), 0);
			typeDecl.setSuperType(Type.OBJECT);
			typeDecl.visit(visitor);
		} else {
			visitSuperTypes(clazz, visitor);
		}
	}

	public void visitSuperTypes(ClassUnit clazz, TypeScriptGenerator visitor) {
		if (clazz.isResolved())
			return;

		Log logger = Log.getLogger();
		clazz.setResolved(true);
		if (clazz.getSignature().toString().startsWith("[")) {
			// Class is an array class without class file: Do nothing.
		} else {
			try {
				if (!J2JSSettings.allowClass.test(clazz.toString())) {
					return;
				}
				compile(clazz, visitor);
			} catch (RuntimeException ex) {
				J2JSSettings.errorCount++;
				logger.error(ex.toString());
				// ex.printStackTrace();
				if (J2JSSettings.failOnError) {
					throw ex;
				}
			}
		}

		ClassUnit superClass = clazz.getSuperUnit();
		if (superClass != null) {
			visitSuperTypes(superClass, visitor);
		}

		for (ClassUnit interfaceUnit : clazz.getInterfaces()) {
			visitSuperTypes(interfaceUnit, visitor);
		}
	}

	private void compile(ClassUnit classUnit, TypeScriptGenerator visitor) {

		if (classUnit.getClassFile() == null) {
			Log.getLogger().warn("Cannot read " + classUnit.getClassFile());
			return;
		}

		Log.getLogger().info("Cross-Compiling " + classUnit);

		if (!classUnit.isPartial()) {
			classUnit.getDeclaredMembers().forEach(m -> m.setResolved(true));
		}

		ExtRegistry.get().invoke("type.visit", null, new VisitorInput<ClassUnit>(classUnit, visitor));

		// Set not current date but date of last modification. This is
		// independent of system clock.
		classUnit.setLastCompiled(classUnit.getLastModified());
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

	public void addFieldAccessor(String field, String code) {
		this.fieldAccessors.put(field, code);
	}

	public String getFieldAccessor(String field) {
		return this.fieldAccessors.get(field);
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttr(String key) {
		return (T) attr.get(key);
	}

	public void putAttr(String key, Object val) {
		attr.put(key, val);
	}
}
