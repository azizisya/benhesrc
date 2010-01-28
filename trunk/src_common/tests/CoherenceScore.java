package tests;

import gnu.trove.TDoubleArrayList;

import java.util.Arrays;

import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.statistics.CosineSimilarity;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.MatchingUtility;

public class CoherenceScore {
	
	protected Index index;
	
	protected DocumentIndex docIndex;
	
	protected DirectIndex directIndex;
	
	protected WeightingModel wmodel;
	
	protected CollectionStatistics cs;
	
	protected Lexicon lexicon;
	
	protected double c;
	
	public CoherenceScore(){
		index = Index.createIndex();
		docIndex = index.getDocumentIndex();
		lexicon = index.getLexicon();
		directIndex = index.getDirectIndex();
		this.wmodel = MatchingUtility.getWeightingModel(index);
	}
	
	public void printCoherenceScore(String resultFilename, int X){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] queryids = results.getQueryids();
		for (int i=0; i<queryids.length; i++){
			TDoubleArrayList scores = new TDoubleArrayList();
			String[] retDocnos = results.getRetrievedDocnos(queryids[i]);
			int top = Math.min(X, retDocnos.length);
			String[] docnos = new String[top];
			System.arraycopy(retDocnos, 0, docnos, 0, top);
			// store temp vectors in memory?
			for (int x=0; x<docnos.length-1; x++){
				double[] vector = this.constructDocumentVector(docnos[x]);
				for (int y=x+1; y<docnos.length; y++){
					scores.add(CosineSimilarity.cosine(vector, constructDocumentVector(docnos[y])));
				}
				vector = null;
			}
			System.out.print(queryids[i]);
			double[] scores_copy = scores.toNativeArray();
			for (int j=0; j<scores_copy.length; j++)
				System.out.print(" "+scores_copy[j]);
			System.out.println();
			scores.clear(); scores = null;
			retDocnos = null; docnos = null;
			scores_copy = null;
		}
	}
	
	private double computeCoherenceScore(double[][] vectors){
		return 0;
	}
	
	private double[] constructDocumentVector(String docno){
		int docid = docIndex.getDocumentId(docno);
		int[][] terms = directIndex.getTerms(docid);
		double[] vector = new double[cs.getNumberOfUniqueTerms()];
		Arrays.fill(vector, 0d);
		int docLength = docIndex.getDocumentLength(docid);
		for (int i=0; i<terms[0].length; i++){
			lexicon.findTerm(terms[0][i]);
			vector[terms[0][i]] = wmodel.score((double)terms[1][i], 
					(double)docLength, (double)lexicon.getNt(), (double)lexicon.getTF(), 1d);
		}
		return vector;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
