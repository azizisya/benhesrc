package org.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import uk.ac.gla.terrier.statistics.ScoreNormaliser;
import uk.ac.gla.terrier.structures.SVMLightFormat;

public class DataConversion {
	/**
	 * 
	 * @param inputFilename
	 * @param col The column of numbers to be smoothed. Zero-based.
	 * @param outputFilename
	 */
	public static void smoothScores(String inputFilename, int col, String outputFilename){
		try{
			ArrayList<String> lineList = new ArrayList();
			BufferedReader br = Files.openFileReader(inputFilename);
			String line = null;
			while ((line=br.readLine())!=null)
				lineList.add(line);
			br.close();
			String[] lines = (String[])lineList.toArray();
			int cols = lines[0].split(" ").length;
			double[][] data = new double[cols][lines.length];
			for (int i=0; i<lines.length; i++){
				String[] strs = lines[i].split(" ");
				for (int j=0; j<strs.length; j++)
					data[j][i] = Double.parseDouble(strs[j]);
			}
			ScoreNormaliser.smoothScores(data[col]);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			for (int i=0; i<lines.length; i++){
				StringBuilder sb = new StringBuilder();
				for (int j=0; j<data.length; j++){
					sb.append(data[j][i]);
					if (j!=data.length-1)
						sb.append(" ");
					else
						sb.append(ApplicationSetup.EOL);
				}
				bw.write(sb.toString());
			}
			bw.close();
		}catch(IOException ioe){
			
		}
	}
	
	public static void CSV2SVMLight(String csvFilename, String outputFilename, String normalDelimiter){
		System.out.println("Normal delimiter: "+normalDelimiter);
		SVMLightFormat svmLight = new SVMLightFormat(normalDelimiter);
		int flushSize = 1000;
		try{
			StringBuilder buf = new StringBuilder();
			BufferedReader br = Files.openFileReader(csvFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String entry = null;
			int counter = 0;
			while ((entry=br.readLine())!=null){
				buf.append(svmLight.convertTo(entry)+ApplicationSetup.EOL);
				counter++;
				if (counter%flushSize==0){
					bw.write(buf.toString());
					buf = new StringBuilder();
				}
			}
			br.close();
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
