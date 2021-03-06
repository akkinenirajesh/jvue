package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * @author kuehn
 */
public class InfixExpression extends Expression {

	static public class Operator {
		static public Operator CONDITIONAL_AND = new Operator("&&");
		static public Operator CONDITIONAL_OR = new Operator("||");
		static public Operator PLUS = new Operator("+");
		static public Operator MINUS = new Operator("-");
		static public Operator TIMES = new Operator("*");
		static public Operator DIVIDE = new Operator("/");
		static public Operator REMAINDER = new Operator("%");
		static public Operator XOR = new Operator("^");
		static public Operator AND = new Operator("&");
		static public Operator OR = new Operator("|");
		static public Operator EQUALS = new Operator("==");
		static public Operator NOT_EQUALS = new Operator("!=");
		static public Operator GREATER_EQUALS = new Operator(">=");
		static public Operator GREATER = new Operator(">");
		static public Operator LESS_EQUALS = new Operator("<=");
		static public Operator LESS = new Operator("<");
		static public Operator RIGHT_SHIFT_SIGNED = new Operator(">>");
		static public Operator LEFT_SHIFT = new Operator("<<");
		static public Operator RIGHT_SHIFT_UNSIGNED = new Operator(">>>");

		static {
			EQUALS.complement = NOT_EQUALS;
			NOT_EQUALS.complement = EQUALS;
			GREATER_EQUALS.complement = LESS;
			GREATER.complement = LESS_EQUALS;
			LESS_EQUALS.complement = GREATER;
			LESS.complement = GREATER_EQUALS;
			CONDITIONAL_AND.complement = CONDITIONAL_OR;
			CONDITIONAL_OR.complement = CONDITIONAL_AND;
		}

		private String token;
		private Operator complement;

		Operator(String theToken) {
			token = theToken;
		}

		public String toString() {
			return token;
		}

		/**
		 * @return Returns the complement.
		 */
		public Operator getComplement() {
			return complement;
		}
	}

	private Operator operator;

	public InfixExpression(Operator op) {
		super();
		operator = op;
		if (operator.getComplement() != null)
			type = org.apache.bcel.generic.Type.BOOLEAN;
	}

	public void visit(AbstractVisitor visitor) {
		visitor.visit(this);
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
		if (leftOperand instanceof CompareExpression) {
			CompareExpression cmp = (CompareExpression) leftOperand;
			if (NumberLiteral.isZero(rightOperand)) {
				leftOperand = cmp.getLeftOperand();
				rightOperand = cmp.getRightOperand();
			}
		}

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

	/**
	 * @return Returns the operator.
	 */
	public Operator getOperator() {
		return operator;
	}

	/**
	 * @param theOperator
	 *            The operator to set.
	 */
	public void setOperator(Operator theOperator) {
		operator = theOperator;
	}
}
