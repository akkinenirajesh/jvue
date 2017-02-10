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
	private TypeDeclaration type;

	public MethodContext(TypeDeclaration type, MethodDeclaration method, List<MethodContext> list) {
		this.type = type;
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

	public TypeDeclaration getType() {
		return type;
	}

	public List<MethodContext> getList() {
		return list;
	}
}
