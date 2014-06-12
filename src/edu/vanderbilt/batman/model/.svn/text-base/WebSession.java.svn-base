package edu.vanderbilt.batman.model;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

/**
 * This class is designed to hold information during one web session (from login to logout).
 * @author xli
 *
 */
public class WebSession {
	
	@Getter @Setter private UserIdentity user;
	
	@Getter private List<WebInteraction> interactions = new ArrayList<WebInteraction>();
	
	public WebSession() {
	}
	
	public void addInteraction(WebInteraction interaction) {
		interactions.add(interaction);
	}

}
