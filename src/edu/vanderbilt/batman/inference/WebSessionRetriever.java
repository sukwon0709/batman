package edu.vanderbilt.batman.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.vanderbilt.batman.model.UserIdentity;
import edu.vanderbilt.batman.model.UserRole;
import edu.vanderbilt.batman.model.WebSession;

public class WebSessionRetriever {
	
	public static HashMap<UserRole, List<WebSession>> retrieveByRole(List<WebSession> sessions) {
		
		HashMap<UserRole, List<WebSession>> population = new HashMap<UserRole, List<WebSession>>();
		
		for (WebSession session: sessions) {
			UserRole role = new UserRole(session.getUser().getRole());
			if (!population.containsKey(role)) {
				population.put(role, new ArrayList<WebSession>());
			}
			population.get(role).add(session);
		}
		return population;
	}
	
	public static HashMap<UserIdentity, List<WebSession>> retrieveByUser(List<WebSession> sessions) {
		
		HashMap<UserIdentity, List<WebSession>> population = new HashMap<UserIdentity, List<WebSession>>();
		
		for (WebSession session: sessions) {
			UserIdentity user = session.getUser();
			if (!population.containsKey(user)) {
				population.put(user, new ArrayList<WebSession>());
			}
			population.get(user).add(session);
		}
		
		return population;
	}
}
