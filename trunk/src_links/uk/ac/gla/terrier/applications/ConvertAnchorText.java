package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author vassilis
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ConvertAnchorText {

	public static void main(String[] args) {
		boolean debug = (new Boolean(System.getProperty("debug","true"))).booleanValue();
				
		if (args.length!=2) {
			System.out.println("java ConvertAnchorText [input filename] [output filename]");
			System.exit(1);
		}
		String inputFilename = args[0];
		String outputFilename = args[1];
		
		BufferedReader input = null;
		PrintWriter output = null;
		
		try {
			input = new BufferedReader(new InputStreamReader(
							new GZIPInputStream(new FileInputStream(inputFilename)),"UTF-8"));
			output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new GZIPOutputStream(new FileOutputStream(outputFilename)),"UTF-8")));

	
			String line = null;

			int startIndexOfSourceLang;
			int endIndexOfSourceLang;
			String lang = null;
			String firstPartOfLine = null;
			String secondPartOfLine = null;
			final String escapeSeq = "12lang34";
			String toPrint = null;
			//<ATEXT src="srcdocid" lang="srclang">text</ATEXT>
			while ((line = input.readLine())!=null) {
				if (line.startsWith("<ATEXT src=") ) {
					startIndexOfSourceLang = line.indexOf("lang=\"")+6;
					endIndexOfSourceLang = line.indexOf("\">",startIndexOfSourceLang);
					lang = line.substring(startIndexOfSourceLang, endIndexOfSourceLang);

					//remove any encoding information from the language
					lang = lang.replaceAll("-.*$","");
					
					firstPartOfLine = line.substring(0, endIndexOfSourceLang+2);
					secondPartOfLine = line.substring(endIndexOfSourceLang+2, line.length());
					
					toPrint = firstPartOfLine + escapeSeq + lang + " " + secondPartOfLine;
					if (debug)
						System.out.println(toPrint);
					output.println(toPrint);
				} else {
					if (debug)
						System.out.println(line);
					output.println(line);
				}
			}
			
			input.close();
			output.close();
		} catch(IOException ioe) {
			System.out.println("oups, io exception: " + ioe);
			ioe.printStackTrace();
		}
	}
}
