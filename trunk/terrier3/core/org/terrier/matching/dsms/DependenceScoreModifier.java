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
 * The Original Code is DependenceScoreModifier.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   Jie Peng <pj{a.}dcs.gla.ac.uk>
 */
package org.terrier.matching.dsms;

import java.io.IOException;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.sorting.MultiSort;
import org.terrier.structures.BitIndexPointer;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.postings.BlockPosting;
import org.terrier.structures.postings.IterablePosting;
import org.terrier.structures.postings.Posting;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Distance;

/** Base class for Dependence models. Document scores are modified using n-grams,
 * approximating the dependence of terms between documents. Implemented as a document 
 * score modifier, similarly to PhraseScoreModifier. Postings lists are traversed in a 
 * DAAT fashion.
 * <p>
 * <b>Properties</b>
 * <ul>
 * <li><tt>proximity.dependency.type</tt> - one of SD, FD for sequential
 * dependence or full dependence</li>
 * <li><tt>proximity.ngram.length</tt> - proxmity windows for FD, in tokens</li>
 * <li><tt>proximity.w_t</tt> - weight of unigram in combination, defaults 1.0d</li>
 * <li><tt>proximity.w_o</tt> - weight of SD in combination, default 1.0d</li>
 * <li><tt>proximity.w_u</tt> - weight of FD in combination, default 1.0d</li>
 * <li><tt>proximity.qtw.fnid</tt> - combination function to combine the qtws of
 * two terms involved in a phrase. See below.</li>
 * </ul>
 * <p>
 * <b>QTW Combination Functions</b>
 * <ol>
 * <li><tt>1</tt>: phraseQTW = 0.5 * (qtw1 + qtw2)</li>
 * <li><tt>2</tt>: phraseQTW = qtw1 * qtw2</li>
 * <li><tt>3</tt>: phraseQTW = min(qtw1, qtw2)</li>
 * <li><tt>4</tt>: phraseQTW = max(qtw1, qtw2)</li>
 * </ol>
 * 
 * @author Craig Macdonald, Vassilis Plachouras, Jie Peng
 * @since 3.0
 */
public abstract class DependenceScoreModifier  implements DocumentScoreModifier {

	public Object clone() {
		try{
			return super.clone();
		} catch (Exception e) {
			return null;
		}
	}
	
	/** The size of the considered ngrams */
	protected int ngramLength = Integer.parseInt(ApplicationSetup.getProperty(
				"proximity.ngram.length", "2"));

	protected abstract double scoreFDSD(int matchingNGrams, int docLength);

	/** type of proximity to use */
	protected String dependency = ApplicationSetup.getProperty(
				"proximity.dependency.type", "");
	protected final int phraseQTWfnid = Integer.parseInt(ApplicationSetup
				.getProperty("proximity.qtw.fnid", "1"));
	/** weight of unigram model */
	protected double w_t = Double.parseDouble(ApplicationSetup.getProperty(
				"proximity.w_t", "1.0d"));
	/** weight of ordered dependence model */
	protected double w_o = Double.parseDouble(ApplicationSetup.getProperty(
				"proximity.w_o", "1.0d"));
	/** weight of unordered dependence model */
	protected double w_u = Double.parseDouble(ApplicationSetup.getProperty(
				"proximity.w_u", "1.0d"));
	/** A list of the strings of the phrase terms. */
	protected String[] phraseTerms;
	protected double avgDocLen = 0.0d;
	protected double numTokens;

