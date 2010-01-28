package uk.ac.gla.terrier.links;
import gnu.trove.TObjectLongHashMap;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.utility.TerrierTimer;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** 
 * Provides an implementation of the URLIndex interface.
 * It stores the information in four main files. The first file 
 * contains the different domains encountered. The second file 
 * contains the path of each indexed URL. The third file 
 * contains entries of the type: <docid> <domainOffset>  
 * <domainLength> <pathOffset> <pathLength>, where 
 * <domainOffset> is the offset of the correspondning entry 
 * in the first file, domainLength is the length of the URL's 
 * domain, pathOffset is the offset of the corresponding entry 
 * in the second file, pathLength is the length of the URL's 
 * path. Finally, the fourth file contains entries of the 
 * form: ( MD5(domain) MD5(path) ) docid, where the MD5 hash 
 * keys are merged by taking the first 48 bits from MD5(domain) 
 * and the 80 last bits from MD5(path). 
 * <br>
 * The input file required should be a text file 
 * (uncompressed, or compressed with gzip) with one entry per 
 * line: <docid> <doc url>. <docid> is a unique sequential integer 
 * (starting from zero) assigned to each of the documents, which 
 * are indexed by Terrier. The <docid> should be exactly the 
 * integer assigned to the document by Terrier during indexing.
 * The input filename is specified by the property <tt>urlserver.input</tt>,
 * while the standard prefix of the output files is specified by
 * the property <tt>urlserver.prefix</tt> and its default value 
 * is urlserver.
 * @author Vassilis Plachouras
 */
public class URLServer implements URLIndex {

	enum inputFormat {
		docid2url ,
		docno2url ,
		url2docno ,
		url2docid 
	}
	
	//the filenames for the input file and the data structures
	//that are saved on disk.
	
	//the input file
	protected static String URLSERVER_INPUTFILE;
	
	static {
		//TODO: remove this static block
		URLSERVER_INPUTFILE = ApplicationSetup.makeAbsolute(ApplicationSetup.getProperty("urlserver.input","id2url.gz"),  ApplicationSetup.TERRIER_INDEX_PATH);
	}

	/** The file that contains the entries docid domain path.*/
	protected RandomAccessFile documentIndexFile;
	
	/** The length of the documentIndexFile.*/
	protected long documentIndexLength;

	/** The file that contains the domains.*/
	protected RandomAccessFile domainsFile;

	/** The file that contains the paths.*/
	protected RandomAccessFile pathsFile;

	/** The file that contains the md5 codes and docids.*/
	protected RandomAccessFile urlsFile;

	/** a buffer for temporary storage.*/
	protected final byte[] buffer = new byte[maxURLLength];
	protected static final int maxURLLength = 10240;
	
	protected static final String FileMode = ApplicationSetup.getProperty("urlserver.filemode","r");

