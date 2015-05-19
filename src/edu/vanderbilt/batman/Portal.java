package edu.vanderbilt.batman;

import java.io.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.vanderbilt.batman.testing.TestingEngine;

/**
 * Servlet implementation class Portal
 */
public class Portal extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private TestingEngine _engine = null;
	
	public static int MODE_LOGGING = 0;
	public static int MODE_TESTING = 1;
	public static int MODE_DEBUGING = 2;

	public static final int mode = MODE_LOGGING;
	//public static final int mode = MODE_TESTING;
	//public static final int mode = MODE_DEBUGING;
	
	// Configurations::
	// local storage.
	public static String working_dir = "/home/soh/trace/Batman/";
	//public static String project = "scarf";
	//public static String project = "securephoto";
	//public static String project = "events";
	//public static String project = "bloggit";
	//public static String project = "minibloggie";
	public static String project = "JsForum";
	//public static String project = "jspblog";
	
    //public static String web_server = "129.59.89.23";
	public static String web_server = "129.59.89.23:8080";  // for jsp apps
	
	public static String trace_dir = working_dir + project + "/";
	private static String log_file = trace_dir + project + ".log";
	private static String sql_log_file = trace_dir + project + "_sql_log";
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public Portal() throws Exception {
        super();
        File dir = new File(trace_dir);
		if(!dir.exists() && !dir.mkdir()) {
			System.out.println("FATAL: Project logging directory does not exist.");
			return;
		}
    }
    
    // should locate WebScarab proxy and Portal together (in future, the ultimate Scanner/Crawler).
    
    /**
	 * Used for receiving index from both WebScarab proxy and Mysql proxy.
	 * Note that the length limit for url is around 2000 characters.
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// trigger the testing procedure.
		if (mode == MODE_TESTING && _engine == null) {
			try {
				_engine = new TestingEngine();
			    _engine.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		String dataType = request.getParameter("type");
		String dataIndex = request.getParameter("index");
		//String timeStamp = request.getParameter("time");
			    
		if (mode == MODE_LOGGING && dataType != null && dataIndex != null) {
			BufferedWriter bw = new BufferedWriter(new FileWriter(log_file, true));
		    try {
		    	//System.out.println(log_file);    	
		    	if (!(dataType.equals("HTTP_REQUEST") || dataType.equals("HTTP_RESPONSE") 
		    		  || dataType.equals("SQL_QUERY") || dataType.equals("SQL_RESPONSE")
		    		  || dataType.equals("HTTP_SESSION"))){
		    		System.err.println("Unrecognized datatype!");
		    		bw.close();
		    		return;
		    	}
		    	StringBuilder builder = new StringBuilder();
		    	builder.append("[");
		    	builder.append(dataType);
		    	builder.append("][");
		    	builder.append(dataIndex);
		    	builder.append("]\n");
		    	bw.write(builder.toString());
		    	System.out.print(builder.toString());
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		 	bw.close();
		}
	}

	protected Map<String, String> decodeParameters(String queryStr) {
		String decoded = null;
		Map<String, String> parameters = new HashMap<>();
		try {
			decoded = URLDecoder.decode(queryStr, "UTF-8");
			String[] params = decoded.split("&");

			for (String param : params) {
				String[] kv = param.split("=");
				parameters.put(kv[0], kv[1]);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		return parameters;
	}
	
	/**
	 * Used for receiving REAL DATA (HTTP/SQL REQUEST/RESPONSE) from both WebScarab proxy and Mysql proxy. Now only for mysql proxy.
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String dataType = request.getParameter("type");
		String dataIndex = request.getParameter("index");
		String data = request.getParameter("content");
/*		try {
			data = URLDecoder.decode(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}*/

		
		if (mode == MODE_LOGGING) {
			if (dataType != null && (dataType.equals("SQL_QUERY") || dataType.equals("SQL_RESPONSE")) && data != null) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(sql_log_file, true));
				BufferedWriter bw1 = new BufferedWriter(new FileWriter(log_file, true));
				try {
					bw.write(data);	// soh: I re-enabled it. Hope it doesn't break stuff.
					// record index.
					StringBuilder builder = new StringBuilder();
			    	builder.append("[");
			    	builder.append(dataType);
			    	builder.append("][");
			    	builder.append(dataIndex);
			    	builder.append("]\n");
			    	System.out.print(builder.toString());
			    	//bw1.write(builder.toString());
				} catch (Exception e){
					e.printStackTrace();
				}
				bw.close();
				bw1.close();
			} else {
				System.err.println("ERROR!");
			}
		}
		
		if (mode == MODE_TESTING && _engine != null 
			&& dataType != null && dataIndex != null && data != null) {
			//System.out.println("type: " + dataType + " index: " + dataIndex + " data: " + data);
			if (dataType.equals("SQL_QUERY")) {
				_engine.sqlQueryReceived(data, dataIndex);
				System.out.println(data);
			} else if (dataType.equals("SQL_RESPONSE")) {
				_engine.sqlResponseReceived(data, dataIndex);
			} else {
				System.err.println("Unrecognized dataType!");
			}
		}
		
		if (mode == MODE_DEBUGING) {
			System.out.println(data);
		}
	}

}
