package uk.ac.gla.terrier.links;

/** 
 * Interface for checking named set membershpi of documents.
 * Typical implementation is GroupingServer, which uses a file
 * containing series of Regexes to identify each Group.
 */
public interface GroupingIndex
{
	/** Check to see if GroupName is a valid group name. Group
	 * names are case insensitive.
	 */
	public boolean isGroup(String GroupName);

	/** Check to see if docId is a member of the group named GroupName.
	 * Group names are case insensitive.
	 */	
	public boolean isMember(String GroupName, int docId);

	/** Returns a list of the group names that docId is a member of.*/	
	public String[] Membership(int docId);
		

}
