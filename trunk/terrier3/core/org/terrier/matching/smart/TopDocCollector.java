package org.terrier.matching.smart;

import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.dutir.util.PriorityQueue;
import org.terrier.matching.AccumulatorResultSet;
import org.terrier.matching.QueryResultSet;
import org.terrier.matching.ResultSet;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A {@link HitCollector} implementation that collects the top-scoring
 * documents, returning them as a {@link TopDocs}. This is used by
 * {@link IndexSearcher} to implement {@link TopDocs}-based search.
 * 
 * <p>
 * This may be extended, overriding the collect method to, e.g., conditionally
 * invoke <code>super()</code> in order to filter which documents are collected.
 **/
public class TopDocCollector extends HitCollector {

	private static Logger logger = Logger.getRootLogger();

	public int[] docids;
	public double[] scores;
	public short[] occurrences;
	protected boolean arraysInitialised = false;

	private ScoreDoc reusableSD;

	/** The total number of hits the collector encountered. */
	protected int totalHits;
	protected int resultSize;
	
	/** The priority queue which holds the top-scoring documents. */
	protected PriorityQueue hq;

	/**
	 * Construct to collect a given number of hits.
	 * 
	 * @param numHits
	 *            the maximum number of hits to collect
	 */
	public TopDocCollector(int numHits) {
		this(new HitQueue(numHits));
	}

	/**
	 * @deprecated use TopDocCollector(hq) instead. numHits is not used by this
	 *             constructor. It will be removed in a future release.
	 */
	TopDocCollector(int numHits, PriorityQueue hq) {
		this.hq = hq;
	}

	/**
	 * Constructor to collect the top-scoring documents by using the given PQ.
	 * 
	 * @param hq
	 *            the PQ to use by this instance.
	 */
	protected TopDocCollector(PriorityQueue hq) {
		this.hq = hq;
	}

	// javadoc inherited
	public void collect(int doc, double score, short mask) {
		// if (score > 0.0f) {
		totalHits++;
		if (reusableSD == null) {
			reusableSD = new ScoreDoc(doc, score, mask);
		} else if (score >= reusableSD.score) {
			// reusableSD holds the last "rejected" entry, so, if
			// this new score is not better than that, there's no
			// need to try inserting it
			reusableSD.doc = doc;
			reusableSD.score = score;
		} else {
			return;
		}
		reusableSD = (ScoreDoc) hq.insertWithOverflow(reusableSD);
		// }
	}

	/** The total number of documents that matched this query. */
	public int getTotalHits() {
		return totalHits;
	}

	private TopDocs topDocsCache = null;

	private int statusCode;

	/**
	 * The top-scoring hits. originally, this method can only be called once.
	 * Revise yezheng: cache the TopDocs, so that it can be reused.
	 * */
	public TopDocs topDocs() {
		if (topDocsCache == null) {
			ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
			for (int i = hq.size() - 1; i >= 0; i--)
				// put docs in array
				scoreDocs[i] = (ScoreDoc) hq.pop();

			double maxScore = (totalHits == 0) ? Double.NEGATIVE_INFINITY
					: scoreDocs[0].score;

			topDocsCache = new TopDocs(totalHits, scoreDocs, maxScore);
		}
		return topDocsCache;
	}

	public void initialise(double[] scs) {
		throw new UnsupportedOperationException(
				"This method is not available for class "
						+ AccumulatorResultSet.class);
	}

	/** {@inheritDoc} */
	public int[] getDocids() {
		if (arraysInitialised)
			return docids;
		else
			throw new UnsupportedOperationException("");
	}

	/** {@inheritDoc} */
	public int getResultSize() {
		if(arraysInitialised){
			return resultSize;
		}else{
			return hq.size();
		}
		
	}

	/** {@inheritDoc} */
	public short[] getOccurrences() {
		if (arraysInitialised)
			return occurrences;
		else
			throw new UnsupportedOperationException("");
	}

	/** {@inheritDoc} */
	public int getExactResultSize() {
		return totalHits;
	}

	/** {@inheritDoc} */
	public double[] getScores() {
		if (arraysInitialised)
			return scores;
		else
			throw new UnsupportedOperationException("");
	}

	/** {@inheritDoc} */
	public void setResultSize(int newResultSize) {
		totalHits = newResultSize;
	}

	/** {@inheritDoc} */
	public void setExactResultSize(int newExactResultSize) {
//		throw new UnsupportedOperationException("");
	}

	/** Unsupported */
	public void addMetaItem(String name, int docid, String value) {
	}

	/** Unsupported */
	public void addMetaItems(String name, String[] values) {
	}

	/** Unsupported */
	public String getMetaItem(String name, int docid) {
		return null;
	}

	/** Unsupported */
	public String[] getMetaItems(String name) {
		return null;
	}

	/** Unsupported */
	public boolean hasMetaItems(String name) {
		return false;
	}

	/** Unsupported */
	public String[] getMetaKeys() {
		return new String[0];
	}

	/** {@inheritDoc} */
	public ResultSet getResultSet(int start, int length) {
		if (arraysInitialised) {
			length = length < docids.length ? length : docids.length;
			QueryResultSet resultSet = new QueryResultSet(length);
			System.arraycopy(docids, start, resultSet.getDocids(), 0, length);
			System.arraycopy(scores, start, resultSet.getScores(), 0, length);
			System.arraycopy(occurrences, start, resultSet.getOccurrences(), 0,
					length);
			return resultSet;
		} else
			throw new UnsupportedOperationException("");
	}

	/** {@inheritDoc} */
	public ResultSet getResultSet(int[] positions) {
		if (arraysInitialised) {
			int NewSize = positions.length;
			if (logger.isDebugEnabled())
				logger.debug("New results size is " + NewSize);
			QueryResultSet resultSet = new QueryResultSet(NewSize);
			int newDocids[] = resultSet.getDocids();
			double newScores[] = resultSet.getScores();
			short newOccurs[] = resultSet.getOccurrences();
			int thisPosition;
			for (int i = 0; i < NewSize; i++) {
				thisPosition = positions[i];
				if (logger.isDebugEnabled())
					logger.debug("adding result at " + i);
				newDocids[i] = docids[thisPosition];
				newScores[i] = scores[thisPosition];
				newOccurs[i] = occurrences[thisPosition];
			}
			return resultSet;
		} else
			throw new UnsupportedOperationException("");
	}

	@Override
	public Lock getLock() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("");
	}

	@Override
	public int getStatusCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void initialise() {
		if (!arraysInitialised) {
			TopDocs topdocs = topDocs();
			int len = resultSize = topdocs.scoreDocs.length;
			this.docids = new int[len];
			this.scores = new double[len];
			this.occurrences = new short[len];
			for (int i = 0; i < len; i++) {
				docids[i] = topdocs.scoreDocs[i].doc;
				// double _value = Math.pow(2, topdocs.scoreDocs[i].score);
				scores[i] = topdocs.scoreDocs[i].score;
				occurrences[i] = topdocs.scoreDocs[i].mask;
			}
		}
		arraysInitialised = true;
	}

	@Override
	public void setStatusCode(int statusCode) {
		throw new UnsupportedOperationException("");
	}

}
