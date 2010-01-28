package uk.ac.gla.terrier.links;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectByteIterator;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.structures.Index;
/**
 * Implements a data structure that saves on disk an id
 * related to a part of the document's URL, such as the 
 * domain, or a combination of the domain and the path.
 * This structure is used in order to have fast access to
 * URL related information, without having to read the 
 * text of a URL.
 * @author vassilis
 * @version $Revision: 1.1 $
 */
public class DomainServer {

	/** The array that holds the data in memory. */
	private int[] data = null;
	
	/** Maps the Given String DomainID to the Domain **/
	private HashMap<String, String> domains;
	
	private int currentIndex;
	
	/** The logger used for reporting informative messages, or errors. */
	private static Logger logger = Logger.getRootLogger();
	
	/** The extension of the generated file on disk. */
	private static String extension = ".domains.oos.gz";
	
	/** The Number of Distinct Domains **/
	private int noDomains;
	
	/** 
	 * Constructs a domain server with the given path and prefix.
	 * @param path the absolute path to the domain server's data file.
	 * @param prefix the prefix of the domain server's data file.
	 */
	public DomainServer(String path, String prefix) {
		try {
			domains = new HashMap<String, String>();
			String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix + extension;
			ObjectInputStream ois = new ObjectInputStream(Files.openFileStream(filename));
			data = (int[]) ois.readObject();
			noDomains = ois.readInt();
			final int nds = ois.readInt();
			for(int i=0; i<nds; i++){
				String domain = (String)ois.readObject();
				int domainId = ois.readInt();
				domains.put( domainId+"", domain );		
			}
			ois.close();
			currentIndex = 0;
			logger.info("domain server: loaded from file " + filename);
		} catch(Exception e) {
			logger.error("exception while loading domain server.", e);
		}
	}
	
	/**
	 * Generates the data file of the domain server. The data file is
	 * saved in the location specified by the given path and prefix. 
	 * <br>
	 * The property <tt>domain.server.urlpart</tt> specifies which part
	 * of the document's URL will be used by the domain server. By default, 
	 * the value of this property is <tt>DOMAIN</tt>, and generates a domain
	 * server, which assigns the same identifier for each domain
	 * in a set of documents. If the property is set equal to <tt>DOMAIN_PATH</tt>, 
	 * then the domain server uses both the domain and the path of each URL to 
	 * assign a unique identifier. 
	 * @param path the absolute path to create the data file.
	 * @param prefix the prefix of the data file.
	 * @throws IOException if there is any problem during reading or writing
	 *		 the data on and from the disk.
	 */
	public static void create(String path, String prefix) throws IOException {
		String outputFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + extension;
			
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		TObjectIntHashMap<String> domains = new TObjectIntHashMap<String>();
		
		TIntArrayList tempData = new TIntArrayList();
		
		int domainId = 0;
				
		String line = null;
		String url = null;
		String domain = null;
		
		TObjectIntHashMap<String> depth = new TObjectIntHashMap<String>();
		depth.put("DOCUMENT.PATH", 0);
		depth.put("DEPTH_1",1);
		depth.put("DEPTH_2",2);
		depth.put("DEPTH_3",3);
		
		
		String urlPart = ApplicationSetup.getProperty("domain.server.urlpart","DEPTH_1");
		//by default, or if a wrong value has been provided, equivalent to DOMAIN.
		//If more options become available later, a hashmap should be introduced.
		int urlPartId = depth.get(urlPart);
		
		int lineNo = 1;
		while ((line = br.readLine())!=null) {
			url = URLServer.normaliseURL(line.split("\\s+")[1]);
			
			if (url.indexOf('/') == -1 && url.indexOf('?') > 0)
				url = url.replace("?", "/?");
			
			if(urlPartId == 0){
				domain = url.substring(0,url.lastIndexOf('/'));
			}else{
				System.out.println(url);
				//System.out.println("Depth ="+urlPartId);
				
				String temp = url;
				int index = 0;
				for(int i=0; i<urlPartId; i++){
					try{
						index = index + temp.indexOf('/');
						temp = temp.substring( temp.indexOf('/')+1);
					}catch (Exception e) {
						e.printStackTrace();
						System.err.println(e.getLocalizedMessage());
						System.out.println(line);
						System.out.println(url);
						System.out.println(temp);
						System.exit(-1);
					}
				}
				url = url.substring(0, index+urlPartId-1 );
				domain = url;
			}
					
			//System.out.println("Line "+lineNo+++" : "+domain);
			
			if (domains.containsKey(domain)) {
				tempData.add(domains.get(domain));
			} else {
				domains.put(domain, domainId);
				tempData.add(domainId);					
				domainId++;
			}
		}
		
		logger.info("found " + domains.size() + " distinct domains.");
		ObjectOutputStream oos = new ObjectOutputStream(
			Files.writeFileStream(outputFilename));
		oos.writeObject(tempData.toNativeArray());
		oos.writeInt(domainId);
		
		Object[] keys = domains.keys();
		oos.writeInt(keys.length);
		for(int i=0; i<keys.length; i++){
			oos.writeObject( (String)keys[i] );
			oos.writeInt(domains.get((String)keys[i]));
		}
		oos.flush();
		oos.close();

		final Index i = Index.createIndex(path, prefix);
		if (i != null)
		{
			i.addIndexStructure("domain", "uk.ac.gla.terrier.links.DomainServer");
			i.flush();
			i.close();
		}
	}
	
	/**
	 * Returns the array with all the data.
	 * @return array containing all the domain identifiers.
	 */
	public int[] getData() {
		return data;
	}
	
	/**
	 * Returns the domain identifier for the given document identifier.
	 * @param docid the document identifier.
	 * @return the domain identifier.
	 */
	public int getDomainId(int docid) {
		return data[docid];
	}

	/**
	 * Returns the number of distinct domains found by the domain server
	 * @return the number of distinct domains
	 */
	public int getNoDomains(){
		return noDomains;	
	}
	
	/**
	 * Returns the Domain(URL PART) for domainNo
	 * @param the Domain Number or Site Number
	 * @return the Domain
	 */
	public String getDomain(String domainNo){
		return domains.get(domainNo);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getNextDocID(){
		if( currentIndex < data.length){
			return currentIndex++;
		}else{
			return -1;
		}
	}
	
	/**
	 * @param domainNo
	 * @return The Domain Id
	 */
	public int getDomainId(String domainNo){
		return Integer.parseInt(domainNo.substring(1));
	}
	
	public HashMap<String, String> getMap(){
		return domains;
	}
	/**
	 * 
	 * @param i
	 * @return
	 */
	public static String toString(int i){
		String it = "";
		
		for(int z=9; z >= 0; z--){
			final int lastdigit = i%10;
			i = i / 10;
			it = lastdigit+it;
		}
		return it;
	}
	
	public void close() {}
	
	/**
	 * The main method used to create the domain server.
	 * If the creation of the domain server fails for
	 * some reason, then the program returns with exit
	 * code 1.
	 * @param args the command line arguments for the
	 *		domain server. There are two arguments. The
	 *		first one is the absolute path to the data file
	 *		of the server. The second one is the prefix of 
	 *		the data file.
	 */
	public static void main(String[] args) {
		try {
			String path = args[0];
			String prefix = args[1];
		
			DomainServer.create(path, prefix);
		} catch(Exception e) {
			logger.error("Exception while creating the domain server", e);
			System.exit(1);
		}
	}
	
}
