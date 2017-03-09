package com.j2js.ts;

import java.util.List;

import com.j2js.dom.MethodDeclaration;
import com.j2js.dom.TypeDeclaration;
import com.j2js.ts.TypeContext.TSPrintStream;

public class MethodContext {
	private MethodDeclaration method;
	private TSPrintStream params;
	private TSPrintStream body;
	private List<MethodContext> list;
	private TypeContext typeContext;
	private int access;

	public MethodContext(TypeContext typeContext, MethodDeclaration method, List<MethodContext> list) {
		this.typeContext = typeContext;
		this.method = method;
		this.list = list;
		this.params = new TSPrintStream();
		this.body = new TSPrintStream();
		if (method != null) {
			this.access = method.getAccess();
		}
	}

	public void setAccess(int access) {
		this.access = access;
	}

	public void addAccess(int access) {
		this.access |= access;
	}

	public MethodDeclaration getMethod() {
		return method;
	}

	public TSPrintStream getBody() {
		return body;
	}

	public TSPrintStream getParams() {
		return params;
	}

	public TypeContext getTypeContext() {
		return typeContext;
	}

	public TypeDeclaration getType() {
		return typeContext.getType();
	}

	public List<MethodContext> getList() {
		return list;
	}

	public int getAccess() {
		return access;
	}
}
