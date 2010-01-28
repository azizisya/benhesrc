package uk.ac.gla.terrier.applications;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class DetectDuplicateDocuments {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();

	/** 
	 * A property <tt>enable.duplicate.docid.output</tt> for enabling the output of a list 
	 * of documents to black list. The default value is <tt>false</tt>.*/
	private boolean isDuplicateDocidOutputEnabled = (new Boolean(ApplicationSetup.getProperty("enable.duplicate.docid.output","false"))).booleanValue();
	
	/**
	 * A property <tt>duplicate.docid.output.filename</tt>, the value of which 
	 * specifies the filename in which to store the list of duplicate document ids.
	 * This can be used to form the input for the black lists. There is no default value,
	 * but it is required to be specified if the property <tt>enable.duplicate.docid.output</tt>
	 * has been set to true.
	 */
	private String duplicateDocidOutputFilename = ApplicationSetup.getProperty("duplicate.docid.output.filename", "");
	
	/** Stores a mapping from [labrador md5 of url] to [docid] */
	private TObjectIntHashMap md5sToDocids = null;
	
	public boolean containsKey(BigInteger key) {
		return md5sToDocids.containsKey(key);
	}
	
	public int get(BigInteger key) {
		return md5sToDocids.get(key);
	}
	
	public DetectDuplicateDocuments() {
		md5sToDocids = new TObjectIntHashMap();
	}
	
	protected void loadHashSet(BufferedReader br) throws IOException {

		PrintWriter pw = null;
		final boolean _isDuplicateDocidOutputEnabled = this.isDuplicateDocidOutputEnabled;
		if (_isDuplicateDocidOutputEnabled) {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(
					ApplicationSetup.makeAbsolute(duplicateDocidOutputFilename, ApplicationSetup.TERRIER_HOME))));
		}
		
		//the input is of the form: [md5 of url] [url]
		String line = null;
		String md5;
		BigInteger md5Number;
		int docid = 0;
		int originalDocid;
		while ((line = br.readLine())!=null) {
			md5 = line.substring(0, line.indexOf(" "));
			md5Number = new BigInteger(md5, 16);
			if (md5sToDocids.contains(md5Number)) {
				originalDocid = md5sToDocids.get(md5Number);
				logger.debug("duplicate md5 in the hashset: " + md5Number.toString(16) + " for docid " + docid +". The original docid is " + originalDocid);
				
				if (_isDuplicateDocidOutputEnabled) {
					pw.println(docid);
				}
			} else {
				md5sToDocids.put(md5Number, docid);
			}
			docid++;
		}
		
		if (_isDuplicateDocidOutputEnabled) {
			pw.close();
		}
	}

	/** 
	 * Normalise the url from the input
	 * Compute the md5 of the normalised url
	 * add (md5[normalised url] -> md5 of url) to a hash table
	 * if a key exists in the hash table, then report the second url, docid as a duplicate 
	 * @param br
	 */
	protected void loadHashSetNormalised(BufferedReader br) throws IOException {

		PrintWriter pw = null;
		final boolean _isDuplicateDocidOutputEnabled = this.isDuplicateDocidOutputEnabled;
		if (_isDuplicateDocidOutputEnabled) {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(
					ApplicationSetup.makeAbsolute(duplicateDocidOutputFilename, ApplicationSetup.TERRIER_HOME))));
		}
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch(NoSuchAlgorithmException nsae) {
			//should not be here because MD5 is included
		}
		
		//the input is of the form: [md5 of url] [url]
		String line = null;
		String url;		
		BigInteger md5Number;
		int docid = 0;
		int originalDocid;
		while ((line = br.readLine())!=null) {
			url = line.substring(line.indexOf(" ") + 1, line.length());
			md.update(URLServer.normaliseURL(url).getBytes());
			md5Number = new BigInteger(md.digest());
			if (md5sToDocids.contains(md5Number)) {
				originalDocid = md5sToDocids.get(md5Number);
				logger.debug("duplicate md5 in the hashset: " + md5Number.toString(16) + " for docid " + docid +". The original docid is " + originalDocid);
				
				if (_isDuplicateDocidOutputEnabled) {
					pw.println(docid);
				}
			} else {
				md5sToDocids.put(md5Number, docid);
			}
			docid++;
		}
		if (_isDuplicateDocidOutputEnabled) {
			pw.close();
		}

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length!=1) {
				System.out.println("usage: DetectDuplicateDocuments [url-text.gz file]");
			}
			ApplicationSetup.setupFilenames(); //just to initialise ApplicationSetup
			logger.info("started DetectDuplicateDocuments");
			DetectDuplicateDocuments mat = new DetectDuplicateDocuments();

			try {
				BufferedReader br = null;
				if (args[0].toLowerCase().endsWith(".gz"))
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[0]))));	
				else 
					br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
				
				
				mat.loadHashSetNormalised(br);
				
				br.close();			
			} catch(IOException ioe) {
				logger.error("input output exception while detecting duplicate documents.", ioe);
			}

			
			
			logger.info("finished DetectDuplicateDocuments");
		} catch(Throwable t) {
			logger.error("caught throwable while detecting duplicate documents.", t);
		}
	}

}