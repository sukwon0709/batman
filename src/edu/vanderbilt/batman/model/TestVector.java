package edu.vanderbilt.batman.model;

import org.jdom.Element;

import lombok.Getter;
import lombok.Setter;

public class TestVector {
	
	@Getter private int testType;
	@Getter @Setter private SQLOperation testSQLOperation;
	@Getter @Setter private WebRequest testWebRequest;
	@Getter @Setter private UserIdentity testUser = null;
	
	public static int ROLE_TEST = 0;
	@Getter @Setter UserRole testRole = null;
	@Getter @Setter UserRole refRole = null;
	
	public static int SPECIFIC_CONST_TEST = 1;
	@Getter @Setter private PropagationPathConstraint parameterPropPath = null;
	@Getter @Setter private UserSpecificVariableConstraint specificValueConst = null;
	
	public static int OWNERSHIP_CONST_TEST = 2;
	@Getter @Setter private WebRequest lastWebRequest = null;
	@Getter @Setter private UserOwnershipConstraint ownershipConst = null;
	
	
	public TestVector() {
	}
	
	public TestVector(int type) {
		testType = type;
	}
	
	public Element genXMLElement() {
		Element element = new Element("TestVector");
		element.setAttribute("TestType", Integer.toString(testType));
		element.addContent(testSQLOperation.genXMLElement());
		element.addContent(testWebRequest.genXMLElement());
		
		if (testUser != null){
			element.addContent(testUser.genXMLElement());
		}
		
		if (testType == ROLE_TEST) {
			if (testRole != null){
				element.setAttribute("TestRole", Integer.toString(testRole.getRole()));
			}
			if (refRole != null){
				element.setAttribute("RefRole", Integer.toString(refRole.getRole()));
			}
		} else if (testType == SPECIFIC_CONST_TEST) {
			if (parameterPropPath != null){
				element.addContent(parameterPropPath.genXMLElement(false));
			}
			if (specificValueConst != null){
				element.addContent(specificValueConst.genXMLElement(true));
			}
		} else if (testType == OWNERSHIP_CONST_TEST) {
			if (lastWebRequest != null) {
				Element lastWebRequestElement = lastWebRequest.genXMLElement();
				lastWebRequestElement.setAttribute("LastWebRequest", "1");
				element.addContent(lastWebRequestElement);
			}
			if (ownershipConst != null) {
				element.addContent(ownershipConst.genXMLElement(true));
			}
		}
		return element;
	}
	
	public void loadXMLElement(Element element) {
		
		if (!element.getName().equals("TestVector")) return;
		testType = Integer.parseInt(element.getAttributeValue("TestType"));
		
		if (testType == ROLE_TEST) {
			testRole = new UserRole(Integer.parseInt(element.getAttributeValue("TestRole")));
			refRole = new UserRole(Integer.parseInt(element.getAttributeValue("RefRole")));
		}
		
		for (Object o: element.getContent()) {
			if (!(o instanceof Element)) continue;
			Element ele = (Element) o;
			String name = ele.getName();
			if (name.equals("WebRequest")) {
				WebRequest request = new WebRequest();
				request.loadXMLElement(ele);
				if (testType == OWNERSHIP_CONST_TEST && ele.getAttributeValue("LastWebRequest") != null) {
					this.setLastWebRequest(request);
				} else {
					this.setTestWebRequest(request);
				}
				
			} else if (name.equals("UserIdentity")) {
				int role = Integer.parseInt(ele.getAttributeValue("role"));
				String id = ele.getAttributeValue("userId");
				testUser = new UserIdentity(new UserRole(role), id);
				testUser.setUsername(ele.getAttributeValue("username"));
				testUser.setPassword(ele.getAttributeValue("password"));
				
			} else if (name.equals("SQLOperation")) {
				String query = ele.getAttributeValue("querySkeleton");
				SQLOperation operation = new SQLOperation(query);
				operation.parseXMLElement(ele);
				operation.genOperationSignature();
				this.setTestSQLOperation(operation);
				
			} else if (name.equals("PropagationPathConstraint")) {
				parameterPropPath = new PropagationPathConstraint();
				parameterPropPath.parseXMLElement(ele);
				
			} else if (name.equals("UserOwnershipConstraint")) {
				ownershipConst = new UserOwnershipConstraint();
				ownershipConst.parseXMLElement(ele);
				
			} else if (name.equals("UserSpecificVariableConstraint")) {
				specificValueConst = new UserSpecificVariableConstraint();
				specificValueConst.parseXMLElement(ele);
			}
		}
	}
	
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TestType: " + testType + "\n");
		builder.append("TestSQLOperation: " + testSQLOperation.toString() + "\n");
		
		if (testUser != null) {
			builder.append("TestUser: " + testUser.toString() + "\n");
		}
		if (testRole != null) {
			builder.append("TestRole: " + testRole.getRole() + "\n");
		}
		if (refRole != null) {
			builder.append("RefRole: " + refRole.getRole() + "\n");
		}		

		// test web request.
		builder.append("HttpRequestMethod: " + testWebRequest.getMethod() + "\n");
		builder.append("HttpRequestURL: " + testWebRequest.getUrl() + "\n");
		builder.append("HttpRequestGetParameters: " +testWebRequest.getGetParameters() + "\n");
		builder.append("HttpRequestPostParameters: " + testWebRequest.getPostParameters() + "\n");
		
		if (specificValueConst != null) {
			builder.append("SpecificValueConst: " + specificValueConst.getConstraint() + "\n");
		}
		if (parameterPropPath != null) {
			builder.append("ParameterPropPathConst: " + parameterPropPath.getConstraint() + "\n");
		}
		if (ownershipConst != null) {
			builder.append("OwnershipConst: " + ownershipConst.getConstraint() + "\n");
		}
		if (lastWebRequest != null) {
			builder.append("LastRequestMethod: " + lastWebRequest.getMethod() + "\n");
			builder.append("LastRequestURL: " + lastWebRequest.getUrl() + "\n");
			builder.append("LastRequestGetParameters: " +lastWebRequest.getGetParameters() + "\n");
			builder.append("LastRequestPostParameters: " + lastWebRequest.getPostParameters() + "\n");
		}
		
		return builder.toString();
	}

}
