package com.j2js.visitors;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import com.j2js.J2JSSettings;
import com.j2js.Log;
import com.j2js.dom.TypeDeclaration;

public abstract class Generator extends AbstractVisitor {

	final String whiteSpace;

	protected int depth;

	char lastChar;

	String[] indents;

	private PrintStream stream;

	protected TypeDeclaration typeDecl;

	public Generator() {
		if (J2JSSettings.compression) {
			whiteSpace = "";
		} else {
			whiteSpace = " ";
		}
	}

	public PrintStream getOutputStream() {
		return stream;
	}

	public void setOutputPath(String path) throws FileNotFoundException {
		Log.getLogger().info("Destination file is " + path);
		setOutputStream(new PrintStream(new FileOutputStream(path)));
	}

	public void setOutputStream(PrintStream theStream) {
		stream = theStream;
	}

	public void flush() {
		stream.flush();
	}

	public void indent() {
		// No indentation if compression is on.
		if (J2JSSettings.compression)
			return;
		String INDENT = "\t";
		if (indents == null || depth >= indents.length) {
			indents = new String[2 * depth];
			indents[0] = "";
			for (int i = 1; i < indents.length; i++) {
				indents[i] = indents[i - 1] + INDENT;
			}
		}
		print(indents[depth]);
	}

	public Generator print(String s) {
		stream.print(s);
		if (s.length() > 0)
			lastChar = s.charAt(s.length() - 1);
		return this;
	}

	public void println(String s) {
		print(s);
		stream.println("");
	}

	public void indentln(String s) {
		indent();
		println(s);
	}

	public Generator indent(String s) {
		indent();
		print(s);
		return this;
	}

	public void incDepth(int l) {
		depth += l;
	}

	public void decDepth(int l) {
		depth -= l;
	}

	public void incDepth() {
		depth++;
	}

	public void decDepth() {
		depth--;
	}
}
