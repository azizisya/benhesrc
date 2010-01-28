package uk.ac.gla.terrier.links;

import gnu.trove.TObjectIntHashMap;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//for File Input work
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.compression.BitFile;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * A GroupingServer allows mappings between a document ID and sets of URLs of which it is a members.
 * Examples might be all home directories on a site, originally described by the regex www.dcs.gla.ac.uk/~.
 * <B>NB:</B><br/>Note that this implementation of GroupingIndex is limited to 32 groups maximum.
 */
public class GroupingServer implements GroupingIndex
{
	private static int maxGroups = 32;
	/**
	 * The compressed data file object we're working with
	 */
	private BitFile file;

	/**
	 * The number of groups described in the description file
	 */
	private int NoOfGroups = 0;

	/**
	 * The names of the groups described in the description file, and their bit offsets in each entry
	 */
	private TObjectIntHashMap GroupOffsets = new TObjectIntHashMap();


	/**
	 * Creates a Grouping Server
	 * @param GroupingDescriptionFilename The filename of the config file used to create the groups. This is required
	 * so that a mapping between group names and bit offsets can occur
	 * @param GroupingDataFile Path to the grouping data file. 
	 */
	public GroupingServer(String GroupingDescriptionFilename, String GroupingDataFile)
	{
		file = new BitFile(GroupingDataFile);
		getGroupNames(GroupingDescriptionFilename, maxGroups);
	}

	/** Returns true if the specified group name is a valid group
	  * @param GroupName the name of the group to be tested
	  * @return true if GroupName is a valid group */
	public boolean isGroup(String GroupName)
	{
		return GroupOffsets.containsKey(GroupName.toLowerCase());
	}

	/**
	 * Determines whether a given docId is a member of the named Group
	 * @param GroupName is docID a member of this Group?
	 * @param docId the docId to check 
	 */
	public boolean isMember(String GroupName, int docId)
	{
		int bitId = -1 + GroupOffsets.get(GroupName.toLowerCase()) ;
		//first check that group actually exists
		if (bitId == -1)
			return false;//a document is never a member of any group by default
		return isMemberOf(bitId, getDataEntry(docId));
	}

	/**
	 * Returns a list of group names that the given document is a member of
	 * @param docId The document in question.
	 */
	public String[] Membership(int docId)
	{
		//will contain the group names that the document is a member of.
		ArrayList membership = new ArrayList();

		//obtain the document's entry from the data file
		int DataEntry = getDataEntry(docId);

		//for each Group, 
		String groupNames[] = (String[])GroupOffsets.keys(); final int l = groupNames.length;
		for (int i=0;i<l;i++)
		{
			//check if this groups bit is set in the data entry
			if (isMemberOf(GroupOffsets.get(groupNames[i])-1, DataEntry))
				membership.add(groupNames[i]);
		}

		
		return (String[])membership.toArray(new String[0]);

	}


	/**
	 * Returns the data entry stored in the data file for that docId
	 * @param docId the document to retrieve the data file entry for
	 */
	public int getDataEntry(int docId)
	{
		int TotalBitOffset = docId * NoOfGroups;
		int EndOffset = TotalBitOffset + NoOfGroups -1;
		file.readReset(
			(long)(TotalBitOffset /8), 
			(byte)(TotalBitOffset %8),
			(long)(EndOffset /8),
			(byte)(EndOffset %8) );
		return file.readBinary(NoOfGroups);
	}


	/**
	 * Contains the arithmetic logic for testing membership. Returns true if Entry has the bit at Offset set to 1.
	 */
	private static boolean isMemberOf(int Offset, int Entry)
	{
		return ((Entry >> Offset) & 1)  ==1;
	}


	/**
	 * Populates GroupOffsets and NoOfGroups fields 
	 */
	private void getGroupNames(String InputFilename, final int MaxGroups)
	{
		BufferedReader br = null;
		try
		{
			if (InputFilename.toLowerCase().endsWith(".gz"))
				br =
					new BufferedReader(
					new InputStreamReader(
					new GZIPInputStream(
					new FileInputStream(InputFilename))));
			else
				br =
					new BufferedReader(
					new InputStreamReader(
					new FileInputStream(InputFilename)));
		}
		catch (Exception e)
		{
			System.err.println("Error opening grouping file : "+e);
			return;
		}

		String line = ""; int LineNo = 0;
		try
		{
			boolean inGroup = false; String GroupName = null;
			Pattern newGroup = Pattern.compile("\\[(\\w+)\\]");
			byte GroupID = 0; ArrayList thisGroup = null;
			
			//for each line in the file
			while ((line = br.readLine()) != null)
			{
				Matcher isNewGroup = newGroup.matcher(line);
				if (isNewGroup.matches())
				{
					GroupName = isNewGroup.group(1).toLowerCase(); //get the groupname from the parenthesis in the regex
					
					//add the bit number as the first element of the array
					//put +1 of group ids into hashmap
					//also post increment the group id counter
					GroupOffsets.put(GroupName, 1+(GroupID++) );
					inGroup = true;
					if (GroupID == MaxGroups)
					{
						System.err.println("ERROR: The used grouping index class does not support more than "+MaxGroups);
						System.err.println("Please reduce the number of groups in the groups_definition file, or use a different Grouping Index implementation");
					}
				}
				else if(! inGroup) //have we reached the first group marker yet?
				{
					System.err.println("Ignoring extraneous line in grouping descriptions file, line "+LineNo);
				}
			}
			br.close();
		}
		catch(Exception e)
		{
			System.err.println("Error parsing grouping file, line "+LineNo+" : "+e);
			System.err.println(line);
			e.printStackTrace(System.err);
		}
		NoOfGroups = GroupOffsets.size();
	}

	public static void main(String[] args) throws IOException
	{
		GroupingServer gs = new GroupingServer(
			ApplicationSetup.makeAbsolute("groups_definitions", ApplicationSetup.TERRIER_INDEX_PATH),
			ApplicationSetup.makeAbsolute(ApplicationSetup.TERRIER_INDEX_PREFIX+".groups", ApplicationSetup.TERRIER_INDEX_PATH)
			);
		if (args.length > 0 && args[0].equals("-print"))
		{
			uk.ac.gla.terrier.structures.CollectionStatistics CS = Index.createIndex().getCollectionStatistics();
			for(int i=0;i<CS.getNumberOfDocuments();i++)
				System.out.println(gs.getDataEntry(i));
		}
	}

}
