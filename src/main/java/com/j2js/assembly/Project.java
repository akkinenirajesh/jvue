package com.j2js.assembly;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import com.j2js.FileManager;
import com.j2js.J2JSCompiler;
import com.j2js.J2JSSettings;
import com.j2js.Utils;
import com.j2js.dom.ArrayCreation;
import com.j2js.dom.FieldAccess;
import com.j2js.dom.MethodBinding;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.TypeDeclaration;
import com.j2js.ts.TSHelper;
import com.j2js.visitors.AbstractVisitor;

public class Project implements Serializable {

	static final long serialVersionUID = 0;

	// All managed classes mapped by class name.
	private Map<String, ClassUnit> classesByName = new HashMap<>();

	private ClassUnit javaLangObject;

	private Map<String, Signature> signatures = new HashMap<>();

	private transient Stack<Integer> ids;

	private transient int currentId;

	private transient int currentIndex;

	public transient int currentGeneratedMethods;

	public transient FileManager fileManager;

	public transient AbstractVisitor generator;

	public Map<String, Integer> lambdaArgs = new HashMap<>();
	public Map<String, Boolean> enums = new HashMap<>();
	private Map<String, Map<String, String>> methodReplacers = new HashMap<>();

	private Set<String> objectMethods = new HashSet<>();

	private J2JSSettings settings;

	public Project(J2JSSettings settings) {
		objectMethods.add("hashCode");
		objectMethods.add("equals");
		objectMethods.add("toString");
		enums.put("char", false);
		enums.put("byte", false);
		enums.put("int", false);
		this.settings = settings;
	}

	public Signature getArraySignature(Type type) {
		String signatureString = type.getSignature();
		/*
		 * Examples: L[java.lang.Integer;; -> [java.lang.Integer; L[I; -> [I
		 */
		if (!signatureString.startsWith("L") || !signatureString.endsWith(";")) {
			throw new RuntimeException("Not a class signature: " + signatureString);
		}
		signatureString = signatureString.substring(1, signatureString.length() - 1);
		return getSignature(signatureString);
	}

	/**
	 * All request for a signature delegate to this method.
	 */
	public Signature getSignature(String signatureString) {
		if (signatureString.endsWith(";")) {
			// throw new RuntimeException("Invalid signature: " +
			// signatureString);
		}
		signatureString = signatureString.replaceAll("/", ".");

		Signature signature = signatures.get(signatureString);
		if (signature == null) {
			signature = new Signature(signatureString, getUniqueId(), settings.compression);
			signatures.put(signatureString, signature);
		}

		return signature;
	}

	public Signature getSignature(String className, String relativeSignature) {
		return getSignature(className + '#' + relativeSignature);
	}

	public Signature getSignature(FieldAccess fa) {
		return getSignature(fa.getType().getClassName(), fa.getName());
	}

	private int getUniqueId() {
		if (ids == null) {
			ids = new Stack<Integer>();
			for (Signature signature : signatures.values()) {
				ids.add(signature.getId());
			}
			Collections.sort(ids);
		}

		while (currentIndex < ids.size() && ids.get(currentIndex) == currentId) {
			currentId += 1;
			currentIndex += 1;
		}

		currentId++;
		return currentId - 1;
	}

	public void remove(ClassUnit clazz) {
		classesByName.remove(clazz);
	}

	void visitSuperTypes(ClassUnit clazz, TypeVisitor visitor) {
		visitor.visit(clazz);
		ClassUnit superClass = clazz.getSuperUnit();
		if (superClass != null) {
			visitSuperTypes(superClass, visitor);
		}

		for (ClassUnit interfaceUnit : clazz.getInterfaces()) {
			visitor.visit(interfaceUnit);
			visitSuperTypes(interfaceUnit, visitor);
		}
	}

	public ClassUnit getJavaLangObject() {
		return javaLangObject;
	}

	public ClassUnit getClassUnit(String className) {
		ClassUnit clazz = classesByName.get(className);
		if (clazz != null)
			return clazz;

		throw new RuntimeException("No such unit: " + className);
	}

	public ClassUnit getClassUnit(ReferenceType type) {
		String signature;
		if (type instanceof ArrayType) {
			ArrayType aType = (ArrayType) type;
			signature = Utils.getSignature(aType.getBasicType());
			for (int i = 0; i < aType.getDimensions(); i++) {
				signature += "[]";
			}
		} else {
			signature = Utils.getSignature(type);
		}

		return getClassUnit(signature);
	}

	public ClassUnit getOrCreateClassUnit(String className) {
		ClassUnit classUnit = classesByName.get(className);
		if (classUnit != null)
			return classUnit;

		Signature signature = getSignature(className);
		classUnit = new ClassUnit(this, fileManager, className, signature);
		classesByName.put(className, classUnit);

		if (className.equals("java.lang.Object")) {
			classUnit.setResolved(true);
			javaLangObject = classUnit;
		}

		return classUnit;
	}

	private MemberUnit getMemberUnitOrNull(String className, Signature signature) {
		ClassUnit classUnit = getOrCreateClassUnit(className);
		if (classUnit == null)
			return null;
		return classUnit.getDeclaredMember(signature.toString());
	}

	private MemberUnit getMemberUnit(String className, Signature signature) {
		MemberUnit unit = getMemberUnitOrNull(className, signature);
		if (unit == null) {
			throw new RuntimeException("No such unit: " + className + "#" + signature);
		}

		return unit;
	}

	public ProcedureUnit getProcedureUnit(MethodBinding methodBinding) {
		Signature signature = getSignature(methodBinding.getRelativeSignature());
		String className = methodBinding.getDeclaringClass().getClassName();
		return (ProcedureUnit) getMemberUnit(className, signature);
	}

