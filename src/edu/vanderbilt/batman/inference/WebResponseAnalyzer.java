package edu.vanderbilt.batman.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.vanderbilt.batman.model.Variable;
import edu.vanderbilt.batman.model.WebElement;
import edu.vanderbilt.batman.model.WebResponse;

/**
 * This class parses a HTML page into desired format (e.g., a set of links, forms, parameter nameValue pairs).
 * @author xli
 *
 */
public class WebResponseAnalyzer {
	

	public static Set<Variable> getWebResponseVariables(WebResponse response) {
		
		// extract elements from web response (html)
		Set<WebElement> elements = getWebResponseParameters(response);
		
		Set<Variable> variables = new HashSet<Variable>();
		for (WebElement element: elements) {
			Variable var = new Variable(element.getUniqueId(), element.getXpath());    // use xpath as variable name; uniqueId as context.
			var.addValue(element.getValue());
			variables.add(var);
		}
		return variables;
	}
	
	public static Set<Variable> getWebResponseVariablesByXpath(WebResponse response, String xpath) {
		
		Set<Variable> variables = new HashSet<Variable>();
		for (Variable var: getWebResponseVariables(response)) {
			if (var.getName().equals(xpath)) {
				variables.add(var);
			}
		}
		return variables;
	}
	
	public static HashMap<String, List<String>> getWebResponseValues(WebResponse response) {
		
		HashMap<String, List<String>> values = new HashMap<String, List<String>>();
		for (Variable var: getWebResponseVariables(response)) {
			String xpath = var.getName();
			if (!values.containsKey(xpath)) {
				values.put(xpath, new ArrayList<String>());
			}
			values.get(xpath).addAll(var.getValues());
		}
		return values;
	}
	

	public static Set<WebElement> getWebResponseParameters(WebResponse response) {
		Set<WebElement> parameters = new TreeSet<WebElement>();
		traverse(response.getElements(), parameters);
		return parameters;
	}
	
	private static void traverse(Set<WebElement> elements, Set<WebElement> parameters) {
		if (elements.isEmpty()) return;
		for (WebElement element: elements) {
			if (element.getType() == WebElement.WEB_PARAMETER || element.getType() == WebElement.TEXT) {
				parameters.add(element);
			}
			traverse(element.getChildren(), parameters);
		}
	}
}