	/**
	 * Returns the name of the modifier. 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return this.getClass().getSimpleName();
	}

	protected static boolean NOR(final boolean[] in) {
		for(boolean b : in)
		{
			if (b)
				return false;
		}
		return true;
	}

	/**
	 * Modifies the scores of documents, in which there exist, or there does not
	 * exist a given phrase.
	 * 
	 * @param index
	 *            Index the data structures to use.
	 * @param terms
	 *            MatchingQueryTerms the terms to be matched for the query. This
	 *            does not correspond to the phrase terms necessarily, but to
	 *            all the terms of the query.
	 * @param set
	 *            ResultSet the result set for the query.
	 * @return true if any scores have been altered
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms terms, ResultSet set) {
		try {
			if (phraseQTWfnid < 1 || phraseQTWfnid > 4) {
				System.err
				.println("ERROR: Wrong function id specified for ProximityScoreModifierTREC2009");
			}
	
			phraseTerms = terms.getTerms();
			
			final int phraseLength = phraseTerms.length;
			if (phraseLength == 1)
				return false;
			
			final double[] phraseTermWeights = new double[phraseLength];
			for (int i = 0; i < phraseLength; i++) {
				phraseTermWeights[i] = terms.getTermWeight(phraseTerms[i]);
				System.err.println("phrase term: " + phraseTerms[i]);
			}
	
			
			w_t = Double.parseDouble(ApplicationSetup.getProperty(
					"proximity.w_t", "1.0d"));
			w_o = Double.parseDouble(ApplicationSetup.getProperty(
					"proximity.w_o", "1.0d"));
			w_u = Double.parseDouble(ApplicationSetup.getProperty(
					"proximity.w_u", "1.0d"));
	
			if (dependency.equals("FD")) {
				doDependency(index, set, phraseTermWeights, false);
			} else if (dependency.equals("SD")) {
				doDependency(index, set, phraseTermWeights, true);
			} else {
				System.err.println("WARNING: proximity.dependency.type not set. Set it to either FD or SD");
				return false;
			}
		} catch (Exception e) {
			System.err.println("Error in " + this.getClass().getName() + " "
					+ e);
			e.printStackTrace();
		}
		// returning true, assuming that we have modified the scores of
		// documents
		return true;
	}

	protected void doDependency(Index index, ResultSet rs, final double[] phraseTermWeights, boolean SD) throws IOException {
				final Lexicon<String> lexicon = index.getLexicon();
				final LexiconEntry les[] = new LexiconEntry[phraseTerms.length];
				final IterablePosting ips[] = new IterablePosting[phraseTerms.length];
				final boolean[] postingListFinished = new boolean[phraseTerms.length];
			
				this.setCollectionStatistics(index.getCollectionStatistics(), index);
			
				for (int i = 0; i < phraseTerms.length; i++) {
					les[i] = lexicon.getLexiconEntry(phraseTerms[i]);
					if (les[i] != null) {
						ips[i] = index.getInvertedIndex().getPostings(
								(BitIndexPointer) les[i]);
						postingListFinished[i] = ips[i].next() == IterablePosting.EOL;
					}
				}
			
				final int[] docids = rs.getDocids();
				final double[] scores = rs.getScores();
				final short[] occurrences = rs.getOccurrences();
			
				// Sort by docid so that term postings can be read sequentially (ip.next())
				MultiSort.ascendingHeapSort(docids, scores, occurrences, docids.length);
			
				
				// firstly, apply w_t to all document scores
				final int docidsLength = docids.length;
				for (int i = 0; i < docidsLength; i++) {
					scores[i] = w_t * scores[i];
				}
			
				// for each retrieved document
				DOC: for (int k = 0; k < docidsLength; k++) {
					// update the posting iterators to be in the correct place
					int i = -1;
					int targetDocId = docids[k];
					if (scores[k] <= 0.0d)
						continue DOC;
					//System.err.print("docid=" + targetDocId);
					
					// ok to use is set for each term when that term has a posting for
					// the current docid
					boolean[] okToUse = new boolean[phraseTerms.length];
					TERM: for (IterablePosting ip : ips)
					{
						i++;
						if (postingListFinished[i]) {
							okToUse[i] = false;
							continue TERM;
						}
						okToUse[i] = true;
						if (ip == null) {
							okToUse[i] = false;
							continue TERM;
						}
						
						while (ip.getId() < targetDocId) {
						//do {
							if (! (ip.next() != IterablePosting.EOL)) {
								okToUse[i] = false;
								postingListFinished[i] = true;
								continue TERM;
							}
						} //while (ip.getId() < targetDocId);
						if (ip.getId() > targetDocId) {
							// this term doesnt have it.
							okToUse[i] = false;
							continue TERM;
						}
						okToUse[i] = true;
					}
			
					if (countTrue(okToUse) < 2)
					{
						//this document will not be considered, as it has no pair of query terms present
						continue DOC;
					}
					
					// ok, all postings which have okToUse set to true, can be used in
					// prox calculation
					if (SD) {
						TERM: for (i = 0; i < phraseTerms.length - 1; i++) {
							if (!okToUse[i] || !okToUse[i + 1])
								continue TERM;
							double combinedPhraseQTWWeight;
							switch (phraseQTWfnid) {
							case 1:
								combinedPhraseQTWWeight = 0.5 * phraseTermWeights[i]
								                                                  + 0.5 * phraseTermWeights[i + 1];
								break;
							case 2:
								combinedPhraseQTWWeight = phraseTermWeights[i]
								                                            * phraseTermWeights[i + 1];
								break;
							case 3:
								combinedPhraseQTWWeight = Math.min(
										phraseTermWeights[i], phraseTermWeights[i + 1]);
								break;
							case 4:
								combinedPhraseQTWWeight = Math.max(
										phraseTermWeights[i], phraseTermWeights[i + 1]);
								break;
							default:
								combinedPhraseQTWWeight = 1.0d;
							}
							double s = scoreFDSD(SD, ips[i], ips[i + 1],
									avgDocLen);
							scores[k] += combinedPhraseQTWWeight * w_o * s;
						}
					} else {
						for (i = 0; i < phraseTerms.length - 1; i++) {
							INNERTERM: for (int j = i + 1; j < phraseTerms.length; j++) {
								if (!okToUse[i] || !okToUse[j])
									continue INNERTERM;
								double combinedPhraseQTWWeight;
								switch (phraseQTWfnid) {
								case 1:
									combinedPhraseQTWWeight = 0.5
									* phraseTermWeights[i] + 0.5
									* phraseTermWeights[j];
									break;
								case 2:
									combinedPhraseQTWWeight = phraseTermWeights[i]
									                                            * phraseTermWeights[j];
									break;
								case 3:
									combinedPhraseQTWWeight = Math.min(
											phraseTermWeights[i], phraseTermWeights[j]);
									break;
								case 4:
									combinedPhraseQTWWeight = Math.max(
											phraseTermWeights[i], phraseTermWeights[j]);
									break;
								default:
									combinedPhraseQTWWeight = 1.0d;
								}
			
								double s = scoreFDSD(SD, ips[i], ips[j], avgDocLen);
								scores[k] += w_u * combinedPhraseQTWWeight * s;
							}
						}
					}
				}
			
				for (IterablePosting ip : ips) {
					if (ip != null)
						ip.close();
				}
			
			}

	protected static int countTrue(final boolean[] in) {
		int count = 0;
		for (boolean b : in)
		{
			if (b) count++;
		}
		return count;
	}

	public DependenceScoreModifier() {
		super();
	}

	public void setCollectionStatistics(CollectionStatistics cs, Index _index) {
		numTokens = (double)cs.getNumberOfTokens();
		long numDocs = (long) (cs.getNumberOfDocuments());
		avgDocLen = ((double) (numTokens - numDocs
				* (ngramLength - 1)))
				/ (double) numDocs;
		
	}

	public double score(Posting[] postings) {
		double score = 0;
		boolean SD = true;
		double avgDocLen = 0.0d;
		if (SD)
		{
			for(int i=0;i<postings.length-1;i++)
			{
				score += scoreFDSD(SD, postings[i], postings[i+1], avgDocLen);
			}
		}
		return w_o * score;
	}

	/**
	 * how likely is it that these two postings have so many near-occurrences,
	 * given the length of this document
	 */
	private double scoreFDSD(boolean SD, final Posting ip1, final Posting ip2, final double avgDocLen) {
			
				final int[] blocks1 = ((BlockPosting) ip1).getPositions();
				final int[] blocks2 = ((BlockPosting) ip2).getPositions();
				int docLength = ip1.getDocumentLength();
			
				final int matchingNGrams = SD ? Distance.noTimesSameOrder(blocks1, 0,
						blocks1.length, blocks2, 0, blocks2.length, ngramLength,
						docLength) : Distance.noTimes(blocks1, 0, blocks1.length,
								blocks2, 0, blocks2.length, ngramLength, docLength);
				return scoreFDSD(matchingNGrams, docLength);
			}

}
