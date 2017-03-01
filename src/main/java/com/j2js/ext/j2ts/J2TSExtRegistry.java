package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.lang.reflect.Modifier;

import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ts.TypeScriptGenerator;
import com.j2js.ts.VisitorInput;

public class J2TSExtRegistry {

	public static void register() {
		ExtRegistry r = ExtRegistry.get();

		r.add("imports", new Imports());
		r.add("import", new ImportReplace());
		r.add("import", new Import());
		r.add("import.lib", new LibImport());

		r.add("methods.visit", new MethodsVisit());
		r.add("fields.visit", new FieldsVisit());

		r.add("method", new Method());
		r.add("method.name", new MethodName());
		r.add("method.params", new MethodParams());
		r.add("method.body.start", new MethodStart());
		r.add("method.body.end", new MethodEnd());

		r.add("class", new ClassDeclaration());
		r.add("class.name", new ClassName());
		r.add("class.extends", new ClassExtends());
		r.add("class.body", new ClassBody());

		r.add("class.field.decl", new ClassFieldDeclaration());
		r.add("class.fields", new ClassFields());

		r.add("dummy.constructor.params", new DummyConstructorParams());
		r.add("this", new This());
		r.add("super", new Super());

		r.add("field.visit", new ExtInvocation<VisitorInput<VariableDeclaration>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<VariableDeclaration> input, ExtChain ch) {
				TypeScriptGenerator generator = input.getGenerator();
				generator.setOutputStream(ps);
				generator.indent();
				VariableDeclaration in = input.getInput();

				if (Modifier.isStatic(in.getModifiers())) {
					ps.print("static ");
				}
				in.visit(generator);
				generator.println(";");
				ch.next(ps, input);
			}
		});

		r.add("method.visit", new ExtInvocation<VisitorInput<MethodDeclaration>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<MethodDeclaration> input, ExtChain ch) {
				TypeScriptGenerator generator = input.getGenerator();
				generator.setOutputStream(ps);
				input.getInput().visit(generator);
				ch.next(ps, input);
			}
		});

		r.add("methodinvocation.visit", new ExtInvocation<VisitorInput<MethodInvocation>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<MethodInvocation> input, ExtChain ch) {
				TypeScriptGenerator generator = input.getGenerator();
				generator.setOutputStream(ps);
				generator.methodInvocation(input.getInput());
				ch.next(ps, input);
			}
		});
	}
}
