package edu.vanderbilt.batman.model;

import lombok.Getter;

import org.jdom.Element;

public class PropagationPathConstraint extends Constraint implements Comparable<PropagationPathConstraint> {
	
	@Getter private Variable srcVariable;
	@Getter private Variable destVariable;
	
	public PropagationPathConstraint() {
		this.setType("PropagationPathConstraint");
	}
	
	public PropagationPathConstraint(Variable src, Variable dest) {
		srcVariable = src;
		destVariable = dest;
		this.setType("PropagationPathConstraint");
		this.setConstraint(genConstraint());
	}
	
	private String genConstraint() {
		StringBuilder builder = new StringBuilder();
		builder.append(srcVariable.toString());
		builder.append("->");
		builder.append(destVariable.toString());
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return this.getConstraint();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof PropagationPathConstraint) {
			PropagationPathConstraint c = (PropagationPathConstraint)o;
			return this.getConstraint().equals(c.getConstraint());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public Element genXMLElement(boolean flag) {
		Element element = new Element(this.getType());
		element.setAttribute("constraint", this.getConstraint());
		element.setAttribute("srcVariable", srcVariable.toString());
		element.setAttribute("destVariable", destVariable.toString());
		return element;
	}

	@Override
	public void parseXMLElement(Element element) {
		srcVariable = new Variable(element.getAttributeValue("srcVariable"));
		destVariable = new Variable(element.getAttributeValue("destVariable"));
		this.setConstraint(element.getAttributeValue("constraint"));
	}

	@Override
	public int compareTo(PropagationPathConstraint o) {
		return this.getConstraint().compareTo(o.getConstraint());
	}
	
	
}
