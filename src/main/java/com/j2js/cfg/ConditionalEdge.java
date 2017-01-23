package com.j2js.cfg;

import com.j2js.dom.BooleanExpression;

public class ConditionalEdge extends Edge {
    
    private BooleanExpression expression;
    private boolean negate = false;
    
    ConditionalEdge(Graph graph, Node theSource, Node theTarget) {
        super(graph, theSource, theTarget);
    }
    
    public BooleanExpression getBooleanExpression() {
        return expression;
    }
    
    public void setBooleanExpression(BooleanExpression expr) {
        expression = expr;
    }

    public boolean isNegate() {
        return negate;
    }

    public void setNegate(boolean theNegate) {
        negate = theNegate;
    }
}
