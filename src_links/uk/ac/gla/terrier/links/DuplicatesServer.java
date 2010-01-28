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
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.structures.JDBMHashtable;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author vassilis
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DuplicatesServer {
	
	protected URLNormaliser urln = new URLNormaliser();
	
	//the filename for the duplicates persistent hashtable
	protected String DUPLICATES_OUTPUT;
	
	//the duplicates hashtable
	protected JDBMHashtable[] duplicatesHashtables;

	//the number of hashtables
	int numOfTables = 10;
	
	//the url server
	URLServer3 urlServer = new URLServer3();
	
	public DuplicatesServer() {
		String prefix = ApplicationSetup.TERRIER_INDEX_PREFIX + '.' + ApplicationSetup.getProperty("duplicates.prefix","duplicates");
		DUPLICATES_OUTPUT = ApplicationSetup.TERRIER_INDEX_PATH + ApplicationSetup.FILE_SEPARATOR + prefix;
		duplicatesHashtables = new JDBMHashtable[numOfTables];
		for (int i=0; i<numOfTables; i++) {
			duplicatesHashtables[i] = new JDBMHashtable(DUPLICATES_OUTPUT+i);
		}
	}
	
	public DuplicatesServer(String output) {
		duplicatesHashtables = new JDBMHashtable[numOfTables];
		for (int i=0; i<numOfTables; i++) {
			duplicatesHashtables[i] = new JDBMHashtable(output+i);
		}	
	}
	
	public String normaliseURL(String url) {
		return urln.normalise(url);
	}
	
	/**
	 * Adds to the persistent hashtable the duplicate urls.
	 */
	public void add(String inputFilename) {
		Vector vec = new Vector();
		try {
			BufferedReader br = null;
			if (inputFilename.toLowerCase().endsWith(".gz")) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFilename))));	
			} else {
				br = new BufferedReader(new FileReader(inputFilename));
			}
			int counterLines = 0;
			int realCounter = 0;
			int table;
			int[] counter = new int[numOfTables];
			String inputLine = null;
			StringTokenizer tokens;
			String tmp;
			while ((inputLine=br.readLine())!=null) {
				System.out.print("+ "+realCounter++);
				if (inputLine.endsWith(".gif") || inputLine.endsWith(".jpg")) {
					System.out.println(": ends with gif or jpg");
					continue;
				}
				tokens = new StringTokenizer(inputLine, " ");
				tokens.nextToken();
				int count = 0;
				int found = -1;
				while (tokens.hasMoreTokens()) {
					tmp = urln.normalise(tokens.nextToken());
					int index = tmp.indexOf("://");
					tmp = tmp.substring(index+3);
					vec.add(tmp);
					if (found == -1 && (urlServer.getDocid(tmp)>-1))
						found = count;
					count++;
				}
				if (found==-1) {
					System.out.println(": no docs were found");
					vec.clear();
					continue;
				}
					
				String storedFileURL = (String)vec.elementAt(found);
				for (int i=0; i<count; i++) {
					if (i!=found) {
						tmp = (String)vec.elementAt(i);
						table = Math.abs(tmp.hashCode()) % numOfTables;
						duplicatesHashtables[table].put(tmp, storedFileURL);
						counter[table]++;
						if (counter[table] > 2000) {
							counter[table] = 0;
							duplicatesHashtables[table].commit();
						}
					}
				}
				System.out.println(": stored " + (count-1) + " pairs");
				vec.clear();
				if (++counterLines > 2000) {
					counterLines = 0;
					System.out.println("processed 2000 entries. commiting.");
					urlServer.clearCache();
					System.gc();
				}
			}
			System.err.println("stored " + counter + " entries.");
			for (int i=0; i<numOfTables; i++) 
				duplicatesHashtables[i].commit();
			br.close();
		} catch(IOException ioe) {
			System.err.println("io exception while creating the duplicates server.");
			System.err.println(ioe);
			System.err.println("exiting...");
			System.exit(2);
		}
	}
	
	public String getDuplicate(String url) {
		
		return duplicatesHashtables[Math.abs(url.hashCode()) % numOfTables].get(url);
	}
	
	public void close() {
		for (int i=0; i<numOfTables; i++) 
			duplicatesHashtables[i].close();
	}
	
	public void clear() {
		for (int i=0; i<numOfTables; i++) 
			duplicatesHashtables[i].clear();
	}
	
	public static void main(String[] args) {
		DuplicatesServer dServer = new DuplicatesServer("duplicates");
		if (args[0].equals("-c")) {
			long start = System.currentTimeMillis();
			dServer.clear();
			for (int i=1; i<args.length; i++) {
				dServer.add(args[i]);
			}
			long end = System.currentTimeMillis();
			System.err.println("time elapsed is " + ((end - start)/1000.0d));
		} else if (args[0].equals("-q")) {
			System.out.println(dServer.getDuplicate(dServer.normaliseURL(args[1])));
		}

		dServer.close();
	}
}
