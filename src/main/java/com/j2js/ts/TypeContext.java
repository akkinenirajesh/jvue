package com.j2js.ts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import com.j2js.dom.MethodBinding;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtRegistry;

public class TypeContext {

	private ByteArrayOutputStream io;

	private Map<String, List<MethodContext>> methods = new HashMap<>();

	private Map<String, TypeContext> anonymousClasses = new HashMap<>();

	private TSPrintStream fields;

	private Set<ObjectType> imports = new HashSet<>();

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

	public PrintStream getFieldsStream() {
		return fields;
	}

	public MethodContext getMethod(MethodDeclaration method) {
		MethodBinding binding = method.getMethodBinding();
		String name = binding.getName();
		List<MethodContext> list = methods.get(name);
		if (list == null) {
			methods.put(name, list = new ArrayList<>());
		}
		MethodContext mc = new MethodContext(method);
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
			// Do not add anonymous imports as they wont refer from other
			// classes
			if (type.getClassName().contains("$")) {
				return;
			}

			// Don't need to import same class
			if (type.getClassName().equals(this.type.getClassName())) {
				return;
			}
			imports.add(type);
		}
	}

	public void write(PrintStream ps) throws IOException {

		ExtRegistry.get().invoke("imports", ps,
				this.imports.stream().map(i -> i.getClassName()).collect(Collectors.toSet()));

		String className = type.getUnQualifiedName();
		ps.print("export class ");
		ps.print(className);

		if (type.hasSuperClass()) {
			ExtRegistry.get().invoke("extends", ps, type.getSuperType().getClassName());
		}
		ps.println(" {");

		fields.write(ps);
		List<MethodContext> remove = methods.remove("<init>");
		if (remove != null) {
			generateMethod(ps, "<init>", remove);
		}
		methods.forEach((name, list) -> {
			generateMethod(ps, name, list);
		});
		if (remove != null) {
			methods.put("<init>", remove);
		}

		ps.println("}");

		anonymousClasses.forEach((n, c) -> {
			try {
				ps.println();
				c.write(ps);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void generateMethod(PrintStream ps, String name, List<MethodContext> list) {
		try {
			if (name.equals("<init>")) {
				name = "constructor";
			}

			if (list.size() == 1) {
				generateMethod(ps, name, list.get(0),
						name.equals("constructor") && !isAnonymousClass(type.getClassName()));
			} else {

				generateOverloadMethod(ps, name, list);

				int i = 1;
				for (MethodContext m : list) {
					generateMethod(ps, name + i, m, false);
					i++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private boolean isAnonymousClass(String name) {
		return name.contains("$");
	}

	private void generateOverloadMethod(PrintStream ps, String name, List<MethodContext> list) {
		List<Map<String, String>> parameterReplacers = new ArrayList<>();

		// find max parameters count
		int totalParams = 0;
		for (MethodContext c : list) {
			Collection<VariableDeclaration> parameters = c.method.getParameters();
			if (totalParams < parameters.size()) {
				totalParams = parameters.size();
			}
			int i = 1;
			Map<String, String> replacers = new HashMap<>();
			for (VariableDeclaration p : parameters) {
				replacers.put(p.getName(), ("_p" + i));
				i++;
			}
			parameterReplacers.add(replacers);
		}

		if (name.equals("constructor")) {
			generateDummyConstructor(ps, totalParams);
			ps.println("");
			indent(ps, 1);
			ps.print("constructor0");
			ps.print("(");
		} else {
			indent(ps, 1);
			ps.print(name);
			ps.print("(");
		}

		generateParameters(ps, totalParams);
		ps.println(") {");

		indent(ps, 2);
		int i = 0;
		for (MethodContext c : list) {
			Collection<VariableDeclaration> parameters = c.method.getParameters();
			// if(((typeof test === 'string') || test === null) && ((t != null
			// && t instanceof org.ecgine.vue.acc.Transaction) || t === null)) {
			Map<String, String> replacers = parameterReplacers.get(i++);
			String condition = generateConstructorCondition(parameters, replacers, totalParams);
			ps.println("if(" + condition + ") {");
			indent(ps, 3);
			ps.print("this." + name + i);
			ps.print("(");
			int j = 0;
			for (VariableDeclaration v : parameters) {
				if (j != 0) {
					ps.print(", ");
				}
				ps.print(replacers.get(v.getName()));
				j++;
			}
			ps.println(");");
			indent(ps, 2);
			ps.print("} else ");
		}
		ps.println("{}");

		indent(ps, 1);
		ps.println("}");

	}

	private String generateConstructorCondition(Collection<VariableDeclaration> parameters,
			Map<String, String> replacers, int totalParams) {
		// Map<String, String> reverse = new HashMap<>();
		// replacers.forEach((a, b) -> reverse.put(b, a));
		int i = 1;
		List<String> conditions = new ArrayList<>();
		for (VariableDeclaration p : parameters) {
			i++;
			String replace = replacers.get(p.getName());
			String condition = generateConstCondition(p, replace);
			if (totalParams == 1) {
				conditions.add(condition);
			} else {
				conditions.add('(' + condition + ')');
			}
		}
		for (; i <= totalParams; i++) {
			conditions.add("_p" + i + " === undefined");
		}

		StringBuilder b = new StringBuilder();
		i = 0;
		for (String c : conditions) {
			if (i != 0) {
				b.append(" && ");
			}
			b.append(c);
			i++;
		}
		return b.toString();
	}

	private String generateConstCondition(VariableDeclaration p, String var) {
		Type type = p.getType();
		switch (type.toString()) {
		case "java.lang.String":
		case "char":
			// test === null || typeof test === 'string'
			return var + " === null || " + "typeof " + var + " === 'string'";
		case "float":
		case "int":
		case "long":
		case "double":
		case "short":
		case "byte":
			// test === null || typeof test === 'number'
			return var + " === null || " + "typeof " + var + " === 'number'";
		case "boolean":
			// test === null || typeof test === 'boolean'
			return var + " === null || " + "typeof " + var + " === 'boolean'";
		default:
			// t === null || t instanceof org.ecgine.vue.acc.Transaction
			return var + " === null || " + var + " instanceof " + getSimpleName(type.toString());
		}
	}

	private void generateDummyConstructor(PrintStream ps, int totalParams) {
		indent(ps, 1);
		ps.print("constructor");
		ps.print("(");
		generateParameters(ps, totalParams);
		ps.println(") {");
		if (type.hasSuperClass()) {
			indent(ps, 2);
			ps.println("super();");
		}
		indent(ps, 1);
		ps.println("}");
	}

	private void generateParameters(PrintStream ps, int totalParams) {
		for (int i = 1; i <= totalParams; i++) {
			if (i != 1) {
				ps.print(", ");
			}
			ps.print("_p" + i);
			ps.print("?: any");
		}
	}

	private void generateMethod(PrintStream ps, String name, MethodContext m, boolean needSuper) throws IOException {
		ps.println("");
		indent(ps, 1);
		ps.print(name);
		m.params.write(ps);
		ps.println("");
		if (needSuper) {
			indent(ps, 2);
			ps.println("super();");
		}
		m.body.write(ps);
		indent(ps, 1);
		ps.println("}");

	}

	private void indent(PrintStream ps, int i) {
		for (; i > 0; i--) {
			ps.print("\t");
		}

	}

	private String getSimpleName(String fullName) {
		return fullName.substring(fullName.lastIndexOf(".") + 1);
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

		public void write(PrintStream ps) throws IOException {
			ps.write(out.toByteArray());
		}

		public TSPrintStream(ByteArrayOutputStream out) {
			super(out);
			this.out = out;
		}
	}

	public static class MethodContext {

		private MethodDeclaration method;
		private TSPrintStream params;
		private TSPrintStream body;

		public MethodContext(MethodDeclaration method) {
			this.method = method;
			this.params = new TSPrintStream();
			this.body = new TSPrintStream();
		}

		public TSPrintStream getBody() {
			return body;
		}

		public TSPrintStream getParams() {
			return params;
		}
	}
}
