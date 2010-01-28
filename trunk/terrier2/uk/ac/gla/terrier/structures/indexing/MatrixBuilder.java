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
 * The Original Code is DirectIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>(original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures.indexing;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * Builds a direct index, using field information optionally.
 * @author Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class MatrixBuilder {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	protected RandomAccessFile matrix;
	
	protected RandomAccessFile matrixIndex;
	
	protected long offset = 0L;
	
	static final protected String mxSuffix = ".mtx";

	/** Constructs an instance of the direct index
	  * using the given index path/prefix 
	  */
	public MatrixBuilder(String path, String prefix)
	{
		this(path + ApplicationSetup.FILE_SEPARATOR + prefix + mxSuffix);
	}

	/**
	 * Constructs an instance of the direct index
	 * in the default index location for the direct file.
	 */
	public MatrixBuilder() {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	/**
	 * Constructs an instance of the direct index
	 * with a non-default name for the underlying direct file.
	 * @param filename the non-default filename used
	 *	for the underlying direct file.
	 */
	public MatrixBuilder(String filename) {
		try{	
			matrix = new RandomAccessFile(filename, "rw");
			matrixIndex = new RandomAccessFile(filename+"id", "rw");
		} catch (IOException ioe) {
			logger.error(ioe);
		}
		//resetBuffer();
	}
	// [0][i]: termid
	// [1][i]: unscale
	// [2][i]: scale
	public void addVector(int[][] vector) throws IOException{
		if (vector==null){
			matrixIndex.writeLong(offset);
			matrixIndex.writeInt(0);
		}else{
			int size = vector[0].length;
			for (int i=0; i<size; i++){
				matrix.writeInt(vector[0][i]);// write termid
				matrix.writeInt(vector[1][i]);// write unscale
				matrix.writeInt(vector[2][i]);// write scale
			}
			matrixIndex.writeLong(offset);
			matrixIndex.writeInt(size);
			offset=matrix.getFilePointer();
		}
	}

	public void close() throws IOException{
		matrix.close();
		matrixIndex.close();
	}
	
}
