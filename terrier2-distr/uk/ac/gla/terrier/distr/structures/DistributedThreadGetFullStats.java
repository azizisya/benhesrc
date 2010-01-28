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

public class DistributedThreadGetFullStats extends DistributedThread {
	
	protected String term;
	
	protected Manager dr;
	/**
	 * The statistics of the given term on the server.
	 */
	protected double[] termStats;
	
	public DistributedThreadGetFullStats(DistributedMatchingServer dmatch,
			String servername,
			String id,
			String term,
			Manager dr) {
		super(dmatch, servername, id);
		this.term = term;
		this.dr = dr;
		this.start();

	}

	public void run() {
		try {
			termStats = dmatch.getFullStats(term);
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
	
	public double[] getTermStats(){
		return this.termStats;
	}
}
