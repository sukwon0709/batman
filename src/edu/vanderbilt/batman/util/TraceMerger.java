package edu.vanderbilt.batman.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.jsqlparser.JSQLParserException;

import edu.vanderbilt.batman.model.SQLQuery;
import edu.vanderbilt.batman.model.SQLQueryResponsePair;
import edu.vanderbilt.batman.model.SQLResponse;
import edu.vanderbilt.batman.model.UserIdentity;
import edu.vanderbilt.batman.model.UserRole;
import edu.vanderbilt.batman.model.WebInteraction;
import edu.vanderbilt.batman.model.WebRequest;
import edu.vanderbilt.batman.model.WebResponse;
import edu.vanderbilt.batman.model.WebSession;

/**
 * This class is used for retrieving real traces based on index file.
 * @author xli
 *
 */
public class TraceMerger {
	
	public static ArrayList<WebSession> merge(String working_dir, String project) {
		
		String trace_dir = working_dir + project + "/";
		String index_file = trace_dir + project + ".log";   // by Portal. get user/session info from crawler.
		String sql_file = trace_dir + project + "_sql_log";   // by mysql-proxy. cannot differentiate users/sessions.
		String log_dir = null;      // by webscarab proxy.
		String request_file = null;
		String html_dir = null;
		
		String merged_file = trace_dir + project + "_merge_log";
		
		boolean output_stat = true;
		int requestNum = 0;
		int queryNum = 0;
		
		int session_count = 0;
		
		ArrayList<WebSession> sessions = new ArrayList<WebSession>();
		
		try {
			BufferedReader indexReader = new BufferedReader(new FileReader(index_file));
			BufferedReader sqlReader = new BufferedReader(new FileReader(sql_file));
			BufferedReader reqReader = null;
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(merged_file));
			
			String cur_log_id = "";
			WebRequest request = null;
			WebSession session = null;   // current web session.
			WebInteraction interaction = null;  // current web interaction.
			//WebInteraction lastInteraction = null;
			SQLQuery query = null;
			SQLQueryResponsePair context = null;
			List<WebInteraction> interactions = new ArrayList<WebInteraction>();
			
			boolean print = false;
			
			int lineCount = 0;
			
			while (indexReader.ready()) {
				lineCount ++;
				//System.out.println(lineCount);
				String line = indexReader.readLine();
				String[] strs = line.split("[\\[\\]]");
				if (strs.length != 4) continue;
				// index file is in format [type][index]
				String type = strs[1];
				String index = strs[3];

				if (type.equals("HTTP_SESSION")) {
					// index is in format "role userId"
					UserIdentity user = parseUserInfo(index);
					//if (user.getRole().getRole() == 0) print = true;  else print = false;
					if (print) System.out.println("USER: " + user.getUserId());
					session = new WebSession();
					session.setUser(user);
					sessions.add(session);
					interaction = null;
					//lastInteraction = null;
					interactions.clear();
					bw.write(line + "\n");
					session_count ++;
					//if (session_count > 6) break;   // we only analyze 6 sessions for bloggit right now.

				} else if (type.equals("HTTP_REQUEST")) {
                    // from looking at HTTP_REQUEST and HTTP_RESPONSE,
                    // index seems to be in format [log_id-file_id]
					// request file's name in format "/home/soh/trace/Batman/JsForum/log_1/1-requestFile"
					// html dir in format "/home/soh/trace/Batman/JsForum/log_1/html/"
					
					// parse log id.
					String log_id = index.substring(0, index.indexOf("-"));
					if (!log_id.equals(cur_log_id)) {
						cur_log_id = log_id;
						log_dir = trace_dir + "log_" + log_id + "/";
						request_file = log_dir + log_id + "-requestFile";
						html_dir = log_dir + "html/";
						reqReader = new BufferedReader(new FileReader(request_file));  // pre-process into hashmap?
					}
					
					if (reqReader == null)  {
						System.err.println("Request reader is not initialized! Abort!");
						indexReader.close();
						sqlReader.close();
						return sessions;
					}

                    // from looking at parseWebRequestLog function, id-requestFile seems to contain
                    // a request trace in the following format.
                    // [index][method][url][param1][param2]...[paramN]
					String requestLine = reqReader.readLine();    // read from request file.
					request = parseWebRequestLog(requestLine, index);
					
					bw.write(requestLine + "\n");
					
					if (print) System.out.println(requestLine);
					
					if (request != null) {
						// for statistics: 
						requestNum ++;
						
						interaction = new WebInteraction(request);
						interaction.setUser(session.getUser());
						//interaction.setLastInteraction(lastInteraction);
						interaction.addPreviousInteractions(interactions);
						
						/*
						WebInteraction newInteraction = new WebInteraction(request);
						newInteraction.setUser(session.getUser());
						if (interaction != null) {
							newInteraction.setLastInteraction(interaction);
						}
						session.addInteraction(newInteraction);
						interaction = newInteraction;
						*/
					} else {
						System.err.println("Error during parsing web request: " + line); // should abort this interaction.
						interaction = null;
					}
					
				} else if (type.equals("HTTP_RESPONSE")) {
					// from looking at HTTP_REQUEST and HTTP_RESPONSE,
					// index seems to be in format [log_id-file_id]
					// request file's name in format "/home/soh/trace/Batman/JsForum/log_1/1-requestFile"
					// html dir in format "/home/soh/trace/Batman/JsForum/log_1/html/"
					
					// file id.
					String file_id = index.substring(index.indexOf("-")+1);
					String html_file = html_dir + file_id + ".html";
					WebResponse response = WebResponseParser.parseHtmlFile(html_file);
					if (response != null && interaction != null) {
						interaction.setWebResponse(response);
					} else {
						System.err.println("Web Response parsing failed or current interaction is null!");
					}

					if (interaction != null) {
						if (interaction.getResponse() != null) {
							session.addInteraction(interaction);
							if (print) System.out.println("add interaction: " + interaction.getRequest().getRequestKey());
							//lastInteraction = interaction;
							interactions.add(interaction);
						} else {
							System.out.println("web response is null. drop this interaction. " + index);
						}
					}
					
					bw.write(html_file + "\n");
					
				} else if (type.equals("SQL_QUERY")) {
					
					String queryLog = sqlReader.readLine();
					//if (queryLog.contains("response")) queryLog = sqlReader.readLine();					
					try {
                        // sql log has format [query][index][stmt]
                        // or [response][index][select][result]
                        // or [response][index][nselect][status][row#]
						// remember: index is from index log and SQL query is from sql log,
						// so this is trying to process right number of SQL query/response pairs
						// per web interaction.
						query = parseSqlQueryMessage(queryLog, index);
					} catch (Exception e) {
						query = null;
						//System.out.println(queryLog);
					}
					
					if (query != null) {
						queryNum++;
						if (interaction != null) {
							context = new SQLQueryResponsePair(query);
							interaction.addSQLQueryResponsePair(context);
							if (print && queryLog.contains("INSERT INTO users")) {
								System.out.println("Add query after parsing: " + query.getSkeleton());
								System.out.println(interaction.getRequest().getRequestKey());
							}
						}
						bw.write(queryLog + "\n");
						
					} else {
						//System.err.println("SQL query parsing failed!");
						sqlReader.readLine();   // jump over its sql response.
						indexReader.readLine();
						lineCount ++;
					}
					
				} else if (type.equals("SQL_RESPONSE")) {
					// sql log has format [query][index][stmt]
					// or [response][index][select][result]
					// or [response][index][nselect][status][row#]
					// remember: index is from index log and SQL query is from sql log,
					// so this is trying to process right number of SQL query/response pairs
					// per web interaction.

					String responseLog = sqlReader.readLine();
					SQLResponse response = parseSqlResponseMessage(responseLog, index, query);
					if (response != null && context != null) {
						context.setSqlResponse(response);
						bw.write(responseLog + "\n");
					} else {
						System.err.println("SQL response parsing failed or current context is null!");
					}
				}				
			}
			indexReader.close();
			sqlReader.close();
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (output_stat) {
			System.out.println("Number of Web Sessions is: "  + sessions.size());
			for (WebSession session: sessions) {
				System.out.println("Session role: " + session.getUser().getRole().getRole() + " user: " + session.getUser().getUserId() + " has " + session.getInteractions().size() + " interactions.");
				for (WebInteraction interaction: session.getInteractions()) {
					if (interaction.getPreviousInteractions().isEmpty()) System.out.println("previous interactions not exist.");
					if (interaction.getRequest() == null) System.out.println("web request is null.");
					if (interaction.getResponse() == null) System.out.println("web response is null.");
					//System.out.println("sqlQueryReponsePair size: " + interaction.getSqlQueryResponsePairs().size());
				}
			}
			System.out.println("Number of requests: " + requestNum + " ; Number of sql queries: " + queryNum);
		}
		
		return sessions;
	}

