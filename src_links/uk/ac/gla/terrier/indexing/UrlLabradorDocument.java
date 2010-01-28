package uk.ac.gla.terrier.indexing;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;

public class UrlLabradorDocument extends LabradorDocument {
	/** the terms that match part of the url */
	protected HashSet<String> urlTextMatchedTerms = null; 
	
	/** the terms that have been checked against the url */
	protected HashSet<String> urlTextCheckedTerms = null;
	
	/** the string of the document's url */
	protected String url = null;
	public UrlLabradorDocument(InputStream s, Map<String,String> properties)
	{
		super(s, properties);
		urlTextMatchedTerms = new HashSet<String>(5);//5 is heuristic
		urlTextCheckedTerms = new HashSet<String>(100);//avg document length
		url = properties.get("url");
	}

	public UrlLabradorDocument(InputStream s, Map<String,String> properties, PrintWriter... writers /*PrintWriter aTextW, PrintWriter tTextW, PrintWriter eTextW, PrintWriter ctTextW, PrintWriter urlTextW*/) {
		super(s, properties, writers/*aTextW, tTextW, eTextW, ctTextW, urlTextW*/);
		urlTextMatchedTerms = new HashSet<String>(5);
		urlTextCheckedTerms = new HashSet<String>(100);
		url = properties.get("url");
	}
	
	protected static HashSet<String> urlFields = new HashSet<String>(); 
	
	static {
		urlFields.add("URLHOST");
		urlFields.add("URLFILE");
		urlFields.add("URLQUERY");
		urlFields.add("URLPATH1");
		urlFields.add("URLPATH2");
		urlFields.add("URLPATHOTHER");
	}
	public String getNextTerm() {
		fields.removeAll(urlFields);
		String outputTerm = super.getNextTerm();
		if (outputTerm==null)
			return null;
		
		if (urlTextMatchedTerms.contains(outputTerm) || urlTextCheckedTerms.contains(outputTerm))
			return outputTerm;
		
		if (url.contains(outputTerm)) {
			if (base.getHost().contains(outputTerm)) {
				urlTextMatchedTerms.add(outputTerm);
				fields.add("URLHOST");
			}
			if (base.getFile().contains(outputTerm)) {
				urlTextMatchedTerms.add(outputTerm);
				fields.add("URLFILE");
			}
			String query = base.getQuery();
			if (query!=null && query.contains(outputTerm)) {
				urlTextMatchedTerms.add(outputTerm);
				fields.add("URLQUERY");
			}
			
			//get to the path
			String sPath = base.getPath();
			String[] pathBits = sPath.split("/");
			if (pathBits.length > 1) {
				boolean pathFound = false;
				if (pathBits[0].contains(outputTerm)) {
					urlTextMatchedTerms.add(outputTerm);
					pathFound = true;
					fields.add("URLPATH1");
				}
				if (pathBits.length > 1 && pathBits[1].contains(outputTerm)) {
					pathFound = true;
					urlTextMatchedTerms.add(outputTerm);
					fields.add("URLPATH2");
				}
				
				if (!pathFound && sPath.contains(outputTerm)) {
					urlTextMatchedTerms.add(outputTerm);
					fields.add("URLPATHOTHER");
				}
			}
		}
		return outputTerm;
	}
}
