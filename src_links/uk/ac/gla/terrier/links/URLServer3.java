/*
 * Created on 15-Aug-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.links;
import gnu.trove.TObjectLongHashMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.links.URLBtree;
import uk.ac.gla.terrier.links.URLNormaliser;
import uk.ac.gla.terrier.links.URLIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;

/**
 * @author vassilis
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class URLServer3 implements URLIndex {
	
	int id = 0;
	
	//DocumentIndexEncoded docIndexEncoded = null;
	
	//the filenames for the input file and the data structures
	//that are saved on disk.
	
	//the input file
	protected static String URLSERVER_INPUTFILE;
	
	//the output file that contains the domains
	protected static String URLSERVER_DOMAINS;
	
	//the output file that contains the paths
	protected static String URLSERVER_PATHS;
	
	//the output file that contains the url to docid index
	protected static String URLSERVER_URLS;
	protected URLBtree[] urlsTree;
	int[] counters;
	int numOfTrees = 20;
	int from = 0;
	int to = numOfTrees;
	
	//the output file that contains the mapping from docid to the
	//paths and the domains
	protected static String URLSERVER_DOCINDEX;
	
	static {
		URLSERVER_INPUTFILE = ApplicationSetup.makeAbsolute(ApplicationSetup.getProperty("urlserver.input","id2url.gz"),  ApplicationSetup.TERRIER_INDEX_PATH);
		URLSERVER_DOMAINS = ApplicationSetup.makeAbsolute(ApplicationSetup.TERRIER_INDEX_PREFIX + ".domains", ApplicationSetup.TERRIER_INDEX_PATH);
		URLSERVER_PATHS = ApplicationSetup.makeAbsolute(ApplicationSetup.TERRIER_INDEX_PREFIX + ".paths", ApplicationSetup.TERRIER_INDEX_PATH);
		URLSERVER_URLS = ApplicationSetup.makeAbsolute(ApplicationSetup.TERRIER_INDEX_PREFIX + ".urls", ApplicationSetup.TERRIER_INDEX_PATH) ;
		URLSERVER_DOCINDEX = ApplicationSetup.makeAbsolute(ApplicationSetup.TERRIER_INDEX_PREFIX + ".urldocindex", ApplicationSetup.TERRIER_INDEX_PATH);
	}

	/** The file that contains the entries docid domain path.*/
	protected RandomAccessFile documentIndexFile;
	
	/** The length of the documentIndexFile.*/
	protected long documentIndexLength;

	/** The file that contains the domains.*/
	protected RandomAccessFile domainsFile;

	/** The file that contains the paths.*/
	protected int numOfPathsFiles = 2;
	protected RandomAccessFile[] pathsFiles;
	

	/** The file that contains the md5 codes and docids.*/
	//protected RandomAccessFile urlsFile;

	/** a buffer for temporary storage.*/
	protected byte[] buffer = new byte[10240];

	public void setFirstId(int fid) {
		id = fid;
	}
	public void setRange(int from, int to) {
		this.from = from;
		this.to = to;
	}
	/**
	 * Creates an instance of the class. The main functionality is 
	 * the opening of the files.
	 */
	public URLServer3() {
	
		try {
			//docIndexEncoded = new DocumentIndexEncoded();
			documentIndexFile = new RandomAccessFile(URLSERVER_DOCINDEX, "rw");
			
			documentIndexLength = documentIndexFile.length();
			
			domainsFile = new RandomAccessFile(URLSERVER_DOMAINS, "rw");
			pathsFiles = new RandomAccessFile[numOfPathsFiles];
			for (int i=0; i<numOfPathsFiles; i++) 
				pathsFiles[i] = new RandomAccessFile(URLSERVER_PATHS+i, "rw");
			urlsTree = new URLBtree[numOfTrees];
			counters = new int[numOfTrees];
			for (int i=0; i<numOfTrees; i++) {
				String id = "tree" + i;
				urlsTree[i] = new URLBtree(URLSERVER_URLS+id, "1");
			}
				
		} catch (IOException ioe) {
			System.err.println(
				"Input/Output exception during construction of URLServer.");
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * calls the methods createIndexUrlToID and createIdToURLIndex.
	 * @throws IOException
	 */
	public void createIndex(boolean a, boolean b) throws IOException {
		if (a)
			createIndexUrlToID();
		if (b)
			createIdToURLIndex();
	}
	
	/** 
	 * Creates the structures needed to provide the described functionality.
	 * The input file is a text file, maybe compressed, containing in each line
	 * a docid and a url.
	*/
	public void createIndexUrlToID() throws IOException {
		//Opening the input file
		BufferedReader br = null;
		final int TOTAL_DOCS = 25205179;

		int counter = 0;
		int counter2 = 0;
		
		String line;
		String inputFiles[] = URLSERVER_INPUTFILE.split("\\s*,\\s*");

		//close the jdbms we wont be using
		for (int i=0; i<numOfTrees; i++)
			if (i<from || i>= to)
			{
				urlsTree[i].close();
				urlsTree[i] = null;
			}


		for (int in = 0; in< inputFiles.length; in++) {
			String inputFilename = inputFiles[in];
			try {
				if (inputFilename.toLowerCase().endsWith(".gz"))
					br =
						new BufferedReader(
							new InputStreamReader(
								new GZIPInputStream(
									new FileInputStream(inputFilename))));
				else
					br =
						new BufferedReader(
							new InputStreamReader(
								new FileInputStream(inputFilename)));
			} catch (IOException ioe) {
				System.err.println(
					"Input/Output exception while opening the input file in URLServer.create(). Stack trace follows.");
				ioe.printStackTrace();
				System.exit(1);
			}
	
			//for each line of the input file
			long startMillis = System.currentTimeMillis();
			while ((line = br.readLine()) != null) {
				//split line into docid and url
				//int docid = (new Integer(line.substring(0, line.indexOf(' ')))).intValue();
				int docid = id++;
				//String docno = line.substring(0, line.indexOf(' '));
				//int docid = docIndexEncoded.getDocumentId(docno);
				String url = line.substring(line.indexOf(' ') + 1, line.length());
				//url = URLNormaliser.normalise(url);
				url = URLNormaliser.normalise(url);
						
				//store the url hash key appropriately
				int treeToStore = Math.abs(url.hashCode()) % numOfTrees;
				if (!(from<=treeToStore && treeToStore<to))
                /*if (!(from<=counter2 && counter2 <to))*/
					continue;
				counter++;
				urlsTree[treeToStore].insert(url, docid);
				counters[treeToStore]++;
				counter2++;
				counter++;
				if (counters[treeToStore] == 1000) {
					urlsTree[treeToStore].commit();
					System.out.println("commiting tree " + treeToStore + " at docid + " + docid);
					counters[treeToStore] = 0;
					long checkpoint = System.currentTimeMillis();
	
				}
				
				if (counter == 1000) {
					long checkpoint = System.currentTimeMillis();
					System.out.println("time to process 1000 (docid " + docid + "="+
						(int)((100*docid)/TOTAL_DOCS)+"% done): " + (checkpoint - startMillis));
					startMillis = checkpoint;
					counter = 0;
				}
	
			}

			br.close();

			for (int i=0; i<numOfTrees; i++) {
				if (urlsTree[i] != null)
				{
					urlsTree[i].commit();
					urlsTree[i].close();
				}
				urlsTree[i] = null;
			}
		}
	}

	public void createIdToURLIndex() throws IOException {

		/* 
		 * close the already open random access files documentIndexFile, domainsFile and
		 * pathsFile, in order to open buffered streams.
		 */
		documentIndexFile.close();
		domainsFile.close();
		for (int i=0; i<numOfPathsFiles; i++) 
			pathsFiles[i].close();
		
		DataOutputStream documentIndexStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(URLSERVER_DOCINDEX)));
		DataOutputStream domainsStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(URLSERVER_DOMAINS)));
		DataOutputStream[] pathsStreams = new DataOutputStream[numOfPathsFiles];
		for (int i=0; i<numOfPathsFiles; i++) 
			pathsStreams[i] = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(URLSERVER_PATHS+i)));

		//A map for checking whether a domain has been already encountered.
		//The name should be domainToDocid. 
		TObjectLongHashMap urlToDocid = new TObjectLongHashMap();

		//Opening the input file
		BufferedReader br = null;

		
		int counter = 0;
		int counter2 = 0;


		
		String line;
		String[] inputFiles = URLSERVER_INPUTFILE.split("\\s*,\\s*");
		for (int in = 0; in< inputFiles.length; in++) {
			String inputFilename = inputFiles[in];
			try {
				if (inputFilename.toLowerCase().endsWith(".gz"))
					br =
						new BufferedReader(
							new InputStreamReader(
								new GZIPInputStream(
									new FileInputStream(inputFilename))));
				else
					br =
						new BufferedReader(
							new InputStreamReader(
								new FileInputStream(inputFilename)));
			} catch (IOException ioe) {
				System.err.println(
					"Input/Output exception while opening the input file in URLServer.create(). Stack trace follows.");
				ioe.printStackTrace();
				System.exit(1);
			}
	
			//for each line of the input file
			long startMillis = System.currentTimeMillis();
			while ((line = br.readLine()) != null) {
				//split line into docid and url
				//int docid = (new Integer(line.substring(0, line.indexOf(' ')))).intValue();
				int docid = id++;
				//String docno = line.substring(0, line.indexOf(' '));
				//int docid = docIndexEncoded.getDocumentId(docno);
				String url = line.substring(line.indexOf(' ') + 1, line.length());
				url = URLNormaliser.normalise(url);
				//url = urln.normalise(url);
				
				//split url into domain and path
				String domain = null;
				String path = null;
				long domainOffset;
				long pathOffset;
				int domainLength;
				int pathLength;
				int firstIndexOfSlash = url.indexOf('/');
				if (firstIndexOfSlash == -1) {
					domain = url;
					path = "";
				} else {
					domain = url.substring(0, firstIndexOfSlash);
					path = url.substring(firstIndexOfSlash);
				}
	
				if (urlToDocid.containsKey(domain)) {
					//System.err.println("found the domain " + domain + " from before.");
					domainOffset = urlToDocid.get(domain);
				} else {
					//System.err.println("did not find the domain " + domain + " + from before.");
					domainOffset = domainsStream.size();
					urlToDocid.put(domain, domainOffset);
					domainsStream.writeBytes(domain);
				}
				domainLength = domain.length();
	
				//store the path appropriately
				int pathFile;
				if ((docid & 1) == 0)
					pathFile = 0;
				else 
					pathFile = 1;
				
				if (path == null) {
					pathOffset = -1;
					pathLength = 0;
				} else {
					pathOffset = pathsStreams[pathFile].size();
					pathLength = path.length();
					pathsStreams[pathFile].writeBytes(path);
				}
	
				//store the document index information appropriately
				documentIndexStream.writeInt(docid);
				documentIndexStream.writeLong(domainOffset);
				documentIndexStream.writeInt(domainLength);
				documentIndexStream.writeLong(pathOffset);
				documentIndexStream.writeInt(pathLength);
				
			}
			br.close();
		}
		//close the streams and the input file
		documentIndexStream.close();
		domainsStream.close();
		for (int i=0; i<numOfPathsFiles; i++) 
			pathsStreams[i].close();
		
		
		//open the random access files.
		documentIndexFile = new RandomAccessFile(URLSERVER_DOCINDEX, "rw");
		documentIndexLength = documentIndexFile.length();
		domainsFile = new RandomAccessFile(URLSERVER_DOMAINS, "rw");
		for (int i=0; i<numOfPathsFiles; i++) 
			pathsFiles[i] = new RandomAccessFile(URLSERVER_PATHS+i, "rw");
	}	
	
	/**
	 * Returns the url for the given docid, or 
	 * null if no document is found.
	 * @param docid The document id of which the URL is wanted.
	 * @return the url of the document with the given docid, or null if no document is found.
	 */
	public String getURL(int docid) throws IOException {

		int pathFile;
		if ((docid & 1) == 0)
			pathFile = 0;
		else 
			pathFile = 1;
		
		long offset = docid * 28L;
		if (offset > documentIndexLength) {
			return null;
		}
		documentIndexFile.seek(offset);
		documentIndexFile.read(buffer, 0, 28);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, 28));
		int id = dis.readInt();
		long domainOffset = dis.readLong();
		int domainLength = dis.readInt();
		long pathOffset = dis.readLong();
		int pathLength = dis.readInt();

		domainsFile.seek(domainOffset);
		pathsFiles[pathFile].seek(pathOffset);

		domainsFile.read(buffer, 0, domainLength);
		String domain = new String(buffer, 0, domainLength);

		pathsFiles[pathFile].read(buffer, 0, pathLength);
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
		String url = URLNormaliser.normalise(_url);
		//String url = urln.normalise(_url);
		int treeToSearch = Math.abs(url.hashCode()) % numOfTrees;
		return urlsTree[treeToSearch].find(_url);
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
		documentIndexFile.read(buffer, 0, 12);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, 28));
		
		long domainOffset = dis.readLong();
		int domainLength = dis.readInt();

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
		int pathFile;
		if ((docid & 1) == 0)
			pathFile = 0;
		else 
			pathFile = 1;
		
		long offset = docid * 28L + 16L;
		if (offset > documentIndexLength) {
			return null;
		}
		documentIndexFile.seek(offset);
		documentIndexFile.read(buffer, 0, 12);
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, 28));
		
		long pathOffset = dis.readLong();
		int pathLength = dis.readInt();

		pathsFiles[pathFile].seek(pathOffset);
		pathsFiles[pathFile].read(buffer, 0, pathLength);
		String path = new String(buffer, 0, pathLength);
		return path;
	}

	/**
	 * Closes the underlying files.
	 * @throws IOException if an input/output error is encountered.
	 */
	public void close() throws IOException {
		documentIndexFile.close();
		domainsFile.close();
		for (int i=0; i<numOfPathsFiles; i++){
			if (pathsFiles[i] != null)
				pathsFiles[i].close();
			pathsFiles[i] = null;
		}
		for (int i=0; i<numOfTrees; i++)
		{
			if (urlsTree[i] != null)
				urlsTree[i].close();
			urlsTree[i] = null;
		}
	}
	
	public void clearCache() {
		for (int i=0; i<numOfTrees; i++)
			urlsTree[i].clearCache();
	}

	/**
	 * The main method for creating the URL index.
	 */	
	public static void main(String[] args) {
		 URLServer3 urlServer = new URLServer3();
		 try {
			if (args[0].equals("-help")) {
				System.err.println("Usage: java URLServer3 [ -help | -create | -getid <url> | geturl <id>");
				System.exit(0);
			} else if (args[0].equals("-create")) {
				boolean a = true;
                boolean b = true;
				if (args.length>1) {
					a = false;
					b = false;
					if (args[1].equals("-range")) {
						int from = Integer.parseInt(args[2]);
						int to = Integer.parseInt(args[3]);
						urlServer.setRange(from, to);
						a = true;
					} else if (args[1].equals("both")) {
						a = b = true;	
					} else if (args[1].equals("first")) {
						a = true;
					} else if (args[1].equals("second")) {
						b = true;
					}
					
				}
				//if (args.length>1) 
				//	urlServer.setFirstId(Integer.parseInt(args[1]));
				urlServer.createIndex(a,b);
				urlServer.close();
			} else if (args[0].equals("-getid")) {
				int docid = -1;
				if (args[1]!= null) 
					docid = urlServer.getDocid(URLNormaliser.normalise(args[1]));
				if (docid == -1) 
					System.out.println("URL not found in index.");
				else
					System.out.println("Docid is " + docid);
			} else if (args[0].equals("-geturl")) {
				String url = null;
				int docid = 0;
				if (args[1]!=null) {
					docid = (new Integer(args[1])).intValue();
					url = urlServer.getURL(docid);
				}
				if (url==null) 
					System.out.println("URL not found in the index for the corresponding docid.");
				else
					System.out.println("url: " + url);			
		 	} else if (args[0].equals("-loop")) {
		 		int start = Integer.parseInt(args[1]);
		 		int end = Integer.parseInt(args[2]);
		 		for (int i=start; i<=end; i++)
		 			System.out.println(i+": " + urlServer.getURL(i));
		 	} else if (args[0].equals("-loopid")) {
		 		int start = Integer.parseInt(args[1]);
		 		int end = Integer.parseInt(args[2]);
		 		for (int i=start; i<=end; i++)
		 			System.out.println(i+": " + urlServer.getDocid(urlServer.getURL(i)));
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
}
