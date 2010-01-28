package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
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
public class MatchAnchorText2 {

	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** Stores a mapping from [md5 of normalised url] to [md5 docno] */
	private Hashtable<BigInteger,DocumentNumberIdPair> md5sToDocids = null;
	
	/** The md5 message digest used for the urls */
	private MessageDigest md5 = null;
	
	public boolean containsKey(BigInteger key) {
		return md5sToDocids.containsKey(key);
	}
	
	public DocumentNumberIdPair get(BigInteger key) {
		return md5sToDocids.get(key);
	}
	
	public MatchAnchorText2(String inputFile) {
		try {
			md5 = MessageDigest.getInstance("MD5");
			
			BufferedReader br = Files.openFileReader( inputFile) ; 
			md5sToDocids = new Hashtable<BigInteger,DocumentNumberIdPair>();
			loadHashSet(br);
			br.close();			
		} catch(IOException ioe) {
			logger.error("input output exception while creating an instance of MatchAnchorText.", ioe);
		} catch(NoSuchAlgorithmException nsae) {
			logger.error("no such algorithm exception while instantiating message digest.", nsae);
		}
	}

	
	
	protected void loadHashSet(BufferedReader br) throws IOException {
		
		//the input is of the form: [md5 of url] [url]
		String line = null;
		String docnoMD5;
		String url;
		BigInteger docnoMD5Number;
		BigInteger urlMD5Number;
		int docid = 0;
		DocumentNumberIdPair docnoidPair;
		while ((line = br.readLine())!=null) {
			docnoMD5 = line.substring(0, line.indexOf(" "));
			url = line.substring(line.indexOf(" ")+1);
			docnoMD5Number = new BigInteger(docnoMD5, 16);
			
			docnoidPair = new DocumentNumberIdPair(docnoMD5Number, docid);
			
			md5.reset();
			md5.update(URLServer.normaliseURL(url).getBytes());
			urlMD5Number = new BigInteger(md5.digest());
			
			if (md5sToDocids.contains(urlMD5Number)) {
				logger.debug("duplicate md5 in the hashset: " + urlMD5Number.toString(16) + " " + docid + " - The anchor text will be assigned to the first occurrence.");
			} else 
				md5sToDocids.put(urlMD5Number, docnoidPair);
			docid++;
			
			if (docid % 10000 == 0) 
				logger.debug("so far done " + docid + " documents");
		}
	}

	private void match(BufferedReader br) throws IOException {
		String line;
		String targetURL;
		BigInteger targetURLMD5;
		int spaceIndex;
		DocumentNumberIdPair docnoidPair;
		
		while ((line=br.readLine())!=null) {
			spaceIndex = line.indexOf(" ");
			if (spaceIndex < 0) { //no space found, report the error
				logger.debug("line ignored because no space found: " + line);
				continue;
			}
			targetURL = line.substring(0, spaceIndex);
			try {
				md5.reset();
				md5.update(URLServer.normaliseURL(targetURL).getBytes());
				targetURLMD5 = new BigInteger(md5.digest());
				if (md5sToDocids.containsKey(targetURLMD5)) {
					docnoidPair = md5sToDocids.get(targetURLMD5);
					System.out.println(docnoidPair.docnoMD5.toString(16) + " " + docnoidPair.docid + line.substring(spaceIndex));
				}
			} catch(NumberFormatException nfe) {
				logger.debug("number format exception in line: " + line, nfe);
			}
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
			MatchAnchorText2 mat = new MatchAnchorText2(args[0]);
			logger.info("read url text. proceeding to match.");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			mat.match(br);
			br.close();
			logger.info("finished MatchAnchorText");
		} catch(Throwable t) {
			logger.error("caught throwable while matching anchor text.", t);
		}
	}
}

class DocumentNumberIdPair {
	public BigInteger docnoMD5;
	public int docid;
	
	public DocumentNumberIdPair(BigInteger docnoMD5, int docid) {
		this.docnoMD5 = docnoMD5;
		this.docid = docid;
	}
}
