package com.j2js.cfg.transformation;

import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.generic.Type;

import com.j2js.cfg.Edge;
import com.j2js.cfg.Node;
import com.j2js.cfg.SwitchEdge;
import com.j2js.dom.ASTNode;
import com.j2js.dom.ArrayAccess;
import com.j2js.dom.Block;
import com.j2js.dom.Expression;
import com.j2js.dom.MethodInvocation;
import com.j2js.dom.NumberLiteral;
import com.j2js.dom.StringLiteral;
import com.j2js.dom.SwitchCase;
import com.j2js.dom.SwitchStatement;
import com.j2js.util.TypeUtils;

/**
 */
public class Switch extends Transformation {

	private List<Node> caseGroups = new ArrayList<Node>();
	private List<List<NumberLiteral>> caseGroupExpressions = new ArrayList<List<NumberLiteral>>();

	public Switch() {
	}

	public boolean applies_() {
		return header.isSwitchHeader;
	}

	private void removeFallThroughEdgesl() {
		Edge prevPotentialFallThroughEdge = null;

		for (Edge e : header.getOutEdges()) {
			if (!(e instanceof SwitchEdge))
				continue;

			SwitchEdge edge = (SwitchEdge) e;
			Node caseGroup = edge.target;

			if (prevPotentialFallThroughEdge != null && prevPotentialFallThroughEdge.target == caseGroup) {
				// This is a fall through edge.
				graph.removeEdge(prevPotentialFallThroughEdge);
			}

			prevPotentialFallThroughEdge = caseGroup.getLocalOutEdgeOrNull();
		}
	}

	void apply_() {
		removeFallThroughEdgesl();

		for (Edge e : new ArrayList<Edge>(header.getOutEdges())) {
			if (!(e instanceof SwitchEdge))
				continue;

			SwitchEdge edge = (SwitchEdge) e;
			Node caseGroup = edge.target;
			caseGroups.add(caseGroup);
			caseGroupExpressions.add(edge.expressions);
			graph.rerootOutEdges(caseGroup, newNode, true);
			graph.removeOutEdges(caseGroup);
			graph.removeInEdges(caseGroup);
			graph.removeNode(caseGroup);
		}
	}

	void rollOut_(Block block) {
		boolean isEnum = false;
		SwitchStatement switchStmt = new SwitchStatement();
		switchStmt.setExpression(header.switchExpression);
		if (switchStmt.getExpression() instanceof ArrayAccess) {
			ArrayAccess aa = (ArrayAccess) switchStmt.getExpression();
			ASTNode child = aa.getFirstChild();
			if (child instanceof MethodInvocation) {
				MethodInvocation mi = (MethodInvocation) child;
				MethodInvocation field = (MethodInvocation) mi.getNextSibling();
				Expression expression = field.getExpression();
				Type typeBinding = expression.getTypeBinding();
				String name = TypeUtils.extractClassName(typeBinding.toString());
				Class<?> cls = getEnumClass(name);
				if (cls != null && cls.isEnum()) {
					switchStmt.setEnumCls(cls);
					isEnum = true;
					switchStmt.setExpression(expression);
				}
			}
		}
		for (int i = 0; i < caseGroups.size(); i++) {
			Node scNode = caseGroups.get(i);
			SwitchCase switchCase = new SwitchCase(scNode.getInitialPc());
			List<NumberLiteral> list = caseGroupExpressions.get(i);
			if (isEnum) {
				List<StringLiteral> cases = new ArrayList<>();
				list.forEach(l -> {
					int ordinal = (int) l.getValue();
					StringLiteral lit = new StringLiteral(
							switchStmt.getEnumCls().getEnumConstants()[ordinal - 1].toString());
					cases.add(lit);
				});
				switchCase.setExpressions(cases);
			} else {
				switchCase.setExpressions(list);
			}
			switchStmt.appendChild(switchCase);

			graph.rollOut(scNode, switchCase);
		}

		block.appendChild(switchStmt);
	}

	private Class<?> getEnumClass(String type) {
		try {
			return project.fileManager.getClassLoader().loadClass(type);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public String toString() {
		String s = super.toString() + "(" + header;
		for (int i = 0; i < caseGroups.size(); i++) {
			s += ", " + caseGroups.get(i);
		}
		return s + ")";
	}
}
