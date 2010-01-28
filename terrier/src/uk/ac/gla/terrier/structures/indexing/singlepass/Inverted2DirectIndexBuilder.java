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
 * The Original Code is Inverted2DirectIndexBuilder.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald (craigm{at}dcs.gla.ac.uk)
 */
package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.compression.BitInputStream;
import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.compression.BitOutputStream;
import uk.ac.gla.terrier.compression.MemorySBOS;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.DocumentIndexOutputStream;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndexInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import uk.ac.gla.terrier.utility.Files;

/** Create a direct index from an InvertedIndex. The algorithm is similar to that followed by
  * InvertedIndexBuilder. To summarise, InvertedIndexBuilder builds an InvertedIndex from a DirectIndex.
  * This class does the opposite, building a DirectIndex from an InvertedIndex.
  * <P><B>Algorithm:</b><br>
  * For a selection of document ids
  * &nbsp;(Scan the inverted index looking for postings with these document ids)
  * &nbsp;For each term in the inverted index
  * &nbsp;&nbsp;Select required postings from all the postings of that term
  * &nbsp;&nbsp;Add these to posting objects that represents each document
  * &nsbp;For each posting object
  * &nbsp;&nbsp;Write out the postings for that document
  * <p><b>Notes:</b><br>
  * This algorithm assumes that termids start at 0 and are strictly increasing. This assumption holds true
  * only for inverted indices generated by the single pass indexing method.
  * <p><b>Properties:</b> 
  * <ol>
  * <li><tt>inverted2direct.processtokens</tt> - total number of tokens to attempt each iteration. Defaults to 100000000. Memory usage would more likely
  * be linked to the number of pointers, however as the document index does not contain the number of unique terms in each document, the pointers 
  * calculation is impossible to make.</li>
  * </ol>
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  * @since 2.0 */
public class Inverted2DirectIndexBuilder {
	/** The logger used */
	protected static Logger logger = Logger.getRootLogger();
	/** index currently being used */
	protected Index index;
	
	/** The number of different fields that are used for indexing field information.*/
	protected static final int fieldTags = FieldScore.FIELDS_COUNT;

	/** Indicates whether field information is used. */
	protected static final boolean saveTagInformation = FieldScore.USE_FIELD_INFORMATION;

	/** Class to read the generated direct index */
	protected String directIndexClass = "uk.ac.gla.terrier.structures.DirectIndex";
	/** Class to read the generated inverted index */
	protected String directIndexInputStreamClass = "uk.ac.gla.terrier.structures.DirectIndexInputStream";
	/** number of tokens limit per iteration */
	protected long  processTokens = Long.parseLong(ApplicationSetup.getProperty("inverted2direct.processtokens", "100000000"));
	
	/** Construct a new instance of this builder class */
	public Inverted2DirectIndexBuilder(Index i)
	{
		this.index =  i;
	}
	
