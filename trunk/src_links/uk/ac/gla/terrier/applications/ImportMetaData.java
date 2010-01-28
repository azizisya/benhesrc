package uk.ac.gla.terrier.applications;

import uk.ac.gla.terrier.links.MetaServer2;
import uk.ac.gla.terrier.links.MetaIndex;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

class ImportMetaData
{

	static MetaIndex metaServer = new MetaServer2();

	public static void main(String[] args)
	{
		int added= 0;
		String InputFilename = args[0]; String tag = args[1].trim();
		System.out.println("Reading "+args[0]+" for meta item "+args[1].trim());
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
			System.err.println("Error metaimport:"+e);
			System.exit(1);
		}
		String line; int last_docid =-1;
		try
		{
			while ((line = br.readLine()) != null) 
			{
				int docid = Integer.parseInt( line.substring(0, line.indexOf(' ')) );
				//	(new Integer(line.substring(0, line.indexOf(' ')))).intValue();
				String title = line.substring(line.indexOf(' ') + 1, line.length());
			
				if (docid - last_docid > 1)	
					for(int i=last_docid+1;i<docid;i++)
					{
						metaServer.addItem(tag, docid, "");
						System.err.println("Added a default metaitem @"+i+","+last_docid);
					}
				
				/* replace anything not (space, char, digit, or puncutation), including 
				 * some random control characters
				 * then replace some <>& with their XML entities
				 */
				metaServer.addItem(tag, docid, 
					title.replaceAll("[^\\w\\s\\d\\p{Punct}]|\f|\013|\014", "")
						.replaceAll("<", "&lt;").replaceAll(">", "&gt;")
						.replaceAll("&", "&amp;").trim());
				added++;	
				last_docid = docid;
			}
			metaServer.close();
			br.close();
			System.err.println("Added "+added+" items");
		}
		catch(Exception e)
		{
			System.err.println(last_docid +"Error2 metaimport:"+e);
			e.printStackTrace(System.err);
		}



	}

}
