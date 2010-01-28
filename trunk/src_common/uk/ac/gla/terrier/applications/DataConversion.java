package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import uk.ac.gla.terrier.structures.SVMLightFormat;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class DataConversion {
	
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