	/** create the direct index when the collection contains an existing inverted index */
	public void createDirectIndex()
	{
		if( ! index.hasIndexStructure("inverted"))
		{
			logger.error("This index has no inverted index, aborting direct index build");
			return;
		}
		if ( index.hasIndexStructure("direct"))
		{
			logger.error("This index already has a direct index, no need to create one.");
			return;
		}
		if (index.getIndexProperty("index.terrier.version", "2.0").startsWith("1.") )
		{
			logger.error("Index version from Terrier 1.x - it is likely that the termids are not aligned, and hence df creation would not be correct - aborting direct index build");
			return;
		}
		logger.info("Generating a direct index from an inverted index");
		int firstDocid = 0;
		int lastDocid = 0;
		final long totalTokens = index.getCollectionStatistics().getNumberOfTokens();
		final String iterationSuffix = (processTokens > totalTokens) ? 
				" of 1 iteration" : 
					" of " + (int)((totalTokens%processTokens==0)?(totalTokens/processTokens)
							:(totalTokens/processTokens+1)) + " iterations";
		long numberOfTokensFound = 0;	
		int iteration = 0;
		try{
			DocumentIndexInputStream diis =  (DocumentIndexInputStream) index.getIndexStructureInputStream("document"); 
			final DataOutputStream offsetsTmpFile = new DataOutputStream(
					Files.writeFileStream( 
							index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DF_SUFFIX + ".offsets")
					);
			final BitOut bos = new BitOutputStream(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DF_SUFFIX);
			do//for each pass of the inverted file
			{
				iteration++;
				logger.info("Iteration "+iteration  + iterationSuffix);
				//get a copy of the inverted index
				final InvertedIndexInputStream iiis = (InvertedIndexInputStream) index.getIndexStructureInputStream("inverted");
				//work out how many document we can scan for
				lastDocid = scanDocumentIndexForTokens(processTokens, diis);
				logger.info("Generating postings for documents with ids "+firstDocid + " to " + lastDocid);
				//get a set of posting objects to save the compressed postings for each of the documents to
				final Posting[] postings = getPostings(lastDocid - firstDocid +1 );
				//get postings for these documents
				numberOfTokensFound += traverseInvertedFile(iiis, firstDocid, lastDocid , postings);
				logger.info("Writing the postings to disk");
				int id = firstDocid;
				for (Posting p : postings)
				{	
					//logger.debug("Document " + id  + " length="+ p.getDocF());
					id++;
					//if the document is non-empty
					if (p.getDocF() > 0)
					{
					
						//obtain the compressed memory posting list
						final MemorySBOS Docs = p.getDocs();
						//some obscure problem when reading from memory rather than disk.
						//by padding the posting list with some non zero bytes the problem
						//is solved. Thanks to Roicho for working this one out.
						Docs.writeGamma(1);
						Docs.writeGamma(1);
						Docs.pad();
					
						//use a SimplePostingInRun to decompress the postings stored in memory
						final PostingInRun pir = getPostingReader();
						pir.setDf(p.getDocF());
						pir.setTF(p.getTF());
						pir.setPostingSource(new BitInputStream(new ByteArrayInputStream(
							Docs.getMOS().getBuffer())));
						//System.err.println("temp compressed buffer size="+Docs.getMOS().getPos() + " length="+Docs.getMOS().getBuffer().length);
						//decompress the memory postings and write out to the direct file
						pir.append(bos, -1);
					}

					//get and decrement the offsets by 1 bit
					long endByte = bos.getByteOffset();
					byte endBit = bos.getBitOffset();
					endBit--;
				
					if (endBit < 0 && endByte > 0) {
						endBit = 7;
						endByte--;
					}
					//take note of the offset for this document in the df
					offsetsTmpFile.writeLong(endByte);
					offsetsTmpFile.writeByte(endBit);
				}// /for document postings
				firstDocid = lastDocid +1;
			} while(firstDocid <  -1 + index.getCollectionStatistics().getNumberOfDocuments());

			if (numberOfTokensFound != totalTokens)
			{
				logger.warn("Number of tokens found while scanning inverted index does not match expected. Expected "
					+index.getCollectionStatistics().getNumberOfTokens()+ ", found " + numberOfTokensFound);
			}
			logger.info("Finishing up: rewriting document index");	
			offsetsTmpFile.close();
			//write the offsets to the DocumentIndex
			final DataInputStream dis = new DataInputStream(Files.openFileStream(
				index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DF_SUFFIX + ".offsets"));
			final DocumentIndexOutputStream dios = new DocumentIndexOutputStream(
				index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DOC_INDEX_SUFFIX + ".withoffsets");
			diis = (DocumentIndexInputStream) index.getIndexStructureInputStream("document");
			while(diis.readNextEntry() > -1)
			{
				long endByte = dis.readLong();
				byte endBit = dis.readByte();
				dios.addEntry(diis.getDocumentNumber() , diis.getDocumentLength(), endByte, endBit);
			}
			bos.close();
			diis.close();
			dis.close();
			dios.close();
			Files.delete(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DF_SUFFIX + ".offsets");
			Files.delete(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DOC_INDEX_SUFFIX);
			Files.rename(index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DOC_INDEX_SUFFIX + ".withoffsets",
				index.getPath() + ApplicationSetup.FILE_SEPARATOR + index.getPrefix() + ApplicationSetup.DOC_INDEX_SUFFIX);

			//inform the index about the new data structure
			index.addIndexStructure(
					"direct", 
					directIndexClass,
					"uk.ac.gla.terrier.structures.DocumentIndex,java.lang.String,java.lang.String", 
					"document,path,prefix");
			index.addIndexStructureInputStream(
					"direct",
					directIndexInputStreamClass,
					"java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.DocumentIndexInputStream",
					"path,prefix,document-inputstream");
			index.setIndexProperty("num.direct.fields.bits", ""+fieldTags);
			index.flush();//save changes
			
			logger.info("Finished generating a direct index from an inverted index");

		}catch (IOException ioe) {
			logger.error("Couldnt create a direct index from the inverted index", ioe);
		}
	}

