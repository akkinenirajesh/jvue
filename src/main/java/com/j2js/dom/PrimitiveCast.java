package com.j2js.dom;

import org.apache.bcel.generic.Type;

import com.j2js.visitors.AbstractVisitor;

public class PrimitiveCast extends Expression {
    public int castType = 0;
    public Expression expression;
    
    public PrimitiveCast(int theCastType, Expression expr, Type typeBinding) {
        super();
        type = typeBinding;
        castType = theCastType;
        expression = expr;
    }
    
    public void visit(AbstractVisitor visitor) {
        visitor.visit(this);
    }

    public Expression getExpression() {
        return expression;
    }
}
