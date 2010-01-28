package uk.ac.gla.terrier.utility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.compression.BitFile;
import uk.ac.gla.terrier.links.URLIndex;
import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;


/** URLGrouping class is responsible for creating a docid to URL sets subset
  * index, such that querying is supported on a subset of the documents.
  * Typically this is done using the group: directive supported by 
  * XMLOutputFormat and groups querying performed by links.GroupingIndex
  * implementations such as links.GroupingServer.
  * This class is responsible for creating a index (usually called dcs.groups)
  * which contains a fixed-length entry for each document in the colleciton.
  * Definitions are managed using a descriptrions file. In the description file,
  * each group is started by [GROUPNAME], followed by one or more regular 
  * expressions describing members of that group.
  * During group index creation, each URL is checked against each regular 
  * expression for each group, and set membership recorded in a bit array for
  * each document ID. This allows direct access to the index based on a 
  * calculation on document ID to obtain the offset to read.
  * <b>Sample Grouping Definitions File</b>
  * <code>
  * [IR]
  * ^www\.dcs\.gla\.ac\.uk/ir.*
  * ^ir\.dcs\.gla\.ac\.uk.*
  * ^www\.dcs\.gla\.ac\.uk/(?:~|%7e|%7E|people/personal/)(?:ounis|keith|jj|vassilis|ben|whiter|sachi|azreen|bailliem|sumitha|caid|skris|iraklis|xristina|alu|innes|rompas|jana|hzheng|gianni).*
  * [HOMES]
  * ^www(?:\.brc)?\.dcs\.gla\.ac\.uk/(?:~|%7e|%7E|people/personal/).*
  * </code>
  */
public class URLGrouping
{

	private static final String DEFAULT_Definitions = 
		"/local/terrier_tmp/macdonch/terrier/experiments/groups_definitions";
	private static final String DEFAULT_OutFile = 
		"/local/terrier_tmp/macdonch/terrier/experiments/InvFileCollection/dcs.groups";

	/** Default constructor. Calls other constructor using some hardcoded location paths. */
	URLGrouping()
	{
		this(DEFAULT_Definitions, DEFAULT_OutFile);
	}
	
	/** Creates the new groups index, based on the descriptions file. Data 
	 *  is saved in GroupDataFilename.
	 *  @param GroupDescriptionFile Filename of descriptions file
	 *  @param GroupDataFilename Filename to write document membership to */
	URLGrouping(String GroupDescriptionFile, String GroupDataFilename)
	{
		try{	
		//load in the regular expressions for each group
		Hashtable MappingREs = obtainMappingREs(GroupDescriptionFile);

		//load in the URL server components
		URLIndex urlserver = new URLServer(/*TODO */);

		//open up the output (gamma-compressed) file
		BitFile gcb = new BitFile(GroupDataFilename);
		gcb.writeReset();
		CollectionStatistics CS = null;
		try{
			CS = Index.createIndex().getCollectionStatistics();
		} catch (Exception ioe) {
			System.err.println("Couldn't determine num docs in index");
			return;
		}
		int numDocs = CS.getNumberOfDocuments();
		System.out.println(numDocs);
		//for each document
		for(int docid=0; docid < numDocs; docid++)
		{	
			//obtain the URL of this doc
			String URL = "";
			try
			{
				URL = urlserver.getURL(docid).toLowerCase();
			}
			catch(IOException ioe)
			{
				System.err.println("Failed to obtain a URL for docid "+docid +" : "+ioe);
			}

			int Status = 0; //this is the bit array of set membership - change type if more sets
			String sStatus = "";
			//check we obtained a valid URL. If not, assume the URL is a member of no groups
			if (URL != null && URL.length() > 0)
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
						if(Regex.matcher(URL).matches())
						{
							sStatus = sStatus + group_name + " ";
							Status |= 1 << Shift; 
							break; //and break out of the regex loop
						}
					}
				}
			}//if url length

			System.out.println("URL: "+URL+" Docid: "+docid+" Status:"+Status);
			gcb.writeBinary(MappingREs.size(), (int)Status);
		}
		gcb.writeFlush();
		gcb.close();
		} catch (Throwable t)  {  
			System.err.println("URLGroupingServer threw an exception: "+t);
			t.printStackTrace();
		}
	}

	/** Reads in the definitions file, and creates a Hashtable of Regex objects
	 *  for easy scanning of URLs using the REs to determine set membership
	 */
	private Hashtable obtainMappingREs(String InputFilename)
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
		catch (Exception e)
		{
			System.err.println("Error opening grouping file : "+e);
			return rtr;
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
					GroupName = isNewGroup.group(1); //get the groupname from the parenthesis in the regex
					thisGroup = new ArrayList();
					//add the bit number as the first element of the array
					thisGroup.add(new Integer(GroupID++).toString());
					rtr.put(GroupName, thisGroup);
					inGroup = true;
				}
				else if(! inGroup) //have we reached the first group marker yet?
				{
					System.err.println("Ignoring extraneous line in grouping descriptions file, line "+LineNo);
				}
				else
				{
					//save the next RE
					System.out.println("Adding "+line+" to "+GroupName);
					thisGroup.add(Pattern.compile(line));
				}
			}
			br.close();
		}
		catch(Exception e){
			System.err.println("Error parsing grouping file, line "+LineNo+" : "+e);
			System.err.println(line);
			e.printStackTrace(System.err);
			System.exit(1);
		}
		return rtr;
	}

	public static void main(String args[])
	{
		if (args.length != 2)
			System.err.println("Usage: java -Dterrier.links.setup=experiments/terrier.properties "+
				"-Dterrier.setup=experiments/terrier.properties uk.ac.gla.terrier.utility.URLGrouping "+
				"inputdescriptionfile outputdatafile");
		
		new URLGrouping(args[0], args[1]);
	}
}
