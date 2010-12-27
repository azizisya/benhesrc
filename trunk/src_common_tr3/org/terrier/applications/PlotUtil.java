package org.terrier.applications;

import java.util.Arrays;

import org.terrier.utility.DataUtility;

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleHashSet;

public class PlotUtil {
	
	public static void writeAverageResults(String filename1, String filename2, String outputFilename){
		TDoubleDoubleHashMap map1 = DataUtility.loadDoubleDoubleHashMap(filename1, 0, 1);
		TDoubleDoubleHashMap map2 = DataUtility.loadDoubleDoubleHashMap(filename2, 0, 1);
		TDoubleHashSet keySet = new TDoubleHashSet();
		keySet.addAll(map1.keys()); keySet.addAll(map2.keys());
		double[] keys = keySet.toArray();
		Arrays.sort(keys);
		TDoubleDoubleHashMap meanMap = new TDoubleDoubleHashMap();
		for (double key : keys){
			double mean = (map1.get(key)+map2.get(key))/2d;
			meanMap.put(key, mean);
		}
		DataUtility.writeDoubleDoubleHashMap(outputFilename, meanMap, true, true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--writeaverageresults")){
			// --writeaverageresults String filename1, String filename2, String outputFilename
			PlotUtil.writeAverageResults(args[1], args[2], args[3]);
		}

	}

}
