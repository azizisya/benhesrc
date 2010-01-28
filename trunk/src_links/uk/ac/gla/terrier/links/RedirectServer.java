/*
 * Created on 02-Aug-2004
 *
 */
package uk.ac.gla.terrier.links;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.structures.JDBMHashtable;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author vassilis
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RedirectServer {

	URLNormaliser urln = new URLNormaliser();
	
	//the filename for the duplicates persistent hashtable
	protected String REDIRECT_OUTPUT;
	
	//the duplicates hashtable
	protected JDBMHashtable redirectHashtable;
	
	public RedirectServer() {
		String prefix = ApplicationSetup.TERRIER_INDEX_PREFIX + '.' + ApplicationSetup.getProperty("redirect.prefix","redirect");
		REDIRECT_OUTPUT = ApplicationSetup.TERRIER_INDEX_PATH + ApplicationSetup.FILE_SEPARATOR + prefix;
		redirectHashtable = new JDBMHashtable(REDIRECT_OUTPUT);
	}
	
	public String normaliseURL(String url) {
		return urln.normalise(url);
	}
	
	public RedirectServer(String output) {
		redirectHashtable = new JDBMHashtable(output);
	}
		
	/**
	 * Adds to the persistent hashtable the input data.
	 */
	public void add(String inputFile) {
		try {
			
			BufferedReader br = null;
			if (inputFile.toLowerCase().endsWith(".gz")) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFile))));	
			} else {
				br = new BufferedReader(new FileReader(inputFile));
			}
			
			int counter = 0;
			int commitCounter = 0;
			String inputLine = null;
			int index;
			String srcURL; 
			String dstURL;
			while ((inputLine=br.readLine())!=null) {
				StringTokenizer tokens = new StringTokenizer(inputLine, " ");
				tokens.nextToken();
				srcURL = urln.normalise(tokens.nextToken());
				index = srcURL.indexOf("://");
				srcURL = srcURL.substring(index+3);
																
				while (tokens.hasMoreTokens()) {
					counter++;
					commitCounter++;
					dstURL = urln.normalise(tokens.nextToken());
					index = dstURL.indexOf("://");
					dstURL = dstURL.substring(index+3);
					redirectHashtable.put(srcURL, dstURL);
				}
				if (commitCounter == 2000) {
					commitCounter = 0;
					redirectHashtable.commit();
					System.out.println("commited at " + counter);
				}
			}
			System.err.println("stored " + counter + " entries.");
			redirectHashtable.commit();
			br.close();
		} catch(IOException ioe) {
			System.err.println("io exception while creating the duplicates server.");
			System.err.println(ioe);
			System.err.println("exiting...");
			System.exit(2);
		}
	}
	
	public String getRedirect(String url) {
		return redirectHashtable.get(url);
	}
	
	public void close() {
		redirectHashtable.close();	
	}
	
	public void clear() {
		redirectHashtable.clear();
	}
	
	public static void main(String[] args) {
		if (args.length == 0)
		{
			System.err.println("Usage: uk.ac.gla.terrier.links.RedirectServer [-c filename] [-q URL]");
			System.exit(1);
		}
		else
		{
			RedirectServer rServer = new RedirectServer("redirect.jdbm");
			if (args[0].equals("-c")) {
				long start = System.currentTimeMillis();
				rServer.clear();
				for (int i=1; i<args.length; i++) {
					rServer.add(args[i]);
				}	
				long end = System.currentTimeMillis();
				System.err.println("time elapsed is " + ((end - start)/1000.0d));
			} else if (args[0].equals("-q")) {
					System.out.println(rServer.getRedirect(rServer.normaliseURL(args[1])));
			}
			rServer.close();
		}
	}
}
