package edu.vanderbilt.batman.testing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.owasp.webscarab.httpclient.HTTPClient;
import org.owasp.webscarab.httpclient.HTTPClientFactory;
import org.owasp.webscarab.model.Request;
import org.owasp.webscarab.model.Response;

import edu.vanderbilt.batman.Portal;
import edu.vanderbilt.batman.inference.WebResponseAnalyzer;
import edu.vanderbilt.batman.model.PropagationPathConstraint;
import edu.vanderbilt.batman.model.SQLOperation;
import edu.vanderbilt.batman.model.SQLQuery;
import edu.vanderbilt.batman.model.SQLQueryResponsePair;
import edu.vanderbilt.batman.model.SQLResponse;
import edu.vanderbilt.batman.model.TestVector;
import edu.vanderbilt.batman.model.Variable;
import edu.vanderbilt.batman.model.WebRequest;
import edu.vanderbilt.batman.model.WebResponse;
import edu.vanderbilt.batman.util.RequestGenerator;
import edu.vanderbilt.batman.util.TraceMerger;
import edu.vanderbilt.batman.util.UserManager;
import edu.vanderbilt.batman.util.WebResponseParser;

public class TestingEngine {
	
	// init.
	private String host = null;     // server address
	private String project = null;    // project name
	private String trace_dir = null;
	
	private List<TestVector> testVectors = new ArrayList<TestVector>();
	private RequestGenerator _generator = null;
	
	// during testing.
	private String cookie = null;
	//private String session_id = null;
	private boolean running = false;
	private TestVector currentTestVector = null;
	private BufferedWriter logWriter = null;
	private boolean evaluate = false;
	
	// statistics.
	private int[] test_counts = new int[3];
	private int[] flagged_counts = new int[3];
	private HashMap<String, List<TestVector>> vulnRequestKeys = new HashMap<String, List<TestVector>>();
	
	private WebResponse curWebResponse = null;
	private List<SQLQueryResponsePair> curSqlQueryRespPairs = new ArrayList<SQLQueryResponsePair>();
	private static SQLQueryResponsePair curSqlQueryRespPair = null;
	
	
	public TestingEngine() throws Exception {
		host = Portal.web_server;
		project = Portal.project;
		trace_dir = Portal.trace_dir;
		// init log file.
		logWriter = new BufferedWriter(new FileWriter(trace_dir + project + "_TestLog"));
		
		_generator = new RequestGenerator(host);
	}
	
	public void loadTestProfile(String file) throws JDOMException, IOException {
		
		SAXBuilder parser = new SAXBuilder();
		Document doc = parser.build(new File(file));
		for (Object o: doc.getContent()) {
			Element root = (Element) o;
			if (!root.getName().equals("TestProfile")) continue;
			for (Object s: root.getContent()) {
				if (!(s instanceof Element)) continue;
				Element element = (Element) s;
				TestVector vector = new TestVector();
				vector.loadXMLElement(element);
				testVectors.add(vector);
			}
		}
	}	
	
