package uk.ac.gla.terrier.terms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** HungarianHunspellStemmer is a stemmer for the hungarian language. It
  * uses the hunstem program from hunspell to stem each word. One process
  * is forked for every instance of the class, and each word passed through 
  * the pipline is piped to the stemmer program and the answer read.<p>
  * Hunspell is a derivate of myspell, and it can be downloaded from 
  * <a href="http://magyarispell.sourceforge.net/">http://magyarispell.sourceforge.net/</a>
  * <p>Properties:<ul>
  * <li><tt>terms.HungarianHunspellStemmer.program</tt> - specifies the location of the hunstem program</li>
  * <li><tt>terms.HungarianHunspellStemmer.usefirststem</tt> - Specifies whether the hungarian stemmer 
  * should use the first or last stem returned by the external stemmer.</li></ul>
  */

public class HungarianHunspellStemmer implements TermPipeline {
	/** The next object in the term pipeline to pass the term to.*/
	TermPipeline next = null;

	/** The location of the external stemming program to use. 
	  * Directly associated with the property <tt>terms.HungarianHunspellStemmer.program</tt> */	
	static String stemmerProgram = 
		ApplicationSetup.getProperty("terms.HungarianHunspellStemmer.program",
			"/local/terrier_tmp/CLEF/XristinaResources/HungarianStemmer/install/bin/hunspell -b -s");

	/** Whether to use the first stem or the last stem if the stemmer gives more than one option. 
	  * Directly associated with the property <tt>terms.HungarianHunspellStemmer.usefirststem</tt> */
	static boolean useFirstStem =
		new Boolean(
			ApplicationSetup.getProperty("terms.HungarianHunspellStemmer.usefirststem",
				"true")
			).booleanValue();

	final static String externalEncoding = "ISO-8859-2";
			
	/** The external program we're using */
	Process extStemmer = null;
	/** Handle to STDIN of the external stemmer */
	OutputStream pipeInRaw = null;
	PrintWriter pipeIn = null;
	/** Handle to STDOUT of the external stemer */
	BufferedReader pipeOut = null;

	BufferedReader stdErr = null;
	
	/** Construct a new HungarianHunspellStemmer object */
	public HungarianHunspellStemmer(TermPipeline next)
	{
		/* Take note of the term pipeline object we want to goto next */
		this.next = next;
		/* Fork the external stemmer program */
		try{
			Runtime r = Runtime.getRuntime();
			extStemmer = r.exec(
				//new String[]{"/local/terrier_tmp/CLEF/XristinaResources/HungarianStemmer/install/bin/hunspell", "-s"});
				stemmerProgram);
			/* Save handles to STDIN and STDOUT of the external stemmer program */
			pipeInRaw = extStemmer.getOutputStream();
			pipeIn = new PrintWriter(extStemmer.getOutputStream(), true);
			pipeOut = new BufferedReader(new InputStreamReader(extStemmer.getInputStream()));
			stdErr = new BufferedReader(new InputStreamReader(extStemmer.getErrorStream(), externalEncoding));
			//System.err.println("Using stemmer " + pipeOut.readLine());
			if (stdErr.ready())
			{
				System.err.println("Err: "+stdErr.readLine());
			}
		} catch (IOException ioe) {
			System.err.println("WARNING: Couldn't fork a external stemming program : "+ ioe);
			ioe.printStackTrace();
		}
	}
	

	public void processTerm(String t)
	{
		//no term passed, return
		if (t == null)
			return;
		
		t = stem(t);

		//pass the stemmed term onto the next item in the term pipeline object
		if (t != null)
			next.processTerm(t);
	}
	
	public String stem(String t)
	{
		//pipe the term to the external stemmer	
		//pipeIn.println(t+"\n")  ; pipeIn.flush();
		try{
			pipeInRaw.write(t.getBytes(externalEncoding));
			pipeInRaw.write('\n');
			pipeInRaw.flush();
		} catch (UnsupportedEncodingException uee) {
			System.err.println("WARNING: Couldn't write term to external stemmer -  unsupported encoding : "+ uee);
		} catch (IOException ioe ) {
			System.err.println("WARNING: IOException writing raw bytes to external stemmer: "+ ioe);
		}
		//System.err.println("INFO: Write a line : "+t);

		String out = null;
		boolean another = true;	
		//in case more than one stem is returned
		ArrayList stems = new ArrayList(3); //never likely to get more than 3 stems, so optimise for that
		do{
			try{
				// read one answer
				//System.err.println("INFO: About to attempt a readline");
				out = pipeOut.readLine();
				//System.err.println("INFO: Read a line : "+out);
			} catch(IOException ioe) {
				System.err.println("WARNING: Error reading from external hungarian stemmer : "+ioe);
				ioe.printStackTrace();
			}


			if (out == null || out.length() == 0)
			{
				//end of the stems for the current term
				another = false;
			}
			else if (out.startsWith("#"))
			{	//the word wasn't recognised, so use the existing term
				//read one more time to get rid of the blank line
			}
			else
			{
				//record the stem
				stems.add(out);
			}
		
		} while(another);

		/* if we have more then one stem, then choose the correct one, using
		 * the setup specified in the property */
		if (stems.size() > 0)
		{
			if (useFirstStem)
				t = (String) stems.get(0);
			else
				t = (String) stems.get(stems.size() -1);
		}

		return t;
		
	}

	public static void main(String[] args)
	{
		try{
			HungarianHunspellStemmer stemmer = new HungarianHunspellStemmer(null);
			BufferedReader b= new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			while((line = b.readLine()) != null)
			{
				line = line.trim();
				System.err.print("Stemming: "+line);
				System.out.println(" -> "+stemmer.stem(line));
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
			ioe.printStackTrace();
		}
	}

}
