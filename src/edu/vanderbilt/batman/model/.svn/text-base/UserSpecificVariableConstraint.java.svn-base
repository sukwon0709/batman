package edu.vanderbilt.batman.model;

import lombok.Getter;

import org.jdom.Element;

/**
 * This constraint captures the parameter whose value is specific to each sample. (e.g., user identifier)
 * @author xli
 *
 */
public class UserSpecificVariableConstraint extends Constraint {
	
	@Getter private Variable variable;
	
	public UserSpecificVariableConstraint() {
		this.setType("UserSpecificVariableConstraint");
	}
	
	public UserSpecificVariableConstraint(Variable var) {
		variable = var;
		this.setType("UserSpecificVariableConstraint");
		this.setConstraint(genConstraint());
	}
	
	private String genConstraint() {
		StringBuilder builder = new StringBuilder();
		builder.append(variable.toString());
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return this.getConstraint();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof UserSpecificVariableConstraint) {
			UserSpecificVariableConstraint c = (UserSpecificVariableConstraint)o;
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
		if (flag) {   // output values for testing.			
			for (String value: variable.getValues()) {
				Element valueElement = new Element("variableValue");
				valueElement.setAttribute("value", value);
				element.addContent(valueElement);
			}			
		}
		return element;
	}

	@Override
	public void parseXMLElement(Element element) {
		variable = new Variable(element.getAttributeValue("variable"));
		this.setConstraint(element.getAttributeValue("constraint"));
		
		for (Object obj: element.getContent()) {
			if (!(obj instanceof Element)) continue;
			Element value = (Element) obj;
			variable.addValue(value.getAttributeValue("value"));
		}		
	}
}
