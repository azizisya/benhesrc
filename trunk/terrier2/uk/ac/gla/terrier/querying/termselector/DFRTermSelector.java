package uk.ac.gla.terrier.querying.termselector;


import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

public class DFRTermSelector extends TermSelector {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	protected int EXPANSION_MIN_DOCUMENTS;
	
	public DFRTermSelector(){
		super();
		this.EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup.getProperty("expansion.mindocuments","2"));
	}
	
	public DFRTermSelector(Index index) {
		super(index);
		this.EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup.getProperty("expansion.mindocuments","2"));
	}

	@Override
	public void assignTermWeights(ResultSet resultSet, int feedbackSetSize, WeightingModel QEModel, Lexicon bgLexicon){
		int[] docids = resultSet.getDocids();
		feedbackSetSize = Math.min(feedbackSetSize, docids.length);
		assignTermWeights(Arrays.copyOf(docids, feedbackSetSize), QEModel, bgLexicon);
	}

	@Override
	public void assignTermWeights(int[] docids, WeightingModel QEModel, Lexicon bgLexicon) {
		this.getTerms(docids);
		
		// NOTE: if the set of candidate terms is empty, there is no need to
		// perform any term re-weighing.
		if (termMap.size() == 0) {
			return;
		}
		
		QEModel.setNumberOfTokens(index.getCollectionStatistics().getNumberOfTokens());
		QEModel.setAverageDocumentLength(index.getCollectionStatistics().getAverageDocumentLength());
		QEModel.setNumberOfDocuments(index.getCollectionStatistics().getNumberOfDocuments());
		
		// weight the terms
		Object[] arr = termMap.getValues();
		ExpansionTerm[] allTerms = new ExpansionTerm[arr.length];
		final int len = allTerms.length;
		for(int i=0;i<len;i++)
			allTerms[i] = (ExpansionTerm)arr[i];
		boolean classicalFiltering = Boolean.parseBoolean(ApplicationSetup.getProperty("expansion.classical.filter", "true"));
		for (int i=0; i<len; i++){
			try{
				//only consider terms which occur in 2 or more documents. Alter using the expansion.mindocuments property.
				
				if (classicalFiltering && docids.length>1&&allTerms[i].getDocumentFrequency() < EXPANSION_MIN_DOCUMENTS &&
						!originalQueryTermidSet.contains(allTerms[i].getTermID())){
					allTerms[i].setWeightExpansion(0);
					continue;
				}
				/**
				 * 17/02/2009 Ben: this condition is changed to: only consider terms which occur in at least half of the feedback documents. 
				 */
				else if (!classicalFiltering){
					int minDocs = (docids.length%2==0)?(docids.length/2-1):(docids.length/2);
					if (docids.length>1&&allTerms[i].getDocumentFrequency() < minDocs &&
						!originalQueryTermidSet.contains(allTerms[i].getTermID())){
						allTerms[i].setWeightExpansion(0);
						continue;
					}
				}
				
				// collection tf
				double TF = 0;
				// collection df
				double Nt = 0;
				bgLexicon.findTerm(allTerms[i].getTermID());
				TF = bgLexicon.getTF();
				Nt = bgLexicon.getNt();
				
				
				
				allTerms[i].setWeightExpansion(QEModel.weight(
						allTerms[i].getWithinDocumentFrequency(), 
						feedbackSetLength, Nt, TF));
				
			} catch(NullPointerException npe) {
				//TODO print something more explanatory here
				logger.fatal("A nullpointer exception occured while iterating over expansion terms at iteration number: "+"i = " + i,npe);
			}
		}
		Arrays.sort(allTerms);
		
		// determine double normalizing factor
		double normaliser = allTerms[0].getWeightExpansion();
		if (QEModel.PARAMETER_FREE && QEModel.SUPPORT_PARAMETER_FREE_QE){
			normaliser = QEModel.parameterFreeNormaliser(
					allTerms[0].getWithinDocumentFrequency(), 
					index.getCollectionStatistics().getNumberOfTokens(), feedbackSetLength);
			//if(logger.isInfoEnabled()){
				//logger.info("parameter free query expansion.");
			//}
		}
		
		
		// print term with highest weight (the normalization weight)
		bgLexicon.findTerm(allTerms[0].getTermID());
		if(logger.isDebugEnabled()){
			logger.debug("term with the maximum weight: " + lexicon.getTerm() +
					", normaliser: " + Rounding.toString(normaliser, 4));
		}
		
		boolean normalizeWeights = Boolean.parseBoolean(metaMap.get("normalize.weights"));
		
		// add all terms to the returning collection
		if (normalizeWeights)
			for (ExpansionTerm term : allTerms){
				// lexicon.findTerm(term.getTermID());
				//NOTE: if all terms are assigned a zero weight, the maximum
				//weight (i.e., the normaliser) will also be zero, and we'll
				//get a NaN weight while trying to apply normalization
				if (normaliser != 0) {
					term.setWeightExpansion(term.getWeightExpansion()/normaliser);
				}
			}
	}

}
