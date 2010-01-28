package uk.ac.gla.terrier.learning;

import gnu.trove.THashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

public abstract class LearningAlgorithm {
	protected String classifierName;
	
	public LearningAlgorithm(String classifierName) {
		super();
		this.classifierName = classifierName;
	}

	// input: positive/negative examples
	// output: the learnt model. Can be a file where the learnt model is saved
	public abstract String learn(String[] ids, int[] labels, THashMap<String, double[]> dataMap, String args);
	
	// input in arff format
	public abstract String learn(String arffDataFilename, String args);
	
	public abstract String predict(String[] ids, THashMap<String, double[]> dataMap, String modelFilename, 
			TObjectIntHashMap<String> idLabelHashMap, TObjectDoubleHashMap<String> idConfHashMap);
	
	public abstract String predict(String arffDataFilename, String modelFilename, TObjectIntHashMap<String> idLabelHashMap, 
			TObjectDoubleHashMap<String> idConfHashMap);
	
	public abstract double[] getPerformanceByClass(int label);
}
