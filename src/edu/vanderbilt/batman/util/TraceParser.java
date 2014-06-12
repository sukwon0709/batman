package edu.vanderbilt.batman.util;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

import org.owasp.webscarab.model.HttpUrl;

import net.sf.jsqlparser.JSQLParserException;
import edu.vanderbilt.batman.model.*;

public class TraceParser {
	
	private static QueryParser _parser = new QueryParser();
	
	public static WebRequest parseWebRequest(String method, String link, String postParas) {
		WebRequest request = null;
		try {
			// Temporary fix for parsing spaces in url parameters. Should be done at RequestDeliver or Portal.
			//link = link.replaceAll(" ", "+");
			
			HttpUrl url = new HttpUrl(link);
			request = new WebRequest(method, url.getPath());
			
			//Yuan debug here
			//System.out.println("method: " + method + " link: " + link + " url: " + url.getPath() + " postParas: " + postParas +" url.getQuery(): "+ url.getQuery());
			
			//Yuan add: need discussion
			if (url.getQuery()!=null) {
				HashMap<String, String> parameters = parseRequestParameters(url.getQuery());
				for (String parameter: parameters.keySet()) {
					request.addGetParameter(parameter, parameters.get(parameter));
				}
			}

			if (postParas!=null) {
				HashMap<String, String> parameters = parseRequestParameters(postParas);
				for (String parameter: parameters.keySet()) {
					request.addPostParameter(parameter, parameters.get(parameter));
				}
			}
			
		} catch (MalformedURLException e) {
			System.err.println("Malformed URL.");
		}
		return request;
	}
	
