package com.j2js.ext.j2js;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.Set;

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

public class J2JSExtRegistry {

	public static void register() {
		ExtRegistry r = ExtRegistry.get();

		r.add("file.create", new ExtInvocation<Object>() {

			@Override
			public void invoke(PrintStream ps, Object input, ExtChain ch) {
				ps.println("\"use strict\";");
				ps.println("var __extends = (this && this.__extends) || function (d, b) {");
				ps.println("\tfor (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];");
				ps.println("\tfunction __() { this.constructor = d; }");
				ps.println("\td.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());");
				ps.println("};");
				ps.println("var Lang_1 = require('./Lang');");// TODO
			}
		}, -1);

		r.add("imports", new ExtInvocation<Set<String>>() {

			@Override
			public void invoke(PrintStream ps, Set<String> input, ExtChain ch) {
			}
		}, -1);

		r.add("class", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				if (input.getType().hasSuperClass()) {
					String className = input.getType().getSuperType().getClassName();
					String simpleName = TSHelper.getSimpleName(className);
					ps.print("(");
					ps.print(simpleName);
					ps.println("));");
				} else {
					ps.println("());");
				}
				ps.print("exports.");
				String name = input.getType().getUnQualifiedName();
				ps.print(name);
				ps.print("= ");
				ps.print(name);
				ps.println(";");

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

		r.add("class.field.decl", new ExtInvocation<Tuple<VariableDeclaration, TypeContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<VariableDeclaration, TypeContext> input, ExtChain ch) {
				if (Modifier.isStatic(input.getT().getModifiers())) {
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
					if (input.getR().getMethod() == null || !Modifier.isStatic(input.getR().getMethod().getAccess())) {
						ps.print("prototype.");
					}
					ps.print(input.getT());
					ps.print(" = function ");
				}
			}
		}, -1);

		r.add("method.body.start", new ExtInvocation<Tuple<String, MethodContext>>() {

			@Override
			public void invoke(PrintStream ps, Tuple<String, MethodContext> input, ExtChain ch) {
				ps.println("");
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
				ch.next(ps, input);
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

	}
}
