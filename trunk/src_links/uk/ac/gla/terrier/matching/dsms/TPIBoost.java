/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is BlockScoreModifier.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching.dsms;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Iterator;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class modifers the scores of documents based on
 * the position of the query terms in the document. Heavily based on code
 * originally by Douglas Johnson and Vassilis Plachouras, Craig and Vassillis
 * later ripped out all Okapi related code and renamed the class TPIBoost.
 * 
 * This class implements the DocumentScoreModifier interface.
 * <b>Properties:</b><br/>
 * <ul>
 *  <li><tt>tpi.debug</tt> - display more debugging statements. Default value false.</li>
 *  <li><tt>tpi.omega</tt> - . Default value 1.0</li>
 *  <li><tt>tpi.kappa</tt> - . Default value 1.0</li>
 *  <li><tt>tpi.topdocs</tt> - the number of top-ranked documents to examine. Default value 100</li>
 * </ul>
 * @author Douglas Johnson, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $.
 */
public class TPIBoost implements DocumentScoreModifier {
	
	/**
	 * Controls the printing of debug messages. 
	 * It is set equal to false by default and
	 * it can be modified with the property
	 * tpi.debug. The possible values are either true 
	 * or false. 
	 */
	protected static final boolean debug = (new Boolean(ApplicationSetup.getProperty("tpi.debug", "false"))).booleanValue();
	
	/**
	 * the parameter omega for the formula
	 * score := score + omega + kappa / (kappa + tpi)
	 * It is updated with the property
	 * tpi.omega. The default value for 
	 * the parameter is 1.0.
	 */
	protected double omega = 1.0d;

	/**
	 * the parameter kappa for the formula
	 * score := score + omega + kappa / (kappa + tpi)
	 * It is updated with the property
	 * tpi.kappa. The default value for 
	 * the parameter is 1.0.
	 */
	protected double kappa = 1.0d;

	/** 
	 * The number of top ranked documents to process. 
	 * It is updated with the property tpi.topdocs. The
	 * default value for the parameter is 100.
	 */
	protected int topDocs = 100;
	
	/** The block lexicon to use.*/
	protected Lexicon lexicon;
	
	/** The block inverted index to use.*/
	protected InvertedIndex invertedIndex;

	/** Default constructor for this class. Just calls does a call to initialise to ensure
	  * the the objects getName() method returns the correct parameters */
	public TPIBoost()
	{
		initialise();
	}

	/** Sets the parameters from this module */
	protected void initialise()
	{
		omega = Double.parseDouble(ApplicationSetup.getProperty("tpi.omega","1.0"));
		kappa = Double.parseDouble(ApplicationSetup.getProperty("tpi.kappa","1.0"));
		topDocs = Integer.parseInt(ApplicationSetup.getProperty("tpi.topdocs","100"));
	}
	
	/** Returns the name of this class, customised with the current parameter values. */
	public String getName() {
		return "TPIBoost_o"+omega+"_k"+kappa+"_td"+topDocs;
	}
	
