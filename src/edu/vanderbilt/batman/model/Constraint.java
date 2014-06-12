package edu.vanderbilt.batman.model;

import org.jdom.Element;

import lombok.Getter;
import lombok.Setter;

public abstract class Constraint {
	
	@Getter @Setter private String type;
	
	@Getter @Setter private String constraint;     // string representation.
	
	public abstract Element genXMLElement(boolean flag);
	
	public abstract void parseXMLElement(Element element);

}
