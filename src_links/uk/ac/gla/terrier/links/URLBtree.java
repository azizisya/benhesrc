package uk.ac.gla.terrier.links;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.recman.CacheRecordManager;
/**
 * Important - the default size of the btree should be set equal to 
 * 2000 in the source code of the BTree class.
 * @author vassilis
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class URLBtree {

    static String BTREE_NAME = "URLtoID";

    RecordManager recman;
    long          recid;
    Tuple         tuple = new Tuple();
    TupleBrowser  browser;
    BTree         tree;
    Properties    props;

    MessageDigest md5Hash;
    
    public URLBtree(String filename, String name) {
		md5Hash = null;
		try {
			md5Hash = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			System.err.println(
				"The choosen algorithm is not supported. Exception thrown during the initialization of MessageDigest.");
			nsae.printStackTrace();
			System.exit(1);
		}
    	
    	props = new Properties();
		//props.setProperty("jdbm.cache.size", 1);
    	//props.setProperty(RecordManagerOptions.CACHE_TYPE, RecordManagerOptions.SOFT_REF_CACHE); - was never fucking implemented
		props.setProperty("jdbm.disableTransactions", "true");
        try {
            // open database and setup an object cache
            recman = RecordManagerFactory.createRecordManager( filename, props );

            // try to reload an existing B+Tree
            recid = recman.getNamedObject( name );
            if ( recid != 0 ) {
                tree = BTree.load( recman, recid );
                //System.out.println( "Reloaded existing BTree with " + tree.size()
                                    //+ " famous people." );
            } else {
                // create a new B+Tree data structure and use a StringComparator
                // to order the records based on people's name.
                tree = BTree.createInstance( recman, new StringComparator() );
                recman.setNamedObject( name, tree.getRecid() );
                //System.out.println( "Created a new empty BTree" );
            }
        } catch(IOException ioe) {
        	ioe.printStackTrace();
        }
    }
    
    public void insert(String url, int docid) {
    	String domain;
    	String path;		
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
    	try {
    		tree.insert(new String(domainHash), new Integer(docid), false);
    	} catch(IOException ioe) {
    		ioe.printStackTrace();
    	}
    }
    
    public int find(String url) {
    	String domain;
    	String path;		
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
    	
    	//Integer docidInteger = null;
    	try {
    		Integer docidInteger = (Integer)tree.find(new String(domainHash));
        	if (docidInteger == null) 
        		return -1;
        	else return docidInteger.intValue();
    	} catch(IOException ioe) {
    		ioe.printStackTrace();
    	}
    	//if (docidInteger == null) 
    	//	return -1;
    	//else return docidInteger.intValue();
    	return -1;
    }
    
    public void commit() {
    	try {
    		recman.commit();
			clearCache();
    	} catch(IOException ioe) {
    		ioe.printStackTrace();
    	}
    }
    
    public void close() {
    	try {
    		recman.close();
    	} catch(IOException ioe) {
    		ioe.printStackTrace();
    	}
    }
    

    
    /**
     * Example main entrypoint.
     */
    public static void main( String[] args ) {
    	URLBtree tree = new URLBtree("C:\\tree", "1");
    	tree.insert("http://www.dcs.gla.ac.uk/", 0);
    	tree.insert("http://www.dcs.gla.ac.uk/~vassilis", 1);
    	tree.commit();
    	System.out.println(tree.find("http://www.dcs.gla.ac.uk/"));
    	System.out.println(tree.find("http://www.dcs.gla.ac.uk/~vassilis"));
    	
    	tree.close();
    }

    public void clearCache() {
    	((CacheRecordManager)recman).getCachePolicy().removeAll();
    }
}

