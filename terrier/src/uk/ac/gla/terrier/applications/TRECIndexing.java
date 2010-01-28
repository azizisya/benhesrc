/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.ac.gla.uk
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
 * The Original Code is TRECIndexing.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.applications;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.indexing.BasicSinglePassIndexer;
import uk.ac.gla.terrier.indexing.BlockSinglePassIndexer;
import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.indexing.CollectionFactory;
import uk.ac.gla.terrier.indexing.Indexer;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class creates the indices for a test collection.
 * <p>
 * <b>Properties:</b>
 * <ul>
 * <li><tt>trec.indexer.class</tt> - name of the class to use as the indexer. This only applies to the Index method.</li>
 * <li><tt>trec.collection.class</tt> - name of the class to use as the Collection.</li>
 * </ul>
 * @author Gianni Amati, Vassilis Plachouras, Ben He, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class TRECIndexing {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	/** The collection to index. */
	Collection collectionTREC;
	
	String path; String prefix;
	
	/** The indexer object.*/
	Indexer indexer;
	/**
	 * A default constructor that initialised the data structures
	 * to use for indexing.
	 */
	public TRECIndexing(String _path, String _prefix)
	{
		path = _path; prefix = _prefix;
		//load the appropriate collection
		final String collectionName = ApplicationSetup.getProperty("trec.collection.class", "TRECCollection");
		collectionTREC = CollectionFactory.loadCollection(collectionName);
		if (collectionTREC == null)
		{
			logger.fatal("Collection class named "+ collectionName + " not found, aborting");
		}

		//load the appropriate indexer
		String indexerName = ApplicationSetup.getProperty(
			"trec.indexer.class",
			ApplicationSetup.BLOCK_INDEXING
				? "BlockIndexer"
				: "BasicIndexer");
		if (indexerName.indexOf('.') == -1)
			indexerName = "uk.ac.gla.terrier.indexing."+indexerName;
		try{
			indexer = (Indexer) Class.forName(indexerName)
				.getConstructor(String.class, String.class)
				.newInstance(path, prefix);
		} catch (ClassNotFoundException e) {
			logger.fatal("Indexer class named "+ indexerName + " not found", e);
		} catch (InstantiationException ie) {
			logger.fatal("Error while instantiating Indexer class named "+ indexerName + " : " + ie.getCause(), ie);
		} catch (Exception e){
			logger.fatal("Indexer class named "+ indexerName + "problem", e);
		}
	}

	/**
	 * A default constructor that initialised the data structures
	 * to use for indexing.
	 */
	public TRECIndexing() {
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	
	/**
	 * Calls the method index(Collection[]) of the
	 * class Indexer in order to build the data
	 * structures for a set of collections. This 
	 * particular method of the Indexer uses a 
	 * set of builders for a subset of the collection
	 * and builds separate data structures, which are 
	 * later merged.
	 */
	public void index() {
		if (Index.existsIndex(path, prefix))
		{
			logger.fatal("Cannot index while an index exists at "+path + ","+ prefix);
			return;
		}
		indexer.index(new Collection[] {collectionTREC});
		collectionTREC.close();
	}
	
	/**
	 * Building the inverted file.
	 */
	public void createInvertedFile() {
		if (Index.existsIndex(path, prefix))
		{
			Index i = Index.createIndex();
			if (i == null) {}
			else if (i.hasIndexStructure("inverted"))
			{
				logger.fatal("Cannot create an inverted structure while an index with a inverted structure exists at "+path + ","+ prefix);
				return;
			}
			else if (! i.hasIndexStructure("direct"))
			{
				logger.fatal("Cannot create an inverted structure without a direct structure in the index at "+path + ","+ prefix);
				return;
			}
		}
		else
		{
			logger.fatal("Cannot create an inverted structure without an index at "+path + ","+ prefix);
			return;
		}
		if(logger.isInfoEnabled())
			logger.info("Started building the inverted index...");
		long beginTimestamp = System.currentTimeMillis();
		indexer.createInvertedIndex();
		long endTimestamp = System.currentTimeMillis();
		if(logger.isInfoEnabled()){
			logger.info("Finished building the inverted index...");
			double seconds = (endTimestamp - beginTimestamp) / 1000.0d;
			logger.info("Time elapsed for inverted file: " + seconds);
		}
	}
	
	/**
	 * Builds the direct file and lexicon. This method goes through the 
	 * input files specified in the <tt>collections.spec</tt> file 
	 * and processes them in groups of n documents, where n is specified 
	 * by the property <tt>bundle.size</tt>. Then, it merges the 
	 * temporary lexicon files. If it necessary, it calls for the 
	 * optimisation of the identifiers assigned to terms.
	 */
	public void createDirectFile() {
		if (Index.existsIndex(path, prefix))
		{
			Index i = Index.createIndex();
			if (i == null) {}
			else if (i.hasIndexStructure("direct"))
			{	
				logger.fatal("Cannot create a direct structure while an index with a direct structure exists at "+path + ","+ prefix);
				return;
			}
		}	
		long startTime = System.currentTimeMillis();
		indexer.createDirectIndex(new Collection[] {collectionTREC});
		long endTime = System.currentTimeMillis();
		if(logger.isInfoEnabled())	
			logger.info("Direct index built in "+((endTime-startTime)/1000.0D) 
						 + " seconds.");
		collectionTREC.close();
	}
	
	/**
	 * Builds the inverted file from scratch, single pass method
	 */
	public void createSinglePass(){
		if (Index.existsIndex(path, prefix))
		{
			Index i = Index.createIndex();
			if (i.hasIndexStructure("inverted"))
			{
				logger.fatal("Cannot create an inverted structure while an index with a inverted structure exists at "+path + ","+ prefix);
				return;
			}
		}
		System.err.println("Starting building the inverted file "
			+ (ApplicationSetup.BLOCK_INDEXING ? "(with blocks)" : "") 
			+ "...");
		final long beginTimestamp = System.currentTimeMillis();
		BasicSinglePassIndexer indexer;
		if (ApplicationSetup.BLOCK_INDEXING)
			indexer = new BlockSinglePassIndexer(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
		else
			indexer = new BasicSinglePassIndexer(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
		indexer.index(new Collection[] {collectionTREC});
		long endTimestamp = System.currentTimeMillis();
		System.err.println("Finished building the inverted index...");
		double seconds = (endTimestamp - beginTimestamp) / 1000.0d;
		System.err.println("Time elapsed for inverted file: " + seconds);
		collectionTREC.close();
	}
	
	/** 
	 * Used for testing purposes.
	 * @param args the command line arguments.
	 */
	public static void main(String[] args)
	{
		long startTime = System.currentTimeMillis();
		TRECIndexing t = new TRECIndexing();
		
		t.index();
		long endTime = System.currentTimeMillis();
		if(logger.isInfoEnabled())
			logger.info("Elapsed time="+((endTime-startTime)/1000.0D));
	}
	
	
}
