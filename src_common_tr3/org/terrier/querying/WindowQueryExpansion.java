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
 * The Original Code is WindowQueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben.he.09@gmail.com> (original author)
 */
package org.terrier.querying;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.UnitScorer;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.querying.termselector.TermSelector;
import org.terrier.structures.BlockIndexDocument;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.ExpansionTerm;
import org.terrier.structures.Index;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.TextWindow;
import org.terrier.utility.ApplicationSetup;


/**
 * Implements automatic window-based query expansion as PostFilter that is applied to the resultset
 * after 1st-time matching.
 * <B>Controls</B>
 * <ul><li><tt>qemodel</tt> : The query expansion model used for Query Expansion. 
 * Defauls to Bo1.</li></ul>
 * <B>Properties</B>
 * <ul><li><tt>expansion.terms</tt> : The maximum number of most weighted terms in the 
 * pseudo relevance set to be added to the original query. The system performs a conservative
 * query expansion if this property is set to 0. A conservation query expansion only reweighs
 * the original query terms without adding new terms to the query.</li>
 * <li><tt>expansion.documents</tt> : The number of top documents from the 1st pass 
 * retrieval to use for QE. The query is expanded from this set of docuemnts, also 
 * known as the pseudo relevance set.</li>
 * </ul>
 * @version $Revision: 1.1 $
 * @author Ben He
 */
public class WindowQueryExpansion extends QueryExpansion {
	protected static Logger logger = Logger.getRootLogger();
	
	/**
	* The default constructor of QueryExpansion.
	*/
	public WindowQueryExpansion() {super();}
	
	public ExpansionTerm[] expandFromDocuments(
			int[] docIDs, 
			MatchingQueryTerms query, 
			int numberOfTermsToReweight,
			Index index,
			QueryExpansionModel QEModel,
			TermSelector selector){
		
		if (query!=null)
			selector.setOriginalQueryTerms(query.getTerms());
		
		int wSize = Integer.parseInt(ApplicationSetup.getProperty("text.window.size", "100"));
		/** Number of top-ranked text windows used for relevance feedback. */
		
		
		
		/**
		 * Get text windows. Assign weights to each window wrt. the original query, 
		 * and expand the query from the top-weighted windows.
		 */
		
		// TextWindow[][] windows = new TextWindow[docIDs.length][];
		ArrayList<TextWindow> windowList = new ArrayList<TextWindow>(); 
		double overlap = Double.parseDouble(ApplicationSetup.getProperty("text.window.overlap", "0.5d"));
		for (int i=0; i<docIDs.length; i++){
			TextWindow[] windows = BlockIndexDocument.segmentDocumentIntoOverlappedWindows(index, docIDs[i], wSize, overlap);
			for (TextWindow window : windows)
				windowList.add(window);
		}
		// get background term frequency and document frequencies
		int numberOfWindows = windowList.size();
		TextWindow[] windows = (TextWindow[])windowList.toArray(new TextWindow[windowList.size()]);
		logger.debug(docIDs.length+" feedback documents segmented into "+windows.length+" text windows.");
		int numberOfTopWindows = Math.max(1, (int)(Double.parseDouble(ApplicationSetup.getProperty("text.window.topx", "0.60d"))*windows.length));
		/*Integer.parseInt(ApplicationSetup.getProperty("text.window.topx", "10"));*/
		windowList.clear(); windowList = null;
		TIntIntHashMap[] termidFreqMaps = new TIntIntHashMap[numberOfWindows];
		int[] unitLength = new int[numberOfWindows];
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<numberOfWindows; i++){
			termidFreqMaps[i] = windows[i].termidFreqMap;
			termidSet.addAll(termidFreqMaps[i].keys());
			unitLength[i] = windows[i].getWindowSize();
		}
		TIntIntHashMap termidTFMap = new TIntIntHashMap();
		TIntIntHashMap termidNtMap = new TIntIntHashMap();
		Lexicon lexicon = index.getLexicon();
		for (int termid : termidSet.toArray()){
			LexiconEntry entry = (LexiconEntry)lexicon.getLexiconEntry(termid).getValue();
			if (entry!=null){
				termidTFMap.put(termid, entry.getFrequency());
				termidNtMap.put(termid, entry.getDocumentFrequency());
			}
		}
		
		// get info of original query terms
		TIntDoubleHashMap termidQtwMap = new TIntDoubleHashMap();
		String[] terms = query.getTerms();
		for (String term : terms){
			int termid = lexicon.getLexiconEntry(term).getTermId();
			termidQtwMap.put(termid, query.getTermWeight(term));
		}
			
		
		// assign weights to windows for original queries
		double[] scores = new double[numberOfWindows];
		UnitScorer.score(termidFreqMaps, unitLength, termidTFMap, termidNtMap, termidQtwMap, QEModel, scores);
		
		for (int i=0; i<numberOfWindows; i++)
			windows[i].setWindowScore(scores[i]);
		scores = null;
		Arrays.sort(windows);
		
		if (numberOfTopWindows >= windows.length)
			logger.warn("Too few text windows.");
		numberOfTopWindows = Math.min(numberOfTopWindows, windows.length);
		
		// get top windows
		TextWindow[] topWindows = new TextWindow[numberOfTopWindows];
		
		// merge top-ranked documents in the same document and expanded from
		// documents represented by top-ranked windows.
		for (int i=0; i<numberOfTopWindows; i++){
			topWindows[i] = windows[i];
		}
		TIntIntHashMap[] topTermidFreqMaps = TextWindow.mergeTextWindowFreq(topWindows);
		
		/**
		 * Expand from top ranked windows.
		TIntIntHashMap[] topTermidFreqMaps = new TIntIntHashMap[numberOfTopWindows];
		for (int i=0; i<numberOfTopWindows; i++){
			topWindows[i] = windows[i];
			topTermidFreqMaps[i] = termidFreqMaps[i];
		}*/
		// expand from top-ranked windows
		selector.assignTermWeights(topTermidFreqMaps, QEModel, termidTFMap, termidNtMap);
		
		/**selector.assignTermWeights(docIDs, QEModel, index.getLexicon());
		
		for (int i=0; i<docIDs.length; i++)
			logger.debug("doc "+(i+1)+": "+docIDs[i]);*/
		logger.debug("Number of unique terms in the feedback set: "+selector.getNumberOfUniqueTerms());
		TIntObjectHashMap<ExpansionTerm> queryTerms = selector.getMostWeightedTermsInHashMap(numberOfTermsToReweight);
		
		ExpansionTerm[] expTerms = new ExpansionTerm[queryTerms.size()];
		int counter = 0;
		for (int i : queryTerms.keys())
			expTerms[counter++] = queryTerms.get(i);
		Arrays.sort(expTerms);
		termidTFMap.clear(); termidTFMap = null;
		termidNtMap.clear(); termidNtMap = null;
		termidQtwMap.clear(); termidQtwMap = null;
		termidFreqMaps = null; windows = null;
		topWindows = null; topTermidFreqMaps = null;
		return expTerms;
	}
	
}
