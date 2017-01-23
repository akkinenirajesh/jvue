package com.j2js.dom;

import com.j2js.visitors.AbstractVisitor;

/**
 * Class representing the union of pre- and post-fix expression.
 * @author kuehn
 */
public class PStarExpression extends Expression {
	
	// Operators common to both pre- and post-fix expression.
	static public Operator INCREMENT = new Operator("++");
    static public Operator DECREMENT = new Operator("--");
    
    static public class Operator {
        
        private String token;
        
        Operator(String theToken) {
            token = theToken;
        }
        
        public String toString() {
            return token;
        }
        
        public Operator complement() {
            if (this == PStarExpression.INCREMENT) return PStarExpression.DECREMENT;
            else if (this == PStarExpression.DECREMENT) return PStarExpression.INCREMENT;
            else throw new RuntimeException("Operation not supported for " + this);
        }
        
    }
    
    private ASTNode operand;
	private Operator operator;
	
	public void visit(AbstractVisitor visitor) {
	    visitor.visit(this);
    }
	
	/**
	 * @return Returns the operand.
	 */
	public ASTNode getOperand() {
		return operand;
	}

	/**
	 * @param leftOperand The leftOperand to set.
	 */
	public void setOperand(ASTNode theOperand) {
		widen(theOperand);
		operand = theOperand;
	}

    /**
     * @return Returns the operator.
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * @param theOperator The operator to set.
     */
    public void setOperator(Operator theOperator) {
        operator = theOperator;
    }
}

