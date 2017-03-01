package com.j2js.ts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ObjectType;

import com.j2js.dom.MethodBinding;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.ext.ExtRegistry;

public class TypeContext {

	private ByteArrayOutputStream io;

	private Map<String, List<MethodContext>> methods = new HashMap<>();

	private Map<String, TypeContext> anonymousClasses = new HashMap<>();

	private TSPrintStream fields;

	private Set<String> imports = new HashSet<>();

	private TypeDeclaration type;

	private TypeContext parent;

	public TypeContext(TypeDeclaration type) {
		this.type = type;
		fields = new TSPrintStream();
	}

	private TypeContext(TypeContext parent, TypeDeclaration type) {
		this(type);
		this.parent = parent;
	}

	public byte[] toByteArray() {
		return io.toByteArray();
	}

	public TSPrintStream getFieldsStream() {
		return fields;
	}

	public MethodContext getMethod(MethodDeclaration method) {
		MethodBinding binding = method.getMethodBinding();
		String name = binding.getName();
		List<MethodContext> list = methods.get(name);
		if (list == null) {
			methods.put(name, list = new ArrayList<>());
		}
		MethodContext mc = new MethodContext(this, method, list);
		list.add(mc);
		return mc;
	}

	public TypeContext getAnonymous(String name) {
		return anonymousClasses.get(name);
	}

	public TypeContext getAnonymous(String name, TypeDeclaration type) {
		TypeContext c = getAnonymous(name);
		if (c == null) {
			c = new TypeContext(this, type);
			anonymousClasses.put(name, c);
		}
		return c;
	}

	public void addImports(ObjectType type) {
		if (parent != null) {
			parent.addImports(type);
		} else {
			String name = type.getClassName();
			if (name.startsWith("[")) {
				name = name.substring(1, name.length());
			}
			if (name.endsWith(";")) {
				name = name.substring(1, name.length() - 1);
			}
			// Do not add anonymous imports as they wont refer from other
			// classes
			if (name.contains("$")) {
				return;
			}

			// Don't need to import same class
			if (name.equals(this.type.getClassName())) {
				return;
			}

			imports.add(name);
		}
	}

	public TypeDeclaration getType() {
		return type;
	}

	public Set<String> getImports() {
		return imports;
	}

	public Map<String, List<MethodContext>> getMethods() {
		return methods;
	}

	public Map<String, TypeContext> getAnonymousClasses() {
		return anonymousClasses;
	}

	public void write(PrintStream ps) throws IOException {

		ExtRegistry.get().invoke("class", ps, this);

		anonymousClasses.forEach((n, c) -> {
			try {
				ps.println();
				c.write(ps);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public String toString() {
		return type.toString();
	}

	public static class TSPrintStream extends PrintStream {

		private ByteArrayOutputStream out;

		public TSPrintStream() {
			this(new ByteArrayOutputStream());
		}

		public void write(PrintStream ps) {
			try {
				ps.write(out.toByteArray());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public TSPrintStream(ByteArrayOutputStream out) {
			super(out);
			this.out = out;
		}
	}
}
