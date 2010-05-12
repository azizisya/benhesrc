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
import org.terrier.utility.ApplicationSetup;

public class RocchioTermSelector extends TermSelector{

	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	protected int EXPANSION_MIN_DOCUMENTS;
	
	public RocchioTermSelector(){
		super();
	}
	
	public RocchioTermSelector(Index index) {
		super(index);
	}

	@Override
	public void assignTermWeights(ResultSet resultSet, int feedbackSetSize, QueryExpansionModel QEModel, Lexicon bgLexicon){
		int[] docids = resultSet.getDocids();
		feedbackSetSize = Math.min(feedbackSetSize, docids.length);
		assignTermWeights(Arrays.copyOf(docids, feedbackSetSize), QEModel, bgLexicon);
	}
	
	public void assignTermWeights(TIntIntHashMap[] termidFreqMaps, QueryExpansionModel QEModel, 
			TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap){
		int effDocuments = termidFreqMaps.length;
		TermSelector selector = TermSelector.getTermSelector("DFRTermSelector", index);
		selector.setOriginalQueryTermids(originalQueryTermidSet.toArray());
		selector.setMetaInfo("normalize.weights", "false");
		ExpansionTerm[][] expTerms = new ExpansionTerm[effDocuments][];
		
		// for each of the top-ranked documents
		for (int i=0; i<effDocuments; i++){
			// obtain the weighted terms
			TIntIntHashMap[] oneMap = {termidFreqMaps[i]};
			selector.assignTermWeights(oneMap, QEModel, bgTermidFreqMap, bgTermidDocfreqMap);
			expTerms[i] = selector.getMostWeightedTerms(selector.getNumberOfUniqueTerms());
		}
		// merge expansion terms: compute mean term weight for each term, sort again
		TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
		TIntIntHashMap termidDFMap = new TIntIntHashMap();
		for (int i=0; i<effDocuments; i++){
			for (int j=0; j<expTerms[i].length; j++){
				termidWeightMap.adjustOrPutValue(expTerms[i][j].getTermID(), expTerms[i][j].getWeightExpansion(), 
						expTerms[i][j].getWeightExpansion());
				termidDFMap.adjustOrPutValue(expTerms[i][j].getTermID(), 1, 1);
			}
		}
		
		ExpansionTerm[] candidateTerms = new ExpansionTerm[termidWeightMap.size()];
		// expansion term should appear in at least half of the feedback documents
		//int minDF = (effDocuments%2==0)?(effDocuments/2):(effDocuments/2+1);
		int minDF = 2;
		int counter = 0;
		for (int termid : termidWeightMap.keys()){
			candidateTerms[counter] = new ExpansionTerm(termid);
			if (effDocuments>minDF&&termidDFMap.get(termid)<minDF)
				candidateTerms[counter].setWeightExpansion(0d);
			else
				candidateTerms[counter].setWeightExpansion(termidWeightMap.get(termid)/termidDFMap.get(termid));
			counter++;
		}
		Arrays.sort(candidateTerms);
		
		termMap = new TIntObjectHashMap<ExpansionTerm>();
		
		// normalise the expansion weights by the maximum weight among the expansion terms
		double normaliser = candidateTerms[0].getWeightExpansion();
		for (ExpansionTerm term : candidateTerms){			
			term.setWeightExpansion(term.getWeightExpansion()/normaliser);
			termMap.put(term.getTermID(), term);
		}
	}
	
	public void mergeWithQuery(QueryExpansionModel QEModel, MatchingQueryTerms query, int numberOfExpansionTerms){
		ExpansionTerm[] expTerms = getMostWeightedTerms(numberOfExpansionTerms);
		for (int i = 0; i < expTerms.length; i++){
			if (expTerms[i].getWeightExpansion()<=0)
				break;
			Entry<String, LexiconEntry> entry = lexicon.getLexiconEntry(expTerms[i].getTermID());
			double finalWeight = (QEModel.PARAMETER_FREE&&QEModel.SUPPORT_PARAMETER_FREE_QE)?
				(QEModel.ROCCHIO_ALPHA*query.getTermWeight(entry.getKey())+expTerms[i].getWeightExpansion()):
					(QEModel.ROCCHIO_ALPHA*query.getTermWeight(entry.getKey())+QEModel.ROCCHIO_BETA*expTerms[i].getWeightExpansion());
			query.setTermProperty(entry.getKey(), finalWeight);
			/**
			if(logger.isDebugEnabled()){
				logger.debug("term " + entry.getKey()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(entry.getKey()), 4));
			}*/
		}
	}

	@Override
	public void assignTermWeights(int[] docids, QueryExpansionModel QEModel, Lexicon bgLexicon) {
		int effDocuments = docids.length;
		TermSelector selector = TermSelector.getTermSelector("DFRTermSelector", index);
		selector.setMetaInfo("normalize.weights", "false");
		ExpansionTerm[][] expTerms = new ExpansionTerm[effDocuments][];
		
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
	}

}
