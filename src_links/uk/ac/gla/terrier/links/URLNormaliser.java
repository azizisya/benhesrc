/*
 * Created on 03-Aug-2004
 */
package uk.ac.gla.terrier.links;

import java.util.regex.Pattern;
import java.io.*;

/**
 * Normalises URLs
 */
public class URLNormaliser {

	protected static final boolean RemoveWWWDot = true;

	protected static final Pattern removeWWWDot = Pattern.compile("www\\.");
	protected static final Pattern removeAnchor = Pattern.compile("#.++$");
	protected static final Pattern removeDoubleDots = Pattern.compile("/[^/]++/\\.\\./");
	protected static final Pattern removeSingleDot = Pattern.compile("/\\./");
	protected static final Pattern removeDefaultPort = Pattern.compile(":80/");
	protected static final Pattern removeDefaultHtml = Pattern.compile("/(?:default|index)\\.(?:htm[l]?|asp[x]?|php[3]?|jsp)$");
	protected static final Pattern replaceTilda = Pattern.compile("%7E");

	/* example a257.g.akamaitech.net/7/257/2422/14mar20010800/
	 * remove anything upto akamai, then anything followed by 4 folders */
	protected static final Pattern removeAkamai = Pattern.compile("(?:\\w|\\.)+akamai[^/]+/+(?:[^/]+/){4}");

	protected final static boolean use_dups = false;
	
	DuplicatesServer dServer;
	RedirectServer rServer;
    int depthCounter = 0;

	public URLNormaliser(DuplicatesServer d, RedirectServer r)
	{
		dServer = d;
		rServer = r;
	}

	public URLNormaliser()
	{
		this(new DuplicatesServer(), new RedirectServer());
	}
	
	public String normaliseRedirDuplicates(String _url) {
		depthCounter++;
		if (depthCounter > 10) {
			depthCounter = 0;
			return _url;
		}
		
		String url = _url;
		//System.out.println(_url);
		String tmp1 = rServer.getRedirect(url);
		if (tmp1 != null && !tmp1.equals(url))
			return normaliseRedirDuplicates(/*dServer, rServer,*/ tmp1);
	
		if (use_dups)
		{		
			String tmp2 = dServer.getDuplicate(url);
			if (tmp2 != null&& !tmp2.equals(url)) 
				return normaliseRedirDuplicates(/*dServer, rServer,*/ tmp2);
			depthCounter = 0;
		}
		return url;
	}
	
	public void close() {
		dServer.close();
		rServer.close();
	}
	
	public static String normalise(String _url) {
		String url = _url.toLowerCase().trim();

		//not interested in mailtos
		if (url.startsWith("mailto"))
			return null;

		if (url.startsWith("feed:"))
			url = url.replaceFirst("feed:","");

		//if (!url.startsWith("http://"))
		//	url = "http://" + url;
		
		int protocolIndex = url.indexOf("://");
		if (protocolIndex >= 0 && url.indexOf("/", protocolIndex+3) == -1)
			url = url + "/";
		if (protocolIndex >= 0)
			url = url.substring(protocolIndex+3);
	
		//it's a private IP address, no chance it can be in the collection
		if (url.startsWith("10."))
			return null;
	
		//remove the anchor bookmark
		url = removeAnchor.matcher(url).replaceFirst("");
		
		String tmp = null;
		do {
			tmp = url;
			url = removeDoubleDots.matcher(url).replaceAll("/");
		} while (!tmp.equals(url));
		
		url = removeSingleDot.matcher(url).replaceAll("/"); 
		
		url = removeDefaultPort.matcher(url).replaceAll("/");
		
		url = removeDefaultHtml.matcher(url).replaceAll("/");
		
		url = replaceTilda.matcher(url).replaceAll("~");
		if (RemoveWWWDot)
			url = removeWWWDot.matcher(url).replaceAll("");

		if (url.indexOf("?")>=0)
			return url;
		

		//if there is no slash, that means that there is
		//only the domain name, then append one at the
		//end of the url.
		final int indexOfSlash = url.indexOf("/");
		if (indexOfSlash<0)
			url += "/";
		else if (!url.endsWith("/") && url.indexOf(".",indexOfSlash)<0)
			url += "/";
		else if (url.lastIndexOf("akamai", indexOfSlash) > 0)
			url = removeAkamai.matcher(url).replaceAll("");

		return url;
	}
	
	public static void sampleNormalise(String url)
	{
		System.out.println("url            : "+ url);
		System.out.println("normalised url : "+ URLNormaliser.normalise(url));
	}

	/*public static void main(String[] args) throws Exception {
		
		if (args.length > 0)
		{
			for(int i=0;i<args.length;i++)
			{
				sampleNormalise(args[i]);
			}
		}
		else
		{
			sampleNormalise("www.dcs.gla.ac.uk/index.html");
			sampleNormalise("http://www.dcs.gla.ac.uk/terrier.html");
		}

//		URLNormaliser urlNormaliser = new URLNormaliser("c:\\experiments\\gov\\duplicates.jdbm", "c:\\experiments\\gov\\redirect.jdbm");
//		System.out.println(URLNormaliser.normalise(args[0]));
//		System.out.println(urlNormaliser.normaliseRedirDuplicates(URLNormaliser.normalise(args[0])));
//		urlNormaliser.close();
	}*/

	public static void main(String args[])
	{
		final int place;
		if (args.length > 0)
			place = Integer.parseInt(args[0]);
		else
			place = 1;
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			while((line = br.readLine()) != null)
			{
				line = line.trim();
				String[] parts = line.split(" ");
				if (parts.length == 1)
					continue;
				if (place == 1)
				{
					System.out.println(parts[0]);
					System.out.print(" ");
					System.out.println(URLNormaliser.normalise(parts[1]));
				}
				else
				{
					System.out.print(URLNormaliser.normalise(parts[0]));
					System.out.print(" ");
					System.out.println(parts[1]);
				}
                //String docno = line.substring(0, place);
                //String url = line.substring(place + 1, line.length());	
			}
	
		} catch (IOException ioe) {

		}	
	}
	
}
