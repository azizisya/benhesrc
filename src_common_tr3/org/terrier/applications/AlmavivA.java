package org.terrier.applications;

import java.io.BufferedReader;
import java.io.IOException;

import org.terrier.utility.Files;
import org.terrier.utility.Rounding;


import gnu.trove.TIntIntHashMap;

public class AlmavivA {
	/**
	* System Output: K, F-measure, Precision, Recall
	* totalPos: total number of positive cases in the data set. 
	* This is used for computing the recall.
	*/
	public static void generatePrecisionTable(String sortedEvalFilename, int totalPos){
		double[] percentages = {0.03, 0.05, 0.10, 0.20, 0.30, 0.50, 0.80, 1.00};
		int foundCounter = 0;
		int entryCounter = 0;
		TIntIntHashMap rankFoundMap = new TIntIntHashMap();// rank is 1-based
		try{
			BufferedReader br = Files.openFileReader(sortedEvalFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				if (line.trim().length()==0)
					continue;
				entryCounter++;
				String[] tokens = line.split(" "); // each line has entryId, score, label
				if (tokens[2].equals("1"))
					foundCounter++;
				rankFoundMap.put(entryCounter, foundCounter);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		int sizeOfClass = rankFoundMap.size();
		System.out.println("% Size of class: "+sizeOfClass);
		for (int i=0; i<percentages.length; i++){
			int rank = (int)(percentages[i]*sizeOfClass);
			int found = rankFoundMap.get(rank);
			double precision = (double)found/rank;
			double recall = (double)found/totalPos;
			double F = 2.0*precision*recall/(precision+recall);
			System.out.println(Rounding.toString(percentages[i]*100, 0)+"\\%"+" & "+
				Rounding.toString(F, 4)+" & "+Rounding.toString(precision, 4)+" & "+
				Rounding.toString(recall, 4)+"\\\\");
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--generateprecisiontable")){
			// --generateprecisiontable sortedEvalFilename totalPos
			AlmavivA.generatePrecisionTable(args[1], Integer.parseInt(args[2]));
		}
	}

}
