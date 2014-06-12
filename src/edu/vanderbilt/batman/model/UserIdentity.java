package edu.vanderbilt.batman.model;

import org.jdom.Element;

import lombok.Getter;
import lombok.Setter;

public class UserIdentity {
	
	@Getter private final UserRole role;
	@Getter private final String userId;
	
	@Getter @Setter private String username = null;
	@Getter @Setter private String password = null;
	
	public UserIdentity(UserRole r, String id) {
		role = r;
		userId = id;
	}
	
	public UserIdentity(UserIdentity identity) {
		role = identity.getRole();
		userId = identity.getUserId();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof UserIdentity) {
			//YUan change
			return (this.getRole().equals(((UserIdentity)o).getRole())) && userId.equals(((UserIdentity)o).getUserId());
		}
		return false;
	}
	
	@Override
	public int hashCode (){
		return 0;
	}
	
	public Element genXMLElement() {
		Element element = new Element("UserIdentity");
		element.setAttribute("role", Integer.toString(role.getRole()));
		element.setAttribute("userId", userId);
		if (username != null) {
			element.setAttribute("username", username);
		}
		if (password != null) {
			element.setAttribute("password", password);
		}
		return element;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(" role=" + role.getRole());
		builder.append(" userId=" + userId);
		builder.append(" username=" + username);
		builder.append(" password=" + password);
		return builder.toString();
	}

}
