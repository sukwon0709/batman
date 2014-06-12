package edu.vanderbilt.batman.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class SQLResponse {
	public final static int SQL_RESPONSE_TYPE_BOOLEAN = 0;		// returned by INSERT, UPDATE, DELETE, etc.
	public final static int SQL_RESPONSE_TYPE_ONE = 1;          // returned by SELECT.
	public final static int SQL_RESPONSE_TYPE_MORE = 2;
	public final static int SQL_RESPONSE_TYPE_UNSPECIFIED = -1;
	public final static int SQL_RESPONSE_TYPE_SUCCESS = 3;
	public final static int SQL_RESPONSE_TYPE_FAIL = 4;
	
	@Getter @Setter private int type;      // 0: boolean; 1: 0-1 row; 2: more than one row.
	@Getter private HashMap<String, List<String>> values;
	// added by xiaowei 05/04
	@Getter private List<HashMap<String, String>> tuples;
	@Getter @Setter private SQLQuery sqlquery;//Yuan add
	
	@Getter @Setter private String responseStr = null;
	
	//Yuan add:
	public SQLResponse(SQLQuery query){
		type = SQLResponse.SQL_RESPONSE_TYPE_UNSPECIFIED;
		values = new HashMap<String, List<String>>();
		tuples = new ArrayList<HashMap<String, String>>();
		sqlquery = new SQLQuery(query);
	}
	
	public SQLResponse(SQLResponse response) {  // deep copy.
		type = response.getType();
		values = new HashMap<String, List<String>>(response.getValues());
		tuples = new ArrayList<HashMap<String, String>>(response.getTuples());
		sqlquery = new SQLQuery(response.getSqlquery());
	}
	
	public SQLResponse(String result) {
		responseStr = result;
	}
	
	/*Yuan comment out
	public SQLResponse(){
		type = SQL_RESPONSE_TYPE_UNSPECIFIED;
		values = new HashMap<String, List<String>>();
	}
	*/
	
	public void clear(){
		type = SQL_RESPONSE_TYPE_UNSPECIFIED;
		values.clear();
		tuples.clear();
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[RESPONSE][" + responseStr + "]\n");
		return builder.toString();
	}
}
