package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

public class CompareExpression extends Expression {

	public CompareExpression() {
	}

	/**
	 * @return Returns the leftOperand.
	 */
	public Expression getLeftOperand() {
		return (Expression) getChildAt(0);
	}

	/**
	 * @param leftOperand
	 *            The leftOperand to set.
	 */
	public void setOperands(Expression leftOperand, Expression rightOperand) {
		widen(leftOperand);
		widen(rightOperand);
		removeChildren();
		appendChild(leftOperand);
		appendChild(rightOperand);
	}

	/**
	 * @return Returns the rightOperand.
	 */
	public Expression getRightOperand() {
		return (Expression) getChildAt(1);
	}

	@Override
	public void visit(AbstractVisitor visitor) {
		throw new RuntimeException("This expression needs to convert into operators like <, >, ==");
	}
}
