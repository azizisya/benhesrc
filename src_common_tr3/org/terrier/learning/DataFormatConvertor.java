package org.terrier.learning;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

public class DataFormatConvertor {
	/**
	 * 
	 * @param ids instance ids
	 * @param labels labels
	 * @param dataMap instance ids -> a sparse double array of feature values. value is 0 if the feature is missing.
	 * @param outputFilename
	 */
	public static void DataToArff(String[] ids, int[] labels, THashMap<String, double[]> dataMap, String outputFilename){
		try{
			int instN = ids.length;
			int featureN = dataMap.get(ids[0]).length;
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			// write relation
			bw.write("@relation "+outputFilename+ApplicationSetup.EOL);
			bw.write(ApplicationSetup.EOL);
			// write attribute declaration
			for (int i=0; i<=featureN; i++){
				bw.write("@attribute att_"+(i+1)+" numeric"+ApplicationSetup.EOL);
			}
			// get unique labels
			TIntHashSet labelSet = new TIntHashSet(labels);
			int[] uniqueLabels = labelSet.toArray();
			Arrays.sort(uniqueLabels);
			int labelN = labelSet.size();
			StringBuilder buf = new StringBuilder();
			buf.append("{"+uniqueLabels[0]);
			for (int i=1; i<labelN; i++)
				buf.append(","+uniqueLabels[i]);
			buf.append("}");
			// bw.write("@attribute class"+buf.toString()+ApplicationSetup.EOL);
			bw.write("@attribute class{-1,1}"+ApplicationSetup.EOL);
			// write data
			bw.write(ApplicationSetup.EOL);
			bw.write("@data"+ApplicationSetup.EOL);
			for (int i=0; i<instN; i++){
				bw.write(ApplicationSetup.EOL);
				double[] values = dataMap.get(ids[i]);
				bw.write("{");
				for (int j=0; j<featureN; j++){
					if (!Double.isNaN(values[j])&&!Double.isInfinite(values[j]));
					bw.write((j+1)+" "+values[j]+",");
				}
				bw.write((featureN+1)+" "+labels[i]+"}");
			}
			bw.close();
			// write instance to id map
//			 write the rank -> id map
			String mapOutputFilename = null;
			if (outputFilename.endsWith(".gz"))
				mapOutputFilename = outputFilename.substring(0, outputFilename.lastIndexOf('.'))+".map.gz";
			else
				mapOutputFilename = outputFilename+".map.gz";
			bw = (BufferedWriter)Files.writeFileWriter(mapOutputFilename);
			for (int i=0; i<instN; i++)
				bw.write((i+1)+" "+ids[i]+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void loadArffData(String[] ids, int[] labels, THashMap<String, double[]> dataMap, String arffDataFilename){
		String mapFilename = null;
		if (arffDataFilename.endsWith(".gz"))
			mapFilename = arffDataFilename.substring(0, arffDataFilename.lastIndexOf('.'))+".map.gz";
		else
			mapFilename = arffDataFilename+".map.gz";
		// load inst id map
		TIntObjectHashMap<String> instIdMap = new TIntObjectHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(mapFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				instIdMap.put(Integer.parseInt(tokens[0]), tokens[1]);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
