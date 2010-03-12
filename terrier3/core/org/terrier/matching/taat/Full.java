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
 * The Original Code is Full.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Nicola Tonellotto (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   
 */
package org.terrier.matching.taat;

import java.io.IOException;

import org.terrier.matching.AccumulatorResultSet;
import org.terrier.matching.BaseMatching;
import org.terrier.matching.CollectionResultSet;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;

/** An exhaustive TAAT approach for matching documents to a query.
 * @author Nicola Tonellotto, Craig Macdonald
 * @since 3.0
 */
public class Full extends BaseMatching
{
	public Full(Index index) 
	{
		super(index);
		resultSet = new AccumulatorResultSet(collectionStatistics.getNumberOfDocuments());		
	}

	public String getInfo() 
	{
		return "Full term-at-a-time (TAAT) algorithm";
	}

	public ResultSet match(String queryNumber, MatchingQueryTerms queryTerms) throws IOException 
	{
		initialise(queryTerms);
		// Check whether we need to match an empty query. If so, then return the existing result set.
		// String[] queryTermStrings = queryTerms.getTerms();
		if (MATCH_EMPTY_QUERY && queryTermsToMatchList.size() == 0) {
			resultSet = new CollectionResultSet(collectionStatistics.getNumberOfDocuments());
			resultSet.setExactResultSize(collectionStatistics.getNumberOfDocuments());
			resultSet.setResultSize(collectionStatistics.getNumberOfDocuments());
			return resultSet;
		}
						
		int queryLength = queryTermsToMatchList.size();
		// The posting list iterator from the inverted file
		IterablePosting postings;		
		for (int i = 0; i < queryLength; i++) 
		{
			LexiconEntry lexiconEntry = queryTermsToMatchList.get(i).getValue();
			postings = invertedIndex.getPostings((BitIndexPointer)lexiconEntry);
			assignScores(i, wm[i], (AccumulatorResultSet) resultSet, postings);
		}

		resultSet.initialise();
		this.numberOfRetrievedDocuments = resultSet.getExactResultSize();
		finalise(queryTerms);
		return resultSet;
	}
	
	protected void assignScores(int i, final WeightingModel[] wModels, AccumulatorResultSet rs, final IterablePosting postings) throws IOException
	{
		int docid;
		double score;
		
		short mask = 0;
		if (i < 16)
			mask = (short)(1 << i);
		
		while (postings.next() != IterablePosting.EOL)
		{
			score = 0.0; docid = postings.getId();

			for (WeightingModel wmodel: wModels)
				score += wmodel.score(postings);
			if ((!rs.scoresMap.contains(docid)) && (score > 0.0d))
				numberOfRetrievedDocuments++;
			else if ((rs.scoresMap.contains(docid)) && (score < 0.0d))
				numberOfRetrievedDocuments--;

			rs.scoresMap.adjustOrPutValue(docid, score, score);
			rs.occurrencesMap.put(docid, (short)(rs.occurrencesMap.get(docid) | mask));
		}
	}
}
