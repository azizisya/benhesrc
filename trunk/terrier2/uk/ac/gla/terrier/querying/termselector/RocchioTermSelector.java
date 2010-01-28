package uk.ac.gla.terrier.querying.termselector;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;

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
	public void assignTermWeights(ResultSet resultSet, int feedbackSetSize, WeightingModel QEModel, Lexicon bgLexicon){
		int[] docids = resultSet.getDocids();
		feedbackSetSize = Math.min(feedbackSetSize, docids.length);
		assignTermWeights(Arrays.copyOf(docids, feedbackSetSize), QEModel, bgLexicon);
	}

	@Override
	public void assignTermWeights(int[] docids, WeightingModel QEModel, Lexicon bgLexicon) {
		int effDocuments = docids.length;
		TermSelector selector = TermSelector.getTermSelector("DFRTermSelector", index);
		selector.setMetaInfo("normalize.weights", "false");
		ExpansionTerm[][] expTerms = new ExpansionTerm[effDocuments][];
		
		// for each of the top-ranked documents
		for (int i=0; i<effDocuments; i++){
			// obtain the weighted terms
			int[] oneDocid = {docids[i]};
			selector.assignTermWeights(oneDocid, QEModel, bgLexicon);
			expTerms[i] = selector.getMostWeightedTerms(selector.getNumberOfUniqueTerms());
		}
		// merge expansion terms: compute mean term weight for each term, sort again
		TIntDoubleHashMap termidWeightMap = new TIntDoubleHashMap();
		TIntIntHashMap termidDFMap = new TIntIntHashMap();
		for (int i=0; i<effDocuments; i++){
			for (int j=0; j<expTerms[i].length; j++){
				termidWeightMap.adjustOrPutValue(expTerms[i][j].getTermID(), expTerms[i][j].getWeightExpansion(), expTerms[i][j].getWeightExpansion());
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

}
