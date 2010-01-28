package uk.ac.gla.terrier.utility;

import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;

public class StringUtility {
	/**
	 * Get overlap between two arrays of strings.
	 * @param idsA
	 * @param idsB
	 * @return
	 */
	public static String[] getOverlap(String[] idsA, String[] idsB){
		THashSet<String> idASet = new THashSet<String>();
		THashSet<String> idSet = new THashSet<String>();
		for (int i=0; i<idsA.length; i++)
			idASet.add(idsA[i]);
		for (int i=0; i<idsB.length; i++)
			if (idASet.contains(idsB[i]))
				idSet.add(idsB[i]);
		return (String[])idSet.toArray(new String[idSet.size()]);
	}
	/**
	 * Convert integral strings to a hash set of integers.
	 * @param strs
	 * @return
	 */
	public static TIntHashSet stringsToIntSet(String[] strs){
		TIntHashSet intSet = new TIntHashSet();
		for (int i=0; i<strs.length; i++)
			intSet.add(Integer.parseInt(strs[i]));
		return intSet;
	}
	/**
	 * Keep only numeric part of a string. For example, "WT04-065" is interpretated as "65".
	 * @param str A given string.
	 * @return The numeric part of the given string.
	 */
	public static String keepNumericChars(String str){
		StringBuilder queryNoTmp = new StringBuilder();
		boolean firstNumericChar = false;
		for (int i = str.length()-1; i >=0; i--){
			char ch = str.charAt(i);
			if (Character.isDigit(ch)){
				queryNoTmp.append(str.charAt(i));
				firstNumericChar = true;
			}
			else if (firstNumericChar)
				break;
		}
		return ""+Integer.parseInt(queryNoTmp.reverse().toString());
	}
	
	public static boolean isNumber(String str){
		try{
			Double.parseDouble(str);
			return true;
		}catch(NumberFormatException e){
			return false;
		}
	}
	
	/**
	 * Convert String[] -> int[]
	 * @param strs
	 * @return
	 */
	public static int[] stringsToInts(String[] strs){
		int[] ints = new int[strs.length];
		for (int i=0; i<strs.length; i++)
			ints[i]=(Integer.parseInt(strs[i]));
		return ints;
	}
}