	/** get an array of posting object of the specified size. These will be used to hold
	  * the postings for a range of documents */	
	protected Posting[] getPostings(final int count)
	{
		Posting[] rtr = new Posting[count];
		if (saveTagInformation)
		{
			for(int i=0;i<count;i++)
				rtr[i] = new FieldPosting();
		}
		else
		{
			for(int i=0;i<count;i++)
				rtr[i] = new Posting();
		}
		return rtr;
	}

	/** returns the SPIR implementation that should be used for reading the postings
	  * written earlier */	
	protected PostingInRun getPostingReader()
	{
		if (saveTagInformation)
		{
			return new FieldPostingInRun();
		}
		return new SimplePostingInRun();
	}
	
	/** traverse the inverted file, looking for all occurrences of documents in the given range
	  * @return the number of tokens found in all of the document. */
	protected long traverseInvertedFile(final InvertedIndexInputStream iiis, int firstDocid, int lastDocid, final Posting[] directPostings)
		throws IOException
	{
		//foreach posting list in the inverted index
			//for each (in range) posting in list
				//add termid->tf tuple to the Posting array
		long tokens = 0; long numPostings = 0;
		int[][] postings;
		int termId = -1;
		//array recording which of the current set of documents has had any postings written thus far
		boolean[] prevUse = new boolean[lastDocid - firstDocid+1];
		Arrays.fill(prevUse, false);
		
		while((postings = iiis.getNextDocuments()) != null)
		{
			termId++;
			final int[] postings0 = postings[0];
			final int[] postings1 = postings[1];
			final int[] postings2 = saveTagInformation ? postings[2] : null;
			int startOffset = Arrays.binarySearch(postings0, firstDocid);
			int endOffset = Arrays.binarySearch(postings0, lastDocid+1);
			if (startOffset < 0)
				startOffset = -(startOffset+1);
			//no documents in range for this term
			if (startOffset == postings0.length)
				continue;
			if (endOffset < 0)
				endOffset = -(endOffset+1);
			if (endOffset == 0)
				continue;
			//System.err.println("postings_Length="+postings0.length+" start="+startOffset+ " end="+endOffset);
			for(int offset = startOffset; offset<endOffset;offset++)
			{
				//System.err.println("Processing posting at offset="+offset);
				if (postings0[offset] >= firstDocid && postings0[offset] <= lastDocid)
				{
					final int writerOffset = postings0[offset] - firstDocid;
					tokens += postings1[offset];
					numPostings++;
					if (prevUse[writerOffset])
					{
						if (saveTagInformation)
							((FieldPosting)directPostings[writerOffset]).insert(termId, postings1[offset],  postings2[offset]);
						else
							directPostings[writerOffset].insert(termId, postings1[offset]);
					}
					else
					{
						prevUse[writerOffset] = true;
						if (saveTagInformation)
							((FieldPosting)directPostings[writerOffset]).writeFirstDoc(termId, postings1[offset],  postings2[offset]);
						else
							directPostings[writerOffset].writeFirstDoc(termId, postings1[offset]);
					}
				}
			}
		}
		logger.info("Finished scanning inverted file, identified "+numPostings+" postings ("+tokens+" tokens) from "+termId + " terms");
		return tokens;
	}
	
	/** Iterates through the document index, until it has reached the given number of terms
	  * @param processTerms Number of terms to stop reading the lexicon after
	  * @param docidStream the document index stream to read 
	  * @return the number of documents to process
	  */
	protected int scanDocumentIndexForTokens(
		final long processTokens, 
		final DocumentIndexInputStream docidStream)
		throws IOException
	{
		long tokens = 0;
		while( docidStream.readNextEntry() != -1)	
		{
			tokens += docidStream.getDocumentLength();
			if (tokens > processTokens)
				break;
		}
		return docidStream.getDocumentId();
	}
	
	public static void main (String[] args)
	{
		Index i = Index.createIndex();
		if (i== null)
		{
			System.err.println("Sorry, no index could be found in default location");
			return;
		}
		new Inverted2DirectIndexBuilder(i).createDirectIndex();
		i.close();
	}
}
