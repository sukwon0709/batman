package edu.vanderbilt.batman.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.vanderbilt.batman.model.WebElement;
import edu.vanderbilt.batman.model.WebResponse;

public class WebResponseParser {
	
	private static HashMap<String, Integer> xpathCounts = new HashMap<String, Integer>();  // used for generating unique id.
	
	public static WebResponse parseHTTPResponseString(String responseStr) {
		try {
			Document dom = getDocument(responseStr);
			xpathCounts.clear();
			WebResponse response = new WebResponse();
			traverse(dom, "", "", null, response);     // DFS traversal to get WebResponse.
			
			return response;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static WebResponse parseHtmlFile(String file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			StringBuilder builder = new StringBuilder();
			while(br.ready()) {
				builder.append(br.readLine());
			}
			br.close();
			String rawHtml = builder.toString();
			
			Document dom = getDocument(rawHtml);
			xpathCounts.clear();
			WebResponse response = new WebResponse();
			traverse(dom, "", "", null, response);     // DFS traversal to get WebResponse.
			
			return response;
		} catch (Exception e) {
			System.out.println("FILE: " + file);
			e.printStackTrace();
		}
		return null;
	}
	
	
	private static void traverse(Node node, String xpath, String uniquePath, WebElement parent, WebResponse response)  {
		
		if(node == null) return; // base condition.
		
		if (node.getNodeType() == Node.TEXT_NODE && (xpath.endsWith("FONT") || xpath.endsWith("SCRIPT"))){  // filter out font and script text.
			return;
		}
		
		xpath += "/" + node.getNodeName();  // accumulate xpath.
		
		if (!xpathCounts.containsKey(xpath)) {
			xpathCounts.put(xpath, 1);
		} else {
			int id = xpathCounts.get(xpath);
			xpathCounts.put(xpath, id+1);
		}
		uniquePath = xpath + "[" + xpathCounts.get(xpath) + "]";   // unique id.
		
		if (node.getNodeType() == Node.TEXT_NODE && !node.getNodeValue().trim().equals("")) {    // handle text node.

			if (xpath == null || uniquePath == null) System.out.println("HERE!");
			WebElement text = new WebElement(xpath, uniquePath, WebElement.TEXT);
			text.setValue(node.getNodeValue());
			if (parent != null) {
				parent.addChildElement(text);
			} else {
				response.addWebElement(text);
			}
			
		} else if (node.hasAttributes()) {
			
			if (node.getNodeName().equalsIgnoreCase("a")){   // handle anchor node.
				
				Node href = node.getAttributes().getNamedItem("href");
				//if (href.getNodeValue().contains("mailto:")) return;    // filter out mail link.
				//if (href.getNodeValue().contains("sourceforge.net")) return;	 // filter out certain link. temp tweak here.
				
				if (href != null) {   // handle url link here.
					WebElement link = new WebElement(xpath, uniquePath, WebElement.WEB_LINK);
					String linkUrl = href.getNodeValue();
					link.setValue(linkUrl);
					
					// parse link url parameter:
					Set<WebElement> parameters = parseUrlParameters(linkUrl, xpath, uniquePath);
					for (WebElement element: parameters) {
						link.addChildElement(element);
					}
					
					response.addWebElement(link);
				}
				
			} else if (node.getNodeName().equalsIgnoreCase("form")) {     // handle form element.
				
				Node method = node.getAttributes().getNamedItem("method");
				String formMethod = method != null? method.getNodeValue() : "post";  // default is post.
				Node action = node.getAttributes().getNamedItem("action");
				String actionUrl = (action != null)? action.getNodeValue() : "";
				
				WebElement form = new WebElement(xpath, uniquePath, WebElement.WEB_FORM);
				form.setValue(formMethod + "-" + actionUrl);
				
				Set<WebElement> parameters = parseUrlParameters(actionUrl, xpath, uniquePath);
				for (WebElement element: parameters) {
					form.addChildElement(element);
				}
				response.addWebElement(form);
				parent = form;
				
			} else if (node.getNodeName().equalsIgnoreCase("input") || node.getNodeName().equalsIgnoreCase("select")) {  // parent is form element.
				
				Node name = node.getAttributes().getNamedItem("name");
				if (name != null) {
					String inputName = name.getNodeValue();
					WebElement input = new WebElement(xpath+":"+inputName, uniquePath+":"+inputName, WebElement.FORM_INPUT);
					
					Node value = node.getAttributes().getNamedItem("value");
					if (value != null) {
						input.setValue(value.getNodeValue());
					} else {
						input.setValue("");
					}
					if (parent != null) {
						parent.addChildElement(input);
					}
					parent = input;   // text, options.
				}
				
				//Node type = node.getAttributes().getNamedItem("type");
				//String inputType = (type != null)? type.getNodeValue() : "NullType";
				//System.out.println("input: " + inputName + " type: " + inputType);
				
			} else if (node.getNodeName().equalsIgnoreCase("option")) {   // parent is form element.
				
				Node value = node.getAttributes().getNamedItem("value");
				String optionValue = (value != null)? value.getNodeValue() : "";
				WebElement element = new WebElement(xpath, uniquePath, WebElement.WEB_PARAMETER);
				element.setValue(optionValue);
				parent.addChildElement(element);
			}
		}
		
		// traverse down.
		NodeList nl = node.getChildNodes();
		for(int i = 0; i < nl.getLength(); ++i) {
			traverse(nl.item(i), xpath, uniquePath, parent, response);
		}
		// reset parent?
		
	}
	
	
	private static Set<WebElement> parseUrlParameters(String linkUrl, String xpath, String uniquePath) {
		Set<WebElement> parameters = new TreeSet<WebElement>();
		if (linkUrl == null || linkUrl.equals("")) return parameters; 
		
		if(linkUrl.indexOf("?") != -1) {
			String paras = linkUrl.substring(linkUrl.indexOf("?")+1);
			linkUrl = linkUrl.substring(0, linkUrl.indexOf("?"));
			StringTokenizer st = new StringTokenizer(paras, "&");
			while(st.hasMoreTokens()) {
				StringTokenizer st1 = new StringTokenizer(st.nextToken(), "=");
				String name = st1.nextToken();
				//if (name.equals("referer")) continue;     								// filter referer parameter.
				String value = (st1.hasMoreTokens())? st1.nextToken().trim() : "";
				//if (xpath == null || uniquePath == null) System.out.println("HERE!");
				WebElement parameter = new WebElement(xpath+":"+linkUrl+":"+name, uniquePath+":"+linkUrl+":"+name, WebElement.WEB_PARAMETER);
				parameter.setValue(value);
				parameters.add(parameter);
			}
		}
		return parameters;
	}
	
	
	/**
	 * transforms a string into a Document object. TODO This needs more optimizations. As it seems
	 * the getDocument is called way too much times causing a lot of parsing which is slow and not
	 * necessary.
	 * 
	 * @param html
	 *            the HTML string.
	 * @return The DOM Document version of the HTML string.
	 * @throws IOException
	 *             if an IO failure occurs.
	 * @throws SAXException
	 *             if an exception occurs while parsing the HTML string.
	 */
	public static Document getDocument(String html) throws SAXException, IOException {
		DOMParser domParser = new DOMParser();
		domParser.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
		domParser.setFeature("http://xml.org/sax/features/namespaces", false);
		domParser.parse(new InputSource(new StringReader(html)));
		return domParser.getDocument();
	}

	
	public static String documentToString( Document document ) throws TransformerException {
	    TransformerFactory tFactory = TransformerFactory.newInstance();
	    Transformer transformer = tFactory.newTransformer();
	   
	    DOMSource source = new DOMSource(document);
	    StringWriter sw = new StringWriter();
	    StreamResult result = new StreamResult(sw);
	   
	    transformer.setOutputProperty( OutputKeys.INDENT, "yes" );

	    transformer.transform(source, result);
	   
	    return sw.toString();
	}	

}
