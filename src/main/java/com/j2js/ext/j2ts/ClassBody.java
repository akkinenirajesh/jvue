package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.generic.Type;

import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ext.Tuple;
import com.j2js.ts.MethodContext;
import com.j2js.ts.TypeContext;
import com.j2js.ts.TypeContext.TSPrintStream;

public class ClassBody implements ExtInvocation<TypeContext> {

	@Override
	public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {

		ExtRegistry.get().invoke("class.fields", ps, input.getFieldsStream());

		Map<String, List<MethodContext>> methods = input.getMethods();
		List<MethodContext> remove = methods.remove("<init>");

		if (remove != null) {
			generateMethod(ps, input, "<init>", remove);
		}

		methods.forEach((name, list) -> {
			generateMethod(ps, input, name, list);
		});

		if (remove != null) {
			methods.put("<init>", remove);
		}

		ch.next(ps, input);
	}

	private void generateMethod(PrintStream ps, TypeContext input, String name, List<MethodContext> list) {
		try {
			if (name.equals("<init>")) {
				name = "constructor";
			}

			if (list.size() == 1) {
				generateMethod(ps, name, list.get(0));
			} else {

				generateOverloadMethod(ps, input, name, list);

				int i = 1;
				for (MethodContext m : list) {
					generateMethod(ps, name + i, m);
					i++;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void generateOverloadMethod(PrintStream ps, TypeContext input, String name, List<MethodContext> list) {
		List<Map<String, String>> parameterReplacers = new ArrayList<>();

		// find max parameters count
		int totalParams = 0;
		for (MethodContext c : list) {
			Collection<VariableDeclaration> parameters = c.getMethod().getParameters();
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
		String dummyMethodName;
		if (name.equals("constructor")) {
			List<MethodContext> dummyList = new ArrayList<>();
			MethodContext context = new MethodContext(input, null, dummyList);
			dummyList.add(context);
			context.getParams().append("(");
			generateParameters(context.getParams(), totalParams);
			context.getParams().append("){");
			generateMethod(ps, name, context);
			dummyMethodName = "constructor0";
		} else {
			dummyMethodName = name;
		}

		List<MethodContext> dummyList = new ArrayList<>();
		MethodContext context = new MethodContext(input, null, dummyList);
		dummyList.add(context);
		context.getParams().append("(");
		generateParameters(context.getParams(), totalParams);
		context.getParams().append("){");
		int i = 0;
		TSPrintStream body = context.getBody();
		body.print("\t\t");
		for (MethodContext c : list) {
			Collection<VariableDeclaration> parameters = c.getMethod().getParameters();
			// if(((typeof test === 'string') || test === null) && ((t != null
			// && t instanceof org.ecgine.vue.acc.Transaction) || t === null)) {
			Map<String, String> replacers = parameterReplacers.get(i++);
			String condition = generateConstructorCondition(parameters, replacers, totalParams);
			body.println("if(" + condition + ") {");
			body.print("\t\t\t");
			body.print("this." + name + i);
			body.print("(");
			int j = 0;
			for (VariableDeclaration v : parameters) {
				if (j != 0) {
					body.print(", ");
				}
				body.print(replacers.get(v.getName()));
				j++;
			}
			body.println(");");
			body.print("\t\t");
			body.print("} else ");
		}
		body.println("{}");
		generateMethod(ps, dummyMethodName, context);
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

	private String getSimpleName(String fullName) {
		return fullName.substring(fullName.lastIndexOf(".") + 1);
	}

	private void generateParameters(PrintStream ps, int totalParams) {
		ExtRegistry.get().invoke("dummy.constructor.params", ps, totalParams);
	}

	private void generateMethod(PrintStream ps, String name, MethodContext m) {
		Tuple<String, MethodContext> input = new Tuple<String, MethodContext>(name, m);
		ExtRegistry.get().invoke("method", ps, input);
	}

}