	private void prepare() {		
		try {
			String test_file = trace_dir + project + "_TestProfile";
			loadTestProfile(test_file);
			String login_file = trace_dir + project + "_LoginProfile";
			UserManager.loadUsers(login_file);
			// check test cases.
			if (testVectors == null || testVectors.isEmpty()) {
				log("Nothing to test. Should load testProfile first!");
				return;
			}
			
			// basic ping request to get the cookie/session id.			
			submitRequest(_generator.constructBaseRequest(project));
			if (cookie != null) {
				log("Cookie acquired. Preparation done successfully.");
			} else {
				log("Warning: fail to acquire cookie.");
			}
		} catch (MalformedURLException e) {
			log("Error: fail to ping the web app.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	public void run() throws Exception {
		prepare();
		
		System.out.println("==Testing/Replay Start==");
		running = true;
		for (TestVector vector: testVectors) {
			if (!running) {
				log("Abort testing...");
				logWriter.close();
				return;
			}
			currentTestVector = vector;
			log("CURRENT TEST VECTOR :::\n" + vector.toString());
			//if (vector.getTestType() != TestVector.OWNERSHIP_CONST_TEST) continue;
			launchTestVector(vector);
		}
		
		log("==Testing Summary==");
		log("Role Test: # test inputs: " + test_counts[TestVector.ROLE_TEST] + " # flagged: " + flagged_counts[TestVector.ROLE_TEST]);
		log("User Specific Value Test: # test inputs: " + test_counts[TestVector.SPECIFIC_CONST_TEST] + " # flagged: " + flagged_counts[TestVector.SPECIFIC_CONST_TEST]);
		log("MembershipConst Test: # test inputs: " + test_counts[TestVector.OWNERSHIP_CONST_TEST] + " # flagged: " + flagged_counts[TestVector.OWNERSHIP_CONST_TEST]);
		
		for (String key: vulnRequestKeys.keySet()) {
			log("Vulnerable Request Key: " + key + " Count: " + vulnRequestKeys.get(key).size());
			for (TestVector vector: vulnRequestKeys.get(key)) {
				log(vector.toString());
			}
		}
		
		logWriter.close();
		System.out.println("==Testing Finished.==");
	}
	
	// little helper function.
	private void log(String message) {
		try {
			//System.out.println(message);
			logWriter.write(message + "\n\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void launchTestVector(TestVector vector) throws MalformedURLException {
		int testType = vector.getTestType();
		
		if (vector.getTestUser() == null) {
			log("Test user is null. Quit testing.");
			return;
		}
		// login test user.
		if (vector.getTestUser().getRole().getRole() != 0) {
			WebRequest loginRequest = UserManager.constructUserLogicRequest(vector.getTestUser());
			log("LOGIN REQUEST:::\n" + loginRequest);
			System.out.println("LOGIN REQUEST:::\n" + loginRequest);
			submitRequest(_generator.constructRequest(loginRequest, cookie));
		}
		
		
		if (testType == TestVector.OWNERSHIP_CONST_TEST) {
			// need to replay two interactions.
			
			if (vector.getLastWebRequest() == null || vector.getOwnershipConst() == null) {
				log("Last web request or ownership const is null. Quit testing.");
				return;
			}
			
			// submit 1st web request:
			log("FIRST REQUEST:::\n" + vector.getLastWebRequest());
			submitRequest(_generator.constructRequest(vector.getLastWebRequest(), cookie));
			
			// a callback to TestGenerator to violate response constraint.
			if (curWebResponse == null) {
				log("Current Web Response is null. Cannot violate userownershipConst!");
				return;
			}
			vector = TestGenerator.violateMembershipConstraint(vector, curWebResponse);
			if (vector == null) {
				log("Fail to violate userownershipConstraint!");
				return;
			}
		}

		evaluate = true;
		// submit the test web request.
		log("TEST REQUEST:::\n" + vector.getTestWebRequest());
		submitRequest(_generator.constructRequest(vector.getTestWebRequest(), cookie));
		test_counts[testType]++;
		evaluate = false;
		cookie = null; // invalidate the current session.
	}
	
	
	private void evaluateTestInteraction() {
		try {
			SQLOperation operation = currentTestVector.getTestSQLOperation();
			System.out.println("current test operation: " + operation.getOperationSignature());
			
			for (SQLQueryResponsePair pair: curSqlQueryRespPairs) {
				if (matchSQLOperation(operation, pair, curWebResponse)) {
					System.out.println("Operation match!, flag runtime violation!");
					log("Operation match!, flag runtime violation!");
					
					if (!operation.getQuerySkeleton().startsWith("SELECT")){
						// check sql response: if operation succeed?
						if (pair.getSqlResponse().getType() == SQLResponse.SQL_RESPONSE_TYPE_FAIL) {
							log("Operation match!, but response fails!");
						}
					}
					
					flagged_counts[currentTestVector.getTestType()] ++;
					String requestKey = currentTestVector.getTestWebRequest().getRequestKey();
					if (!vulnRequestKeys.containsKey(requestKey)) {
						vulnRequestKeys.put(requestKey, new ArrayList<TestVector>());
					}
					vulnRequestKeys.get(requestKey).add(currentTestVector);					

					log("Query Statement: " + pair.getSqlQuery().getQueryStr());
					log("Query Response: " + pair.getSqlResponse().getResponseStr());
					break;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean matchSQLOperation(SQLOperation operation, SQLQueryResponsePair pair, WebResponse response) {
		if (pair.getSqlQuery().getSkeleton().contains("DELETE FROM blogdata WHERE blogdata.post_id =")) {
			pair.getSqlQuery().setSkeleton("DELETE FROM blogdata WHERE blogdata.post_id = {STRING}");
		}
		if (!operation.getQuerySkeleton().equals(pair.getSqlQuery().getSkeleton())) {
			return false;
		}
		// TODO: refactor later.
		if (!operation.getQuerySkeleton().startsWith("SELECT")){  // UPDATE, INSERT, DELETE queries. Does response matter? relate to DB state.
			return true;
		}
		if (operation.getAttributes().isEmpty()) {    // only consider select queries which propagate info back to user as sensitive.
			return false;
		}
		
		boolean print = false;
		if (currentTestVector != null && currentTestVector.getTestWebRequest().getRequestKey().contains("delete.php") &&
				currentTestVector.getTestSQLOperation().getQuerySkeleton().equals("SELECT * FROM events WHERE events.id = {STRING}")) {
			print = true;
		}
		
		HashMap<String, List<String>> webRespVarValues = WebResponseAnalyzer.getWebResponseValues(response);
		
		if (print) {
			for (String key: webRespVarValues.keySet()) {
				//System.out.println(key);
				//System.out.println(webRespVarValues.get(key));
			}
		}
		
		for (PropagationPathConstraint constraint: operation.getAttributes()) {
			if (print) {
				System.out.println("Constraint: " + constraint.toString());
			}
			String sqlRespVarName = constraint.getSrcVariable().getName();  // sql response variable.
			String webRespVarName = constraint.getDestVariable().getName();  // web response variable.
			
			
			
			if (!pair.getSqlResponse().getValues().containsKey(sqlRespVarName)) {
				return false;
			}
			if (!webRespVarValues.containsKey(webRespVarName)) {
				return false;
			}
			
			List<String> sqlRespValues = pair.getSqlResponse().getValues().get(sqlRespVarName);
			List<String> webRespValues = webRespVarValues.get(webRespVarName);
			if (sqlRespValues.isEmpty()) return false;
			if (webRespValues.isEmpty()) return false;
			
			if (print) {
				System.out.println("sql response: " + sqlRespVarName);
				System.out.println(sqlRespValues);
				System.out.println(webRespValues);
			}
			
			// all resp values come from sql resp
			boolean contain = false;
			for (String webRespValue: webRespValues) {
				for (String sqlRespValue: sqlRespValues) {
					if (print) System.out.println(sqlRespValue + " -- " + webRespValue);
					
					Set<String> webStrSet = new HashSet<String>(Arrays.asList(webRespValue.split(" ")));
					Set<String> sqlStrSet = new HashSet<String>(Arrays.asList(sqlRespValue.split(" ")));				
					// all tokens in ostrs occur in mstrs
					if (webStrSet.containsAll(sqlStrSet)) {
						contain = true;
						break;
					}
					/*
					if (webRespValue.contains(sqlRespValue)) {   // SUBSTRING_MATCH!
						contain = true;
						break;
					}*/
				}
				if (contain) break;
			}
			if (!contain) return false;
			/*
			boolean contain = true;
			for (String sqlRespValue: sqlRespValues) {
				for (String webRespValue: webRespValues) {
					if (webRespValue.contains(sqlRespValue)) {   // SUBSTRING_MATCH!
						contain = true;
						break;
					}
				}
				if (contain) break;
			}
			if (!contain) return false;
			*/
		}
		return true;
	}
	
	
	public void sqlQueryReceived(String queryStatement, String index) {
		try {
			SQLQuery query = TraceMerger.parseSqlQueryMessage(queryStatement, index);
			if (query != null) {
				//log("sqlQuery: " + queryStatement);
				curSqlQueryRespPair = new SQLQueryResponsePair(query);
			} else {
				//System.out.println(index + " " + queryStatement);
				//System.err.println("SQL query parsing failed!");
				curSqlQueryRespPair = null;
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}
	
	
	// receive sql response.
	public void sqlResponseReceived(String result, String index) {
		
		if (curSqlQueryRespPair != null && curSqlQueryRespPair.getSqlQuery() != null) {
			SQLResponse response = TraceMerger.parseSqlResponseMessage(result, index, curSqlQueryRespPair.getSqlQuery());
			if (response != null) {
				//log("sqlResponse: " + result);
				curSqlQueryRespPair.setSqlResponse(response);
				curSqlQueryRespPairs.add(curSqlQueryRespPair);
			} else {
				//System.err.println("SQL response parsing failed!");
			}
		} else {
			//System.err.println("curSqlQueryRespPair or SqlQuery is null!");
		}
	}
	
	// submit web request to application.
	private void submitRequest(Request request) {
		HTTPClient client = HTTPClientFactory.getInstance().getHTTPClient();
		try {
			// cache the request.
			//request.write(new FileOutputStream(cache_dir+interaction_id+"-request"));
			//log("TEST REQUEST:::\n" + request);
			Response response = client.fetchResponse(request);
            response.flushContentStream();
            webResponseReceived(response, request);
        } catch (Exception ioe) {
        	ioe.printStackTrace();
        }
	}
	
	
	private void webResponseReceived(Response response, Request request) {
		if (response == null) {
			log("Null response.");
			curWebResponse = null;
			return;
		}
		
		if (response.getStatus().equals("400") || response.getStatus().equals("404")) {   // Bad request || Not found.
			log("Bad request or not found: " + request);
			curWebResponse = null;
			running = false;        // abort testing. could be server problem.
			return;
		}
		
		// extract cookie and session id (both PHP and JSP).
		if (response.getHeader("Set-Cookie") != null) {
			cookie = response.getHeader("Set-Cookie");
			//System.out.println("Cookie: " + cookie);
			//if (session_id == null && cookie != null && cookie.contains("PHPSESSID")) {  // change session id here.
			String session_id = cookie.substring(cookie.indexOf("=")+1, cookie.indexOf(";"));
			log("Set the PHP session id: " + session_id);
		}
		
		curWebResponse = WebResponseParser.parseHTTPResponseString(new String(response.getContent()));
		
		if (currentTestVector != null && currentTestVector.getTestType() == 2 &&
				currentTestVector.getTestSQLOperation().getQuerySkeleton().equals("DELETE FROM blogdata WHERE blogdata.post_id = {STRING}")) {
			System.out.println(new String(response.getContent()));
		}
		
		
		if (curWebResponse == null) {
			log("Fail to parse web response!");
			return;
		}
		
		if (evaluate) {
			evaluateTestInteraction();
		}
	}

}
