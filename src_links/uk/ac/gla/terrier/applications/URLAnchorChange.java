package uk.ac.gla.terrier.applications;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import uk.ac.gla.terrier.links.*;
import java.io.*;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;


/** This class maps anchor text from target URLs to target DOCNOs.
  */
class URLAnchorChange
{


	public static void main(String args[])
	{
		int NotFound = 0; int LineNo =0; int Orphan=0;
		int Total = 0; int Skipped = 0; int Empty = 0;
		PrintWriter OutFound = null;
		PrintWriter OutNotFound = null;
		BufferedReader In = null;

		//final String docnoPrefix = ApplicationSetup.getProperty("url.change.valid.docno.prefix", null);
		final boolean doAnchorText = Boolean.parseBoolean(ApplicationSetup.getProperty("anchors.url.change.atext", "true"));	
		final boolean doRedirDups = Boolean.parseBoolean(ApplicationSetup.getProperty("anchors.url.change.redirdup", "false"));
	
		if (args.length != 3)
		{
			System.err.println("Usage: uk.ac.gla.terrier.applications.URLAnchorChange data.anchor-text.sorted.gz data.anchor-text-found.gz data.anchor-text-notfound.gz");
			return;
		}

		System.err.println("Starting URLAnchorChange: In="+args[0]+ " OutFound="+args[1]+" OutNotFound="+args[2]);	
		try{
			OutFound = new PrintWriter(Files.writeFileWriter(args[1], "UTF-8"));
			
			OutNotFound = new PrintWriter(Files.writeFileWriter(args[2], "UTF-8"));
			In = Files.openFileReader(args[0], "UTF-8");
			
			URLIndex urls = new URLServer(); // new URLServer3();
			Index index = Index.createIndex();
			if (index == null)
			{
				System.err.println("Could not load an index at "+ApplicationSetup.TERRIER_INDEX_PATH+","+ApplicationSetup.TERRIER_INDEX_PREFIX);
				return;
			}
			DocumentIndex doci = index.getDocumentIndex(); //new DocumentIndex/*Encoded*/();
			URLNormaliser urln = doRedirDups ? new URLNormaliser() : null;

			String line = null; String lastURL = null; String lastDocNo = null;
			long timePer1000 = System.currentTimeMillis();
			NEXTLINE: while((line = In.readLine()) != null)
			{
				Total++; LineNo++;
				
				if (line.length() == 0)
				{
					Empty++;
					continue NEXTLINE;
				} 


				/*
				if (line.charAt(0) != 'E' //this is an orphan from the line above.
					&& last_dst != null) //and the previous line was a valid anchor
				{
					Orphan++;	
					line = line.trim();
					if(line.length() > 0)
						Out.println(last_src + " " + last_lang + " " + last_dst  + " " + line);
					continue NEXTLINE;
				}*/

				try
				{
					String parts[] = line.split(" ");
					String aText = "";
					if (parts.length <4)
					{
						Skipped++;
						continue NEXTLINE;
					}

					String srcDoc = parts[1];
					String targetURL = parts[0];
					String type = parts[2];
				
					if(doAnchorText)//work out what the atext should be
					{
						StringBuilder sb = new StringBuilder();
						for(int i=3;i<parts.length;i++)
						{
							sb.append(parts[i].replaceAll("\"","").trim());
							sb.append(" ");
						}
						aText = sb.toString().trim();
						if (aText.length() == 0)
						{
							Skipped++;
							continue NEXTLINE;
						}
					}
					

					//shortcut: check if url is same as before
					if (targetURL.equals(lastURL))
					{
						if (lastDocNo == null)
						{
							OutNotFound.println(targetURL+" "+srcDoc+" "+type +" " + aText);
							NotFound++;
						}
						else
						{
							OutFound.println(lastDocNo+" "+srcDoc+" "+type+ " "+ aText);
						}
						continue;
					}
					
					//apply normal url normalisation
					targetURL = URLServer.normaliseURL(targetURL);// URLNormaliser.normalise(targetURL);
					//apply advanved url normalisation, if enabled
					if (doRedirDups && targetURL != null)
						targetURL = urln.normaliseRedirDuplicates(targetURL);

					if (targetURL == null)//normaliser didn't like that one
					{
						OutNotFound.println(targetURL+" "+srcDoc+" "+type +" "+aText);
						NotFound++;
						continue;
					}
					else if (targetURL.equals(lastURL))
					{//shortcut: check if url is same as before
						if (lastDocNo == null)
						{
							OutNotFound.println(targetURL+" "+srcDoc+" "+type+" "+ aText);
							NotFound++;
						}
						else
						{
							OutFound.println(lastDocNo+" "+srcDoc+" "+type+" "+aText);
						}
						continue;
					}

					
					final int Docid	= urls.getDocid(targetURL);
					if (Docid != -1)
					{
						lastDocNo = doci.getDocumentNumber(Docid);
						lastURL = targetURL;
						OutFound.println(lastDocNo+" "+srcDoc+" "+type+" "+aText);
					}	
					else
					{
						lastURL = targetURL;
						lastDocNo = null;
						OutNotFound.println(targetURL+" "+srcDoc+" "+type+" "+aText);
						NotFound++;
					}
		
					if (Total %1000 == 0)
					{
						//System.out.println("Java memory in use = " + Runtime.getRuntime().totalMemory() +":"+ Runtime.getRuntime().freeMemory());
						long thisTime = System.currentTimeMillis();
						System.err.println("INFO: Anchor #"+Total+ "; time="+((thisTime-timePer1000)/1000));
						timePer1000 = thisTime;
						if (urls instanceof URLServer3)
							((URLServer3)urls).clearCache();
					}
				}catch(Exception e) {
					System.err.println("Input Line number "+LineNo);
					System.err.println(line);
					System.err.println(e);
					e.printStackTrace();
				}
			}
		//}
		}catch(Exception e) {
			 System.err.println(e);
					e.printStackTrace();
		} finally {
			OutFound.close();
			OutNotFound.close();
		}
		System.err.println("Processed "+Total+" anchors,  didn't find "+NotFound);
		System.err.println("Orphans "+Orphan+" Skipped "+Skipped + "Empty "+Empty);
	}

}

