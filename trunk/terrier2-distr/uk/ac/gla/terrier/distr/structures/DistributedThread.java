package uk.ac.gla.terrier.distr.structures;

import uk.ac.gla.terrier.distr.matching.DistributedMatchingServer;
import uk.ac.gla.terrier.distr.querying.DistributedThreeManager;

/**
 * This class provides a standard interface for query threads.
 * @author ben
 *
 */
public class DistributedThread extends Thread {
	/**
	 * The associated matching server. 
	 */
	protected DistributedMatchingServer dmatch;
	/**
	 * The name of the server.
	 */
	protected String serverName;
	/**
	 * The id of the server.
	 */
	protected String id;
	
	
	public DistributedThread(
			DistributedMatchingServer dmatch,
			String servername,
			String id){
		this.dmatch = dmatch;
		this.serverName = servername;
		this.id = id;
	}
	
	
	
	public void run() {
		
	}
}

