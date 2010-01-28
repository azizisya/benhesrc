package uk.ac.gla.terrier.utility;

import uk.ac.gla.terrier.statistics.Statistics;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

public class TroveUtility {
	public static TIntIntHashMap mergeIntIntHashMaps(TIntIntHashMap map1, TIntIntHashMap map2){
		TIntIntHashMap map = new TIntIntHashMap();
		TIntHashSet keyset = new TIntHashSet();
		keyset.addAll(map1.keys()); keyset.addAll(map2.keys());
		for (int key : keyset.toArray()){
			map.put(key, map1.get(key)+map2.get(key));
		}
		return map;
	}
	
	public static TIntIntHashMap mergeIntIntHashMaps(TIntIntHashMap[] maps){
		TIntIntHashMap map = (TIntIntHashMap)maps[0].clone();
		for (int i=1; i<maps.length; i++)
			mergeIntIntHashMaps(map, maps[i]);
		return map;
	}
	
	public static int sumOfValues(TIntIntHashMap map){
		return Statistics.sum(map.getValues());
	}
	
	public static void dumpIntIntHashMap(TIntIntHashMap map){
		int counter = 0;
		for (int key: map.keys())
			System.out.println((++counter)+": "+key+", "+map.get(key));
	}
	
	public static TIntDoubleHashMap mapIntArrayToDoubleArray(int[] ints, double[] doubles){
		TIntDoubleHashMap map = new TIntDoubleHashMap();
		int length = ints.length;
		for (int i=0; i<length; i++)
			map.put(ints[i], doubles[i]);
		return map;
	}
	
	public static int[] stringArrayToIntArray(String[] strs){
		int length = strs.length;
		int[] ints = new int[length];
		for (int i=0; i<length; i++)
			ints[i] = Integer.parseInt(strs[i]);
		return ints;
	}
	
	public static double[] stringArrayToDoubleArray(String[] strs){
		int length = strs.length;
		double[] ints = new double[length];
		for (int i=0; i<length; i++)
			ints[i] = Double.parseDouble(strs[i]);
		return ints;
	}
	
}