	/**
	 * Modifies scores by applying proximity weighting.
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms query, ResultSet resultSet) {
		// The rest of the method applies proximity weighting as outlined
		// by Yves Rasolofo for queries of 1 < length < 5.
		if (query.length() == 1 || query.length() > 5) 
			return false;
		try {
			initialise();
			
			this.lexicon = index.getLexicon();
			this.invertedIndex = index.getInvertedIndex();
			
			final int[] docids = resultSet.getDocids();
			double[] scores = resultSet.getScores();
			
			//check when the application of proximity started.
			long proximityStart = System.currentTimeMillis();
			
			// the constants used by the algorithm
			final int blockSize = ApplicationSetup.BLOCK_SIZE;
	
			// an array holding the proximity weight for each docid
			// corresponds to the scores array
			double[] TPRSV = new double[scores.length];
	
			TIntIntHashMap docidToTPSRV = new TIntIntHashMap(scores.length);
			int docidToTPSRVCounter = 0;
			
			//arrays to reference the first terms block information
			int[][] term1Pointers;
			int[] term1blockfreqs;
			int[] term1blockids;
			int[] term1docids;
			
			//term2Pointers holds the information for the second term of each pair
			//each of the other arrays are used to reduce the number of references
			int[][] term2Pointers;
			int[] term2docids;
			int[] term2blockfreqs;
			int[] term2blockids;
	
			//prepare caching hashmaps for the posting lists.
			//these hashmaps need to be cleared afterwards to save memory
			//the hashmaps will store a mapping from (termCode+1) to the 
			//postings array
			TIntObjectHashMap postingsCache = new TIntObjectHashMap();
			
			// calculate all the possible combinations of query term pairs
			final ArrayList queryTermPairs = generateQueryTermPairs(query);
			
			Iterator termPairIterator = queryTermPairs.iterator();
			// for all term pairs
			while (termPairIterator.hasNext()) {
				
				ArrayList queryTermPair = (ArrayList) termPairIterator.next();
				Iterator termIterator = queryTermPair.iterator();
				String term1 = (String) termIterator.next();
				String term2 = (String) termIterator.next();
				if (debug) {
					System.out.println("got next term pair");
					System.out.println("term 1 is: " + term1);
					System.out.println("term 2 is: " + term2);
				}
				
				//we seek the query term in the lexicon
				boolean found = (postingsCache.contains(query.getTermCode(term1)+1) || lexicon.findTerm(term1));
				//and if it is not found, we continue with the next term pair
				if (!found)
					continue;
	
				//we seek the query term in the lexicon
				found = (postingsCache.contains(query.getTermCode(term1)+1) || lexicon.findTerm(term2));
				//and if it is not found, we continue with the next term pair
				if (!found)
					continue;
				
				if (debug) 
					System.out.println("found both terms in the lexicon.");
				
				//check the cache first, and otherwise read the inverted index
				if (postingsCache.contains(query.getTermCode(term1)+1)) {
					term1Pointers = (int[][]) postingsCache.get(query.getTermCode(term1)+1);
				} else {
					term1Pointers = invertedIndex.getDocuments(query.getTermCode(term1));
					postingsCache.put(query.getTermCode(term1)+1, term1Pointers);
				}
							
				term1docids = term1Pointers[0];
				term1blockfreqs = term1Pointers[3];
				term1blockids = term1Pointers[4];
	
				//check the cache first, and otherwise read the inverted index
				if (postingsCache.contains(query.getTermCode(term2)+1)) {
					term2Pointers = (int[][]) postingsCache.get(query.getTermCode(term2)+1);
				} else {
					term2Pointers = invertedIndex.getDocuments(query.getTermCode(term2));
					postingsCache.put(query.getTermCode(term2)+1, term2Pointers);
				}
				
				term2docids = term2Pointers[0];
				term2blockfreqs = term2Pointers[3];
				term2blockids = term2Pointers[4];
				
				int length1 = term1docids.length;
				int length2 = term2docids.length;
				// generate a set of docids containing only those which
				// are in the top scores, and contain both term1 and term2
	
				TIntHashSet topdocidSet = new TIntHashSet(topDocs);
				final int DocsToCheck = Math.min(topDocs, docids.length);
				for (int n = 0; n< DocsToCheck;n++) {// = docids.length-1; n > 0 && n > topDocs; n--) {
					topdocidSet.add(docids[n]);
				}
			
				TIntHashSet term1docidSet = new TIntHashSet();
				for (int n = 0; n < term1docids.length; n++) {
					if (topdocidSet.contains(term1docids[n])) {
						term1docidSet.add(term1docids[n]);
					}
				}
				
				TIntHashSet matchingSet = new TIntHashSet();
				for (int n = 0; n < term2docids.length; n++) {
					if (term1docidSet.contains(term2docids[n])) {
						matchingSet.add(term2docids[n]);
					}
				}
				
				int[] matchingDocids = matchingSet.toArray();
				int term1index = 0;
				int term2index = 0;
				int term1blockindex = 0;
				int term2blockindex = 0;
				// for all docids in the matching set
				for (int iter=0; iter<matchingDocids.length; iter++) {
	
					term1index = 0;
					term2index = 0;
					term1blockindex = 0;
					term2blockindex = 0;
	
					
					int docid = matchingDocids[iter];
					int distance;
					
					if (debug)
						System.out.println("docid: " + docid);
					
					double tpi = 0;
					// find the position of this docid
					int term1blockfreq = 0;
	
					while (term1index < length1) {
						if (term1index > 0) term1blockindex += term1blockfreqs[term1index-1];
						term1blockfreq = term1blockfreqs[term1index];
						if (docid == term1docids[term1index])
							break;
						else 
							term1index++;
					}
					
					// find the position of this docid
					int term2blockfreq = 0;
					while (term2index < length2) {
						if (term2index > 0) term2blockindex += term2blockfreqs[term2index-1];
						term2blockfreq = term2blockfreqs[term2index];
						if (docid == term2docids[term2index])
							break;
						else
							term2index++;
					}
					
				
					// for each block containing term1 find the distance to each block
					// containing term2 and if the distance is within maximal distance of 5
					// add this to the tpi score for this document
					if (debug) {
						System.out.println("term1blockindex: " + term1blockindex + ", term1blockfreqs: " + term1blockfreq);
						System.out.println("term2blockindex: " + term2blockindex + ", term2blockfreqs: " + term2blockfreq);
					}
					
					for (int blockidIndex = term1blockindex; blockidIndex < (term1blockindex + term1blockfreq); blockidIndex++) {
						for (int blockidIndex2 = term2blockindex; blockidIndex2 < (term2blockindex + term2blockfreq); blockidIndex2++) {
							
							if (debug)
								System.out.println("blockid1: " + term1blockids[blockidIndex] + ", blockid2: " + term2blockids[blockidIndex2]);
							
							//the computed distance is ok when blockSize is equal to 1
							distance = Math.abs(term1blockids[blockidIndex]-term2blockids[blockidIndex2]);
	
							//can two terms appear in the same block id when blockSize is 1? 
							//V: I would expect that this wouldn't be the case, but it happens
							//C: might this happen if a document exceeds maximum blocks. If not now, then perhaps in future
							if (blockSize == 1 && distance == 0) {
								distance++;
								if (debug)
									System.out.println("block ids of two terms are the same, even if blockSize=1");
							}
							
							//if blockSize is greater than 1 then distance may be zero, so we add 1 and we multiply with blockSize
							if (blockSize > 1) 
								distance = blockSize*(distance+1);
							
							//that's the original code
							//distance = (blockSize * Math.abs(term1blockids[blockidIndex] - term2blockids[blockidIndex2]))+1;
							if (distance < 6) {
								tpi += 1 / Math.pow(distance, 2.0);
								
								if (debug)
									System.out.println("modified tpi to " + tpi);
							}
						}
					}
					if (docidToTPSRV.contains(docid+1))
						TPRSV[docidToTPSRV.get(docid+1)] += omega * tpi / (kappa + tpi);
					else { 
						docidToTPSRV.put(docid+1, docidToTPSRVCounter);
						TPRSV[docidToTPSRVCounter]+=omega * tpi / (kappa + tpi);
						docidToTPSRVCounter++;
					}
					
					if (debug)
						System.out.println("TPRSV["+docidToTPSRV.get(docid+1)+"]= " + TPRSV[docidToTPSRV.get(docid+1)]);
				}
			}
			
			//trying to free memory
			Object[] tmp = postingsCache.getValues();
			for (int i=0; i<tmp.length; i++)
				tmp[i] = null;
			postingsCache.clear();
			
			// for the top documents update their score
			final int DocsToCheck = Math.min(topDocs, docids.length);
			for (int docidIndex = 0; docidIndex < DocsToCheck; docidIndex++) {
				if (!docidToTPSRV.contains(docids[docidIndex]+1))
					continue;
				double oldScore = scores[docidIndex];
				scores[docidIndex] += TPRSV[docidToTPSRV.get(docids[docidIndex]+1)];//TPRSV[docids[docidIndex]];
				if (debug) 
					System.out.println("score of document " + docids[docidIndex] + " at " + docidIndex + " is modified from " + oldScore +" to " + scores[docidIndex]);
			}
			long proximityEnd = System.currentTimeMillis();
			System.err.println("time to apply proximity and resort: " + ((proximityEnd - proximityStart) / 1000.0D));
		} catch(ArrayIndexOutOfBoundsException exc) {
			System.err.println(exc);
			exc.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Generates all possible query term pairs
	 * @param query the query to generate term pairs from
	 */
	protected ArrayList generateQueryTermPairs(MatchingQueryTerms query) {
		String[] terms = query.getTerms();
		final int queryLength = query.length();
		final ArrayList queryTermPairs = new ArrayList(query.length());
		for (int i = 0; i < queryLength - 1; i++) {
			for (int j = i+1; j < queryLength; j++) {
				ArrayList termPair = new ArrayList(2);
				termPair.add(terms[i]);
				termPair.add(terms[j]);
				queryTermPairs.add(termPair);
			}
		}
		return queryTermPairs;
	}

	public Object clone()
	{
		return new TPIBoost();
	}
}
