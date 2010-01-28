package uk.ac.gla.terrier.querying;
import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.collections.map.LRUMap;

import uk.ac.gla.terrier.links.MetaIndex;
import uk.ac.gla.terrier.matching.ResultSet;
  /** @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
public class URLsPerDirectory implements PostFilter
{
	
	protected MetaIndex metaIndex = null;
	protected LRUMap metaIndexCache = null;
	
	protected int urlsLimit = 0;
	protected static final String defaultLimit = "0";
	protected TObjectIntHashMap urlsInDir;
	public void new_query(Manager m, SearchRequest srq, ResultSet rs)
	{
		String tmp = srq.getControl("urlsperdir");
		if (tmp.length() ==0)
			tmp = defaultLimit;
		urlsLimit = Integer.parseInt(tmp);
		if (urlsLimit > 0)
			urlsInDir = new TObjectIntHashMap();
		
		metaIndex = (MetaIndex)m.getIndex().getIndexStructure("meta");
		metaIndexCache = (LRUMap) m.getIndex().getIndexStructure("metacache");
	}

	String[] keys = new String[] {"url", "abstract", "title"};
	
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

		String[] metadata = null;
		
		synchronized(metaIndexCache) {
			try {
				Integer DocNoObject = new Integer(DocNo);
				if (metaIndexCache.containsKey(DocNoObject))
						metadata = (String[]) metaIndexCache.get(DocNoObject);
				else {
					metadata = metaIndex.getItems(keys, DocNo);
					metaIndexCache.put(DocNoObject,metadata);
				}
			} catch(IOException ioe) {} 
		}

		
		try{
			//URL normal = normaliseURL(new URL("http://" + rs.getMetaItem("url", DocAtNumber)));
			URL normal = normaliseURL(new URL(metadata[0]));
			//System.out.println(new URL("http://" + rs.getMetaItem("url", DocAtNumber)).toString() + " => "+
			//	normal.toString());
			if (! urlsInDir.containsKey(normal))
			{
				urlsInDir.put(normal, 1);
				return FILTER_OK;
			}
			urlsInDir.increment(normal);
			int count = urlsInDir.get(normal);
			if (count > urlsLimit)
				return FILTER_REMOVE;
			return FILTER_OK;
		}catch (MalformedURLException mue) {
			return FILTER_OK;
		}
	}

	private URL normaliseURL(URL in) throws MalformedURLException
	{
		String path = in.getPath();
		int end = path.lastIndexOf('/');
		end = (end == -1) ? path.length() : end;
		path = path.substring(0, end);

		return new URL(""+in.getProtocol() + "://" + in.getHost() +":"+ in.getPort() + path);
	}
}
