package org.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.Rounding;

public class LBM {
	
	public static void computeMeanMap(String inputFilename, String outputFilename){
		System.out.println("input: "+inputFilename+", output: "+outputFilename);
		try{
			BufferedReader br = Files.openFileReader(inputFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] str = line.split(" ");
				double b = Double.parseDouble(str[0]);
				double blMap1 = Double.parseDouble(str[1]);
				double blMap2 = Double.parseDouble(str[2]);
				double testMap1 = Double.parseDouble(str[3]);
				double testMap2 = Double.parseDouble(str[4]);
				bw.write(b+" "+Rounding.toString((blMap1+blMap2)/2, 4)+" "+Rounding.toString((testMap1+testMap2)/2, 4)+ApplicationSetup.EOL);
			}
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--computemeanmap")){
			// --computemeanmap inputFilename outputFilename
			LBM.computeMeanMap(args[1], args[2]);
		}

	}

}
