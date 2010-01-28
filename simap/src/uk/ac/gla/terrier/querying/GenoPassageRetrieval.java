/*
 * Created on 24 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.querying;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.applications.GenoPassageExtraction;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;

public class GenoPassageRetrieval implements PostProcess{
	protected static Logger logger = Logger.getRootLogger();
	/** The document index used for retrieval. */
	protected DocumentIndex documentIndex;
	/** The inverted index used for retrieval. */
	protected InvertedIndex invertedIndex;
	/** An instance of Lexicon class. */
	protected Lexicon lexicon;
	/** The direct index used for retrieval. */
	protected DirectIndex directIndex;
	/** The statistics of the index */
	protected CollectionStatistics collStats;
	
	public GenoPassageExtraction genoPassageExtractor;
	
	/**
	* The default constructor of QueryExpansion.
	*/
	public GenoPassageRetrieval() {}

	/** For easier sub-classing of which index the query expansion comes from */
	protected Index getIndex(Manager m)
	{
		return m.getIndex();
	}
	
	public String getInfo(){
		return genoPassageExtractor.getInfo();
	}
	
	public void retrieveGenoPassages(MatchingQueryTerms query, ResultSet resultSet){
		int[] docids = resultSet.getDocids();
		
	}


	/**
	 * Runs the actual passage extraction
	 * @see uk.ac.gla.terrier.querying.PostProcess#process(uk.ac.gla.terrier.querying.Manager,uk.ac.gla.terrier.querying.SearchRequest)
	 */
	public void process(Manager manager, SearchRequest q) {
	   	Index index = getIndex(manager);
		documentIndex = index.getDocumentIndex();
		invertedIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		collStats = index.getCollectionStatistics(); 
		directIndex = index.getDirectIndex();
		if (directIndex == null)
		{
			logger.error("This index does not have a direct index. Query expansion disabled!!");
			return;
		}
		logger.info("Starting genomics passage extraction post-processing.");
		
		
		
		
		// manager.runMatching(q);
	}
}
