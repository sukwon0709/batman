package edu.vanderbilt.batman.model;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

/** 
 * This class is designed to hold information for one HTTP interaction (starting from web request to web response).
 * @author xli
 * @author xue 
 *
 */
public class WebInteraction {
	@Getter @Setter private UserIdentity user = null;
	
	@Getter private final WebRequest request;
	@Getter private WebResponse response = null;
	@Getter private List<WebInteraction> previousInteractions = new ArrayList<WebInteraction>();    // historical interactions.
	
	@Getter private ArrayList<SQLQueryResponsePair> sqlQueryResponsePairs = new ArrayList<SQLQueryResponsePair>(); //list of SQLQuery/Response pair.

    /**
     * Interaction as described in the paper.
	 * Precisely they are one request/response pair + 0 or more SQL query/response pairs.
     *
     * @param r
     */
	public WebInteraction(WebRequest r) {
		request = new WebRequest(r);
	}
	
	public void addSQLQueryResponsePair(SQLQueryResponsePair pair) {
		sqlQueryResponsePairs.add(pair);
	}
	
	public void addPreviousInteractions(List<WebInteraction> interations) {
		previousInteractions.addAll(interations);
	}
	
	public void setWebResponse(WebResponse resp) {
		response = new WebResponse(resp);                  //deep copy.
	}
}