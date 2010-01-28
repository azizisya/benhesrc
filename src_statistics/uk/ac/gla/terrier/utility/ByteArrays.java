/*
 * Created on 13 Sep 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.utility;

public class ByteArrays {
	
	static public double GOLDEN_RATIO = 1.618;
	
	static public byte[] ensureCapacity(byte[] array, int length){
		if (array.length < length){
			byte[] buffer = new byte[length];
			System.arraycopy(array, 0, buffer, 0, array.length);
			array = buffer;
		}
		return array;
	}
	
	static public byte[] ensureCapacity(byte[] array, int length, int preserve){
		if (array.length < length){
			byte[] buffer = new byte[length];
			System.arraycopy(array, 0, buffer, 0, preserve);
			array = buffer;
		}
		return array;
	}
	
	static public byte[] grow(byte[] array, int length){
		if (array.length < length){
			int newsize = Math.max(length, (int)((double)array.length*GOLDEN_RATIO));
			byte[] buffer = new byte[newsize];
			System.arraycopy(array, 0, buffer, 0, newsize);
			array = buffer;
		}
		return array;
	}
	
	static public byte[] grow(byte[] array, int length, int preserve){
		if (array.length < length){
			int newsize = Math.max(length, (int)((double)array.length*GOLDEN_RATIO));
			byte[] buffer = new byte[newsize];
			System.arraycopy(array, 0, buffer, 0, preserve);
			array = buffer;
		}
		return array;
	}
}
