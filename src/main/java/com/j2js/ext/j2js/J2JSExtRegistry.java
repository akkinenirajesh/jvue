package com.j2js.ext.j2js;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ext.Tuple;
import com.j2js.ts.MethodContext;
import com.j2js.ts.TSHelper;
import com.j2js.ts.TypeContext;
import com.j2js.ts.TypeContext.TSPrintStream;
import com.j2js.ts.TypeScriptGenerator;
import com.j2js.ts.VisitorInput;

public class J2JSExtRegistry {

	public static void register() {
		ExtRegistry r = ExtRegistry.get();

//		r.add("file.create", new ExtInvocation<Object>() {
//
//			@Override
//			public void invoke(PrintStream ps, Object input, ExtChain ch) {
//				try {
//					InputStream is = J2JSExtRegistry.class.getClassLoader().getResourceAsStream("javascript/j4ts.js");
//					IOUtils.copy(is, ps);
//					is.close();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}, -1);

		r.add("imports", new ExtInvocation<Set<String>>() {

			@Override
			public void invoke(PrintStream ps, Set<String> input, ExtChain ch) {
			}
		}, -1);

		r.add("enum", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				ps.println("());");
				ch.next(ps, input);
			}
		}, 1);

		r.add("class", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				if (input.getType().hasSuperClass()) {
					String className = input.getType().getSuperType().getClassName();
					ps.print("(");
					ps.print(className);
					ps.println("));");
				} else {
					ps.println("());");
				}
				// ps.print("exports.");
				// String name = input.getType().getUnQualifiedName();
				// ps.print(name);
				// ps.print("= ");
				// ps.print(name);
				// ps.println(";");

				ch.next(ps, input);
			}
		}, 1);

		r.add("class.name", new ExtInvocation<TypeDeclaration>() {

			@Override
			public void invoke(PrintStream ps, TypeDeclaration input, ExtChain ch) {
				// var Account = (function ()
				String className = input.getUnQualifiedName();
				ps.print("var ");
				ps.print(className);
				if (input.hasSuperClass()) {
					ps.print(" = (function (_super)");
				} else {
					ps.print(" = (function ()");
				}
			}
		}, -1);
		r.add("class.extends", new ExtInvocation<TypeDeclaration>() {

			@Override
			public void invoke(PrintStream ps, TypeDeclaration input, ExtChain ch) {
			}
		}, -1);
		r.add("class.body", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				if (input.getType().hasSuperClass()) {
					ps.print("\t__extends(Account, _super);");
				}
				ch.next(ps, input);
			}
		}, -1);

		r.add("field.visit", new ExtInvocation<VisitorInput<VariableDeclaration>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<VariableDeclaration> input, ExtChain ch) {
				TypeScriptGenerator generator = input.getGenerator();
				generator.setOutputStream(ps);
				generator.indent();
				VariableDeclaration in = input.getInput();
				in.visit(generator);
				generator.println(";");
			}
		}, -1);

		r.add("class.field.decl", new ExtInvocation<Tuple<VariableDeclaration, TypeContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<VariableDeclaration, TypeContext> input, ExtChain ch) {
				VariableDeclaration var = input.getT();
				if (Modifier.isStatic(var.getModifiers())) {
					ps.print("\t");
					ps.print(input.getR().getType().getUnQualifiedName());
					ps.print(".");
				} else {
					ps.print("\tthis.");
				}
				ch.next(ps, input);
			}
		}, -1);

		r.add("class.fields", new ExtInvocation<TSPrintStream>() {

			@Override
			public void invoke(PrintStream ps, TSPrintStream input, ExtChain ch) {
			}
		}, -1);

		r.add("method.name", new ExtInvocation<Tuple<String, MethodContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
				ps.print("\t");
				String clsName = input.getR().getType().getUnQualifiedName();
				if (input.getT().equals("constructor")) {
					// function Account
					ps.print("function ");
					ps.print(clsName);
				} else {
					// Account.prototype.constructor0 = function
					ps.print(clsName);
					ps.print(".");
					if (!Modifier.isStatic(input.getR().getAccess())) {
						ps.print("prototype.");
					}
					ps.print(input.getT());
					ps.print(" = function ");
				}
			}
		}, -1);

		r.add("method.return", new ExtInvocation<Tuple<String, MethodContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
			}
		}, -1);

		r.add("method.body.start", new ExtInvocation<Tuple<String, MethodContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
				ps.println(" {");
				ps.print("\t\tvar _this = this;");
				ps.println("");
				input.getR().getBody().write(ps);
			}
		}, -1);

		r.add("method.body.end", new ExtInvocation<Tuple<String, MethodContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
				if (input.getT().equals("constructor")) {
					input.getR().getTypeContext().getFieldsStream().write(ps);
				}
				ps.println("\t};");
			}
		}, -1);

		r.add("dummy.constructor.params", new ExtInvocation<Integer>() {

			@Override
			public void invoke(PrintStream ps, Integer input, ExtChain ch) {
				for (int i = 1; i <= input; i++) {
					if (i != 1) {
						ps.print(", ");
					}
					ps.print("_p" + i);
				}
			}
		}, -1);

		r.add("this", new ExtInvocation<Object>() {

			@Override
			public void invoke(PrintStream ps, Object input, ExtChain ch) {
				ps.print("_this");
			}
		}, -1);

		r.add("super", new ExtInvocation<Object>() {

			@Override
			public void invoke(PrintStream ps, Object input, ExtChain ch) {
				ps.print("_super");
			}
		}, -1);

		r.add("pkg.start", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				String pkgName = TSHelper.getPkgName(input.getType().getClassName());
				String[] split = pkgName.split("\\.");
				// var java; //It should be declared only once
				// (function (java) {
				// var security;
				// (function (security) {
				String s = split[0];
				Set<String> pkgs = input.getCompiler().getAttr("pkg_decl");
				if (pkgs == null) {
					pkgs = new HashSet<>();
					input.getCompiler().putAttr("pkg_decl", pkgs);
				}
				if (!pkgs.contains(s)) {
					pkgs.add(s);
					ps.print("var ");
					ps.print(s);
					ps.println(";");
				}
				ps.print("(function (");
				ps.print(s);
				ps.println(") {");
				for (int i = 1; i < split.length; i++) {
					// String intent = getIntent(i);
					s = split[i];
					// ps.print(intent);
					ps.print("var ");
					ps.print(s);
					ps.println(";");
					// ps.print(intent);
					ps.print("(function (");
					ps.print(s);
					ps.println(") {");
				}
			}
		}, -1);

		r.add("pkg.end", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				String[] split = input.getType().getClassName().split("\\.");

				// security.Message = Message;
				// Message["__class"] = "java.security.Message";
				// String intent = getIntent(split.length - 2);
				// ps.print(intent);
				ps.print(split[split.length - 2]);
				ps.print(".");
				ps.print(split[split.length - 1]);
				ps.print(" = ");
				ps.print(split[split.length - 1]);
				ps.println(";");

				for (int i = split.length - 2; i >= 0; i--) {
					// })(security = java.security || (java.security = {}));
					// intent = getIntent(i);
					String s = split[i];
					// ps.print(intent);
					ps.print("})(");
					ps.print(s);
					ps.print(" = ");
					String remaining = split[0];
					for (int j = 1; j <= i; j++) {
						remaining += '.' + split[j];
					}
					ps.print(remaining);
					ps.print(" || (");
					ps.print(remaining);
					ps.println(" = {}));");
				}

				// })(java || (java = {}));

			}
		}, -1);

	}

	protected static String getIntent(int i) {
		StringBuilder b = new StringBuilder();
		for (; i > 0; i--) {
			b.append("\t");
		}
		return b.toString();
	}
}
