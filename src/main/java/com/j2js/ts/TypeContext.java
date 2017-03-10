package com.j2js.ts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
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
import com.j2js.ext.ExtInvoker;
import com.j2js.ext.ExtRegistry;
import com.j2js.util.TypeUtils;

public class TypeContext {

	private ByteArrayOutputStream io;

	private Map<String, List<MethodContext>> methods = new HashMap<>();
	private Map<String, List<MethodContext>> staticMethods = new HashMap<>();

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
		Map<String, List<MethodContext>> mm = Modifier.isStatic(method.getAccess()) ? staticMethods : methods;
		MethodBinding binding = method.getMethodBinding();
		String name = binding.getName();
		List<MethodContext> list = mm.get(name);
		if (list == null) {
			mm.put(name, list = new ArrayList<>());
		}
		MethodContext mc = new MethodContext(this, method, list);
		list.add(mc);
		return mc;
	}

	public TypeContext getAnonymous(String name) {
		return anonymousClasses.get(name);
	}

	public TypeContext getInnerClass(String name, TypeDeclaration type) {
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
			String name = TypeUtils.extractClassName(type.getClassName());

			// Do not add anonymous imports as they wont refer from other
			// classes
			if (name.contains("$")) {
				return;
			}

			// Don't need to import same class
			if (name.equals(this.type.getClassName())) {
				return;
			}

			if (name.length() == 1) {
				// It is primitive type
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

	public Map<String, List<MethodContext>> getStaticMethods() {
		return staticMethods;
	}

	public Map<String, TypeContext> getAnonymousClasses() {
		return anonymousClasses;
	}

	public void write(ExtInvoker inv, PrintStream ps) throws IOException {
		inv.invoke("pkg.start", ps, this);
		if (type.isEnum()) {
			inv.invoke("enum", ps, this);
		} else {
			inv.invoke("class", ps, this);
		}
		inv.invoke("pkg.end", ps, this);

		anonymousClasses.forEach((n, c) -> {
			try {
				ps.println();
				c.write(inv, ps);
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
