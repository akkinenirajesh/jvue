package com.j2js.ts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ObjectType;

import com.j2js.J2JSSettings;
import com.j2js.assembly.Project;
import com.j2js.dom.ASTNode;
import com.j2js.dom.Block;
import com.j2js.dom.CastExpression;
import com.j2js.dom.ClassInstanceCreation;
import com.j2js.dom.FieldAccess;
import com.j2js.dom.FieldRead;
import com.j2js.dom.InstanceofExpression;
import com.j2js.dom.InvokeDynamic;
import com.j2js.dom.MethodBinding;
import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.PrimitiveCast;
import com.j2js.dom.ReturnStatement;
import com.j2js.dom.ThisExpression;
import com.j2js.dom.ThrowStatement;
import com.j2js.dom.TypeDeclaration;
import com.j2js.dom.VariableBinding;
import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtRegistry;
import com.j2js.visitors.JavaScriptGenerator;

public class TypeScriptGenerator extends JavaScriptGenerator {

	private J2TSCompiler compiler;

	private TypeContext context;
	private PkgContext project;

	public TypeScriptGenerator(J2TSCompiler compiler) {
		super(Project.getSingleton());
		this.compiler = compiler;
		this.project = new PkgContext();
	}

	public void writeToFile() {
		if (J2JSSettings.singleFile) {
			File file = new File(compiler.getBasedir(), "out." + J2JSSettings.ext);
			try {
				PrintStream ps = new PrintStream(new FileOutputStream(file));
				ExtRegistry.get().invoke("file.create", ps, null);
				project.write(ps);
				ps.flush();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			project.write(compiler.getBasedir());
		}
	}

	private void setStream(TypeDeclaration type) {
		this.context = project.get(type);
	}

	public void visit(TypeDeclaration type) {
		setStream(type);
		typeDecl = type;

		depth++;

		if (type.hasSuperClass()) {
			context.addImports(type.getSuperType());
		}

		setOutputStream(context.getFieldsStream());

		List<VariableDeclaration> fields = type.getFields();
		ExtRegistry.get().reduce("fields", fields, decl -> {
			indent();
			decl.visit(this);
			println(";");
		});

		setOutputStream(null);

		MethodDeclaration[] methods = type.getMethods();
		ExtRegistry.get().reduce("methods", methods, m -> {
			m.visit(this);
		});

		depth--;
	}

	public void visit(MethodDeclaration method) {
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

		print(") {");
		setOutputStream(mc.getBody());
		// Generate local variable declarations.
		for (VariableDeclaration decl : method.getLocalVariables()) {
			indent();
			decl.visit(this);
			println(";");
		}

		visit_(method.getBody());
		setOutputStream(null);
	}

	private void addFieldAccessor(MethodDeclaration method) {
		MethodBinding binding = method.getMethodBinding();
		Block body = method.getBody();
		ASTNode node = body.getFirstChild();
		ReturnStatement ret = (ReturnStatement) node;
		FieldRead field = (FieldRead) ret.getExpression();
		String name = field.getName();
		compiler.addFieldAccessor(binding.toString(), normalizeAccess(name));
	}

	private boolean isFieldAccessor(String name) {
		return name.startsWith("access$");
	}

	public void visit(MethodInvocation invocation) {
		ASTNode expression = invocation.getExpression();
		MethodBinding methodBinding = invocation.getMethodBinding();
		if (invocation.isSpecial) {
			if (isAnonymousClass(invocation.getMethodDecl().toString())) {
				ExtRegistry.get().invoke("super", getOutputStream(), null);
			} else if (!typeDecl.hasSuperClass() && methodBinding.isConstructor()) {
				return;
			} else {
				if (methodBinding.isConstructor()) {
					ExtRegistry.get().invoke("super", getOutputStream(), null);
					print(".constructor0");
				} else {
					ExtRegistry.get().invoke("super", getOutputStream(), null);
				}
			}
		} else if (expression != null) {
			expression.visit(this);
			print(".");
		}
		String name = methodBinding.getName();
		if (isFieldAccessor(name)) {
			String code = compiler.getFieldAccessor(methodBinding.toString());
			((ASTNode) invocation.getArguments().get(0)).visit(this);
			print(code);
			return;
		} else if (expression == null) {
			context.addImports(methodBinding.getDeclaringClass());
			// Static invocation
			print(getSimpleName(methodBinding.getDeclaringClass().getClassName())).print(".");
		}
		if (!methodBinding.isConstructor()) {
			if (invocation.isSpecial) {
				print(".");
			}
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
		print("new ").print(getSimpleName(type));
		print("(");
		List args = new ArrayList<>();
		if (next instanceof MethodInvocation) {
			args.addAll(((MethodInvocation) next).getArguments());
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
		if (decl.getLocation() == VariableDeclaration.LOCAL_PARAMETER) {
			print(decl.getName());
			return;
		}

		if (decl.getLocation() == VariableDeclaration.NON_LOCAL) {
			ExtRegistry.get().invoke("class.field.decl", getOutputStream(), decl);
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
			generate(next);
			generate(node);
			node = next.getNextSibling();
		}
		while (node != null) {
			node = generate(node);
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
			context.addImports(fr.getType());
			print(TSHelper.getSimpleName(fr.getType().getClassName()));
		} else if (expression instanceof ThisExpression) {
			expression.visit(this);
		} else if (expression instanceof VariableBinding) {
			expression.visit(this);
		}

		print(normalizeAccess(fr.getName()));
	}

	public void visit(InvokeDynamic node) {
		print("(");
		if (!node.getPrams().isEmpty()) {
			print(node.getPrams().get(0).getName());
			for (int i = 1; i < node.getPrams().size(); i++) {
				print(", ");
				print(node.getPrams().get(i).getName());
			}
		}
		print(") => {");
		node.getInvocation().visit(this);
		print("}");
	}

	public void visit(InstanceofExpression node) {
		node.getLeftOperand().visit(this);
		print(" instanceof ");
		ObjectType rightOperand = (ObjectType) node.getRightOperand();
		context.addImports(rightOperand);
		print(TSHelper.getSimpleName(rightOperand.getClassName()));
	}

	public void visit(ThisExpression reference) {
		ExtRegistry.get().invoke("this", getOutputStream(), null);
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
}
