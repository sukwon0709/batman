package edu.vanderbilt.batman.model;

import lombok.Getter;

import org.jdom.Element;

public class UserOwnershipConstraint extends Constraint {
	
	@Getter public Variable ownerVariable;
	@Getter public Variable memberVariable;
	
	public UserOwnershipConstraint(){
		this.setType("UserOwnershipConstraint");
	}
	
	public UserOwnershipConstraint(Variable owner, Variable member) {
		ownerVariable = owner;
		memberVariable = member;
		this.setType("UserOwnershipConstraint");
		this.setConstraint(genConstraint());
	}
	
	private String genConstraint() {
		StringBuilder builder = new StringBuilder();
		builder.append(ownerVariable.toString());
		builder.append(">>");
		builder.append(memberVariable.toString());
		return builder.toString();
	}

	@Override
	public Element genXMLElement(boolean flag) {	
		Element element = new Element(this.getType());
		element.setAttribute("constraint", this.getConstraint());
		element.setAttribute("ownerVariable", ownerVariable.toString());
		element.setAttribute("memberVariable", memberVariable.toString());
		
		if (flag) {   // output values for testing.
			for (String value: memberVariable.getValues()) {
				Element valueElement = new Element("memberVariableValue");
				valueElement.setAttribute("value", value);
				element.addContent(valueElement);
			}
		}
		return element;
	}
	
	public void parseXMLElement(Element element) {
		ownerVariable = new Variable(element.getAttributeValue("ownerVariable"));
		memberVariable = new Variable(element.getAttributeValue("memberVariable"));
		this.setConstraint(element.getAttributeValue("constraint"));
		
		for (Object obj: element.getContent()) {
			if (!(obj instanceof Element)) continue;
			Element value = (Element) obj;
			memberVariable.addValue(value.getAttributeValue("value"));
		}		
	}
	
	//Yuan: is this ever called? 
	@Override
	public boolean equals(Object o) {
		if (o instanceof UserOwnershipConstraint) {
			UserOwnershipConstraint c = (UserOwnershipConstraint) o;
			return this.getConstraint().equals(c.getConstraint());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public String toString() {
		return this.getConstraint();
 	}
}
