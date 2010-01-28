/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is FileCollectionRecordReader.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   
 */
package uk.ac.gla.terrier.structures.indexing.singlepass.hadoop;


import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MultiFileSplit;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.indexing.CollectionFactory;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Wrapper;
import uk.ac.gla.terrier.utility.io.CountingInputStream;

/**
 * Record Reader for Hadoop Indexing. Reads documents from a file,
 * when one document is empty the next is loaded. Acts like a wrapper
 * around the Terrier Collection Class.
 * @author Richard McCreadie
 * @since 2.2
 * @version $Revision: 1.1 $
 */
public class FileCollectionRecordReader 
		extends CollectionRecordReader<MultiFileSplit> 
		implements RecordReader<Text, Wrapper<Document>>
{

    /** The logger used */
    protected static Logger logger = Logger.getLogger(FileCollectionRecordReader.class);
	/** the current input stream accessing the underlying (uncompressed) file, used 
	 * for counting progress.
	 */
	protected CountingInputStream inputStream = null;
	//TODO: start is unused currently?
	/** where we started in this file */
	protected long start;
	/** factory for accessing compressed files */
	protected CompressionCodecFactory compressionCodecs = null;
	
	
	/**
	 * Constructor
	 * @param jobConf - Configuration
	 * @param split - Input Split (multiple Files)
	 * @throws IOException
	 */
	public FileCollectionRecordReader(JobConf jobConf, MultiFileSplit split) throws IOException 
	{	
		super(jobConf, split);
		compressionCodecs = new CompressionCodecFactory(config);
	}
	
	/**
	 * Gives the input in the raw, uncompressed stream.
	 */
	public long getPos() throws IOException {
		if (inputStream == null) 
			return 0;
		return inputStream.getPos();
	}
	
	/**
	 * Returns the progress of the reading
	 */
	public float getProgress() throws IOException {
		return (float)collectionIndex/(float)(split.getNumPaths());
	}
	
	/** Opens a collection on the next file. */
	@Override
	protected Collection openCollectionSplit(int index) throws IOException
	{
		if (index >= split.getNumPaths())
		{
			//no more splits left to process
			return null;
		}
		Path file = split.getPath(index);
		logger.info("Opening "+file);
		long offset = 0;//TODO populate from split?
		FileSystem fs = file.getFileSystem(config);


		//WT2G collection has incorrectly named extensions. Terrier can deal with this,
		//Hadoop cant
		CompressionCodec codec = compressionCodecs.getCodec(
			new Path(file.toString().replaceAll("\\.GZ$", ".gz")));

		FSDataInputStream _input = fs.open(file); //TODO: we could use utility.Files here if
		//no codec was found	
		InputStream internalInputStream = null;
		start = offset;
		
		if (codec !=null)
		{
			start = 0;
			inputStream = new CountingInputStream(_input);
			internalInputStream = codec.createInputStream(inputStream);
		} 
		else 
		{
			if (start != 0) //TODO: start is always zero? 
			{
		        --start;
		        _input.seek(start);
			}
			internalInputStream = inputStream = new CountingInputStream(_input);
		}
		Collection rtr = CollectionFactory.loadCollection(
			ApplicationSetup.getProperty("trec.collection.class", "TRECCollection"), 
			new Class[]{InputStream.class}, 
			new Object[]{internalInputStream});

		if (rtr == null)
		{
			throw new IOException("Collection did not load properly");
		}
		return rtr;
	}

	/*
	protected void closeCollectionSplit() throws IOException {
		System.err.println("closeCollectionSplit occurred when "+inputStream.getPos()+ " bytes had been read from the input file, documents read is "+currentDocument);
		super.closeCollectionSplit();
    }*/

}

