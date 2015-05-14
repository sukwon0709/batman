package edu.vanderbilt.batman.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class SQLSample {

	@Getter @Setter private UserIdentity user;
	@Getter @Setter private WebRequest webRequest;
	@Getter @Setter private WebResponse webResponse;
	@Getter @Setter private SQLQuery sqlQuery;
	@Getter @Setter private SQLResponse sqlResponse;
	@Getter @Setter private List<SQLSample> samplesInPreviousInteractions;
	
	@Getter @Setter private SQLOperation sqlOperation = null;   // attribute

    /**
     * SQL Sample just as described in the paper.
     * Quote: "Each SQL Sample is retrieved from the traces by including a pair of SQL query
     * and response, as well as the web request and response in the interaction.
     *
     * @param u
     * @param request
     * @param response
     * @param query
     * @param sqlResp
     */
	public SQLSample(UserIdentity u, WebRequest request, WebResponse response, SQLQuery query, SQLResponse sqlResp) {
		user = u;
		webRequest = request;
		webResponse = response;
		sqlQuery = query;
		sqlResponse = sqlResp;
		samplesInPreviousInteractions = new ArrayList<SQLSample>();
	}
	
	public void addSamplesInPreviousInteractions(List<SQLSample> samples) {
		samplesInPreviousInteractions.addAll(samples);
	}
	
	public boolean isSQLOperationSet() {
		return sqlOperation != null;
	}

}
