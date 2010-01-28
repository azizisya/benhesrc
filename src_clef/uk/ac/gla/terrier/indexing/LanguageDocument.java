package uk.ac.gla.terrier.indexing;

import java.io.*;
import java.util.regex.Pattern;
import java.util.Hashtable;




public class LanguageDocument
{

	static String classProgram = "/users/grad/craigm/research/text_cat/text_cat";
	static Runtime r = Runtime.getRuntime();
	static Process classifier = null;
	
	static Pattern greekMatch = Pattern.compile("\\p{InGreek}|\\p{InGreekExtended}");

	static Hashtable LanguageToAbbrev = new Hashtable();

	
	private static boolean IsGreek(final char[] chars)
	{
		for(int i=0;i<chars.length;i++)
		{
			final char c = chars[i];
			if ((c >= 0x0370 && c <= 0x03FF) || 
				(c >= 0x1F00 && c <= 0x1FFF))
				return true;
		}
		return false;
	}
	
	private static boolean IsRussian(final char[] chars)
	{
		for(int i=0;i<chars.length;i++)
		{
			final char c = chars[i];
			if (c >= 0x0400 && c <= 0x04FF)
				return true;
		}
		return false;
	}


	public static String classify(String s)
	{
		String rtr;
		/*if (Pattern.compile("[\u0370-\u03FF]").matcher(s).matches())
		{}
		String t = new String("\u0370");
		int i = (int)t.charAt(0);
		if (Pattern.compile("[\u0370-\u03FF]").matcher(t).matches())
		{}
		if (greekMatch.matcher(s).matches())
		{
			rtr = "greek";
		}*/
		char[] sArray = s.toCharArray(); 
		if (IsGreek(sArray))
		{
			rtr = "greek";
		}
		else if (IsRussian(sArray))
		{
			rtr = "russian";
		}
		else
		{
			BufferedReader pipeErr =null;
			try{
				classifier = r.exec(classProgram);
				PrintWriter pipeIn = new PrintWriter(classifier.getOutputStream());
				pipeErr = new BufferedReader(new InputStreamReader(classifier.getErrorStream()));
				//HERE ENCODING QUESTION
				BufferedReader pipeOut = new BufferedReader(new InputStreamReader(classifier.getInputStream()));
				pipeIn.println(s.toString());
				pipeIn.close();
				String lang = pipeOut.readLine().trim();
				if ("I don't know; Perhaps this is a language I haven't seen before?".equals(lang))
					lang = "unknown";
				rtr = lang;
				//System.out.println(id + " " + lang);
			}catch(IOException ioe) { 
				//System.out.println(id + " unknown");
				rtr = "unknown";
			}catch(Exception ioe) {
				String lErr = null;
				try{
				while((lErr = pipeErr.readLine()) != null)
					System.err.println(lErr);
				}catch(IOException ioe2) {}
				rtr = "unknown";
				
			}
			try{
				classifier.waitFor(); //wait for completion
			} catch (InterruptedException ie) {}
		}
		return rtr;
	}

	static String classify_query(String s)
	{
		String a = classify(s);
		String[] answers = a.replaceAll("\\(\\d+\\)","").split(" or ");
		if(answers.length > 1)
			return "";
		if (! LanguageToAbbrev.containsKey(answers[0].toLowerCase()))
			return "";
		return "lang:" + (String)LanguageToAbbrev.get(answers[0].toLowerCase());
	}

	public static void run(Collection c)
	{
		
		while(c.nextDocument())
		{
			/* get the next document from the collection */
			String docid = c.getDocid();
			Document doc = c.getDocument();
			if (doc == null)
				continue;
			StringBuffer s = new StringBuffer();
			while (!doc.endOfDocument()) {
					String term = null;
					if ((term = doc.getNextTerm())!=null && !term.equals("")) {	
						s.append(term);
						s.append(" ");
					}
			}

			//we now have all the terms of the document in s
			System.out.println(docid +" "+ classify(s.toString()));
		}
	}
	
	public static void run(File f, PrintWriter out) throws IOException
	{
		//TODO gzip
		BufferedReader br = new BufferedReader(new InputStreamReader(
			new FileInputStream(f),"UTF-8"));
			
		String line  = null;
		while((line = br.readLine()) != null)
		{
			int FirstSpace = line.indexOf(' ');
			String queryID = line.substring(0,FirstSpace);
			String query = line.substring(FirstSpace+1);
			
			//we now have all the terms of the document in sQuery
			out.println(queryID +" "+ query+" "+classify_query(query));
		}
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length == 0)
		{
			Collection c = new TRECCollection();
			run(c);
		}
		else 
		{
			LanguageToAbbrev.put("english","en");
			LanguageToAbbrev.put("scots", "en");
			LanguageToAbbrev.put("scots_gaelic","en");
			LanguageToAbbrev.put("welsh","en");
			LanguageToAbbrev.put("french","fr");
			LanguageToAbbrev.put("dutch","nl");
			LanguageToAbbrev.put("german","de");
			LanguageToAbbrev.put("portuguese","pt");
			LanguageToAbbrev.put("spanish","es");
			LanguageToAbbrev.put("italian","it");
			LanguageToAbbrev.put("norwegian","no");
			LanguageToAbbrev.put("swedish","sw");
			LanguageToAbbrev.put("danish","da");
			LanguageToAbbrev.put("greek","el");
			LanguageToAbbrev.put("russian","ru");
			LanguageToAbbrev.put("icelandic","is");
			LanguageToAbbrev.put("hungarian","hu");
			//LanguageToAbbrev.put("
			
			//,scots:en,irish:en,scots_gaelic:en,welsh:en,french:fr,dutch:dt,german:de,portuguese:pt,spanish:es,italian:it,norwegian:no,swedish:sw,danish:da,greek:gr,russian:ru,icelandic:is,default:default,el:gr,gr:gr,en:en,fr:fr,nl:dt,dt:dt,de:de,pt:pt,es:es,it:it,no:no,sw:sw,da:da,ru:ru,is:is

			

			File fIn = new File(args[0]);
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(args[1])), "UTF-8"));
			run(fIn, out);
			out.close();
		}
	}
}
