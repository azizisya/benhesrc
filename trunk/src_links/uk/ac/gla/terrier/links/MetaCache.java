package uk.ac.gla.terrier.links;
import org.apache.commons.collections.map.LRUMap;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** An LRUMap, where the size of the cache is depicted by the property
  * <tt>metadata.cache.size</tt>. Default size 50000.
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
public final class MetaCache extends LRUMap
{
	public MetaCache()
	{
		super(Integer.parseInt(ApplicationSetup.getProperty("metadata.cache.size","50000")));
	}
}
