package org.terrier.querying.termselector;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.structures.ExpansionTerm;
import org.terrier.structures.Index;
import org.terrier.structures.LexiconEntry;

import uk.ac.gla.terrier.statistics.Statistics;

public class RM3TermSelector extends PDWTermSelector{
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	public RM3TermSelector(){
		super();
	}
	
	public RM3TermSelector(Index index) {
		super(index);
	}
	
	@SuppressWarnings("unchecked")
	public void assignTermWeights(TIntIntHashMap[] termidFreqMaps, QueryExpansionModel QEModel, 
			TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap){
		int effDocuments = termidFreqMaps.length;
		QEModel.setBackgroundStatistics(index.getCollectionStatistics());
		/*
		TermSelector selector = TermSelector.getTermSelector("DFRTermSelector", index);
		selector.setOriginalQueryTermids(originalQueryTermidSet.toArray());
		selector.setMetaInfo("normalize.weights", "false");
		
		TIntObjectHashMap<ExpansionTerm>[] expTermMaps = new TIntObjectHashMap[effDocuments];
		*/
		TIntHashSet termidSet = new TIntHashSet();
		
		int[] docLength = new int[effDocuments];
		
		// for each of the top-ranked documents
		for (int i=0; i<effDocuments; i++){
			// obtain the weighted terms
			// TIntIntHashMap[] oneMap = {termidFreqMaps[i]};
			/*
			selector.assignTermWeights(oneMap, QEModel, bgTermidFreqMap, bgTermidDocfreqMap);
			expTermMaps[i] = selector.getMostWeightedTermsInHashMap(selector.getNumberOfUniqueTerms());
			*/
			termidSet.addAll(termidFreqMaps[i].keys());
			docLength[i] = Statistics.sum(termidFreqMaps[i].getValues());
		}
		
		int[] termids = termidSet.toArray();
		Arrays.sort(termids);
		termidSet.clear(); termidSet = null;
		
		TIntIntHashMap termidDFMap = new TIntIntHashMap();
		
		TIntObjectHashMap<ExpansionTerm>[] expTermMaps = new TIntObjectHashMap[effDocuments];
		for (int i=0; i<effDocuments; i++){
			// assign weights for each term seen or unseen in each document
			expTermMaps[i] = new TIntObjectHashMap<ExpansionTerm>();
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
				}else{
					// unseen term
					double score = QEModel.score(0d,
							docLength[i], 
							bgTermidDocfreqMap.get(termid), 
							bgTermidFreqMap.get(termid), 
							1d);
					ExpansionTerm term = new ExpansionTerm(termid);
					term.setWeightExpansion(score);
					expTermMaps[i].put(termid, term);
				}
			}
		}
		
		// merge expansion terms: compute mean term weight for each term, sort again
		// Unlike Rocchio's term selector, RM3 considers unseen terms
		TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
		// TIntIntHashMap termidDFMap = new TIntIntHashMap();
		
		
		double[] queryGenWeights = new double[effDocuments];
		Arrays.fill(queryGenWeights, 1d);
		
		
		/*for (int termid : termids){
			for (int i=0; i<effDocuments; i++){
				if (expTermMaps[i].containsKey(termid)){
					// seen term, do nothing
				}else{
					// unseen term
					double score = QEModel.score(0d, 
							docLength[i], 
							bgTermidDocfreqMap.get(termid), 
							bgTermidFreqMap.get(termid), 
							1d);
					ExpansionTerm term = new ExpansionTerm(termid, 0);
					term.setWeightExpansion(score);
					expTermMaps[i].put(termid, term);
				}
			}
		}*/
		
		for (int termid : originalQueryTermidSet.toArray()){
			for (int i=0; i<effDocuments; i++){
				if (expTermMaps[i].containsKey(termid))
					queryGenWeights[i] *= expTermMaps[i].get(termid).getWeightExpansion();
			}
		}
		double maxDocWeight = 0d;
		for (int i=0; i<effDocuments; i++)
			maxDocWeight = Math.max(maxDocWeight, queryGenWeights[i]);
		for (int i=0; i<effDocuments; i++)
			queryGenWeights[i] += maxDocWeight;
		
		// int querylength = this.originalQueryTermidSet.size();
		
		for (int termid : termids){
			double score = 0d;
			for (int i=0; i<effDocuments; i++){
				score += expTermMaps[i].get(termid).getWeightExpansion()*queryGenWeights[i];
			}
			termidWeightMap.put(termid, score);
		}
		
		ExpansionTerm[] candidateTerms = new ExpansionTerm[termidWeightMap.size()];
		// expansion term should appear in at least half of the feedback documents
		// int minDF = (effDocuments%2==0)?(effDocuments/2):(effDocuments/2+1);
		int minDF = 2;
		int counter = 0;
		for (int termid : termidWeightMap.keys()){
			candidateTerms[counter] = new ExpansionTerm(termid);
			
			if (effDocuments>minDF&&termidDFMap.get(termid)<minDF)
				candidateTerms[counter].setWeightExpansion(0d);
			else
				candidateTerms[counter].setWeightExpansion(termidWeightMap.get(termid)/effDocuments);
			counter++;
		}
		Arrays.sort(candidateTerms);
		
		termMap = new TIntObjectHashMap<ExpansionTerm>();
		/*
		double normaliser = 0d;
		for (ExpansionTerm term : candidateTerms){	
			normaliser += term.getWeightExpansion();
		}*/
		
		// map weights to probabilities
		for (ExpansionTerm term : candidateTerms){			
			// term.setWeightExpansion(term.getWeightExpansion()/normaliser);
			termMap.put(term.getTermID(), term);
		}
		for (int i=0; i<effDocuments; i++){
			expTermMaps[i].clear(); expTermMaps[i] = null;
		}
		termidDFMap.clear(); termidDFMap = null;
	}
	
	public void mergeWithQuery(QueryExpansionModel QEModel, MatchingQueryTerms query, int numberOfExpansionTerms){
		query.normalizeLMQueryModel();
		ExpansionTerm[] expTerms = this.getMostWeightedTerms(numberOfExpansionTerms);
		// this.norm(expTerms);
		// normalize expansion term weights
		double sum = 0d;
		for (ExpansionTerm term : expTerms)
			sum += term.getWeightExpansion();
		
		for (int i = 0; i < expTerms.length; i++){
			if (expTerms[i].getWeightExpansion()<=0)
				break;
			Entry<String, LexiconEntry> entry = lexicon.getLexiconEntry(expTerms[i].getTermID());
			double finalWeight = (1-QEModel.ROCCHIO_BETA)*query.getTermWeight(entry.getKey())+
					QEModel.ROCCHIO_BETA*expTerms[i].getWeightExpansion()/sum;
			query.setTermProperty(entry.getKey(), finalWeight);
			/*
			if(logger.isDebugEnabled()){
				logger.debug("term " + entry.getKey()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(entry.getKey()), 4));
			}*/
		}
	}
}
