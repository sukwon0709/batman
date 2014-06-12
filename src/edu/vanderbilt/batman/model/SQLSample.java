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
