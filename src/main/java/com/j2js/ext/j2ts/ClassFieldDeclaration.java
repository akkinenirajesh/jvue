package com.j2js.ext.j2ts;

import java.io.PrintStream;

import org.apache.bcel.Constants;

import com.j2js.dom.VariableDeclaration;
import com.j2js.ext.ExtChain;
import com.j2js.ext.ExtInvocation;

public class ClassFieldDeclaration implements ExtInvocation<VariableDeclaration> {

	@Override
	public void invoke(PrintStream ps, VariableDeclaration input, ExtChain ch) {
		ps.print(input.getName());
		if (!input.isInitialized())
			return;

		ps.print(" = ");

		switch (input.getType().getType()) {
		case Constants.T_INT:
		case Constants.T_SHORT:
		case Constants.T_BYTE:
		case Constants.T_LONG:
		case Constants.T_DOUBLE:
		case Constants.T_FLOAT:
		case Constants.T_CHAR:
			ps.print("0");
			break;
		case Constants.T_BOOLEAN:
			ps.print("false");
			break;
		default:
			ps.print("null");
			break;
		}

		ch.next(ps, input);

	}

}
