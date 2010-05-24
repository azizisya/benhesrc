package tests;

import org.terrier.querying.Manager;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.TRECQuery;

import uk.ac.gla.terrier.statistics.Statistics;

public class QueryPrediction {
	
	public static void printPredictors(){
		Index index = Index.createIndex();
		Lexicon lex = index.getLexicon();
		TRECQuery queries = new TRECQuery();
		Manager manager = new Manager(index);
		CollectionStatistics collSta = index.getCollectionStatistics();
		while (queries.hasMoreQueries()){
			String query = queries.nextQuery();
			String[] terms = query.split(" ");
			for (int i=0; i<terms.length; i++)
				terms[i] = manager.pipelineTerm(terms[i]);
			// compute SCQ
			double SCQ = 0d;
			double maxSCQ = 0d;
			for (int i=0; i<terms.length; i++){
				if (terms[i]==null) continue;
				if (lex.findTerm(terms[i])){
					double SCQt = termCollSimilarity(lex.getTF(), collSta.getNumberOfDocuments(), lex.getNt());
					maxSCQ = Math.max(maxSCQ, SCQt);
					SCQ += SCQt;
				}
			}
			// compute maxSCQ
			
			// compute stdw
			double stdw = 0d;
			for (int i=0; i<terms.length; i++){
				if (terms[i]==null) continue;
				if (lex.findTerm(terms[i])){
					int[][] docs = index.getInvertedIndex().getDocuments(lex.getTermId());
					stdw+=termVariability(docs[1], collSta.getNumberOfDocuments(), lex.getNt());
				}
			}
			System.out.println(queries.getQueryId()+" "+SCQ+" "+maxSCQ+" "+stdw);
		}
		index.close();
	}
	
	public static double termVariability(int[] tf, int N, int df){
		double[] w = new double[tf.length];
		for (int i=0; i<tf.length; i++)
			w[i] = 1+Math.log(tf[i])*Math.log(1+(double)N/df);
		return Statistics.standardDeviation(w);
	}
	
	public static double termCollSimilarity(double TF, int N, int df){
		return (1d+Math.log(TF))*Math.log(1+(double)N/df);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[1].equals("--printpredictors")){
			printPredictors();
		}
	}

}
