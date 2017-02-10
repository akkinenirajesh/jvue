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

	public MethodContext(TypeContext typeContext, MethodDeclaration method, List<MethodContext> list) {
		this.typeContext = typeContext;
		this.method = method;
		this.list = list;
		this.params = new TSPrintStream();
		this.body = new TSPrintStream();
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
}
