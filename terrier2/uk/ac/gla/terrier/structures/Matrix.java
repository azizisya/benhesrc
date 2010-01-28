/*
 * Created on 25 Sep 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.structures;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class Matrix {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** The matrix stored in a random access file. Each entry
	 * in the matrix corresponds to the term weight vector of
	 * a document. Each element in the vector is represented
	 * by a 3D int array. The first unit in the array is the termid,
	 * the second is the unscaled int of the term weight, and the
	 * third is the scale of the term weight.
	 * term weight = unscaled*10^-scale 
	 *  */
	protected RandomAccessFile matrix;
	/** An index file that stores the starting offset and length
	 * of each entry 
	 * */
	protected RandomAccessFile matrixIndex;
	/** The offset of the matrix */
	protected long offset = 0L;
	/** The default suffix of the matrix file. */
	static final protected String mxSuffix = ".mtx";
	/** The length of each entry in the MATRIX INDEX (not matrix itself!) 
	 *  The ith entry in the matrix index stores the offset (long) of the ith
	 *  document vector (i is docid) in the matrix, and the length (int)
	 *  of the ith document vector. A long value takes 8 bytes and an int
	 *  value takes 4 bytes in storage. 8+4=12.
	 * */
	final protected long entryLength = 12L;
	/**
	 * Number of entries (documents) in the matrix
	 */
	int numberOfEntries;

	/** Constructs an instance of the direct index
	  * using the given index path/prefix 
	  */
	public Matrix(String path, String prefix)
	{
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + mxSuffix);
	}

	/**
	 * Constructs an instance of the direct index
	 * in the default index location for the direct file.
	 */
	public Matrix() {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	/**
	 * Constructs an instance of the direct index
	 * with a non-default name for the underlying direct file.
	 * @param filename the non-default filename used
	 *	for the underlying direct file.
	 */
	public Matrix(String filename) {
		try{	
			matrix = new RandomAccessFile(filename, "r");
			matrixIndex = new RandomAccessFile(filename+"id", "r");
			numberOfEntries = (int)(matrixIndex.length()/entryLength);
		} catch (IOException ioe) {
			logger.error(ioe);
		}
		//resetBuffer();
	}
	
	public int[][] getVector(int id) throws IOException{
		int[][] vector = null;
		matrixIndex.seek(entryLength*id);
		long offset = matrixIndex.readLong();
		int size = matrixIndex.readInt();
		matrix.seek(offset);
		vector = new int[3][size];
		for (int i=0; i<size; i++){
			try{
				vector[0][i] = matrix.readInt();
				vector[1][i] = matrix.readInt();
				vector[2][i] = matrix.readInt();
			}catch(EOFException e){
				System.err.println("i: "+i);
				e.printStackTrace();
				System.exit(1);
			}
		}
		return vector;
	}
	
	public void print() throws IOException{
		System.out.println("numberOfEntries: "+numberOfEntries);
		for (int i=0; i<numberOfEntries; i++){
			int[][] vector = this.getVector(i);
			System.out.println("id "+i+" length "+vector[0].length+", ");
			if (vector.length>0){
				for (int j=0; j<vector[0].length; j++)
					System.out.print("("+vector[0][j]+", "+vector[1][j]+", "+vector[2][j]+")");
				System.out.println();
			}
		}
	}
	
	public void close() throws IOException{
		matrix.close();
		matrixIndex.close();
	}
}
