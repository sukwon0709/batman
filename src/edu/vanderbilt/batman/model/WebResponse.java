package edu.vanderbilt.batman.model;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import lombok.Getter;

/**
 * This class models HTTP response.
 * @author xli
 *
 */
public class WebResponse {
	
	// template + parameters.
	
	@Getter private Set<WebElement> elements = new TreeSet<WebElement>();   // ordered set.
	@Getter private Set<Variable> dynamicVariables = new HashSet<Variable>();   // store variables after pruning.
	
	public WebResponse() {
		
	}

	public WebResponse (WebResponse r) {
		elements.clear();
		elements.addAll(r.getElements());
		dynamicVariables.clear();
		dynamicVariables.addAll(r.getDynamicVariables());
	}	
	
	public void addWebElement(WebElement element) {
		elements.add(element);
	}
	
	public void addVariable(Variable var) {
		dynamicVariables.add(var);
	}
}
