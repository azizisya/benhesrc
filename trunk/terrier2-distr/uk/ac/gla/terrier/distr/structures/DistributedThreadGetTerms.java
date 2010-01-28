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
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.utility.TerrierTimer;

public class DistributedThreadGetTerms extends DistributedThread {

	protected String[] docnos;
	
	protected DistributedMatchingQueryTerms dq;
		
	protected DistributedExpansionTerms distTerms;	
	
	protected Manager dr;
	
	public DistributedThreadGetTerms(DistributedMatchingServer dmatch,
			String servername,
			String id,
			Manager dr,
			String[] docnos,
			DistributedExpansionTerms distTerms) {
		super(dmatch, servername, id);
		this.docnos = docnos;
		this.distTerms = distTerms;
		this.dr = dr;
		this.start();

	}

	public void run() {
		try {
			TerrierTimer timer = new TerrierTimer();
			timer.start();
			if (dr instanceof DistributedThreeManager)
				distTerms = dmatch.getTerms(docnos, distTerms, ((DistributedThreeManager)dr).globalLexiconAddress);
			else if (dr instanceof DistributedFieldManager)
				distTerms = dmatch.getTerms(docnos, distTerms, ((DistributedFieldManager)dr).globalLexiconAddress);
			
			timer.setBreakPoint();
			System.err.println("Parsing on server "+serverName+"-"+id + " finished in "
					+ timer.toStringMinutesSeconds()+
					" with "+distTerms.terms.size()+ " extracted terms");
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
	
	public DistributedExpansionTerms getTerms(){
		return this.distTerms;
	}
}