	/**
	 * Creates an instance of the class. The main functionality is 
	 * the opening of the files.
	 */
	public URLServer() {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);	
	}

	public URLServer(Index index)
	{
		this(index.getPath(), index.getPrefix());
	}
	
	public URLServer(String path, String prefix)
	{
		try {
			documentIndexFile = new RandomAccessFile(
				ApplicationSetup.makeAbsolute(prefix + ".urldocindex", path),
				FileMode);
			documentIndexLength = documentIndexFile.length();
			domainsFile = new RandomAccessFile(ApplicationSetup.makeAbsolute(prefix + ".domains", path), FileMode);
			pathsFile = new RandomAccessFile(ApplicationSetup.makeAbsolute(prefix + ".paths", path), FileMode);
			urlsFile = new RandomAccessFile(ApplicationSetup.makeAbsolute(prefix + ".urls", path), FileMode);
		} catch (IOException ioe) {
			System.err.println(
				"Input/Output exception during construction of URLServer.");
			ioe.printStackTrace();
			//System.exit(1);
		}
	}

	/** 
	 * Creates the structures needed to provide the described functionality.
	 * The input file is a text file, maybe compressed, containing in each line
	 * a docid and a url.
	*/
	public static void createIndex(Index index) throws IOException {
		final int numDocs = index.getCollectionStatistics().getNumberOfDocuments();
		createIndex(index.getDocumentIndex(), numDocs, index.getPath(), index.getPrefix());
		index.addIndexStructure("urls", "uk.ac.gla.terrier.links.URLServer");
		index.flush();
	}

	public static void createIndex(DocumentIndex doi, int numdocs, String path, String prefix) throws IOException {
		final String fileName = ApplicationSetup.makeAbsolute(prefix, path);
		final RandomAccessFile urlsFile = new RandomAccessFile(fileName+".urls", "rw");
		final DataOutputStream documentIndexStream = new DataOutputStream(Files.writeFileStream(fileName+".urldocindex"));
		final DataOutputStream domainsStream = new DataOutputStream(Files.writeFileStream(fileName+".domains"));
		final DataOutputStream pathsStream = new DataOutputStream(Files.writeFileStream(fileName+".paths")); 

		//The class used for computing the MD5 hash keys
		MessageDigest md5Hash = null;
		try {
			md5Hash = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			//UNLIKELY EXCEPTION
			//System.err.println(
			//	"The choosen algorithm is not supported. Exception thrown during the initialization of MessageDigest.");
			//nsae.printStackTrace();
			//System.exit(1);
		}
		
		//A map for checking whether a domain has been already encountered.
		//The name should be domainToDocid. 
		final TObjectLongHashMap<String> domainToOffset = new TObjectLongHashMap<String>();

		//Opening the input file
		BufferedReader br = null;
		
		try {
			if (URLSERVER_INPUTFILE.equals("-") || URLSERVER_INPUTFILE.equals("stdin"))
			{
				br = new BufferedReader(new InputStreamReader(System.in));
			}
			else
			{
				br = Files.openFileReader(URLSERVER_INPUTFILE);
			}
		} catch (IOException ioe) {
			System.err.println(
				"Input/Output exception while opening the input file in URLServer.create(). Stack trace follows."+URLSERVER_INPUTFILE);
			ioe.printStackTrace();
			System.exit(1);
		}

		//what format is the input format?		
		inputFormat format = null;
		try{
			format = inputFormat.valueOf(inputFormat.class, ApplicationSetup.getProperty("urlserver.input.format","url2docno"));
		} catch (Exception e) { format =  inputFormat.url2docno; }
		
		if (format == inputFormat.url2docno && doi == null)
		{
			System.err.println("No way to match docnos to docids, aborting");
			return;	
		}
		else if (format == inputFormat.docno2url)
		{
			System.err.println("No way to match docnos to docids, assuming docnos are in collection order, docids ascending");
		}

		TerrierTimer timer = new TerrierTimer();
		timer.setTotalNumber((double)numdocs);
		
		String line; int doccount = 0;
		int lineCount = -1;
		
		//for each line of the input file
		NEXTLINE: while ((line = br.readLine()) != null) {
			//split line into docid and url
			lineCount++;
			int docid = -1; String url = null;
			line = line.trim();
			final String parts[] = line.split("\\s+");
			if (format == inputFormat.url2docno	)
			{
				url = parts[0];
				String docno = parts[1];
				docid = doi.getDocumentId(docno);
				if (docid == -1)
				{
					System.err.println("WARNING: No docid found for "+docno);
					continue NEXTLINE;
				}	
			}
			else if (format == inputFormat.docno2url)
			{
				url = parts[1];
				String docno = parts[0];
				if (doi == null)
				{
					docid = lineCount;
				}
				else
				{
					docid = doi.getDocumentId(docno);
				}
				if (docid == -1)
				{
					System.err.println("WARNING: No docid found for "+docno);
					continue NEXTLINE;
				}
			}
			else if (format == inputFormat.docid2url)
			{
				url = parts[1];
				docid = Integer.parseInt(parts[0]);
			}
			else if (format == inputFormat.url2docid)
			{
				url = parts[0];
				docid = Integer.parseInt(parts[1]);
			}
			
			url = URLServer.normaliseURL(url);
			doccount++;	
			
			//split url into domain and path
			String url_domain = null;
			String url_path = null;
			long domainOffset;
			long pathOffset;
			int domainLength;
			int pathLength;
			int firstIndexOfSlash = url.indexOf('/');
			if (firstIndexOfSlash == -1) {
				url_domain = url;
				url_path = "";
			} else {
				url_domain = url.substring(0, firstIndexOfSlash);
				url_path = url.substring(firstIndexOfSlash);
			}

			//store the domain appropriately
			md5Hash.update(url_domain.getBytes());
			byte[] hashMD5 = md5Hash.digest();
			md5Hash.reset();

			if (domainToOffset.containsKey(url_domain)) {
				//System.err.println("found the domain " + domain + " from before.");
				domainOffset = domainToOffset.get(url_domain);
			} else {
				//System.err.println("did not find the domain " + domain + " + from before.");
				domainOffset = domainsStream.size();
				domainToOffset.put(url_domain, domainOffset);
				domainsStream.writeBytes(url_domain);
			}
			domainLength = url_domain.length();

			//store the path appropriately
			if (url_path == null) {
				pathOffset = -1;
				pathLength = 0;
			} else {
				pathOffset = pathsStream.size();
				pathLength = path.length();
				pathsStream.writeBytes(url_path);
			}

			//store the document index information appropriately
			documentIndexStream.writeInt(docid);
			documentIndexStream.writeLong(domainOffset);
			documentIndexStream.writeInt(domainLength);
			documentIndexStream.writeLong(pathOffset);
			documentIndexStream.writeInt(pathLength);

			//store the url hash key appropriately
			md5Hash.update(url_domain.getBytes());
			byte[] hashDomain = md5Hash.digest();
			md5Hash.reset();
			md5Hash.update(url_path.getBytes());
			byte[] hashPath = md5Hash.digest();
			md5Hash.reset();

			for (int i = 6; i < 16; i++)
				hashDomain[i] = hashPath[i];

			//urlsFile.write(hashDomain);
			//urlsFile.writeInt(docid);
			byte[] tmpHash = new byte[16];
			long urlsOffset = urlsFile.getFilePointer();
			boolean descentToChild = true;
			if (urlsOffset == 0) {
				urlsFile.write(hashDomain);
				urlsFile.writeInt(docid);
				urlsFile.writeLong(-1);
				urlsFile.writeLong(-1);
			} else {
				urlsFile.seek(0);
				do {
					urlsFile.read(tmpHash, 0, tmpHash.length);
					int id = urlsFile.readInt();
					long left = urlsFile.readLong();
					long right = urlsFile.readLong();

					int compare = compareHashes(hashDomain, tmpHash);
					if (compare == -1) {
						if (left == -1) {
							urlsFile.seek(urlsFile.getFilePointer() - 16);
							urlsFile.writeLong(urlsOffset);
							descentToChild = false;
						} else {
							urlsFile.seek(left);
						}
					} else if (compare == 1) {
						if (right == -1) {
							urlsFile.seek(urlsFile.getFilePointer() - 8);
							urlsFile.writeLong(urlsOffset);
							descentToChild = false;
						} else {
							urlsFile.seek(right);
						}
					}
					else 
					{
						descentToChild = false;
					}
				} while (descentToChild == true);

				urlsFile.seek(urlsOffset);
				urlsFile.write(hashDomain, 0, hashDomain.length);
				urlsFile.writeInt(docid);
				urlsFile.writeLong(-1);
				urlsFile.writeLong(-1);
			}

			if (doccount % 1000 == 0)
			{
				timer.setRemainingTime((double)doccount);
				System.err.println("Doccount = "+doccount+ " " + timer.toStringMinutesSeconds());
			}

		}
		System.err.println("URLServer build done");
		//close the streams and the input file
		documentIndexStream.close();
		domainsStream.close();
		pathsStream.close();
		br.close();
	}

	/**
	 * Returns the url for the given docid, or 
	 * null if no document is found.
	 * @param docid The document id of which the URL is wanted.
	 * @return the url of the document with the given docid, or null if no document is found.
	 */
	public String getURL(int docid) throws IOException {
		
		long offset = docid * 28L;
		if (offset > documentIndexLength) {
			return null;
		}
		documentIndexFile.seek(offset);
		int id = documentIndexFile.readInt();
		long domainOffset = documentIndexFile.readLong();
		int domainLength = documentIndexFile.readInt();
		long pathOffset = documentIndexFile.readLong();
		int pathLength = documentIndexFile.readInt();

		domainsFile.seek(domainOffset);
		pathsFile.seek(pathOffset);

		domainsFile.read(buffer, 0, domainLength);
		String domain = new String(buffer, 0, domainLength);

		pathsFile.read(buffer, 0, pathLength);
		String path = new String(buffer, 0, pathLength);

		return (domain + path);
	}

	/**
	 * Returns the docid for a give url. If the url is not found
	 * in the index, it returns -1
	 * @param _url The url of the document of which the id is wanted.
	 * @return the docid of the document with the given url, or -1 if no document was found.
	 */
	public int getDocid(String _url) throws IOException {
		MessageDigest md5Hash = null;
		try {
			md5Hash = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println(
				"The choosen algorithm is not supported. Exception thrown during the initialization of MessageDigest.");
			nsae.printStackTrace();
			System.exit(1);
		}

		urlsFile.seek(0);

		String domain;
		String path;
		
		String url = URLServer.normaliseURL(_url);
		
		int firstIndexOfSlash = url.indexOf('/');
		if (firstIndexOfSlash == -1) {
			domain = url;
			path = "";
		} else {
			domain = url.substring(0, firstIndexOfSlash);
			path = url.substring(firstIndexOfSlash);
		}

		md5Hash.update(domain.getBytes());
		byte[] domainHash = md5Hash.digest();
		md5Hash.reset();
		md5Hash.update(path.getBytes());
		byte[] pathHash = md5Hash.digest();
		md5Hash.reset();
		for (int i = 6; i < 16; i++) {
			domainHash[i] = pathHash[i];
		}

		boolean descentToChild = true;
		int id;
		do {
			urlsFile.read(buffer, 0, 16);
			id = urlsFile.readInt();
			long left = urlsFile.readLong();
			long right = urlsFile.readLong();

			int compare = compareHashes(domainHash, buffer);
			if (compare == -1) {
				if (left == -1)
					return -1;
				else
					urlsFile.seek(left);
			} else if (compare == 1) {
				if (right == -1)
					return -1;
				else
					urlsFile.seek(right);
			} else if (compare == 0) {
				descentToChild = false;
			}
		} while (descentToChild);
		return id;
	}

	/**
	 * Returns the domain for a given docid, or null
	 * if no document with the given docid is found.
	 * @param docid The docid of the wanted document.
	 * @return the domain of the document, or null if no document is found.
	 */
	public String getDomain(int docid) throws IOException {
		long offset = docid * 28L + 4L;
		if (offset > documentIndexLength) {
			return null;
		}
		documentIndexFile.seek(offset);
		long domainOffset = documentIndexFile.readLong();
		int domainLength = documentIndexFile.readInt();

		domainsFile.seek(domainOffset);
		domainsFile.read(buffer, 0, domainLength);
		String domain = new String(buffer, 0, domainLength);
		return domain;
	}

	/**
	 * Returns the path for a given docid, or null 
	 * if no document is found.
	 * @param docid The docid of the wanted document.
	 * @return the path of the document with the given docid, or null if no document is found.
	 */
	public String getPath(int docid) throws IOException {
		long offset = docid * 28L + 16L;
		if (offset > documentIndexLength) {
			return null;
		}
		documentIndexFile.seek(offset);
		long pathOffset = documentIndexFile.readLong();
		int pathLength = documentIndexFile.readInt();

		pathsFile.seek(pathOffset);
		pathsFile.read(buffer, 0, pathLength);
		String path = new String(buffer, 0, pathLength);
		return path;
	}

	/**
	 * Compares two hash keys.
	 * @param hash1 The first hash key to compare.
	 * @param hash2 The second hash key to compare.
	 * @returns An integer: zero if the hash keys are equal, 1 if hash1>hash2 and -1 if hash1<hash2.
	 */
	protected static int compareHashes(byte[] hash1, byte[] hash2) {
		int hashLength = hash1.length;
		for (int i = 0; i < hashLength; i++) {
			if (hash1[i] < hash2[i])
				return -1;
			else if (hash1[i] > hash2[i])
				return 1;
		}
		return 0;
	}

	/**
	 * Closes the underlying files.
	 * @throws IOException if an input/output error is encountered.
	 */
	public void close() throws IOException {
		documentIndexFile.close();
		domainsFile.close();
		pathsFile.close();
		urlsFile.close();
	}

	/**
	 * The main method for creating the URL index.
	 */	
	public static void main(String[] args) {
		 URLServer urlServer = null;
		 try {
			if (args.length == 0 || args[0].equals("-help")) {
				System.err.println("Usage: java URLServer [ -help | -create [path prefix [numDocs]] | -getid <url> | geturl <id>");
				System.exit(0);
			} else if (args[0].equals("-create")) {
				if (args.length > 1)
				{
					String path = args[1];
					String prefix = args[2];
					if (args.length > 3)
					{
						int numDocs = Integer.parseInt(args[4]);
						createIndex(null, numDocs, path, prefix);
					}
					else
					{
						createIndex(Index.createIndex(path, prefix));
					}
				}
				else
				{
					createIndex(Index.createIndex());
				}
			} else if (args[0].equals("-getid")) {
				urlServer = new URLServer();
				int docid = -1;
				if (args[1]!= null) 
					docid = urlServer.getDocid(args[1]);
				if (docid == -1) 
					System.out.println("URL not found in index.");
				else
					System.out.println("Docid is " + docid);
			} else if (args[0].equals("-geturl")) {
				urlServer = new URLServer();
				String url = null;
				int docid = 0;
				if (args[1]!=null) {
					docid = Integer.parseInt(args[1]);
					url = urlServer.getURL(docid);
				}
				if (url==null) 
					System.out.println("URL not found in the index for the corresponding docid.");
				else
					System.out.println("url: " + url);			
		 	} else {
				System.err.println("Usage: java URLServer [ -help | -create | -getid <url> | geturl <id>");
				System.err.println("Exiting...");
				System.exit(1);
			}

		 } catch (IOException ioe) {
		 	System.err.println("Input/Output exception while creating the URL index.");
		 	System.err.println(ioe);
		 	System.err.println("Stack trace follow.");
		 	ioe.printStackTrace();
		 	System.exit(1);
		 }
	}	
	
	//match http:// or https:// from the beginning of the urls
	//the (?:s)? is not a capturing group, just a conditional match
	static Matcher replaceHttp = Pattern.compile("^http(?:s)?://").matcher("");
	
	//match the www. at the beginning of urls
	static Matcher replaceWWW = Pattern.compile("^www\\.").matcher("");
	
	//match any anchors in the url (i.e. anything following a hash)
	static Matcher replaceAnchor = Pattern.compile("#.*$").matcher("");
	
	//match a sequence of two dots with the right directory
	static Matcher replaceDoubleDots = Pattern.compile("/[^/]++/\\.\\.").matcher("");
	
	//match a single dot within slashes 
	static Matcher replaceSingleDot = Pattern.compile("/\\./").matcher("");
	
	//match the default port
	static Matcher replaceDefaultPort = Pattern.compile(":80/").matcher("");
	
	//match anything that looks like index.{blahblah} not followed by a slash or a question mark
	static Matcher replaceIndexFile = Pattern.compile("/index\\.[^/?]+$").matcher("");
	
	//match any two consecutive slashes 
	static Matcher replaceDoubleSlash = Pattern.compile("//").matcher("");
	
	public static String normaliseURL(String _url) {
		String url = _url;
		//replaceHttp.reset(url).replaceFirst("");
		//url = url.replaceFirst("^http(s?)://", "");
		url = replaceHttp.reset(url).replaceFirst("");
		//url = url.replaceFirst("www\\.", "");
		url = replaceWWW.reset(url).replaceFirst("");
		//url = url.replaceFirst("#.*$", "");
		url = replaceAnchor.reset(url).replaceFirst("");
		
//		String tmp = null;
//		do {
//			tmp = url;
//			url = replaceDoubleDots.reset(url).replaceAll("");
//		} while (!tmp.equals(url));
		
		url = replaceDoubleDots.reset(url).replaceAll("");
		
		url = replaceSingleDot.reset(url).replaceAll("/");
		
		url = replaceDefaultPort.reset(url).replaceAll("/");
		
		//url = url.replaceAll("/default\\.[^/?]++$", "/");
		
		//url = url.replaceAll("/index\\.[^/?]++$", "/");
		url = replaceIndexFile.reset(url).replaceAll("/");
		
		url = replaceDoubleSlash.reset(url).replaceAll("/");
		
		//need to unescape the url
		//url = url.replaceAll("%7E", "~");
		
		//if the url has a parameter, return
		if (url.indexOf("?")>=0)
			return url;
		
		//if there is no slash, that means that there is
		//only the domain name, then append one at the
		//end of the url.
		int indexOfSlash = url.indexOf("/");
		if (indexOfSlash<0)  
			url += "/";
		else if (!url.endsWith("/") && url.indexOf(".",indexOfSlash)<0)
			url += "/";
		return url;
	}
	
}
