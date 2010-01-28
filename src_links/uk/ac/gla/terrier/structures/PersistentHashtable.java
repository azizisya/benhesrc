package uk.ac.gla.terrier.structures;

import java.util.Enumeration;

/**
 * This interface does not extend Map, which is the parent interface of
 * Hashtable as the JDBM interface has direct string support. This saves
 * the casting to Objects and back.
 */
public interface PersistentHashtable
{
	public void clear();
	
	public boolean containsKey(String key);
	
	public boolean equals(Object o);

	public String get(String key);
	
	public int hashCode();

	public boolean isEmpty();

	public Enumeration keys();

	public Enumeration values();
	
	public void put(String key, String value);

	public void remove(String key);

	public int size();

	public void close();	
}
