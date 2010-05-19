package org.terrier.matching.smart;

import gnu.trove.TIntHashSet;

import java.io.IOException;

import org.dutir.util.PriorityQueue;
import org.terrier.matching.BaseMatching;
import org.terrier.matching.CollectionResultSet;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ApplicationSetup;

public class SmartMatching extends BaseMatching {
	/** The number of results to output. */
	static int RESULTS_LENGTH = Integer.parseInt(ApplicationSetup.getProperty(
			"trec.output.format.length", "1000"));

	public SmartMatching(Index index) {
		super(index);
//		resultSet = new AccumulatorResultSet(collectionStatistics
//				.getNumberOfDocuments()); // ???????????
		resultSet = new TopDocCollector(RESULTS_LENGTH);
	}

	@Override
	public String getInfo() {
		return "SmartMatching";
	}

	@Override
	public ResultSet match(String queryNumber, MatchingQueryTerms queryTerms)
			throws IOException {
		initialise(queryTerms);
		String[] queryTermStrings = queryTerms.getTerms();
		// Check whether we need to match an empty query. If so, then return the
		// existing result set.
		// String[] queryTermStrings = queryTerms.getTerms();
		if (MATCH_EMPTY_QUERY && queryTermsToMatchList.size() == 0) {
			resultSet = new CollectionResultSet(collectionStatistics
					.getNumberOfDocuments());
			resultSet.setExactResultSize(collectionStatistics
					.getNumberOfDocuments());
			resultSet
					.setResultSize(collectionStatistics.getNumberOfDocuments());
			return resultSet;
		}
		resultSet = new TopDocCollector(RESULTS_LENGTH);
		int queryLength = queryTermsToMatchList.size();
		// The posting list iterator from the inverted file
		IterablePosting postings;
		Posting[] psts = new Posting[queryLength];
		for (int i = 0; i < queryLength; i++) {
			LexiconEntry lexiconEntry = queryTermsToMatchList.get(i).getValue();
			logger.debug((i + 1) + ": " + queryTermsToMatchList.get(i).getKey() + " with " + lexiconEntry.getDocumentFrequency() 
					+ " documents (TF is " + lexiconEntry.getFrequency() + ").");
			postings = invertedIndex
					.getPostings((BitIndexPointer) lexiconEntry);
			psts[i] = new Posting(postings, wm[i]);
		}
		assignScores(psts, (TopDocCollector) resultSet);

		resultSet.initialise();
		this.numberOfRetrievedDocuments = resultSet.getExactResultSize();
		finalise(queryTerms);
		for (int i=0; i<queryLength; i++)
			psts[i] = null;
		return resultSet;
	}

	protected void assignScores(Posting[] psts, TopDocCollector collector)
			throws IOException {
		int len = psts.length;
		PostingQueue pq = new PostingQueue(len);
		for (int i = 0; i < len; i++) {
			psts[i].initise(pq);
		}

		
		while (pq.size() > 0) {
			int docid = ((Integer) pq.pop()).intValue();
			double value = 0;
			short mask = 0;
			int docLen = this.index.getDocumentIndex()
			.getDocumentLength(docid);
			for (int j = 0; j < len; j++) {
				value += psts[j].score(docid, docLen, pq);
				if (j < 16)
					mask = (short) (mask | (short) (1 << j));
			}
			numberOfRetrievedDocuments++;
			if (ACCEPT_NEGATIVE_SCORE)
				collector.collect(docid, Math.pow(2, value), mask);
			else
				collector.collect(docid, value, mask);
		}

		collector.initialise();
		pq.clear(); pq = null;
	}
	
//	protected void finalise(MatchingQueryTerms queryTerms) {
//		
//	}

	/**
	 * on condition that doc number in the Posting is ordered.
	 * @author yezheng
	 *
	 */
	class Posting {
		IterablePosting postings;
		int doc = -1;
		// int tf = 0;
		// int df = 0;
		WeightingModel[] wm;
		boolean hasTag = false;

		public Posting(IterablePosting postings, WeightingModel[] wm) {
			this.postings = postings;
			this.wm = wm;
		}

		public void initise(PostingQueue pq) {
			try {
				if (postings.next() != IterablePosting.EOL) {
					doc = postings.getId();
					hasTag = true;
					pq.insert(doc);
				} else {
					hasTag = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public double score(int docid, int docLen, PostingQueue pq) {
			double score = 0;
			if (hasTag) {
				if (doc == docid) {
					for (WeightingModel wmodel : wm)
						score += wmodel.score(postings.getFrequency(), docLen);
					try {
						if (postings.next() != IterablePosting.EOL) {
							doc = postings.getId();
							hasTag = true;
							pq.insert(doc);
						} else {
							hasTag = false;
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
//					logger.info("id:" +  docid + ", " + score);
//					return score;
				} else if(docid > doc){
//					logger.fatal("program logic errors");
					System.exit(0);
				}
				else{
					for (WeightingModel wmodel : wm)
						if (wmodel.ACCEPT_UNSEEN)
							score += wmodel.scoreUnseen(docLen);
//					logger.info("_id:" +  docid + ", " + score);
				}
//				return score;
			}else{
				for (WeightingModel wmodel : wm)
					if (wmodel.ACCEPT_UNSEEN)
						score += wmodel.scoreUnseen(docLen);
			}
			return score;
		}
	}

	static class PostingQueue extends PriorityQueue {
		TIntHashSet set = new TIntHashSet();

		PostingQueue(int size) {
			initialize(size);
		}

		protected final boolean lessThan(Object a, Object b) {
			Integer hitA = (Integer) a;
			Integer hitB = (Integer) b;
			return hitA.intValue() < hitB.intValue();
		}

		public Object insertWithOverflow(Object element) {
			int value = ((Integer) element).intValue();
			if (set.contains(value)) {
				return null;
			} else {
				set.add(value);
				return super.insertWithOverflow(element);
			}
		}

		public final Object pop() {
			Object obj = super.pop();
			int value = ((Integer) obj).intValue();
			set.remove(value);
			return obj;
		}
	}

	public static void main(String args[]) {
		PostingQueue pq = new PostingQueue(2);
		pq.insert(-1);
		pq.insert(2);
		pq.insert(3);
		pq.insert(6);
		System.out.println("" + pq.top() + " , " + pq.size());
	}

}
