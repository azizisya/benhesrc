package uk.ac.gla.terrier.querying;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.gla.terrier.links.URLIndex;
import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/** Reduces the resultset to only URLs contained in the most prominent top retrieved documents.
  * Ideal for focused query expansion. 
  * <b>Properties</b>
  * <ul>
  * <li><tt>prominentsite.examinetopdocuments</tt></li>
  * <li><tt>prominentsite.returntopdocuments</tt></li>
  * 
*/

public class ProminentSite implements PostProcess
{
	protected URLIndex urlServer = new URLServer();
	
	protected static final int ExamineTopDocuments = Integer.parseInt(ApplicationSetup.getProperty("prominentsite.examinetopdocuments", "10"));
	protected static final int ReturnTopDocument = Integer.parseInt(ApplicationSetup.getProperty("prominentsite.returntopdocuments", "10"));

	class FindLargestDomains implements TObjectIntProcedure
	{
		ArrayList largestDomains = new ArrayList(2);//rough guess of domains in the top X results
		int largestCount =0;
		public boolean execute(java.lang.Object tmpDomain, int tmpCount)
		{
			if(tmpCount > largestCount)
			{
				largestDomains.clear();
				largestDomains.add(tmpDomain);
				largestCount = tmpCount;
			}
			else if (largestCount > 0 && tmpCount == largestCount)
				largestDomains.add(tmpDomain);
			return true;
		}
	}

    public void process(Manager manager, SearchRequest q)
	{
		try{
		ResultSet resultSet = q.getResultSet();
		int[] docids = resultSet.getDocids();

		//find out the count of each domain in the top ExamineTopDocuments results
		TObjectIntHashMap domainCount = new TObjectIntHashMap(ExamineTopDocuments);
		for(int i=0; i<ExamineTopDocuments;i++)
		{
			String domain =  urlServer.getDomain(docids[i]);
			if (domainCount.containsKey(domain))
				domainCount.increment(domain);
			else
				domainCount.put(domain,1);
		}

		//now derive which domains were the most popular
		FindLargestDomains find = new FindLargestDomains();
		domainCount.forEachEntry(find);
		ArrayList largestDomains = find.largestDomains;
		int largestCount = find.largestCount;

		
		if (largestDomains.size() > 1)
		{
			System.err.println("WARNING: More than 1 domain ("+largestDomains.size()+") had the same number of "+
				"results in the top " + ExamineTopDocuments + " document. Leaving result unchanged.");
		}
		else
		{
			/* one domain is the clear winning site - remove all other domains
			   from the result set */
			String targetDomain = (String)largestDomains.get(0);
			//find out how many results will be in the new resultset
			int Size = Math.min(largestCount,ReturnTopDocument);
			System.err.println("INFO: Reducing resultset to "+Size+" results from "+targetDomain);
			int keepDocids[] = new int[Size];
			int i=0; int KeptCount =0;
			while(KeptCount<Size)
			{
				if (urlServer.getDomain(docids[i]).equals(targetDomain))
				{
					keepDocids[KeptCount++] = i;
				}
				i++;
			}
			//and reduce the result set to only the ones we want
			((Request)q).setResultSet(resultSet.getResultSet(keepDocids));
		}
		
		}catch (IOException ioe) {
			 System.err.println("WARNING: IOException while running ProminentSite filter");
		}
	}

    public String getInfo()
	{
		return "ProminentSite";
	}
}

