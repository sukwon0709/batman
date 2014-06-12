package edu.vanderbilt.batman.model;

import lombok.Getter;

import org.jdom.Element;

/**
 * This constraint captures the parameter whose value always keeps the same.
 * @author xli
 *
 */
public class ConsistentValueConstraint extends Constraint {
	
	@Getter private Variable variable;
	@Getter private String value;
	
	public ConsistentValueConstraint(Variable p, String v) {
		variable = p;
		value = v;
		this.setType("ConsistentValueConstraint");
		this.setConstraint(genConstraint());
	}
	
	private String genConstraint() {
		StringBuilder builder = new StringBuilder();
		builder.append(variable.toString());
		builder.append("===");
		builder.append(value);
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return this.getConstraint();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConsistentValueConstraint) {
			ConsistentValueConstraint c = (ConsistentValueConstraint)o;
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
		element.setAttribute("variable", variable.toString());
		element.setAttribute("value", value);
		return element;
	}

	@Override
	public void parseXMLElement(Element element) {
		// unimplemented.
	}
}
