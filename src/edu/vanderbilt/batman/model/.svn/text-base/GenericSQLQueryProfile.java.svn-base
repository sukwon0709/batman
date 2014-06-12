package edu.vanderbilt.batman.model;

import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;

import lombok.Getter;

public class GenericSQLQueryProfile {
	
	@Getter private String querySkeleton;
	@Getter private String requestKey;
	
	public GenericSQLQueryProfile(String query, String key) {
		querySkeleton = query;
		requestKey = key;
	}
	
	
	@Getter private Set<PropagationPathConstraint> parameterPropPaths = new HashSet<PropagationPathConstraint>();
	@Getter private Set<UserSpecificVariableConstraint> specificValueConsts = new HashSet<UserSpecificVariableConstraint>();
	@Getter private Set<UserOwnershipConstraint> ownershipConsts = new HashSet<UserOwnershipConstraint>();
	
	
	public void loadXMLElementFromQueryConstraints(Element element) {
		
		for (Object o: element.getContent()) {
			if (!(o instanceof Element)) continue;
			Element c = (Element) o;
			String name = c.getName();
			
			if (name.equals("QueryParameterEqualityConstraints")) {	
				for (Object t: c.getContent()) {
					if (!(t instanceof Element)) continue;
					Element constElement = (Element) t;
					String variable = constElement.getAttributeValue("leftVariable");
					
					UserSpecificVariableConstraint constraint = new UserSpecificVariableConstraint(new Variable(querySkeleton, variable));
					specificValueConsts.add(constraint);
				}
			} else if (name.equals("PropagationPaths")) {
				for (Object t: c.getContent()) {
					if (!(t instanceof Element)) continue;
					Element constElement = (Element) t;
					String queryVar = constElement.getAttributeValue("leftVariable");
					String requestVar = constElement.getAttributeValue("rightVariable");
					requestVar = requestVar.substring(requestVar.indexOf("php") + 4);
					Variable queryVariable = new Variable(querySkeleton, queryVar);
					Variable requestVariable = new Variable(requestKey, requestVar);
					
					PropagationPathConstraint constraint = new PropagationPathConstraint(requestVariable, queryVariable);
					parameterPropPaths.add(constraint);
				}
			} else if (name.equals("MembershipConstraints")) {
				for (Object t: c.getContent()) {
					if (!(t instanceof Element)) continue;
					Element constElement = (Element) t;
					String pVar = constElement.getAttributeValue("parentVariable");
					String cVar = constElement.getAttributeValue("childVariable");
					String[] st = pVar.split("[\\[\\]]");
					Variable pVariable = new Variable(st[1], st[3]);
					Variable cVariable = new Variable(querySkeleton, cVar);
					
					UserOwnershipConstraint constraint = new UserOwnershipConstraint(pVariable, cVariable);
					ownershipConsts.add(constraint);
				}
			}
		}
	}
	
	
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof GenericSQLQueryProfile) {
			GenericSQLQueryProfile c = (GenericSQLQueryProfile)o;
			return querySkeleton.equals(c.getQuerySkeleton()) && requestKey.equals(c.getRequestKey());
		}
		return false;
	}

}
