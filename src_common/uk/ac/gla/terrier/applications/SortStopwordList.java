/*
 * Created on 2005-5-23
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.*;
import java.util.*;

import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SortStopwordList {

	public static void main() {
		File stopwordList = new File(ApplicationSetup.TERRIER_SHARE,
				ApplicationSetup.getProperty("stopwords.filename", "stopword-list.txt"));
		try{
			Vector vecStopwords = new Vector();
			BufferedReader br = new BufferedReader(new FileReader(stopwordList));
			String str = null;
			while ((str=br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				vecStopwords.addElement(str.trim());
			}
			br.close();
			String[] stopwords = 
				(String[])vecStopwords.toArray(new String[vecStopwords.size()]);
			Arrays.sort(stopwords);
			StringBuffer buffer = new StringBuffer();
			String EOL = ApplicationSetup.EOL;
			for (int i = 0; i < stopwords.length; i++)
				buffer.append(stopwords[i] + EOL);
			BufferedWriter bw = new BufferedWriter(new FileWriter(stopwordList));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
