package edu.vanderbilt.batman.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import edu.vanderbilt.batman.model.UserIdentity;
import edu.vanderbilt.batman.model.UserRole;
import edu.vanderbilt.batman.model.WebRequest;

/**
 * This class is designed to manage the users/roles in the application; login specific user/role into the application.
 * @author xli
 *
 */
public class UserManager {
	
	private static HashMap<UserRole, List<UserIdentity>> usersByRole = new HashMap<UserRole, List<UserIdentity>>();
	private static String login_url = null;
	private static String method = null;
	private static HashMap<String, String> formFields = new HashMap<String, String>(); // usually username, password, submit.
	
	// load the set of users and login url from file.
	public static void loadUsers(String file) throws JDOMException, IOException {
		
		SAXBuilder parser = new SAXBuilder();
		Document doc = parser.build(file);
		for (Object o: doc.getContent()) {
			Element e = (Element) o;
			if (!e.getName().equals("LoginSpec")) continue;
			
			for (Object l: e.getContent()) {
				if (!(l instanceof Element)) continue;
				Element element = (Element)l;
				if (element.getName().equals("LoginForm")) {
					method = element.getAttributeValue("method");
					login_url = element.getAttributeValue("url");
					
					for (Object p: element.getContent()) {
						if (!(p instanceof Element)) continue;
						Element parameter = (Element)p;
						formFields.put(parameter.getAttributeValue("name"), parameter.getAttributeValue("value"));
					}
					
				} else if (element.getName().equals("UserProfile")) {
					UserRole role = new UserRole(Integer.parseInt(element.getAttributeValue("role")));
					if (!usersByRole.containsKey(role)) {
						usersByRole.put(role, new ArrayList<UserIdentity>());
					}
					//String id = element.getAttributeValue("id");
					String username = element.getAttributeValue("username");
					String password = element.getAttributeValue("password");
					UserIdentity user = new UserIdentity(role, username);
					user.setPassword(password);
					user.setUsername(username);
					usersByRole.get(role).add(user);
				}
			}
		}
	}
	
	public static UserIdentity fetchUser(UserIdentity u) {
		UserRole r = u.getRole();
		if (r.getRole() == 0) {
			UserIdentity user = new UserIdentity(r, "anonymous");
			user.setUsername("anonymous");
			user.setPassword("arbitrary");
			return user;
		}
		
		List<UserIdentity> users = usersByRole.get(r);
		for (UserIdentity ur: users) {
			if (ur.getUsername().equals(u.getUserId())) {
				return ur;
			}
		}
		return null;
	}
	
	
	
	// select a user from a particular role.
	public static UserIdentity selectUser(UserRole role) {
		
		if (role.getRole() == 0) { // guest user.
			UserIdentity user = new UserIdentity(role, "anonymous");
			user.setUsername("anonymous");
			user.setPassword("arbitrary");
			return user;
		}
		
		List<UserIdentity> users = usersByRole.get(role);
		int r = (new Random()).nextInt(users.size());
		return users.get(r);
	}
	
	// login the specific user into the app.
	public static WebRequest constructUserLogicRequest(UserIdentity user) {
		
		if (user.getRole().getRole() == 0) {
			return new WebRequest("GET", login_url);    // for guest user.
		}

		WebRequest request = new WebRequest(method, login_url);
		
		for (String name: formFields.keySet()) {
			if (name.equals("username")) {
				request.addPostParameter(formFields.get(name), user.getUsername());
			} else if (name.equals("password")) {
				request.addPostParameter(formFields.get(name), user.getPassword());
			} else {
				request.addPostParameter(name, formFields.get(name));
			}
		}
		return request;
	}

}
