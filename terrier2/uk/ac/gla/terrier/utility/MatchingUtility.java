package uk.ac.gla.terrier.utility;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;

public class MatchingUtility {
	public static WeightingModel getWeightingModel(Index index){
		String namespace = "uk.ac.gla.terrier.matching.models.";
		String modelName = namespace.concat(ApplicationSetup.getProperty("trec.model", "TF_IDF"));
		WeightingModel wmodel = null;
		double c = Double.parseDouble(ApplicationSetup.getProperty("c", "0.35"));
		try{
			wmodel = (WeightingModel)(Class.forName(modelName)).newInstance();
		}catch(Exception e){
			e.printStackTrace();
		}
		wmodel.setParameter(c);
		CollectionStatistics collStat = index.getCollectionStatistics();
		wmodel.setAverageDocumentLength(collStat.getAverageDocumentLength());
		wmodel.setNumberOfDocuments(collStat.getNumberOfDocuments());
		wmodel.setNumberOfPointers(collStat.getNumberOfPointers());
		wmodel.setNumberOfTokens(collStat.getNumberOfTokens());
		wmodel.setNumberOfUniqueTerms(collStat.getNumberOfUniqueTerms());
		return wmodel;
	}
}
