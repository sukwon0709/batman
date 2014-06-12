package edu.vanderbilt.batman.model;

import lombok.Getter;

import org.jdom.Element;

/**
 * This class is designed to model the equality relationship between two variables.
 * e.g., a query parameter is always equal to a session variable, or a web request parameter.
 * @author xli
 *
 */
public class EqualityConstraint extends Constraint {
	
	@Getter private Variable leftSideVariable;
	@Getter private Variable rightSideVariable;
	
	public EqualityConstraint(Variable left, Variable right) {
		leftSideVariable = left;
		rightSideVariable = right;
		this.setType("EqualityConstraint");
		this.setConstraint(genConstraint());
	}
	
	private String genConstraint() {
		StringBuilder builder = new StringBuilder();
		builder.append(leftSideVariable.toString());
		builder.append("==");
		builder.append(rightSideVariable.toString());
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return this.getConstraint();
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public Element genXMLElement(boolean flag) {
		Element element = new Element(this.getType());
		element.setAttribute("constraint", this.getConstraint());
		element.setAttribute("leftSideVariable", leftSideVariable.toString());
		element.setAttribute("rightSideVariable", rightSideVariable.toString());
		return element;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof EqualityConstraint) {
			EqualityConstraint constraint = (EqualityConstraint)o;
			return ((leftSideVariable.equals(constraint.getLeftSideVariable()) && rightSideVariable.equals(constraint.getRightSideVariable()))
					|| (rightSideVariable.equals(constraint.getLeftSideVariable()) && leftSideVariable.equals(constraint.getRightSideVariable())));
		}
		return false;
	}

	@Override
	public void parseXMLElement(Element element) {
		// TODO implement
		
	}
}