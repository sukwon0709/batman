package edu.vanderbilt.batman.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

public class WebElement implements Comparable<WebElement>{
	
	public static int WEB_LINK = 0;     // href
	public static int WEB_FORM = 1;     // form
	public static int FORM_INPUT = 2;    // form text input, select.
	public static int WEB_PARAMETER = 3;   // href and form action url parameter, option values.
	public static int TEXT = 4;  // text.
	
	@Getter @Setter private String xpath;    // path
	@Getter @Setter private String uniqueId;   // specific location plus link url or element name.
	@Getter @Setter private int type;
	
	@Getter @Setter private String value = null;    // non-null for text, parameter, form node.
	@Getter private Set<WebElement> children = new HashSet<WebElement>();  // empty for text node; non-empty for link, form, select and option nodes.
	
	public WebElement(String path, String id, int t) {
		xpath = path;
		uniqueId = id;
		type = t;
	}
	
	public void addChildElement(WebElement element) {
		if (element.getValue() == null) System.out.println(element.getType() + " " + element.getUniqueId());
		children.add(element);
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof WebElement) {
			WebElement p = (WebElement)o;
			return (xpath.equals(p.xpath) &&  value.equals(p.value) && uniqueId.equals(p.uniqueId)); // consider unique identifier here.
		}
		return false;
	}
	
	@Override
	public int compareTo(WebElement o) {
		return this.getUniqueId().compareTo(o.getUniqueId());
	}

}
