package uk.ac.gla.terrier.utility;

import java.util.ArrayList;
import java.util.Collections;

public class Shuffling {
	/**
	 * Shuffle an array of integers.
	 * @param values
	 * @return
	 */
	public static int[] shuffleInts(int[] values){
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i=0; i<values.length; i++)
			list.add(new Integer(values[i]));
		Collections.shuffle(list);
		for (int i=0; i<values.length; i++)
			values[i] = list.get(i).intValue();
		return values;
	}
}
