package com.j2js.dom;

import java.util.List;

import com.j2js.visitors.AbstractVisitor;

public class InvokeDynamic extends Expression {

	private MethodInvocation invocation;
	private List<VariableDeclaration> params;

	public InvokeDynamic(List<VariableDeclaration> params, MethodInvocation invocation) {
		this.params = params;
		this.invocation = invocation;
	}

	public MethodInvocation getInvocation() {
		return invocation;
	}

	public void visit(AbstractVisitor visitor) {
		visitor.visit(this);
	}

	public List<VariableDeclaration> getPrams() {
		return params;
	}
}
