package org.terrier.applications;

import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

public class Randomize {

	public static void RandomizeTextFile(String filename, int i){
		try{
			BufferedReader br = Files.openFileReader(filename);
			ArrayList<String> lineSet = new ArrayList<String>();
			String line = null;
			while ((line=br.readLine())!=null){
				lineSet.add(line);
			}
			br.close();
			int size = lineSet.size();
			StringBuilder sb = new StringBuilder();
			int counter = 0;
			while (lineSet.size()>0){
				int pos = 0;
				if (lineSet.size()!=1)
					pos = (int)(Math.random()*(lineSet.size()-1));
				try{
					sb.append(lineSet.get(pos)+ApplicationSetup.EOL);
				}catch(IndexOutOfBoundsException e){
					System.err.println("pos: "+pos+", lineSet.size(): "+lineSet.size());
					e.printStackTrace();
					System.exit(1);
				}
				lineSet.remove(pos);
			}
			String outputFilename = filename.concat("."+i);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(sb.toString());
			bw.close();
			System.out.println("Done. Randomized file saved at "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// filename times
		int n = Integer.parseInt(args[1]);
		for (int i=0; i<n; i++)
			Randomize.RandomizeTextFile(args[0], i);

	}

}
