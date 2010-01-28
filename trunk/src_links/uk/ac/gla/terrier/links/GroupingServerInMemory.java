package uk.ac.gla.terrier.links;

import gnu.trove.TIntArrayList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class GroupingServerInMemory implements GroupingIndex {

	private static Logger logger = Logger.getRootLogger();
	
	String groupDefFilename = null;
	
	String groupsFilename = null;
	
	int[] data = null;

	public GroupingServerInMemory(String path, String prefix) {
		groupDefFilename = ApplicationSetup.makeAbsolute("groups_definitions", path);
		groupsFilename = ApplicationSetup.makeAbsolute(prefix+".groups", path);
		
		try {
			ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(groupsFilename)));
			data = (int[]) ois.readObject();
			ois.close();
			getGroupNames(groupDefFilename);
			logger.info("grouping server: loaded from files " + groupDefFilename + " and " + groupsFilename);
			logger.info("grouping server: number of documents is " + data.length);
		} catch(Exception ioe) {
			logger.error("input output exception while loading grouping server.", ioe);
		}
	}
	
	/**
	 * Populates GroupOffsets and NoOfGroups fields 
	 */
	private void getGroupNames(String InputFilename)
	{
		BufferedReader br = null;
		try
		{
			if (InputFilename.endsWith(".gz") ||
				InputFilename.endsWith(".GZ"))
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
		} catch (Exception e) {
			logger.error("Error opening grouping file.",e);
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
				LineNo++;
				Matcher isNewGroup = newGroup.matcher(line);
				if (isNewGroup.matches())
				{
					GroupName = isNewGroup.group(1).toLowerCase(); //get the groupname from the parenthesis in the regex
					//thisGroup = new ArrayList();
					//add the bit number as the first element of the array
					//thisGroup.add(new Integer(GroupID++));
					GroupOffsets.put(GroupName, new Integer(GroupID++));
					inGroup = true;
				}
				else if(! inGroup) { //have we reached the first group marker yet?
					logger.debug("Ignoring extraneous line in grouping descriptions file, line "+LineNo + " line: " + line);
				}
			}
			br.close();
		}
		catch(Exception e) {
			logger.error("Error parsing grouping file, line "+LineNo+".",e);
			logger.error(line);
		}
		NoOfGroups = GroupOffsets.size();
	}

	
	public static void create(String path, String prefix) {
		String groupDefFilename = ApplicationSetup.makeAbsolute("groups_definitions", path);
		String groupsFilename = ApplicationSetup.makeAbsolute(prefix+".groups", path);
		
		//load in the regular expressions for each group
		Hashtable MappingREs = obtainMappingREs(groupDefFilename);

		//open up the output (gamma-compressed) file
		TIntArrayList gcb = new TIntArrayList();
		BufferedReader br = null;
		try {
			//open the url2id file
			br = new BufferedReader(new InputStreamReader(System.in));
		
			//for each document
			String url = null;
			int docid = 0;
			while ((url=br.readLine())!=null) {
				url = url.substring(0, url.indexOf(' '));
				url = URLServer.normaliseURL(url);
				
				int Status = 0; //this is the bit array of set membership - change type if more sets
				String sStatus = "";
				//check we obtained a valid URL. If not, assume the URL is a member of no groups
				if (url != null && url.length() > 0)
				{
					
					//let's iterate through the groups
					for(Enumeration eGroups = MappingREs.keys(); eGroups.hasMoreElements();)
					{
						String group_name = (String)eGroups.nextElement();
						ArrayList REs = (ArrayList)MappingREs.get(group_name);
						byte Shift = Byte.parseByte((String)REs.get(0));
						for(int i=1;i<REs.size(); i++)
						{
							Pattern Regex = (Pattern)REs.get(i);
							if(Regex.matcher(url).matches())
							{
								sStatus = sStatus + group_name + " ";
								Status |= 1 << Shift; 
								break; //and break out of the regex loop
							}
						}
					}
				}//if url length
	
				if (logger.isDebugEnabled())
					logger.debug("URL: "+url+" Docid: "+docid+" Status:"+Status);
				docid++;
				gcb.add((int)Status);
			}
			
			br.close();
			ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(groupsFilename)));
			oos.writeObject(gcb.toNativeArray());
			oos.flush();
			oos.close();
		
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}

	}

	public static void create(String path, String prefix, int[] sizes) {
		String groupDefFilename = ApplicationSetup.makeAbsolute("groups_definitions", path);
		
		int fileIndex = 0;
		
		//load in the regular expressions for each group
		Hashtable MappingREs = obtainMappingREs(groupDefFilename);

		//open up the output (gamma-compressed) file
		TIntArrayList gcb = new TIntArrayList();
		BufferedReader br = null;
		try {
			//open the url2id file
			br = new BufferedReader(new InputStreamReader(System.in));
		
			//BitFile gcb = new BitFile(GroupDataFilename);
			//gcb.writeReset();
			//System.out.println(CollectionStatistics.getNumberOfDocuments());
			//for each document
			String url = null;
			int docid = 0;
			while ((url=br.readLine())!=null) {
				url = url.substring(0, url.indexOf(' '));
				url = URLServer.normaliseURL(url);
				
				int Status = 0; //this is the bit array of set membership - change type if more sets
				String sStatus = "";
				//check we obtained a valid URL. If not, assume the URL is a member of no groups
				if (url != null && url.length() > 0)
				{
					
					//let's iterate through the groups
					for(Enumeration eGroups = MappingREs.keys(); eGroups.hasMoreElements();)
					{
						String group_name = (String)eGroups.nextElement();
						ArrayList REs = (ArrayList)MappingREs.get(group_name);
						byte Shift = Byte.parseByte((String)REs.get(0));
						for(int i=1;i<REs.size(); i++)
						{
							Pattern Regex = (Pattern)REs.get(i);
							if(Regex.matcher(url).matches())
							{
								sStatus = sStatus + group_name + " ";
								Status |= 1 << Shift; 
								break; //and break out of the regex loop
							}
						}
					}
				}//if url length
	
				if (logger.isDebugEnabled())
					logger.debug("URL: "+url+" Docid: "+docid+" Status:"+Status);
				
				docid++;
				gcb.add((int)Status);
				
				if (docid == sizes[fileIndex]) {
					String groupsFilename = ApplicationSetup.makeAbsolute(prefix+"_" + fileIndex +".groups", path);
					ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(groupsFilename)));
					oos.writeObject(gcb.toNativeArray());
					oos.flush();
					oos.close();
					docid = 0;
					gcb.clear();
					fileIndex++;
				}
			}
			
			br.close();
		
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}

	}

	
	
	/** Reads in the definitions file, and creates a Hashtable of Regex objects
	 *  for easy scanning of URLs using the REs to determine set membership
	 */
	private static Hashtable obtainMappingREs(String InputFilename)
	{
		//NB: Changes to this code needs to be mirrored in uk.ac.gla.terrier.links.GroupingServer
		Hashtable rtr = new Hashtable();
		BufferedReader br = null;
		try
		{
			//input file may be compressed
			if (InputFilename.endsWith(".gz") ||
				InputFilename.endsWith(".GZ"))
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
		catch (Exception e) {
			logger.error("Error opening grouping file.",e);
			return rtr;
		}

		
		String line = ""; int LineNo = 0;
		try
		{
			boolean inGroup = false; String GroupName = null;
			Pattern newGroup = Pattern.compile("\\[(\\w+)\\]");
			byte GroupID = 0; 
			ArrayList thisGroup = null;
			
			//for each line in the file
			while ((line = br.readLine()) != null)
			{
				Matcher isNewGroup = newGroup.matcher(line);
				if (isNewGroup.matches())
				{
					GroupName = isNewGroup.group(1); //get the groupname from the parenthesis in the regex
					thisGroup = new ArrayList();
					//add the bit number as the first element of the array
					thisGroup.add(new Integer(GroupID++).toString());
					rtr.put(GroupName, thisGroup);
					inGroup = true;
				}
				else if(! inGroup) {//have we reached the first group marker yet?
					logger.debug("Ignoring extraneous line in grouping descriptions file, line "+LineNo);
				}
				else {
					//save the next RE
					logger.debug("Adding "+line+" to "+GroupName);
					thisGroup.add(Pattern.compile(line));
				}
			}
			br.close();
		}
		catch(Exception e){
			logger.error("Error parsing grouping file, line "+LineNo+".",e);
			logger.error(line);
			System.exit(1);
		}
		return rtr;
	}

	/**
	 * The number of groups described in the description file
	 */
	private int NoOfGroups = 0;

	/**
	 * The names of the groups described in the description file, and their bit offsets in each entry
	 */
	private Hashtable<String,Integer> GroupOffsets = new Hashtable<String,Integer>();

	public boolean isGroup(String GroupName)
	{
		return GroupOffsets.containsKey(GroupName.toLowerCase());
	}

	/** 
	 * return the id of the group, or -1 if 
	 * there is no such group
	 * 
	 * @param GroupName
	 * @return id
	 */
	public int getGroupId(String GroupName) {
		Integer I = (Integer)GroupOffsets.get(GroupName.toLowerCase());
		if (I!=null)
			return I.intValue();
		return -1;
	}
	
	/**
	 * Determines whether a given docId is a member of the named Group
	 * @param GroupName is docID a member of this Group?
	 * @param docId the docId to check 
	 */
	public boolean isMember(String GroupName, int docId)
	{
		Object o = GroupOffsets.get(GroupName.toLowerCase());
		//first check that group actually exists
		if (o == null)
			return false;//TODO discuss this assumption
		return isMemberOf(((Integer)o).intValue(), data[docId]);
	}

	/**
	 * Returns a list of group names that the given document is a member of
	 * @param docId The document in question.
	 */
	public String[] Membership(int docId)
	{
		//will contain the group names that the document is a member of.
		ArrayList<String> membership = new ArrayList<String>();

		//obtain the document's entry from the data file
		int DataEntry = data[docId];

		//for each Group, 
		for(Enumeration eGroups = GroupOffsets.keys(); eGroups.hasMoreElements();)
		{
			String group_name = (String)eGroups.nextElement();
			//check if this groups bit is set in the data entry
			if (isMemberOf(((Integer)GroupOffsets.get(group_name)).intValue(), DataEntry))
				membership.add(group_name);
		}

		
		return (String[])membership.toArray(new String[0]);

	}
	
	/**
	 * Contains the arithmetic logic for testing membership. Returns true if Entry has the bit at Offset set to 1.
	 */
	private static boolean isMemberOf(int Offset, int Entry) {
		return ((Entry >> Offset) & 1)  ==1;
	}

	public int[] getData() {
		return data;
	}

	public static void main(String[] args) {
		
		try {
			if (args[0].equals("create")) {
				//params: path prefix [sizes...]
				int[] sizes = new int[args.length - 3];
				for (int i=3; i<args.length; i++) 
					sizes[i-3] = Integer.parseInt(args[i]);
				GroupingServerInMemory.create(args[1], args[2], sizes);
			} else if (args[0].equals("test")) {
				//params: path prefix group docid
				GroupingServerInMemory gsim = new GroupingServerInMemory(args[1], args[2]);
				logger.info("group: " + args[3] + " docid: " + Integer.parseInt(args[4]) + " member? " + gsim.isMember(args[3], Integer.parseInt(args[4])));
			}
		} catch(Exception e) {
			logger.error("Exception thrown during creating/testing GroupingServerInMemory.", e);
			System.exit(1);
		}
	}
}
