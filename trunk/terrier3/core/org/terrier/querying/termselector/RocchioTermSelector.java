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

import uk.ac.gla.terrier.statistics.ScoreNormaliser;
import uk.ac.gla.terrier.statistics.Statistics;

public class RocchioTermSelector extends TermSelector{

	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
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
	
	protected void assignWeights(TIntIntHashMap[] termidFreqMaps, QueryExpansionModel QEModel, 
			TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap, 
			TIntIntHashMap termidDFMap, int[] termids,
			TIntObjectHashMap<ExpansionTerm>[] expTermMaps){
		int effDocuments = termidFreqMaps.length;
		QEModel.setBackgroundStatistics(index.getCollectionStatistics());
		
		int[] docLength = new int[effDocuments];
		
		// for each of the top-ranked documents
		for (int i=0; i<effDocuments; i++){
			docLength[i] = Statistics.sum(termidFreqMaps[i].getValues());
		}
		for (int i=0; i<effDocuments; i++){
			// assign weights for each term in each document
			for (int termid : termids){
				if (termidFreqMaps[i].containsKey(termid)){
					// seen term
					double score = QEModel.score( termidFreqMaps[i].get(termid),
							docLength[i], 
							bgTermidDocfreqMap.get(termid), 
							bgTermidFreqMap.get(termid), 
							1d);
					ExpansionTerm term = new ExpansionTerm(termid, termidFreqMaps[i].get(termid));
					term.setWeightExpansion(score);
					expTermMaps[i].put(termid, term);
					termidDFMap.adjustOrPutValue(termid, 1, 1);
				}
			}
		}
	}
	
	public void assignTermWeights(TIntIntHashMap[] termidFreqMaps, QueryExpansionModel QEModel, 
			TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap){
		int effDocuments = termidFreqMaps.length;
		
		TIntIntHashMap termidDFMap = new TIntIntHashMap();
		
		TIntObjectHashMap<ExpansionTerm>[] expTermMaps = new TIntObjectHashMap[effDocuments];
		for (int i=0; i<effDocuments; i++){
			expTermMaps[i] = new TIntObjectHashMap<ExpansionTerm>();
		}
		TIntHashSet termidSet = new TIntHashSet();
		
		// for each of the top-ranked documents
		for (int i=0; i<effDocuments; i++){
			termidSet.addAll(termidFreqMaps[i].keys());
		}
		
		int [] termids = termidSet.toArray();
		Arrays.sort(termids);
		this.assignWeights(termidFreqMaps, QEModel, bgTermidFreqMap, bgTermidDocfreqMap, termidDFMap, termids, expTermMaps);
		termidSet.clear(); termidSet = null;
		
		// merge expansion terms: compute mean term weight for each term, sort again
		// Unlike Rocchio's term selector, RM3 considers unseen terms
		TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
		// TIntIntHashMap termidDFMap = new TIntIntHashMap();
		
		
		// int querylength = this.originalQueryTermidSet.size();
		// expansion term should appear in at least half of the feedback documents
		// int minDF = (effDocuments%2==0)?(effDocuments/2):(effDocuments/2+1);
		int minDF = 2;
		
		for (int termid : termids){
			if (effDocuments>minDF && termidDFMap.get(termid) < minDF)
				termidWeightMap.put(termid, 0d);
			else{
				double score = 0d;
				for (int i=0; i<effDocuments; i++){
					if (expTermMaps[i].containsKey(termid))
						score += expTermMaps[i].get(termid).getWeightExpansion();
				}
				termidWeightMap.put(termid, score);
			}
		}
		
		ExpansionTerm[] candidateTerms = new ExpansionTerm[termidWeightMap.size()];
		int counter = 0;
		for (int termid : termidWeightMap.keys()){
			candidateTerms[counter] = new ExpansionTerm(termid);
			candidateTerms[counter].setWeightExpansion(termidWeightMap.get(termid)/effDocuments);
			counter++;
		}
		Arrays.sort(candidateTerms);
		
		termMap = new TIntObjectHashMap<ExpansionTerm>();
		double normaliser = candidateTerms[0].getWeightExpansion();
		
		// normalise the expansion weights by the maximum weight among the expansion terms
		for (ExpansionTerm term : candidateTerms){			
			term.setWeightExpansion(term.getWeightExpansion()/normaliser);
			termMap.put(term.getTermID(), term);
		}
		// release memories
		for (int i=0; i<effDocuments; i++){
			expTermMaps[i].clear(); expTermMaps[i] = null;
		}
		termidDFMap.clear(); termidDFMap = null;
	}
	
	public void mergeWithQuery(QueryExpansionModel QEModel, MatchingQueryTerms query, int numberOfExpansionTerms){
		ExpansionTerm[] expTerms = getMostWeightedTerms(numberOfExpansionTerms);
		for (int i = 0; i < expTerms.length; i++){
			if (expTerms[i].getWeightExpansion()<=0)
				break;
			Entry<String, LexiconEntry> entry = lexicon.getLexiconEntry(expTerms[i].getTermID());
			double finalWeight = QEModel.ROCCHIO_ALPHA*query.getTermWeight(entry.getKey())+QEModel.ROCCHIO_BETA*expTerms[i].getWeightExpansion();
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
