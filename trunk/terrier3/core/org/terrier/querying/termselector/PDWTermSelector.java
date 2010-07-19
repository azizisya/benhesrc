package org.terrier.querying.termselector;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.structures.ExpansionTerm;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;

/**
 * This class implement an abstract of per-document-weighting term selectors.
 * @author Ben He (ben.he.09@gmail.com)
 *
 */

public abstract class PDWTermSelector extends TermSelector{

	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	public PDWTermSelector(){
		super();
	}
	
	public PDWTermSelector(Index index) {
		super(index);
	}

	@Override
	public void assignTermWeights(ResultSet resultSet, int feedbackSetSize, QueryExpansionModel QEModel, Lexicon bgLexicon){
		int[] docids = resultSet.getDocids();
		feedbackSetSize = Math.min(feedbackSetSize, docids.length);
		assignTermWeights(Arrays.copyOf(docids, feedbackSetSize), QEModel, bgLexicon);
	}
	
	abstract public void assignTermWeights(TIntIntHashMap[] termidFreqMaps, QueryExpansionModel QEModel, 
			TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap);
	
	abstract public void mergeWithQuery(QueryExpansionModel QEModel, MatchingQueryTerms query, int numberOfExpansionTerms);

	@Override
	public void assignTermWeights(int[] docids, QueryExpansionModel QEModel, Lexicon bgLexicon) {
		int effDocuments = docids.length;
		
		TIntIntHashMap[] termidFreqMaps = new TIntIntHashMap[effDocuments];
		TIntIntHashMap bgTermidFreqMap = new TIntIntHashMap();
		TIntIntHashMap bgTermidDocfreqMap = new TIntIntHashMap();
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<effDocuments; i++){
			termidFreqMaps[i] = this.extractTerms(docids[i]);
			termidSet.addAll(termidFreqMaps[i].keys());
		}
		
		int[] termids = termidSet.toArray();
		termidSet.clear(); termidSet = null;
		
		for (int termid : termids){
			LexiconEntry lexEntry = (LexiconEntry)bgLexicon.getLexiconEntry(termid).getValue();
			if (lexEntry!=null){
				bgTermidFreqMap.put(termid, lexEntry.getFrequency());
				bgTermidDocfreqMap.put(termid, lexEntry.getDocumentFrequency());
			}
		}
		
		this.assignTermWeights(termidFreqMaps, QEModel, bgTermidFreqMap, bgTermidDocfreqMap);
		for (int i=0; i<effDocuments; i++){
			termidFreqMaps[i].clear(); termidFreqMaps[i] = null;
		}
		bgTermidFreqMap.clear(); bgTermidFreqMap = null; 
		bgTermidDocfreqMap.clear(); bgTermidDocfreqMap = null;
		termids = null;
	}

}
