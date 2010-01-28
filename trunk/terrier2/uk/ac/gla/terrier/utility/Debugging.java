package uk.ac.gla.terrier.utility;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;

import java.util.Arrays;

/**
 * This class provides some static utility methods for debugging.
 * @author ben
 *
 */
public class Debugging {
	/**
	 * Dump content in an array of TIntDoubleHashMap where the keys are given in a TIntHashSet.
	 * @param maps
	 * @param keySet
	 */
	public static void dumpIntDoubleHashMaps(TIntDoubleHashMap[] maps, TIntHashSet keySet){
		int[] keys = keySet.toArray();
		Arrays.sort(keys);
		for (int key:keys){
			System.out.print(key);
			for (int i=0; i<maps.length; i++)
				System.out.print(" "+maps[i].get(key));
			System.out.println();
		}
	}
	/**
	 * Dump content in a TIntDoubleHashMap. info is a string describing content in the hash map.
	 * @param map
	 * @param info
	 */
	public static void dumpIntDoubleHashMap(TIntDoubleHashMap map, String info){
		System.out.println(info);
		int[] keys = map.keys();
		Arrays.sort(keys);
		for (int key : keys)
			System.out.print(key+":"+map.get(key)+" ");
		System.out.println();
	}
}
