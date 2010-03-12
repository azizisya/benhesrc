/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is ArrayUtils.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Ben He ben{a}dcs.gla.ac.uk (original author) 
 *  Craig Macdonald craigm{a}dcs.gla.ac.uk 
 */
package org.terrier.utility;

import java.util.regex.Pattern;

/** Handy methods for resizing arrays, and other handy array methods
 * This is a fresh implementation of the capacity methods, without the
 * use of any prelicensed code.
 * @author Ben He
 * @version $Revision: 1.6 $ */
public class ArrayUtils {
	/* TODO: use as an integer to reduce FP operations */
	/** the Golden ration (&phi;). */ 
	protected static final double GOLDEN_RATIO = 1.618;
	
	/** Grow an array to ensure it is the desired length. 
 	  * @param array input array
 	  * @param length ensure array is this length 
 	  * @return new array with desired length */
	public static byte[] ensureCapacity(byte[] array, int length){
		if (array.length < length){
			byte[] buffer = new byte[length];
			System.arraycopy(array, 0, buffer, 0, array.length);
			array = buffer;
		}
		return array;
	}
	
	/** Grow an array to ensure it is the desired length. Only copy the first preserve
	 * elements from the input array 
	 * @param array input array
	 * @param length new desired length
	 * @param preserve amount of old array to copy to new array in case of reallocation 
	 * @return new array with desired length */
	public static byte[] ensureCapacity(byte[] array, int length, int preserve){
		if (array.length < length){
			byte[] buffer = new byte[length];
			System.arraycopy(array, 0, buffer, 0, preserve);
			array = buffer;
		}
		return array;
	}
	
	/** Grow an array to ensure it is <i>at least</i> the desired length. The golden ratio
	 * is involved in the new length
	 * @param array input array
	 * @param length minimuim length of new array
	 * @return new array appropriately sized
	 */
	public static byte[] grow(byte[] array, int length){
		final int oldlength = array.length; 
		if (oldlength < length){
			int newsize = Math.max(length, (int)(((double)oldlength)*GOLDEN_RATIO));
			byte[] buffer = new byte[newsize];
			System.arraycopy(array, 0, buffer, 0, oldlength);			
			array = buffer;
		}
		return array;
	}
	
	/** Grow an array to ensure it is <i>at least</i> the desired length. The golden ratio
	 * is involved in the new length. Only copy the first preserve
	 * elements from the input array.
	 * @param array input array
	 * @param length minimuim length of new array
	 * @return new array appropriately sized
	 */
	public static byte[] grow(byte[] array, int length, int preserve){
		if (array.length < length){
			int newsize = Math.max(length, (int)((double)array.length*GOLDEN_RATIO));
			byte[] buffer = new byte[newsize];
			System.arraycopy(array, 0, buffer, 0, preserve);
			array = buffer;
		}
		return array;
	}


	/** Join some strings together.
	  * @param in Strings to join
	  * @param join Character or String to join by */
    public static String join (String[] in, String join)
    {
        final StringBuilder s = new StringBuilder();
        if (in.length == 0)
        	return "";
        for(String i : in)
        {
            s.append(i);
            s.append(join);
        }
        s.setLength(s.length() - join.length());
        return s.toString();
    }
    
    /** Join some strings together.
	  * @param in Strings to join
	  * @param join Character or String to join by */
   public static String join (String[] in, char join)
   {
       final StringBuilder s = new StringBuilder();
       if (in.length == 0)
       	return "";
       for(String i : in)
       {
           s.append(i);
           s.append(join);
       }
       s.setLength(s.length() - 1);
       return s.toString();
   }

	public static String join(int[] in, String join) {
		final StringBuilder s = new StringBuilder();
		if (in.length == 0)
        	return "";
        for(int i : in)
        {
            s.append(""+i);
            s.append(join);
        }
        s.setLength(s.length() - join.length());
        return s.toString();
	}
	
	public static String join(boolean[] in, String join) {
		final StringBuilder s = new StringBuilder();
		if (in.length == 0)
        	return "";
        for(boolean i : in)
        {
            s.append(""+i);
            s.append(join);
        }
        s.setLength(s.length() - join.length());
        return s.toString();
	}

	public static int[] grow(int[] array, int length) {
		final int oldlength = array.length; 
		if (oldlength < length){
			int newsize = Math.max(length, (int)(((double)oldlength)*GOLDEN_RATIO));
			int[] buffer = new int[newsize];
			System.arraycopy(array, 0, buffer, 0, oldlength);			
			array = buffer;
		}
		return array;
	}

	public static double[] grow(double[] array, int length) {
		final int oldlength = array.length; 
		if (oldlength < length){
			int newsize = Math.max(length, (int)(((double)oldlength)*GOLDEN_RATIO));
			double[] buffer = new double[newsize];
			System.arraycopy(array, 0, buffer, 0, oldlength);			
			array = buffer;
		}
		return array;
	}

	public static short[] grow(short[] array, int length) {
		final int oldlength = array.length; 
		if (oldlength < length){
			int newsize = Math.max(length, (int)(((double)oldlength)*GOLDEN_RATIO));
			short[] buffer = new short[newsize];
			System.arraycopy(array, 0, buffer, 0, oldlength);			
			array = buffer;
		}
		return array;
	}

	public static String[] parseCommaDelimitedString(String src)
	{
		if (src == null)
			return new String[0];
		src = src.trim();
		if (src.length() == 0)
			return new String[0];
		String[] parts = src.split("\\s*,\\s*");
		return parts;
	}
	
	public static String[] parseDelimitedString(String src, String delim)
	{
		if (src == null)
			return new String[0];
		src = src.trim();
		if (src.length() == 0)
			return new String[0];
		String[] parts = src.split("\\s*"+Pattern.quote(delim)+"\\s*");
		return parts;
	}
	
	public static int[] parseCommaDelimitedInts(String src)
	{
		final String[] parts = parseCommaDelimitedString(src);
		if (parts.length == 0)
			return new int[0];
		int[] rtr = new int[parts.length];
		for(int i=0;i<parts.length;i++)
			rtr[i] = Integer.parseInt(parts[i]);
		return rtr;
	}
	
	public static int[] parseDelimitedInts(String src, String sep)
	{
		final String[] parts = parseDelimitedString(src, sep);
		if (parts.length == 0)
			return new int[0];
		int[] rtr = new int[parts.length];
		for(int i=0;i<parts.length;i++)
			rtr[i] = Integer.parseInt(parts[i]);
		return rtr;
	}

	public static String join(byte[] in, String join) {
		final StringBuilder s = new StringBuilder();
		if (in.length == 0)
        	return "";
        for(byte i : in)
        {
            s.append(""+i);
            s.append(join);
        }
        s.setLength(s.length() - join.length());
        return s.toString();
	}
	
	public static String join(double[] in, String join) {
		final StringBuilder s = new StringBuilder();
		if (in.length == 0)
        	return "";
        for(double i : in)
        {
            s.append(""+i);
            s.append(join);
        }
        s.setLength(s.length() - join.length());
        return s.toString();
	}
	
}
