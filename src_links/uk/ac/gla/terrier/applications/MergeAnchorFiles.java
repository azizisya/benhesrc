package uk.ac.gla.terrier.applications;

import java.io.*;
import java.util.*;
import java.util.zip.*;

class MergeAnchorFiles
{
	static String domain;
	
	static BufferedReader docno2url;
	static PrintWriter domainOut;

	static String anchorsStoreIn;
	static int InStart = 0;
	static int InEnd = 156;

	static int anchorCount =0;

	static HashSet filesExisting = new HashSet(1000000);

	public static void main(String[] args)
	{
	
		if(args.length == 0)
		{
			System.err.println("Usage: uk.ac.gla.terrier.applications.MergeAnchorFiles domain docidlist inputfolder inputfilelist outpustfile.gz");
			System.exit(1);
		}

		try{
			domain = args[0];

			if(args[1].toLowerCase().endsWith(".gz"))
			{
				docno2url = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(new FileInputStream(args[1]))));
			}
			else
			{
				docno2url = new BufferedReader(new FileReader(args[1]));
			}
			anchorsStoreIn = args[2];
			if (!anchorsStoreIn.endsWith("/"))
				anchorsStoreIn = anchorsStoreIn + "/";

		
			readFileList(args[3]);

	

			domainOut = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(
						new GZIPOutputStream(
							new FileOutputStream(args[4])
						),
						"UTF-8"
					)
				));
	
			domainOut.println("<EuroGOV:bin domain=\"at\" id=\"001\">");

			String line = null; int FileCount = 0; int DocCount = 0;
			while((line = docno2url.readLine()) != null)
			{
				DocCount++;
				String[] parts = line.split("\\s+");
				FileCount += processDoc(parts[0]);

			}
			domainOut.println("</EuroGOV:bin>");
			//domainOut.close();
			System.err.println("Wrote domain "+domain+" of "+DocCount+" docs with "+anchorCount+" anchors from "+FileCount+" files");

			domainOut.close();domainOut = null;
			docno2url.close();docno2url = null;

		}catch (IOException ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();
		}
	}

	static int processDoc(String docid)
	{
		int filesFound = 0; int Anchors = 0;
		domainOut.println("<EuroGOV:doc");
		domainOut.println(" id=\""+docid+"\"");
		domainOut.println(" charset=\"UTF-8\">");
		domainOut.println("<EuroGOV:content>\n<![CDATA[");
		for(int i=InStart;i<=InEnd;i++)
		{
			String filename = i+"/"+docid;
			if (filesExisting.contains(filename))
			{
				filesFound++;
				Anchors += processFile(new File(anchorsStoreIn+filename) );
			}
		}
		domainOut.println("]]>\n</EuroGOV:content>\n</EuroGOV:doc>");
		System.err.println("Found "+filesFound+" files, "+Anchors+" anchors for doc "+docid);
		anchorCount += Anchors;
		return filesFound;
	}

	static int processFile(File f)
	{
		int count = 0;
		BufferedReader br = null;
		try{
			br = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(f),
					"UTF-8")
				);
			String line = null;
			while((line = br.readLine()) != null)
			{
				count ++;
				line = line.trim();
				domainOut.println(line);
			}
			br.close();
		} catch (IOException ioe) {
			System.err.println("ioe : "+ioe);
			ioe.printStackTrace();	
		}
		return count;
	}

	static void readFileList(String f)
	{
		BufferedReader br = null;
		try{
			br = new BufferedReader(
				new FileReader(f));
			String l = null;
			while((l=br.readLine()) != null)
			{
				if(l.indexOf(domain) != -1)
				{
					filesExisting.add(l.trim());
				}
			}
		} catch (IOException ioe) {
			System.err.println("ioe : "+ioe);
			ioe.printStackTrace();
		}
	}

}
