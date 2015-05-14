package edu.vanderbilt.batman.model;

import java.util.HashSet;
import java.util.Set;

import org.jdom.Element;

import lombok.Getter;


public class RoleAuthPolicy {
	
	@Getter private final UserRole role;
	@Getter private final Set<SQLOperation> privileges;    // a role is associated with a set of privileges.

    /**
     * RoleAuthPolicy associates a set of SQLOperations to a Role.
     *
     * @param r
     */
	public RoleAuthPolicy(UserRole r) {
		role = new UserRole(r);
		privileges = new HashSet<SQLOperation>();
	}
	
	public void addPrivilege(SQLOperation operation) {
		privileges.add(operation);
	}
	
	public void addPrivileges(Set<SQLOperation> operations) {
		privileges.addAll(operations);
	}
	
	public Element genXMLElement() {
		Element element = new Element("RoleAuthPolicy");
		element.setAttribute("Role", Integer.toString(role.getRole()));
		for (SQLOperation operation: privileges) {
			element.addContent(operation.genXMLElement());
		}
		return element;
	}
	
	public void parseXMLElement(Element element) {
		for (Object s: element.getContent()) {
			if (!(s instanceof Element)) continue;
			Element op = (Element) s;
			String query = op.getAttributeValue("querySkeleton");
			SQLOperation operation = new SQLOperation(query);
			operation.parseXMLElement(op);
			operation.genOperationSignature();
			privileges.add(operation);
		}
	}

}