	public ProcedureUnit getOrCreateProcedureUnit(MethodBinding methodBinding) {
		Signature signature = getSignature(methodBinding.getRelativeSignature());
		String className = methodBinding.getDeclaringClass().getClassName();
		return (ProcedureUnit) getOrCreateMemberUnit(className, signature);
	}

	private MemberUnit getOrCreateMemberUnit(String className, Signature signature) {
		MemberUnit member = getMemberUnitOrNull(className, signature);

		if (member == null) {
			ClassUnit clazz = getClassUnit(className);
			if (signature.isMethod()) {
				member = new MethodUnit(signature, clazz);
			} else if (signature.isConstructor()) {
				member = new ConstructorUnit(signature, clazz);
			} else {
				member = new FieldUnit(signature, clazz);
			}
		}
		return member;
	}

	// public Unit getOrCreateUnit(String signature) {
	// String array[] = signature.split("#");
	// Unit unit;
	// if (array.length == 1) {
	// unit = getOrCreateClassUnit(signature);
	// } else {
	// unit = getOrCreateMemberUnit(array[0],
	// Project.singleton.getSignature(array[1]));
	// }
	// return unit;
	// }

	public FieldUnit getOrCreateFieldUnit(ObjectType type, String name) {
		return (FieldUnit) getOrCreateMemberUnit(type.getClassName(), getSignature(name));
	}

	public void addReference(MethodDeclaration decl, FieldAccess fa) {
		ProcedureUnit source = getOrCreateProcedureUnit(decl.getMethodBinding());
		source.addTarget(getSignature(fa));
	}

	public void addReference(MethodDeclaration decl, MethodInvocation invocation) {
		ProcedureUnit source = getOrCreateProcedureUnit(decl.getMethodBinding());
		source.addTarget(getSignature(invocation.getMethodBinding().toString()));
	}

	public void addReference(MethodDeclaration decl, ArrayCreation ac) {
		ProcedureUnit source = getOrCreateProcedureUnit(decl.getMethodBinding());
		Signature signature = getArraySignature(ac.getTypeBinding());
		for (int i = 0; i < ac.getDimensions().size(); i++) {
			// TODO: Target must be a field or method. Is length the right way?
			source.addTarget(getSignature(signature.toString().substring(i) + "#length"));
		}
	}

	/**
	 * Returns a live collection of all classes managed by this project.
	 */
	public Collection<ClassUnit> getClasses() {
		return classesByName.values();
	}

	public void resolve(ClassUnit clazz) {
		if (clazz.isResolved())
			return;

		if (clazz.getName().startsWith("[")) {
			// This is an array type and not a class.
			clazz.setSuperUnit(getJavaLangObject());
			// clazz.setTainted();
			clazz.setResolved(true);
			// We need a member (and we chose length) in addReference(..) to
			// taint the class.
			new FieldUnit(getSignature("length"), clazz);

			TypeDeclaration typeDecl = new TypeDeclaration(new ObjectType(clazz.getName()), 0,
					isEnum(clazz.getClassFullName()));
			typeDecl.setSuperType(Type.OBJECT);
			typeDecl.visit(generator);
		} else {
			TypeResolver resolver = new TypeResolver(this, generator);
			visitSuperTypes(clazz, resolver);
		}
	}

	public int getLambdaArguments(String signature) {
		String cls = signature.split("\\)L")[1];
		Integer integer = lambdaArgs.get(cls);
		if (integer != null) {
			return integer;
		}
		cls = cls.substring(0, cls.length() - 1).replace('/', '.');
		try {
			Class<?> c = fileManager.getClassLoader().loadClass(cls);
			Method[] methods = c.getMethods();
			for (Method m : methods) {
				if (!m.isDefault() && !Modifier.isStatic(m.getModifiers())) {
					if (!objectMethods.contains(m.getName())) {
						int count = m.getParameters().length;
						lambdaArgs.put(cls, count);
						return count;
					}
				}
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Lambda method not found");
	}

	public boolean isEnum(ObjectType type) {
		return isEnum(type.getClassName());
	}

	public boolean isEnum(String fullName) {
		Boolean val = enums.get(fullName);
		if (val != null) {
			return val;
		}
		Class<?> cls = findClass(fullName);
		if (cls != null) {
			enums.put(fullName, cls.isEnum());
			return cls.isEnum();
		} else {
			enums.put(fullName, false);
			return false;
		}
	}

	public String getMethodReplcerName(String cls, String name) {
		Map<String, String> map = methodReplacers.get(cls);
		if (map == null) {
			map = new HashMap<>();
			methodReplacers.put(cls, map);
		}
		if (map.containsKey(name)) {
			return map.get(name);
		}
		String res = name;
		Class<?> c = findClass(cls);
		while (c != null) {
			try {
				c.getDeclaredField(name);
				res = "_" + name;
				break;
			} catch (Exception e) {
				c = c.getSuperclass();
			}
		}

		if (res.equals("<clinit>")) {
			res = "staticBlock";
		}
		if (res.startsWith("lambda$")) {
			res = TSHelper.getSimpleName(cls) + '$' + res;
		}
		map.put(name, res);
		return res;
	}

	public J2JSSettings getSettings() {
		return settings;
	}

	public Class<?> findClass(String cls) {
		try {
			return fileManager.getClassLoader().loadClass(cls);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static void clearSingleton() {
		// TODO Auto-generated method stub

	}

	public static Project createSingleton(File cacheFile) {
		// TODO Auto-generated method stub
		return null;
	}

	public static Project getSingleton() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void write(J2JSCompiler j2jsCompiler) {
		// TODO Auto-generated method stub

	}
}
