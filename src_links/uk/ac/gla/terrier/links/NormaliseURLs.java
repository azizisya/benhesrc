package uk.ac.gla.terrier.links;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * opens and reads a file in order to normalise the urls
 * @author vassilis
 *
 */
public class NormaliseURLs {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length!=2) {
			System.out.println("usage: java NormaliseURLs [input] [output]");
			System.exit(1);
		}
		
		try {
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(args[1])));
		
		String line = null;
		String url = null;
		String normalisedUrl = null;
		while ((line = br.readLine())!=null) {
			int spaceIndex = line.indexOf(' ');
			url = line.substring(0, spaceIndex);
			normalisedUrl = URLServer.normaliseURL(url);
			pw.println(normalisedUrl + line.substring(spaceIndex));
		}
		
		br.close();
		pw.close();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
		
	}

}
