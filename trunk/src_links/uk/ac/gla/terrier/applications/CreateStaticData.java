package uk.ac.gla.terrier.applications;
import gnu.trove.TFloatArrayList;
import gnu.trove.TDoubleArrayList;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * A class for assigning a score to each document, according to the length of 
 * its URL.
 * URL length can be determined in two ways: by piping docno2url to the class
 * OR by using a pre-built URLServer.
 */
public class CreateStaticData {

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.out.println("usage: cmd [URLLength|INFOTONOISE|other] path prefix <sizes...>" );
			System.exit(1);
		}
		
		String todo = args[0];
		boolean urllength = false;
		boolean infotonoise = false;
		boolean other = false;
		if (todo.equals("URLLength"))
			urllength = true;
		else if (todo.equals("INFOTONOISE"))
			infotonoise = true;
		else if (todo.equals("other"))
			other = true;
		String path = args[1];
		String prefix = args[2];
		int[] sizes = new int[args.length-3];
		for (int i=3; i<args.length; i++) {
			sizes[i-3] = Integer.parseInt(args[i]);
		}
		
		TFloatArrayList scores = new TFloatArrayList(1000000);
		TDoubleArrayList scores2 = new TDoubleArrayList(1000000);
		String line = null;
		int docno = 0;
		int fileIndex = 0;
		try{

			if (urllength) {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				while((line = br.readLine()) != null) {
					final String[] parts = line.split("\\s+");
					scores.add(URLServer.normaliseURL(parts[1]).length());
					docno++;
					if (docno == sizes[fileIndex]) {
						String outFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + "_" + fileIndex + ".urllens";
						ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(outFilename)));
						oos.writeObject(scores.toNativeArray());
						oos.close();
						docno = 0;
						fileIndex++;
						scores.clear();
					}
				}
			} else if (infotonoise) {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				while((line = br.readLine()) != null) {
					final String[] parts = line.split("\\s+");
					scores.add(Float.parseFloat(parts[1]));
					docno++;
					if (docno == sizes[fileIndex]) {
						String outFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + "_" + fileIndex + ".itnratio";
						ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(outFilename)));
						oos.writeObject(scores.toNativeArray());
						oos.close();
						docno = 0;
						fileIndex++;
						scores.clear();
					}
				}
			}
			else if (other) {
				//once	only
				 BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while((line = br.readLine()) != null) {
                    final String[] parts = line.split("\\s+");
                    scores2.add(Double.parseDouble(parts[0]));
				}
				String outFilename = "prior.oos.gz";
				ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(outFilename)));
                oos.writeObject(scores2.toNativeArray());
				oos.close();	
			}	
		} catch (IOException ioe) {
			System.err.println("io exception : "+ioe);
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
}
