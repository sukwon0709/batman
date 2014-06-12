package edu.vanderbilt.batman.model;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.jdom.Element;

import lombok.Getter;

public class SQLOperation {
	
	@Getter private final String querySkeleton;
	@Getter private Set<PropagationPathConstraint> attributes;
	@Getter private boolean defined;
	private String operationSignature;
	
	@Getter private Set<String> requestKeys = new HashSet<String>();
	
	// represent parameter constraints attached to operation, including parameter & response prop path, specific value, membership constraint	
	@Getter private Set<PropagationPathConstraint> parameterPropPaths = new HashSet<PropagationPathConstraint>();
	@Getter private Set<UserSpecificVariableConstraint> specificValueConsts = new HashSet<UserSpecificVariableConstraint>();
	@Getter private Set<UserOwnershipConstraint> ownershipConsts = new HashSet<UserOwnershipConstraint>();
	
	public SQLOperation(String query) {
		querySkeleton = query;
		attributes = new TreeSet<PropagationPathConstraint>();  // ordered set.
		defined = false;
	}
	
	public SQLOperation(SQLOperation op) {
		querySkeleton = op.getQuerySkeleton();
		attributes = new TreeSet<PropagationPathConstraint>(op.getAttributes());
		defined = op.defined;
		parameterPropPaths = new HashSet<PropagationPathConstraint>(op.getParameterPropPaths());
		specificValueConsts = new HashSet<UserSpecificVariableConstraint>(op.getSpecificValueConsts());
		ownershipConsts = new HashSet<UserOwnershipConstraint>(op.getOwnershipConsts());
		requestKeys  = new HashSet<String>(op.getRequestKeys());
	}
	
	public void addAttributes(Set<PropagationPathConstraint> consts) {
		if (!defined) {
			attributes.addAll(consts);
		} else {
			System.err.println("Fail to add attributes due to operation already defined!");
		}
	}
	
	public String getOperationSignature() {
		if (!defined) {
			System.err.println("This operation hasn't been defined!");
			return null;
		} 
		return operationSignature;
	}
	
	public void genOperationSignature() {
		StringBuilder builder = new StringBuilder();
		builder.append("<" + querySkeleton + ">");
		for (Constraint constraint: attributes) {
			builder.append("<" + constraint.getConstraint() + ">");
		}
		operationSignature = builder.toString();
		defined = true;           // when this function is called, this operation becomes immutable/defined.
	}
	
	public void addParameterPropPaths(Set<PropagationPathConstraint> consts) {
		parameterPropPaths.addAll(consts);
	}
	
	public void addSpecificValueConsts(Set<UserSpecificVariableConstraint> consts) {
		specificValueConsts.addAll(consts);
	}
	
	public void addOwnershipConsts(Set<UserOwnershipConstraint> consts) {
		ownershipConsts.addAll(consts);
	}
	
	public void addRequestKey(String key) {
		requestKeys.add(key);
	}
	
	public Set<Variable> getAttributeSources() {
		Set<Variable> sources = new HashSet<Variable>();
		for (PropagationPathConstraint c: attributes) {
			sources.add(c.getSrcVariable());
		}
		return sources;
	}
	
	@Override
	public String toString() {
		return operationSignature;  // TODO: add other constraints.
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof SQLOperation) {
			SQLOperation operation = (SQLOperation)o;
			return (operationSignature.equals(operation.getOperationSignature()));
		}
		return false;
	}
	
	/*
	 * need to be overriden. containsKey() method calls hashCode first, then equals.
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 0;
	}
	
	
	public Element genXMLElement() {
		Element element = new Element("SQLOperation");
		element.setAttribute("querySkeleton", querySkeleton);
		Element attributeElement = new Element("Attributes");
		for (PropagationPathConstraint constraint: attributes) {
			attributeElement.addContent(constraint.genXMLElement(false));
		}
		element.addContent(attributeElement);
		
		Element parameterPropPathElement = new Element("ParameterPropPaths");
		for (PropagationPathConstraint constraint: parameterPropPaths) {
			parameterPropPathElement.addContent(constraint.genXMLElement(false));
		}
		element.addContent(parameterPropPathElement);
		
		Element specificValueConstElement = new Element("SpecificValueConsts");
		for (UserSpecificVariableConstraint constraint: specificValueConsts) {
			specificValueConstElement.addContent(constraint.genXMLElement(false));
		}
		element.addContent(specificValueConstElement);
		
		Element ownershipElement = new Element("OwnershipConsts");
		for (UserOwnershipConstraint constraint: ownershipConsts) {
			ownershipElement.addContent(constraint.genXMLElement(false));
		}
		element.addContent(ownershipElement);
		
		Element requestkeyElement = new Element("RequestKeys");
		for (String key: requestKeys) {
			Element e = new Element("key");
			e.setAttribute("key", key);
			requestkeyElement.addContent(e);
		}
		element.addContent(requestkeyElement);
		return element;
	}
	
	public void parseXMLElement(Element element) {
		for (Object o: element.getContent()) {
			if (!(o instanceof Element)) continue;
			Element ele = (Element) o;
			String name = ele.getName();
			if (name.equals("Attributes")) {
				for (Object obj: ele.getContent()) {
					if (!(obj instanceof Element)) continue;
					Element attr= (Element) obj;
					PropagationPathConstraint attribute = new PropagationPathConstraint();
					attribute.parseXMLElement(attr);
					attributes.add(attribute);
				}
			} else if (name.equals("ParameterPropPaths")) {
				for (Object obj: ele.getContent()) {
					if (!(obj instanceof Element)) continue;
					Element attr= (Element) obj;
					PropagationPathConstraint parameterPropPath = new PropagationPathConstraint();
					parameterPropPath.parseXMLElement(attr);
					parameterPropPaths.add(parameterPropPath);
				}
			} else if (name.equals("SpecificValueConsts")) {
				for (Object obj: ele.getContent()) {
					if (!(obj instanceof Element)) continue;
					Element attr= (Element) obj;
					UserSpecificVariableConstraint specificValueConst = new UserSpecificVariableConstraint();
					specificValueConst.parseXMLElement(attr);
					specificValueConsts.add(specificValueConst);
				}
			} else if (name.equals("OwnershipConsts")) {
				for (Object obj: ele.getContent()) {
					if (!(obj instanceof Element)) continue;
					Element attr= (Element) obj;
					UserOwnershipConstraint ownershipConst = new UserOwnershipConstraint();
					ownershipConst.parseXMLElement(attr);
					ownershipConsts.add(ownershipConst);
				}
			} else if (name.equals("RequestKeys")) {
				for (Object obj: ele.getContent()) {
					if (!(obj instanceof Element)) continue;
					Element attr= (Element) obj;
					String key = attr.getAttributeValue("key");
					requestKeys.add(key);
				}
			}
		}
	}
	
}
