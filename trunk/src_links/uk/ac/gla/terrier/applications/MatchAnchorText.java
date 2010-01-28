package uk.ac.gla.terrier.applications;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * This class matches the extracted anchor text with the documents in 
 * a particular collection. It requires a list of [docno docurl], which is
 * read from a un/compressed file, and a list of [targeturl srcdocno tag text], 
 * which is read from the standard input.
 * @author vassilis
 *
 */
public class MatchAnchorText {

	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
		
	/** Stores a mapping from [labrador md5 of url] to [docid] */
	private TObjectIntHashMap md5sToDocids = null;
	
	public boolean containsKey(BigInteger key) {
		return md5sToDocids.containsKey(key);
	}
	
	public int get(BigInteger key) {
		return md5sToDocids.get(key);
	}
	
	public MatchAnchorText(String inputFile) {
		try {
			BufferedReader br = Files.openFileReader(inputFile);
			md5sToDocids = new TObjectIntHashMap();
			loadHashSetNormalised(br);
			br.close();			
		} catch(IOException ioe) {
			logger.error("input output exception while creating an instance of MatchAnchorText.", ioe);
		}
	}
	
	protected void loadHashSet(BufferedReader br) throws IOException {
		
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
				logger.debug("duplicate md5 in the hashset: " + md5Number.toString(16) + " for docid " + docid +". The anchor text will be assigned to the docid " + originalDocid);
			} else {
				md5sToDocids.put(md5Number, docid);
			}
			docid++;
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
				logger.debug("duplicate md5 in the hashset: " + md5Number.toString(16) + " for docid " + docid +". The anchor text will be assigned to the docid " + originalDocid);
			} else {
				md5sToDocids.put(md5Number, docid);
			}
			docid++;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if (args.length!=1) {
				System.out.println("usage: MatchAnchorText [url-text.gz file]");
			}
			ApplicationSetup.setupFilenames(); //just to initialise ApplicationSetup
			logger.info("started MatchAnchorText");
			MatchAnchorText mat = new MatchAnchorText(args[0]);
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line;
			String targetmd5;
			String targeturl;
			BigInteger md5num;
			int docid;
			int spaceIndex;

			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch(NoSuchAlgorithmException nsae) {
				//should not be here because MD5 is included
			}
			
			while ((line=br.readLine())!=null) {
				spaceIndex = line.indexOf(" ");
				if (spaceIndex < 0) { //no space found, report the error
					logger.debug("line ignored because no space found: " + line);
					continue;
				}
				//targetmd5 = line.substring(0, spaceIndex);
				//assumes that we read the *-anchor-text.gz directly
				//not after the prepare-anchor-text script.
				targeturl = line.substring(0, spaceIndex);
				
				try {
					md.update(URLServer.normaliseURL(targeturl).getBytes());
					md5num = new BigInteger(md.digest());
					
					if (mat.containsKey(md5num)) {
						docid = mat.get(md5num);
						System.out.println(md5num/*.abs()*/.toString(16) + " " + docid + line.substring(spaceIndex));
					}
				} catch(NumberFormatException nfe) {
					logger.debug("number format exception in line: " + line, nfe);
				}
			}
			br.close();
			logger.info("finished MatchAnchorText");
		} catch(Throwable t) {
			logger.error("caught throwable while matching anchor text.", t);
		}
	}
}
