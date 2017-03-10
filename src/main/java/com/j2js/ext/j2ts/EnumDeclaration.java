package com.j2js.ext.j2ts;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.j2js.assembly.Project;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;
import com.j2js.ts.MethodContext;
import com.j2js.ts.TypeContext;

public class EnumDeclaration implements ExtInvocation<TypeContext> {

	@Override
	public void invoke(PrintStream ps, TypeContext input, ExtChain ch) {

		ch.invoke("class.name", ps, input.getType());

		ps.println(" {");

		input.getMethods().clear();

		input.getStaticMethods().remove("values");
		input.getStaticMethods().remove("<clinit>");

		MethodContext context = prepareMethod(ch.getProject(), input);
		input.getStaticMethods().put("values", context.getList());
		// ExtRegistry.get().invoke("method", ps, new Tuple<String,
		// MethodContext>("values", context));
		ch.invoke("class.body", ps, input);

		ps.print("}");

		ch.next(ps, input);

	}

	private MethodContext prepareMethod(Project project, TypeContext input) {
		String enums;
		try {
			Class<?> cls = project.fileManager.getClassLoader().loadClass(input.getType().getClassName());
			StringBuilder b = new StringBuilder("[");
			Enum<?>[] enumConstants = (Enum<?>[]) cls.getEnumConstants();
			if (enumConstants.length > 1) {
				b.append('\'');
				b.append(enumConstants[0].name());
				b.append('\'');
				for (int i = 1; i < enumConstants.length; i++) {
					b.append(", '");
					b.append(enumConstants[i].name());
					b.append('\'');
				}
			}
			b.append("]");
			enums = b.toString();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			enums = "[]";
		}

		// only one method
		// values(){ return ['','','']}
		List<MethodContext> dummyList = new ArrayList<>();
		MethodContext context = new MethodContext(input, null, dummyList);
		context.addAccess(Modifier.STATIC);
		dummyList.add(context);
		context.getParams().append("()");
		context.getBody().print("\t\treturn " + enums + ";\n");
		return context;
	}

}