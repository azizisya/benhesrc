/**
 * 
 */
package org.terrier.querying.termselector;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;

import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.structures.ExpansionTerm;
import org.terrier.structures.Index;

import uk.ac.gla.terrier.statistics.ScoreNormaliser;
import uk.ac.gla.terrier.statistics.Statistics;

/**
 * @author ben
 *
 */
public class WeightedRocchioTermSelector extends RocchioTermSelector {

	/**
	 * 
	 */
	public WeightedRocchioTermSelector() {
		super();
	}

	/**
	 * @param index
	 */
	public WeightedRocchioTermSelector(Index index) {
		super(index);
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
		
		
		double[] docWeights = new double[effDocuments];
		Arrays.fill(docWeights, 0d);
		
		for (int termid : originalQueryTermidSet.toArray()){
			for (int i=0; i<effDocuments; i++){
				if (expTermMaps[i].containsKey(termid))
					docWeights[i] += expTermMaps[i].get(termid).getWeightExpansion();
			}
		}
		double maxDocWeight = 0d;
		for (int i=0; i<docWeights.length; i++)
			maxDocWeight = Math.max(maxDocWeight, docWeights[i]);
		for (int i=0; i<docWeights.length; i++)
			docWeights[i] += maxDocWeight;
		ScoreNormaliser.normalizeScoresByMax(docWeights);
		
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
						score += expTermMaps[i].get(termid).getWeightExpansion()*docWeights[i];
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
	
}
