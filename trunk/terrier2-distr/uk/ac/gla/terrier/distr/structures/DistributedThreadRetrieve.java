/*
 * Created on 2006-5-23
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.distr.structures;

import uk.ac.gla.terrier.distr.matching.DistributedMatchingServer;
import uk.ac.gla.terrier.distr.querying.DistributedFieldManager;
import uk.ac.gla.terrier.distr.querying.DistributedThreeManager;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.Manager;

public class DistributedThreadRetrieve extends DistributedThread {
	
	protected int serverIndex;
	
	protected DistributedMatchingQueryTerms dq; 
	protected ResultSet drs; 
	protected int numberOfRetrievedDocuments;
	
	protected Manager dr;
	/**
	 * The statistics of the given term on the server.
	 */
	protected double[] termStats;
	
	public DistributedThreadRetrieve(DistributedMatchingServer dmatch,
			String servername,
			String id,
			int serverIndex,
			DistributedMatchingQueryTerms dq, 
			ResultSet drs, 
			int numberOfRetrievedDocuments,
			Manager dr) {
		super(dmatch, servername, id);
		this.serverIndex = serverIndex;
		this.dq = dq;
		this.drs = drs;
		this.numberOfRetrievedDocuments = numberOfRetrievedDocuments;
		this.dr = dr;
		this.start();

	}

	public void run() {
		try {
			long startQueryingTime = System.currentTimeMillis();
			drs = (QueryResultSet)dmatch.retrieve(dq, this.numberOfRetrievedDocuments);
			if (drs==null){
				System.err.println("Warning: EMPTY RESULT SET.");
			}
			String[] serverIndexStrings = new String[drs.getExactResultSize()];
			java.util.Arrays.fill(serverIndexStrings, ""+serverIndex);
			drs.addMetaItems("serverIndex", serverIndexStrings);
			System.err.println("Querying on server "+this.serverName+"-"+id + " finished in "
				+(System.currentTimeMillis() - startQueryingTime)+
				"ms with "+drs.getExactResultSize()+ " retrieved documents");
		} catch(Exception e) {
			System.err.println("Exception while updating query from server"+serverName+"-"+id+ ".");
			System.err.println(e.getMessage());
			e.printStackTrace();			
		}
		if (dr instanceof DistributedThreeManager)
			((DistributedThreeManager)dr).queryThreadFinished();
		else if (dr instanceof DistributedFieldManager)
			((DistributedFieldManager)dr).queryThreadFinished();
	}
	/**
	 * Returns the result set that contains the retrieved documents.
	 * @return the distributed result set containing the retrieved documents.
	 */
	public ResultSet getResultSet() {
		return drs;
	}
	
}