	/*
	public static HashMap<String, String> parsePostRequestParameters(String paras) {
		
		HashMap<String, String> params = new HashMap<String, String>();
		
        //Yuan add
        if (paras == null) return params;

		StringTokenizer st = new StringTokenizer(paras, "||");
		while(st.hasMoreTokens()) {
			String para = st.nextToken();
			StringTokenizer st1 = new StringTokenizer(para, ":");
			String name = st1.nextToken();
			if(st1.hasMoreTokens()) {
				String value = st1.nextToken().trim();
				params.put(name, value);
				//System.out.println("Adding GET pair ['"+value+"'] => ['"+name+"']");
			} else {
				params.put(name, "");
			}
		}
		//System.out.println(params);
		return params;
	}
	*/
	
	
	public static HashMap<String, String> parseRequestParameters(String paras) {
        HashMap<String, String> params = new HashMap<String, String>();
        
        //Yuan add
        if (paras == null) return params;
        	
        if(paras.indexOf("?") != -1) {
			paras = paras.substring(paras.indexOf("?")+1);
		}
        
        //System.out.println(" " + paras);
        // accomodate post parameters.
        //paras = paras.replaceAll("||", "&");
        paras = paras.replaceAll(":", "=");
        //System.out.println(" " + paras);
		
		StringTokenizer st = new StringTokenizer(paras, "&");
		while(st.hasMoreTokens()) {
			String para = st.nextToken();
			StringTokenizer st1 = new StringTokenizer(para, "=");
			String name = st1.nextToken();
			if(st1.hasMoreTokens()) {
				String value = st1.nextToken().trim();
				params.put(name, value);
				//System.out.println("Adding GET pair ['"+value+"'] => ['"+name+"']");
			} else {
				params.put(name, "");
			}
		}
		//System.out.println(params);
		return params;
	}
	
	
	public static SQLQuery parseSQLQuery(String query) throws JSQLParserException {
		if (_parser.checkNonAsciiExists(query)) return null;
		if (query.equals("_")||query.equals("__")||query.equalsIgnoreCase("BEGIN;"))  return null;
		query = query.replaceAll("\"", "'");    // temp fix for JsForum app.
		return _parser.parseSQLQuery(query);
	}
	
	
	//Yuan work here --> Xiaowei double check
	public static SQLResponse parseSQLResponse(String response, SQLQuery sqlQuery){
		SQLResponse sqlResponse = new SQLResponse(sqlQuery); //Yuan change
		sqlResponse.setResponseStr(response);
		
		/* return boolean */
		if(response.contains("SUCCESS")) {
			sqlResponse.setType(SQLResponse.SQL_RESPONSE_TYPE_SUCCESS);
			return sqlResponse;	
		} else if (response.contains("FAIL")) {
			sqlResponse.setType(SQLResponse.SQL_RESPONSE_TYPE_FAIL);
			return sqlResponse;
		}
		
	
		//Yuan's new code. :-)
		//System.err.println("Yuan: query:" + sqlQuery.toString());

		String table = null;
		HashMap<Integer, String> colPosNameMap = new HashMap<Integer, String>();
		boolean queryContainColInfo = false;

		String[] split = sqlQuery.getSelectFields().elementAt(0).split("[.]");
		
		if (split[1].contains("*")){ // for SQL query such as "SELECT * from users" where sqlQuery.getSelectFields().elementAt(0) looks like "users.*"
			table = split[0]; // only take the table name here, rely SQL response for column information
			//System.out.println("table:" + table);
		} else{ //for SQL query such as "SELECT pictures.id, pictures.title, users.login from pictures, users", where  sqlQuery.getSelectFields().elementAt(i) looks like "pictures.id", "users.login"
			queryContainColInfo = true;
			for (int i=0; i<sqlQuery.getSelectFields().size(); i++){
				//System.out.println("col:" + sqlQuery.getSelectFields().elementAt(i));
				colPosNameMap.put(i, sqlQuery.getSelectFields().elementAt(i));
				sqlResponse.getValues().put(sqlQuery.getSelectFields().elementAt(i), new ArrayList<String>());
			}
		}
		
		//System.out.println("Yuan: response:" + response);
			 
		String[] row = response.split(";");
		int rowCount = 0;
		
		for (String arow : row){

			//String[] col = arow.split("\\|\\|");
			String[] col = arow.split("[|]+");
			
			if (rowCount == 0) { //firstRow has all the column names
				if (!queryContainColInfo){
					int pos =0;
					for (String element : col){ 
						if (!element.isEmpty()){ // String[] col = arow.split("[||]") gives some empty strings; get filtered out here 
							if (table ==null)
								System.err.println("table is null");
							else {
								String colName = table + "."+ element;
								colPosNameMap.put(pos, colName);
								sqlResponse.getValues().put(colName, new ArrayList<String>());
								pos ++;
							}
						}
					}
				}
			} else{ // populate the values
				int pos =0;
				HashMap<String, String> tuple = new HashMap<String, String>();
				for (String element : col){ 
					if (!element.isEmpty()){ // String[] col = arow.split("[||]") gives some empty strings; get filtered out here 
						String colname = colPosNameMap.get(pos);
						//System.out.println("colname:" + colname + " element: " + element);
						sqlResponse.getValues().get(colname).add(element);
						// add to tuples:
						tuple.put(colname, element);					
						pos ++;
					}
				}
				sqlResponse.getTuples().add(tuple);
			}
			rowCount++;
		}
		if (rowCount <= 1) {
			sqlResponse.setType(SQLResponse.SQL_RESPONSE_TYPE_ONE);
		} else {
			sqlResponse.setType(SQLResponse.SQL_RESPONSE_TYPE_MORE);
		}
		return sqlResponse;
	}
	
	
	/**
	 * 
	 * @param session
	 * @return
	 */
	public static HashMap<String, String> parseSession(String session) {
		HashMap<String, String> sessionMap = new HashMap<String, String>();
		/* check null */
		if(session==null || session.trim().equals("") || session.equals("null")) {
			return sessionMap;
		}
		
		session = sessfilter(session);
		StringTokenizer st = new StringTokenizer(session, ";");
		while (st.hasMoreTokens()){
			String var = st.nextToken();
			if (var.contains("*")){
				continue;
			}
			
			String key = var.substring(0, var.indexOf("|"));         //  Customized for dnscript, wackopicko
			String value = "";
			if (var.contains("\"")){
				value = var.substring(var.indexOf("\"")+1, var.lastIndexOf("\""));
			} else {
				value = var.substring(var.indexOf(":")+1);
			}
			if (!key.equals("") && !value.equals("")){
				sessionMap.put("session."+key, value);
				//System.out.println("Adding SESSION pair ['"+value+"'] => ['"+key+"']");
			}
		}
		return sessionMap;
	}
	
	private static String sessfilter(String session){
		Stack<Integer> positions = new Stack<Integer>();
		boolean flag = false;
		int pt = 0;
		do {
			flag = false;
			int start = 0;
			int end = 0;
			StringBuffer result = new StringBuffer();
			char[] array = session.toCharArray();
			for (int i = pt; i < array.length; i++){
				if (array[i] == '{') {
					positions.push(i);
				} else if (array[i] == '}'){
					start = positions.pop();
					end = i;
					pt = start;
					flag = true;
					break;
				}
			}
			if (flag){
				result.append(session.substring(0, start));
				result.append("*;");
				result.append(session.substring(end+1));
				session = result.toString();
				//System.out.println(session.length() + " " + pt + " " + session);
			}
		} while (flag);
		return session;
	}
}
