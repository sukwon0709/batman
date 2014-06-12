package edu.vanderbilt.batman.model;

import java.util.HashMap;

import lombok.Getter;

import org.jdom.Element;

import edu.vanderbilt.batman.util.TraceParser;

public class WebRequest {
	
	@Getter private String method = null;
	@Getter private String url = null;
	@Getter private HashMap<String, String> getParameters = new HashMap<String, String>();  // GET parameters.
	@Getter private HashMap<String, String> postParameters = new HashMap<String, String>(); // POST parameters.
	@Getter private HashMap<String, String> parameterMap = new HashMap<String, String>();  // key: parameter name; value: GET or POST.
	
	// play as cookie.
	//@Getter @Setter private String session = null;
	
	public WebRequest() {
	}
	
	public WebRequest(String m, String u) {
		method = m;
		url = u;
		getParameters = new HashMap<String, String>();
		postParameters = new HashMap<String, String>();
		parameterMap = new HashMap<String, String>();
	}
	
	public WebRequest(WebRequest request) {
		method = request.getMethod();
		url = request.getUrl();
		getParameters = new HashMap<String, String>(request.getGetParameters());
		postParameters = new HashMap<String, String>(request.getPostParameters());
		parameterMap = new HashMap<String, String>(request.getParameterMap());
	}
	
	public String getRequestKey() {
		return method + "-" + url;
	}
	
	public void addGetParameter(String name, String value) {
		getParameters.put(name, value);
		parameterMap.put(name, "GET");
	}
	
	public void addPostParameter(String name, String value) {
		postParameters.put(name, value);
		parameterMap.put(name, "POST");
	}
	
	public HashMap<String, String> getAllParameters() {
		HashMap<String, String> parameters = new HashMap<String, String>();
		for (String name: parameterMap.keySet()) {
			if (parameterMap.get(name).equals("GET")) {
				parameters.put("GET-" + name, getParameters.get(name));
			} else if (parameterMap.get(name).equals("POST")) {
				parameters.put("POST-" + name, postParameters.get(name));
			}
		}
		return parameters;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[REQUEST][" + method + "]");
		builder.append("[URL][" + url + "]");
		/*
		if (session == null || session.equals("")) {
			builder.append("[SESSION][null]");
		} else {
			builder.append("[SESSION][" + session + "]");
		}*/
		builder.append("[PARAMETER][");
		HashMap<String, String> parameters = getAllParameters();
		for (String name: parameters.keySet()){
			builder.append(name + "=" + parameters.get(name) + "&");
		}
		builder.append("]\n");
		return builder.toString();
	}
	
	public Element genXMLElement() {
		Element element = new Element("WebRequest");
		
		element.setAttribute("HttpRequestMethod", method);
		element.setAttribute("HttpRequestURL", url);
		
		StringBuilder builder = new StringBuilder();
		for (String parameter: getParameters.keySet()) {
			builder.append(parameter + "=" + getParameters.get(parameter) + "&");
		}
		String getParas = builder.toString();
		element.setAttribute("HttpRequestGetParameters", getParas);
		
		builder = new StringBuilder();
		for (String parameter: postParameters.keySet()) {
			builder.append(parameter + "=" + postParameters.get(parameter) + "&");
		}
		String postParas = builder.toString();
		element.setAttribute("HttpRequestPostParameters", postParas);
		
		return element;
	}
	
	public void loadXMLElement(Element element) {
		method = element.getAttributeValue("HttpRequestMethod");
		url = element.getAttributeValue("HttpRequestURL");
		HashMap<String, String> getParas = TraceParser.parseRequestParameters(element.getAttributeValue("HttpRequestGetParameters"));
		HashMap<String, String> postParas = TraceParser.parseRequestParameters(element.getAttributeValue("HttpRequestPostParameters"));
		for (String para: getParas.keySet()) {
			addGetParameter(para, getParas.get(para));
		}
		for (String para: postParas.keySet()){
			addPostParameter(para, postParas.get(para));
		}
	}
}
