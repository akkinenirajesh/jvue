package com.j2js.ts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ObjectType;

import com.j2js.J2JSSettings;
import com.j2js.assembly.Project;
import com.j2js.dom.ASTNode;
import com.j2js.dom.ArrayCreation;
import com.j2js.dom.Block;
import com.j2js.dom.CastExpression;
import com.j2js.dom.ClassInstanceCreation;
import com.j2js.dom.Expression;
import com.j2js.dom.FieldAccess;
import com.j2js.dom.FieldRead;
import com.j2js.dom.InstanceofExpression;
import com.j2js.dom.InvokeDynamic;
import com.j2js.dom.MethodBinding;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.PrimitiveCast;
import com.j2js.dom.ReturnStatement;
import com.j2js.dom.SwitchCase;
import com.j2js.dom.SwitchStatement;
import com.j2js.dom.ThisExpression;
import com.j2js.dom.ThrowStatement;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtInvoker;
import com.j2js.ext.Tuple;
import com.j2js.visitors.JavaScriptGenerator;

public class TypeScriptGenerator extends JavaScriptGenerator {

	private J2TSCompiler compiler;

	private TypeContext context;
	private PkgContext pkg;
	private ExtInvoker inv;

	public TypeScriptGenerator(Project project, J2TSCompiler compiler) {
		super(project);
		this.compiler = compiler;
		this.pkg = new PkgContext(compiler);
	}

	public void setExtInvoker(ExtInvoker inv) {
		this.inv = inv;
	}

	public TypeContext getContext() {
		return context;
	}

