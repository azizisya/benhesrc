package uk.ac.gla.terrier.applications;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.HashSet;

class DumpAnchorDocFiles
{

	static HashSet startedDocs = new HashSet(1000);
	
	//static int ProcessID;
	static String OutputDir;

	static int AnchorsProcessed = 0;

	public static void main(String args[])
	{
		if (! args[0].endsWith("/"))
			args[0] = args[0] + '/';
		OutputDir = args[0];

		if (! (new File(OutputDir)).exists())
		{
			System.err.println("OutputDir "+OutputDir + " does not exist");
			System.exit(1);
		}


		//ProcessID = Integer.parseInt(args[1]);
		for (int i=1; i<args.length;i++)
		{
			try{
				processFile(args[i]);
			}catch (IOException ioe) {
				System.err.println("IOException: "+ioe);
				ioe.printStackTrace();
			}
		}

	}

	static void processFile(String filename) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new GZIPInputStream(
					new FileInputStream(filename)
				), 
			"UTF-8"));

		String line = null;
		while((line = br.readLine()) != null)
		{
			try{
				if (line.length() == 0)
					continue;

				int FirstSpace = line.indexOf(' ');
				int SecondSpace = line.indexOf(' ', FirstSpace+1);
				int ThirdSpace = line.indexOf(' ', SecondSpace+1);

				String srcDoc = line.substring(0,FirstSpace);
				String srcLang = line.substring(FirstSpace+1,SecondSpace);
				String targetDoc = line.substring(SecondSpace+1,ThirdSpace);
				String anchorText = line.substring(ThirdSpace+1).trim();
			
				if(anchorText.length() == 0)
					continue;
	
	
				AnchorsProcessed++;
				writeEntry(srcDoc, srcLang, targetDoc, anchorText);

			}catch(Exception e) {
				System.err.println("Exception: "+e);
                e.printStackTrace();
			}
		}
		System.err.println("Processed "+AnchorsProcessed + " anchors into "+ startedDocs.size() + " files");
	}

	static void writeEntry(String srcDoc, String srcLang, String targetDoc, String anchorText) throws IOException
	{
		PrintWriter out = new PrintWriter(
			new OutputStreamWriter(
				new FileOutputStream(OutputDir+targetDoc/*+"-"+ProcessID*/, true/*append*/),
				"UTF-8"
				)
			);
		out.println("<ATEXT src=\""+srcDoc+"\" lang=\""+srcLang+"\">"+anchorText+"</ATEXT>");
		out.close();
		startedDocs.add(targetDoc);
	}

}

