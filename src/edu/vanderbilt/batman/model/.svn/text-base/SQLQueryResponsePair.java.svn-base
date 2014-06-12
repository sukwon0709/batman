package edu.vanderbilt.batman.model;

import edu.vanderbilt.batman.model.SQLQuery;
import edu.vanderbilt.batman.model.SQLResponse;

import lombok.Getter;
import lombok.Setter;

/**
 * This class is to model a pair of sql query/response.
 * @author xli
 *
 */

public class SQLQueryResponsePair {

	@Getter private final SQLQuery sqlQuery;    //indexing query
	@Getter @Setter private SQLResponse sqlResponse = null; // corresponding response.
	
	
	
	public SQLQueryResponsePair(SQLQuery query) {
		sqlQuery = new SQLQuery(query);
	}
	
	public SQLQueryResponsePair(SQLQueryResponsePair qcon) {
		sqlQuery= new SQLQuery(qcon.sqlQuery);   // deep copy.
		sqlResponse = new SQLResponse(qcon.sqlResponse);
	}
}

