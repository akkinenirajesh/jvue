package com.j2js.assembly;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

import com.j2js.Log;

public abstract class Unit implements Serializable {
	// Transient fields start here.
	private transient boolean isResolved = false;
	private Signature signature;
	private String data;

	private transient boolean isTainted = false;
	private static transient String[] indentPerDepth = new String[10];

	public Unit() {
	}

	abstract void write(int depth, Writer writer) throws IOException;

	String getIndent(int depth) {
		String indent = indentPerDepth[depth];
		if (indent == null) {
			indent = "";
			for (int i = 0; i < depth; i++)
				indent += '\t';
		}
		return indent;
	}

	public boolean isResolved() {
		return isResolved;
	}

	public void setResolved(boolean theIsResolved) {
		isResolved = theIsResolved;
	}

	public String toString() {
		return signature.toString();
	}

	public Signature getSignature() {
		return signature;
	}

	void setSignature(Signature theSignature) {
		signature = theSignature;
	}

	public String getData() {
		return data;
	}

	public void setData(String theData) {
		data = theData;
	}

	public boolean isTainted() {
		return isTainted;
	}

	public void setTainted() {
		if (!isTainted)
			Log.getLogger().debug("Taint " + this);
		isTainted = true;
	}

}
