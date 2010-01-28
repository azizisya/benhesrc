package uk.ac.gla.terrier.structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import java.io.IOException;
import java.util.Vector;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class SingleLineTRECQuery extends uk.ac.gla.terrier.structures.TRECQuery
{
	public SingleLineTRECQuery() {
		super();
	}

	public SingleLineTRECQuery(File queryfile){
		super(queryfile);
	}

	public SingleLineTRECQuery(String queryfilename){
		super(queryfilename);
	}

	public boolean extractQuery(String queryfilename, Vector vecStringQueries, Vector vecStringIds)
	{
		boolean gotSome = false;
		final boolean QueryLineHasQueryID = ApplicationSetup.getProperty("SingleLineTRECQuery.queryid.exists","true").trim().equals("true");
		System.err.println("Extracting queries from "+queryfilename + " - queryids "+QueryLineHasQueryID);
		try {
			BufferedReader br;
			File queryFile = new File(queryfilename);
			if (!(queryFile.exists() && queryFile.canRead())) {
				System.err.println("The topics file " + queryfilename + " does not exist, or it cannot be read.");
				return false;
			}
			br = Files.openFileReader(queryfilename, "UTF-8");	

			String line = null;
			int queryCount =0;
			while((line = br.readLine()) != null)
			{
				line = line.trim();
				if (line.startsWith("#"))
				{
					//comment encountered - skip line
					continue;
				}
				queryCount++;
				String queryID;
				String query;
				if (QueryLineHasQueryID)
				{
					final int firstSpace = line.indexOf(' ');
					queryID = line.substring(0,firstSpace);
					query = line.substring(firstSpace+1).replaceAll("\\.", " ");
				}
				else
				{
					query = line.replaceAll("\\.", " ");
					queryID = ""+queryCount;
				}
				vecStringQueries.add(query);
				vecStringIds.add(queryID);
				gotSome = true;
				System.err.println("Extracted queryID "+queryID+" "+query);
			}

		} catch (IOException ioe) {
			System.err.println("IOException while extracting queries: "+ioe);	
			ioe.printStackTrace();
			return gotSome;
		}
		System.err.println("Extracted "+ vecStringQueries.size() + " queries");
		return gotSome;
	}
}
