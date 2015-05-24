package edu.vanderbilt.batman.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

import lombok.Getter;
import lombok.Setter;

import org.owasp.webscarab.model.HttpUrl;
import org.owasp.webscarab.model.Request;
import org.owasp.webscarab.util.Encoding;

import edu.vanderbilt.batman.model.WebRequest;

public class RequestGenerator {

	@Getter @Setter private String host;
	@Getter @Setter private String baseUrl;
	private String http_version = "HTTP/1.1";
	
	public RequestGenerator (String url) {
		host = url;
		baseUrl = "http://" + host;
	}
	
	public Request constructBaseRequest(String app) throws MalformedURLException {
		app = "";
		Request request = new Request();
		request.setMethod("GET");
		request.setVersion(http_version);
		request.setHeader("host", host);
		request.setURL(new HttpUrl(baseUrl+"/" + app + "/"));
		//request.setURL(new HttpUrl(baseUrl+"/" + app + "/blog.php"));              // Customized for minibloggie.
		return request;
	}
	
	// construct redirection requests.
	public Request constructRedirectRequest(String app, String header, String cookie) throws MalformedURLException {
		app = "";
		Request request = new Request();
		request.setMethod("GET");
		request.setVersion(http_version);
		request.setHeader("host", host);
		if (header != null) {
			if (header.contains(baseUrl)) {
				request.setURL(new HttpUrl(header));
			} else {
				request.setURL(new HttpUrl(baseUrl+"/"+app+"/"+header));
			}
		}
		if (cookie != null) request.addHeader("Cookie", cookie);
		return request;
	}
	
	public Request constructRequest(WebRequest webRequest, String cookie) throws MalformedURLException {
		Request request = new Request();
		request.setMethod(webRequest.getMethod());
		request.setVersion(http_version);
		request.setHeader("host", host);
		if (cookie != null) request.addHeader("Cookie", cookie);
		
		String url = baseUrl + "/"+ webRequest.getUrl();	  // corresponds to parseWebRequest in TraceParser.
		
		// compose GET parameters.
		String query = null;
		for (String parameter: webRequest.getGetParameters().keySet()) {
			String value = webRequest.getGetParameters().get(parameter);
			String q = parameter+"="+Encoding.urlEncode(value);
            if (query == null) {
                query = q;
            } else {
                query = query + "&" + q;
            }
		}
		
		// compose POST parameters.
		ByteArrayOutputStream content = null;
		for (String parameter: webRequest.getPostParameters().keySet()) {
			String value = webRequest.getPostParameters().get(parameter);
			String q = parameter+"="+Encoding.urlEncode(value);
			if (content == null) {
                content = new ByteArrayOutputStream();
                try { content.write(q.getBytes()); }
                catch (IOException ioe) {}
            } else {
                try { content.write(("&"+q).getBytes()); }
                catch (IOException ioe) {}
            }
		}		
		
		if (query != null) url = url + "?" + query;
		//System.out.println(url);
        if (request.getMethod().equals("POST")) {
        	request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        }
        if (content != null) {
            request.setHeader("Content-Length", Integer.toString(content.size()));
            request.setContent(content.toByteArray());
        } else if (request.getMethod().equals("POST")) {
            request.setHeader("Content-Length", "0");
        }
        request.setURL(new HttpUrl(url));
		return request;
	}
}