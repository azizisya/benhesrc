package uk.ac.gla.terrier.applications;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

import uk.ac.gla.terrier.links.URLServer;

/**
 * A class for assigning a score to each document, according to the length of 
 * its URL.
 * URL length can be determined in two ways: by piping docno2url to the class
 * OR by using a pre-built URLServer.
 */
public class URLScore {

	public static void main(String[] args) throws IOException {
		if (args.length < 2 || args.length > 3) {
			System.out.println("usage: cmd [-docno2url] <size> <output file>");
			System.exit(1);
		}
		
		int size;
		String outFile = null;
		boolean docno2url = false;
		if (args[0].equals("-docno2url"))
		{
			size =Integer.parseInt(args[1]);
			outFile = args[2];
			docno2url= true;
		}
		else
		{
			size = Integer.parseInt(args[0]);
			outFile = args[1];
			
		}
		
		double[] scores = new double[size];

		if (docno2url)
		{
			String line = null;
			int docno = 0;
			try{
				BufferedReader br = new BufferedReader(
					new InputStreamReader(System.in));
				while((line = br.readLine()) != null)
				{
					final String[] parts = line.split("\\s+");
					final String url = parts[1].replaceFirst("http://", "");
					int place = url.indexOf('/');
					scores[docno] =  (place == -1) ? url.length() : url.substring(url.indexOf('/')).length();
					docno++;
				}
			} catch (IOException ioe) {
				System.err.println("io exception : "+ioe);
				ioe.printStackTrace();
				System.exit(1);
			}
		}
		else
		{		
			URLServer urlServer = new URLServer();	
			for (int i=0; i<size; i++) {
				String path = urlServer.getPath(i);
				System.out.println("" + i + " : " + path);
				int len = path.length();
				/*int slashes = 0;
				for (int j=0; j<len; j++) {
					if (path.charAt(j)=='/')
						slashes++;
				}*/
				//scores[i] =(float) (1.0D/(1.0D+path.length()));
				//scores[i] = (float) (Math.log(2.0D) / Math.log(1.0D+path.length()));
				scores[i] = len;//path.length();
			}
			urlServer.close();
		}

		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(outFile)));
		oos.writeObject(scores);
		oos.close();
		

	}
	
}