	public void writeToFile() {
		J2JSSettings settings = project.getSettings();
		if (settings.singleFile) {
			File file = new File(settings.getBasedir(), settings.fileName + "." + settings.ext);
			try {
				file.createNewFile();
				PrintStream ps = new PrintStream(new FileOutputStream(file));
				inv.invoke("file.create", ps, null);
				pkg.write(inv, ps);
				inv.invoke("file.end", ps, null);
				ps.flush();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			pkg.write(inv, settings.getBasedir());
		}
	}

	public void setStream(TypeDeclaration type) {
		this.context = pkg.get(type);
	}

	public void visit(TypeDeclaration type, boolean isPartial) {
		setStream(type);
		typeDecl = type;
		depth++;
		PrintStream out = getOutputStream();
		try {
			if (type.hasSuperClass()) {
				context.addImports(type.getSuperType());
			}

			if (!type.isEnum()) {
				setOutputStream(context.getFieldsStream());
				inv.invoke("fields.visit", context.getFieldsStream(),
						new VisitorInput<>(new ArrayList<>(type.getFields()), this));
			}
			if (!isPartial) {
				inv.invoke("methods.visit", getOutputStream(),
						new VisitorInput<>(Arrays.asList(type.getMethods()), this));
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			setOutputStream(out);
			depth--;
		}
	}

	public void visit(MethodDeclaration method) {
		PrintStream out = getOutputStream();
		try {
			if (method.visited) {
				return;
			}
			this.currentMethodDeclaration = method;
			method.visited = true;
			if (Modifier.isVolatile(method.getAccess())) {
				return;
			}

			MethodBinding methodBinding = method.getMethodBinding();
			// Do not generate abstract or native methods.
			if (method.getBody() == null) {
				if (Modifier.isNative(method.getAccess()) || Modifier.isAbstract(method.getAccess())
						|| Modifier.isInterface(typeDecl.getAccess())) {
					return;
				}
				throw new RuntimeException(
						"Method " + method + " with access " + method.getAccess() + " may not have empty body");
			}
			if (!methodBinding.isConstructor()) {
				String name = methodBinding.getName();
				if (isFieldAccessor(name)) {
					addFieldAccessor(method);
				}
			}

			MethodContext mc = context.getMethod(method);
			setOutputStream(mc.getParams());
			print("(");

			Iterator<VariableDeclaration> iterator = method.getParameters().iterator();
			while (iterator.hasNext()) {
				VariableDeclaration decl = iterator.next();
				decl.visit(this);
				print(iterator.hasNext() ? ", " : "");
			}

			print(")");

			setOutputStream(mc.getBody());
			// Generate local variable declarations.
			for (VariableDeclaration decl : method.getLocalVariables()) {
				indent();
				decl.visit(this);
				println(";");
			}

			visit_(method.getBody());
		} finally {
			setOutputStream(out);
		}
	}

	private void addFieldAccessor(MethodDeclaration method) {
		MethodBinding binding = method.getMethodBinding();
		Block body = method.getBody();
		ASTNode node = body.getFirstChild();
		if (node instanceof ReturnStatement) {
			ReturnStatement ret = (ReturnStatement) node;
			if (ret.getExpression() instanceof FieldRead) {
				FieldRead field = (FieldRead) ret.getExpression();
				String name = field.getName();
				compiler.addFieldAccessor(binding.toString(), normalizeAccess(name));
				return;
			}
		}
		System.err.println("unknown access method");
		compiler.addFieldAccessor(binding.toString(), "'unkwon access method'");
	}

	private boolean isFieldAccessor(String name) {
		return name.startsWith("access$");
	}

	public void visit(MethodInvocation invocation) {
		inv.invoke("methodinvocation.visit", getOutputStream(), new VisitorInput<>(invocation, this));
	}

	public void methodInvocation(MethodInvocation invocation) {
		ASTNode expression = invocation.getExpression();
		MethodBinding methodBinding = invocation.getMethodBinding();
		compiler.addClass(methodBinding.toString());
		if (invocation.isSpecial) {
			if (!typeDecl.hasSuperClass() && methodBinding.isConstructor()) {
				return;
			}

			if (expression != null) {
				expression.visit(this);
			} else {
				inv.invoke("super", getOutputStream(), null);
			}

			if (methodBinding.isConstructor() && !isAnonymousClass(invocation.getMethodDecl().toString())) {
				String simpleName = methodBinding.getDeclaringClass().getClassName();
				print(".constructor").print(TSHelper.getSimpleName(simpleName)).print("0");
			}

		} else if (expression != null) {
			expression.visit(this);
			print(".");
		}
		String name = methodBinding.getName();
		if (isFieldAccessor(name)) {
			String code = compiler.getFieldAccessor(methodBinding.toString());
			if (code == null) {
				compiler.addClass(methodBinding.toString(), true);
				code = compiler.getFieldAccessor(methodBinding.toString());
				if (code == null) {
					code = "'is null'";
				}
			}
			List arguments = invocation.getArguments();
			if (!arguments.isEmpty()) {
				((ASTNode) arguments.get(0)).visit(this);
			} else {
				System.err.println("Unhandled method");
			}
			print(code);
			return;
		} else if (expression == null) {
			context.addImports(methodBinding.getDeclaringClass());
			if (project.isEnum(methodBinding.getDeclaringClass())) {
				compiler.addClass(methodBinding.getDeclaringClass().getClassName());
			}
			// Static invocation
			print(methodBinding.getDeclaringClass().getClassName()).print(".");
		}
		if (!methodBinding.isConstructor()) {
			if (invocation.isSpecial) {
				print(".");
			}
			name = project.getMethodReplcerName(methodBinding.getDeclaringClass().getClassName(),
					methodBinding.getName());
			print(name);
		}
		print("(");
		generateList(invocation.getArguments());
		print(")");
	}

	private boolean isAnonymousClass(String name) {
		return name.contains("$");
	}

	public void visit(ClassInstanceCreation cic) {
		context.addImports(cic.getCreationType());
		String type = cic.getCreationType().getClassName();
		print("new ").print(type);
		print("(");
		List args = new ArrayList<>();
		if (next instanceof MethodInvocation) {
			MethodInvocation inv = (MethodInvocation) next;
			compiler.addClass(inv.getMethodBinding().toString());
			args.addAll(inv.getArguments());
			next = next.getNextSibling();
		}
		args.addAll(cic.getArguments());
		generateList(args);
		print(")");
		if (isAnonymousClass(type)) {
			compiler.addClass(type);
		}
	}

	private String getSimpleName(String fullName) {
		return fullName.substring(fullName.lastIndexOf(".") + 1);
	}

	public void visit(VariableDeclaration decl) {
		if (decl.getType() instanceof ObjectType) {
			context.addImports((ObjectType) decl.getType());
		}
		if (decl.getLocation() == VariableDeclaration.LOCAL_PARAMETER) {
			print(decl.getName());
			return;
		}

		if (decl.getLocation() == VariableDeclaration.NON_LOCAL) {
			inv.invoke("class.field.decl", getOutputStream(),
					new Tuple<VariableDeclaration, TypeContext>(decl, context));
			return;
		} else {
			if (decl.getLocation() != VariableDeclaration.LOCAL)
				throw new RuntimeException("Declaration must be local");
			indent("var " + decl.getName());
		}

		if (!decl.isInitialized())
			return;

		print(" = ");

		switch (decl.getType().getType()) {
		case Constants.T_INT:
		case Constants.T_SHORT:
		case Constants.T_BYTE:
		case Constants.T_LONG:
		case Constants.T_DOUBLE:
		case Constants.T_FLOAT:
		case Constants.T_CHAR:
			print("0");
			break;
		case Constants.T_BOOLEAN:
			print("false");
			break;
		default:
			print("null");
			break;
		}
	}

	private ASTNode next;

	public void visit_(Block block) {
		depth++;
		ASTNode prev = this.next;
		try {
			next = null;
			boolean isAnonymouseConstructor = false;
			ASTNode parent = block.getParentNode();
			if (parent instanceof MethodDeclaration) {
				MethodBinding binding = ((MethodDeclaration) parent).getMethodBinding();
				if (binding.isConstructor() && binding.getDeclaringClass().getClassName().contains("$")) {
					isAnonymouseConstructor = true;
				}
			}

			ASTNode node = block.getFirstChild();
			if (isAnonymouseConstructor) {
				ASTNode next = node.getNextSibling();
				if (next != null) {
					generate(next);
				}
				generate(node);
				if (next != null) {
					node = next.getNextSibling();
				} else {
					node = null;
				}
			}
			while (node != null) {
				node = generate(node);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		depth--;
		this.next = prev;
	}

	private ASTNode generate(ASTNode node) {
		currentNode = node;
		indent();

		if (node instanceof Block && ((Block) node).isLabeled()) {
			print(((Block) node).getLabel() + ": ");
		}

		next = node.getNextSibling();
		node.visit(this);

		println(";");
		return next;
	}

	public void visit(FieldAccess fr) {
		ASTNode expression = fr.getExpression();
		if (expression == null) {
			if (project.isEnum(fr.getType())) {
				print("'").print(fr.getName()).print("'");
				return;
			}
			String className = fr.getType().getClassName();
			if (context.getType().getClassName().equals(className)) {
				print(TSHelper.getSimpleName(className));
			} else {
				context.addImports(fr.getType());
				print(className);
			}
		} else {
			expression.visit(this);
		}

		print(normalizeAccess(fr.getName()));
	}

	public void visit(InvokeDynamic node) {
		print("function(");
		if (!node.getPrams().isEmpty()) {
			print(node.getPrams().get(0).getName());
			for (int i = 1; i < node.getPrams().size(); i++) {
				print(", ");
				print(node.getPrams().get(i).getName());
			}
		}
		print(") { return ");
		node.getInvocation().visit(this);
		print(";}");
	}

	public void visit(InstanceofExpression node) {
		node.getLeftOperand().visit(this);
		print(" instanceof ");
		ObjectType rightOperand = (ObjectType) node.getRightOperand();
		context.addImports(rightOperand);
		print(rightOperand.getClassName());
	}

	public void visit(ThisExpression reference) {
		inv.invoke("this", getOutputStream(), null);
	}

	public void visit(ThrowStatement node) {
		print("throw ");
		node.getExpression().visit(this);
	}

	public void visit(CastExpression cast) {
		cast.getExpression().visit(this);
	}

	public void visit(PrimitiveCast node) {
		node.getExpression().visit(this);
	}

	public void visit(ArrayCreation ac) {
		if (ac.getDimensions().size() <= 0) {
			throw new RuntimeException("Expected array dimension > 0, but was" + ac.getDimensions().size());
		}

		if (ac.getInitializer() != null) {
			ac.getInitializer().visit(this);
		} else {
			for (int i = 0; i < ac.getDimensions().size(); i++) {
				print("[]");
			}
		}
	}

	public void visit(SwitchStatement switchStmt) {
		Expression expression = switchStmt.getExpression();

		print("switch (");
		expression.visit(this);
		println(") {");
		ASTNode node = switchStmt.getFirstChild();
		boolean hasDefault = false;
		while (node != null) {
			SwitchCase sc = (SwitchCase) node;
			sc.visit(this);
			if (sc.getExpressions().isEmpty()) {
				hasDefault = true;
			}
			node = node.getNextSibling();
		}
		if (!hasDefault) {
			ASTNode parent = switchStmt.getParentNode();
			if (parent instanceof Block && ((Block) parent).isLabeled()) {
				Block b = (Block) parent;
				indentln("default: break " + b.getLabel() + ";");
			}
		}
		indentln("}");
	}
}