	/**
	 * Extracts a user identity from index part of the index log.
	 * Is userId from user table in DB?
	 *
	 * @param info
	 * @return
	 */
	private static UserIdentity parseUserInfo(String info) {
		String[] strs = info.split(" ");      // change to info.split(" "); for crawl trace.
		int role = Integer.parseInt(strs[0]);
		String identity = strs[1];
		return new UserIdentity(new UserRole(role), identity);
	}
	
	private static WebRequest parseWebRequestLog(String line, String index) {
		
		StringTokenizer st = new StringTokenizer(line, "[]");
		//st.nextToken();  // INDEX
		String id = st.nextToken();
		if (!id.equals(index)) {
			System.err.println("Web request index doesn't match! " + id + " " + index);
			return null;
		}
		//st.nextToken();   // METHOD
		String method = st.nextToken();
		//st.nextToken();   // URL
		String url = st.nextToken();
		//st.nextToken();   // PARAMETER
		String postParas = "";
		if (st.hasMoreTokens()) {
			postParas = st.nextToken();
		}
		return TraceParser.parseWebRequest(method, url, postParas);
	}
	
	
	public static SQLQuery parseSqlQueryMessage(String line, String index) throws JSQLParserException {
		StringTokenizer st = new StringTokenizer(line, "[]");
		String header = st.nextToken();  // query
		if (!header.equals("query")) {
			System.err.println("Type is not SQL_QUERY! " + line);
			return null;
		}
		String id = st.nextToken();
		if (!index.equals("") && !id.equals(index)) {
			System.err.println("SQL query index doesn't match! " + id + " " + index);
			return null;
		}
		String queryStatement = st.nextToken();
		
		if (queryStatement.contains("INSERT INTO forum_users(user_name)")) {    // temp fix for JsForum trace.
			queryStatement += ")";
		}
		if (queryStatement.contains("mysql-connector-java-") || queryStatement.contains("@@")
				|| queryStatement.startsWith("SET") || queryStatement.startsWith("SHOW")) {  // filter for jsp apps
			return null;
		}
		return TraceParser.parseSQLQuery(queryStatement);
	}
	
	public static SQLResponse parseSqlResponseMessage(String line, String index, SQLQuery query) {
		StringTokenizer st = new StringTokenizer(line, "[]");
		String header = st.nextToken();  // response
		if (!header.equals("response")) {
			System.err.println("Type is not SQL_RESPONSE!");
			return null;
		}
		String id = st.nextToken();
		if (!index.equals("") && !id.equals(index)) {
			System.err.println("SQL response index doesn't match! " + id + " " + index);
			return null;
		}
		String type = st.nextToken();
		String responseStr = "";
		if (type.equals("select")) {
			responseStr = st.nextToken();
		} else if (type.equals("nselect")) {
			String status = st.nextToken();
			int row = Integer.parseInt(st.nextToken());
			if (status.equalsIgnoreCase("STATUS_OK") && row > 0) {
				responseStr = "SUCCESS:" + status + ":" + row;
			} else {
				responseStr = "FAIL:" + status + ":" + row;
			}
		}
		SQLResponse response = TraceParser.parseSQLResponse(responseStr, query);
		return response;
	}
}
