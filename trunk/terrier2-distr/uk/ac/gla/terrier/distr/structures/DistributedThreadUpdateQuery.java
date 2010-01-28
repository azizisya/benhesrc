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
import uk.ac.gla.terrier.querying.Manager;

public class DistributedThreadUpdateQuery extends DistributedThread {
	
	protected LocalMatchingQueryTerms lmqt;
	
	protected Manager dr;
	
	public DistributedThreadUpdateQuery(DistributedMatchingServer dmatch,
			LocalMatchingQueryTerms lmqt,
			Manager dr,
			String servername,
			String id) {
		super(dmatch, servername, id);
		this.lmqt = lmqt;
		this.dr = dr;
		this.start();

	}

	public void run() {
		try {
			lmqt = dmatch.updateQuery(lmqt);
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
	
	public LocalMatchingQueryTerms getUpdatedQuery(){
		return this.lmqt;
	}
}
