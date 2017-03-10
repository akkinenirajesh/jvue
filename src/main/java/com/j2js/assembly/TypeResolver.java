package com.j2js.assembly;

import com.j2js.Log;
import com.j2js.Parser;
import com.j2js.dom.TypeDeclaration;
import com.j2js.visitors.AbstractVisitor;

public class TypeResolver implements TypeVisitor {

	private AbstractVisitor generator;

	private Project project;

	public TypeResolver(Project theProject, AbstractVisitor theGenerator) {
		project = theProject;
		generator = theGenerator;
	}

	public void visit(ClassUnit clazz) {
		if (clazz.isResolved())
			return;

		Log logger = Log.getLogger();

		if (clazz.getSignature().toString().startsWith("[")) {
			// Class is an array class without class file: Do nothing.
		} else if (!clazz.isUpToDate()) {
			clazz.clear();
			try {
				compile(clazz);
				project.getSettings().compileCount++;
			} catch (RuntimeException ex) {
				project.getSettings().errorCount++;
				logger.error(ex.toString());
				// ex.printStackTrace();
				if (project.getSettings().failOnError) {
					throw ex;
				}
			}
		} else {
			logger.debug("Up to date: " + clazz);
		}

		clazz.setResolved(true);
	}

	/**
	 * Compiles the unit.
	 */
	private void compile(ClassUnit classUnit) {

		if (classUnit.getClassFile() == null) {
			Log.getLogger().warn("Cannot read " + classUnit.getClassFile());
			return;
		}

		Log.getLogger().info("Cross-Compiling " + classUnit);

		TypeDeclaration typeDecl = parse(classUnit);

		// TODO
		// if (!Modifier.isInterface(typeDecl.getAccess())) {
		typeDecl.visit(generator);
		// }

		// Set not current date but date of last modification. This is
		// independent of system clock.
		classUnit.setLastCompiled(classUnit.getLastModified());
	}

	private TypeDeclaration parse(ClassUnit classUnit) {
		Parser parser = new Parser(project, classUnit);
		TypeDeclaration typeDecl = parser.parse();

		return typeDecl;
	}
}
