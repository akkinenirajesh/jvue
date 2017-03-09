package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.lang.reflect.Modifier;

import com.j2js.Parser;
import com.j2js.assembly.ClassUnit;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ext.ExtRegistry;
import com.j2js.ts.TSHelper;
import com.j2js.ts.TypeContext;
import com.j2js.ts.TypeScriptGenerator;
import com.j2js.ts.VisitorInput;

public class J2TSExtRegistry {

	public static void register() {
		ExtRegistry r = ExtRegistry.get();

		r.add("imports", new EnumImports());
		r.add("imports", new Imports());
		r.add("import", new ImportReplace());
		r.add("import", new Import());
		r.add("import.lib", new LibImport());

		r.add("methods.visit", new MethodsVisit());
		r.add("fields.visit", new FieldsVisit());

		r.add("method", new Method());
		r.add("method.name", new MethodName());
		r.add("method.params", new MethodParams());
		r.add("method.return", new MethodReturn());
		r.add("method.body.start", new MethodStart());
		r.add("method.body.end", new MethodEnd());

		r.add("enum", new EnumDeclaration());
		r.add("class", new ClassDeclaration());
		r.add("class.name", new ClassName());
		r.add("class.extends", new ClassExtends());
		r.add("class.body", new ClassBody());

		r.add("class.field.decl", new ClassFieldDeclaration());
		r.add("class.fields", new ClassFields());

		r.add("dummy.constructor.params", new DummyConstructorParams());
		r.add("this", new This());
		r.add("super", new Super());

		r.add("type.visit", new ExtInvocation<VisitorInput<ClassUnit>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<ClassUnit> input, ExtChain ch) {
				ClassUnit classUnit = input.getInput();
				if (classUnit.typeDecl != null) {
					return;
				}
				Parser parser = new Parser(classUnit);
				TypeDeclaration typeDecl = parser.parse();
				classUnit.typeDecl = typeDecl;

				TypeScriptGenerator visitor = input.getGenerator();
				visitor.visit(typeDecl, classUnit.isPartial());
			}
		});

		r.add("field.visit", new ExtInvocation<VisitorInput<VariableDeclaration>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<VariableDeclaration> input, ExtChain ch) {
				TypeScriptGenerator generator = input.getGenerator();
				generator.indent();
				VariableDeclaration in = input.getInput();

				if (Modifier.isStatic(in.getModifiers())) {
					generator.print("static ");
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
				input.getInput().visit(generator);
				ch.next(ps, input);
			}
		});

		r.add("methodinvocation.visit", new ExtInvocation<VisitorInput<MethodInvocation>>() {

			@Override
			public void invoke(PrintStream ps, VisitorInput<MethodInvocation> input, ExtChain ch) {
				TypeScriptGenerator generator = input.getGenerator();
				generator.methodInvocation(input.getInput());
				ch.next(ps, input);
			}
		});

		r.add("pkg.start", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				// declare namespace java.io {
				ps.print("declare namespace ");
				ps.print(TSHelper.getPkgName(input.getType().getClassName()));
				ps.println(" {");
				ch.next(ps, input);
			}
		});

		r.add("pkg.end", new ExtInvocation<TypeContext>() {

			@Override
			public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {
				// }
				ps.println();
				ps.print("}");
				ch.next(ps, input);
			}
		});
	}
}
