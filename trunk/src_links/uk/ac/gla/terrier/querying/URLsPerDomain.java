package uk.ac.gla.terrier.querying;

import gnu.trove.TIntIntHashMap;
import uk.ac.gla.terrier.links.DomainServer;
import uk.ac.gla.terrier.matching.ResultSet;

/**
 * A filter for allowing to retrieve up to a maximum number of 
 * documents from a particular domain. It employs a domain server, 
 * in order to retrieve the information related to the domain of 
 * documents.
 * @author Craig Macdonald, Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class URLsPerDomain implements PostFilter {

	/** The domain server providing the identifiers for a given URL. */
	protected DomainServer domains = null;
	
	/** The maximum number of results permitted from a given domain. */
	protected int urlsLimit = 0;
	
	/** The default value for the maximum number of results permitted from a given domain. */
	protected static final String defaultLimit = "0";
	
	/** 
	 * The hashmap that saves the current state of the filter. 
	 * This hashmap saves a mapping from a domain id to the number
	 * of retrieved documents from that particular domain.  
	 */
	protected TIntIntHashMap urlsInDom;

	/**
	 * Prepares the filter to process a new query. Initialises the 
	 * limit and the hashmap, and obtains the domain server from the 
	 * employed manager.
	 * @param m the employed manager.
	 * @param srq the search request to process
	 * @param rs the set of retrieved documents
	 */
	public void new_query(Manager m, SearchRequest srq, ResultSet rs) {
		String tmp = srq.getControl("urlsperdom");
		if (tmp.length() ==0)
			tmp = defaultLimit;
		urlsLimit = Integer.parseInt(tmp);
		if (urlsLimit > 0)
			urlsInDom = new TIntIntHashMap();
		domains = (DomainServer) m.getIndex().getIndexStructure("domain");
	}
	
	/**
	  * Called for each result in the resultset, used to filter out unwanted results.
	  * @param m The manager controlling this query
	  * @param srq The search request being processed
	  * @param DocAtNumber which array index in the resultset have we reached
	  * @param DocNo The document number of the currently being procesed result.
	  */
	public byte filter(Manager m, SearchRequest srq, ResultSet rs, int DocAtNumber, int DocNo)
	{
		if (urlsLimit ==0)
			return FILTER_OK;
	
		int domainId = domains.getDomainId(DocNo);
				
		if (!urlsInDom.contains(domainId))
		{
			urlsInDom.put(domainId, 1);
			return FILTER_OK;
		}
		urlsInDom.increment(domainId);
		int count = urlsInDom.get(domainId);
		if (count > urlsLimit)
			return FILTER_REMOVE;
		return FILTER_OK;
	}
}
