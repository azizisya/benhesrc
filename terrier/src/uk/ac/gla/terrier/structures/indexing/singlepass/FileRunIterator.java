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
 * The Original Code is FileRunIterator.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Roi Blanco
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk
 */
package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.DataInputStream;
import java.io.IOException;

import uk.ac.gla.terrier.compression.BitIn;
import uk.ac.gla.terrier.compression.BitInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/** Reads runs of flushed term posting lists by reading them from files.
  * @since 2.2
  * @author Craig Macdonald
  * @version $Revision: 1.1 $ 
  */
public class FileRunIterator<K extends PostingInRun> extends RunIterator {

	 /** has the UTF encoding been used to write out terms? */	
	protected final boolean UTFindexing = Boolean.parseBoolean(ApplicationSetup.getProperty("string.use_utf", "false"));
	
	/** Input stream for reading the run. */
	protected BitIn mbis;
	/** Input stream for reading the terms. */
	protected DataInputStream stringDIS;	
	/** Number of postings in this run */
	protected int size;
	/** Current Posting List number */
	protected int currentPosting;
	/** max number of pointers any term in the run */
	protected int maxSize;

	/** Load a new run from files.
	  * @param filename the filename of the file containing the posting lists
	  * @param termsFile the filename of the file containing the term names
	  * @param runNo the number of this run
	  * @param _postingInRunClass the class that all postings in this class have
	  */	
	public FileRunIterator(String filename, String termsFile, int runNo, Class<? extends PostingInRun> _postingInRunClass) throws Exception{
		super(_postingInRunClass, runNo);
		mbis = new BitInputStream(filename);
		stringDIS = new DataInputStream( Files.openFileStream(termsFile) );
		if (Files.length(filename) > 0)
		{
			maxSize = mbis.readGamma();
			size = mbis.readGamma();
		}
		createPosting();
		currentPosting = 0;
	}

	/** Closes the run files being processed */	
	@Override
	public void close()
	{
		try{
			mbis.close();
			stringDIS.close();
		} catch (Exception e) {}
	}
	

	/** Are there more posting to process in this run? */
	@Override
	public boolean hasNext() {
		return currentPosting != size;
	}


	/** Move to the next posting in this run */
	@Override
	public PostingInRun next() 
	{
		try{
			posting.setTerm(readString());
			posting.setDf(mbis.readGamma());
			posting.setTF(readTermFrequency());
			posting.setPostingSource(mbis);
			currentPosting++;
		} catch (Exception e) {
			//TODO
		}
		return posting;
	}
	
	/**
	 * Reads the term frequency for the current posting, and aligns the stream.
	 * @return the frequency read.
	 * @throws IOException if an I/O error occurs.
	 */
	public int readTermFrequency() throws IOException{
		int temp = mbis.readGamma();
		mbis.align();
		return temp;
	}
	
	/**
	 * Reads the String identifying a term from the underlying stream.
	 * @return the String with the term.
	 * @throws IOException if an I/O error occurs.
	 */
	public String readString() throws IOException{
		int size = mbis.readGamma();
		if (UTFindexing)
		{
			//readUTF knows the length of a written string, size not needed 
			return stringDIS.readUTF();
		}
		else	
		{
			byte[] array = new byte[size];
			stringDIS.readFully(array,0,size);
			return new String(array);
		}
	}

}
