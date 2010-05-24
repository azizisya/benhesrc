/*
 * Created on 14 Jun 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.matching.dsms;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Distance;
import org.terrier.utility.Files;

import gnu.trove.THashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectDoubleHashMap;

public class ProximityScoreModifierOF2008 extends ProximityScoreModifierTREC2008 {
	protected String[] opTerms = null;
	protected TObjectDoubleHashMap opTermScoreMap = null;
	
	protected double w_top;
	protected double w_oop;
	protected double w_uop;
	
	protected final String oplistFilename = ApplicationSetup.getProperty("opinion.term.list.filename", "");
	
	private void loadOpinionTermList(String filename){
		THashSet<String> termSet = new THashSet<String>();
		opTermScoreMap = new TObjectDoubleHashMap();
		double maxWeight = 0d;
		final int topX = Integer.parseInt(ApplicationSetup.getProperty("opinion.term.list.topX", "100"));
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			int counter = 0;
			while ((str=br.readLine())!=null){
				String[] tokens = str.split(" ");
				termSet.add(tokens[0]);
				double weight = Double.parseDouble(tokens[1]);
				maxWeight = Math.max(weight, maxWeight);
				opTermScoreMap.put(tokens[0], weight);
				if (++counter == topX)
					break;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		this.opTerms = (String[])termSet.toArray(new String[termSet.size()]);
		// normalise the weights
		for (int i=0; i<opTerms.length; i++){
			double weight = opTermScoreMap.get(opTerms[i]);
			this.opTermScoreMap.adjustValue(opTerms[i], weight/maxWeight - weight);
			System.err.println(opTerms[i]+", weight: "+opTermScoreMap.get(opTerms[i]));
		}
	}
	
	
	
	/**
	 * Modifies the scores of documents, in which there exist, or there does not
	 * exist a given phrase.
	 * 
	 * @param index
	 *			Index the data structures to use.
	 * @param terms
	 *			MatchingQueryTerms the terms to be matched for the query. This
	 *			does not correspond to the phrase terms necessarily, but to
	 *			all the terms of the query.
	 * @param set
	 *			ResultSet the result set for the query.
	 * @return true if any scores have been altered
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms terms, ResultSet set) {

		if (phraseQTWfnid < 1 || phraseQTWfnid > 4)
		{
			System.err.println("ERROR: Wrong function id specified for ProximityScoreModifierTREC2008");
		}
		if (this.index == null){
			this.index = index;
			doi = index.getDocumentIndex();
			final CollectionStatistics collStats = index.getCollectionStatistics();
			numTokens = collStats.getNumberOfTokens();
			numDocs = (long)(collStats.getNumberOfDocuments());
			avgDocLen =  ((double)(numTokens - numDocs *(ngramLength-1))) / (double)numDocs;
			avgDocLen_SD = ((double)(numTokens - numDocs*(ngramLength_SD - 1))) / (double)numDocs;
			lexicon = index.getLexicon();
		}
		
		w_top = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_top","1.0d"));
		w_oop = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_oop","1.0d"));
		w_uop = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_uop","1.0d"));

		//get local references for the document ids and the
		//scores of documents from the result set.
		double[] scores = set.getScores();
		int[] docids = set.getDocids();
	
		//ordered dependence scores
		double[] score_o = new double[scores.length];
		//unordered dependence scores
		double[] score_u = new double[scores.length];
		//phraseterm - opinionTerm dependence scores
		double[] score_op = new double[scores.length];

		//the number of terms in the phrase
		phraseTerms = terms.getTerms();
		if (onlyHighWeightQTerms)
		{
			ArrayList<String> proxterms = new ArrayList<String>();
			for (String t : phraseTerms)
			{
				if (terms.getTermWeight(t) >= 1.0d)
					proxterms.add(t);
			}
			phraseTerms = proxterms.toArray(new String[0]);
		}
		final int phraseLength = phraseTerms.length;
		final double[] phraseTermWeights = new double[phraseLength];
		for (int i=0; i<phraseLength; i++)
		{
			phraseTermWeights[i] = terms.getTermWeight(phraseTerms[i]);
			System.err.println("phrase term: " + phraseTerms[i]);
		}
		if (phraseLength == 0)
			System.err.println("Warning: no phrase term chosen.");
		String term1 = null;
		String term2 = null;
		
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.opc","1.0d"));
		final int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.oplength","2"));
		
		if (dependency.equals("FD"))
			System.err.println("w_top: "+w_top+", w_uop: "+w_uop+", fnid: "+phraseQTWfnid+", ngramOFc: "+ngramC);
		else if (dependency.equals("SD"))
			System.err.println("w_top: "+w_top+", w_oop: "+w_oop+", fnid: "+phraseQTWfnid+", ngramOFc: "+ngramC);
		if (opTerms == null){
			loadOpinionTermList(oplistFilename);
		}
		if (dependency.equals("FD"))
		{
			/*for (int i=0; i<phraseLength-1; i++) {
				for (int j=i+1; j<phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					this.computeFDScore(term1, term2, scores, score_u, docids, 
							phraseTermWeights[i], phraseTermWeights[j], ngramC, index);
	   			}
			}*/
			for (int i=0; i<phraseLength; i++){
				for (int j=0; j<opTerms.length; j++){
					term1 = phraseTerms[i];
					term2 = opTerms[j];
					if (term1.equals(term2))
						continue;
					term1 = phraseTerms[i];
					term2 = opTerms[j];
					this.computeFDScore(term1, term2, scores, score_u, docids, 
							phraseTermWeights[i], opTermScoreMap.get(opTerms[j]), ngramC, ngramLength, index);
				}
			}
		
			for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_top * scores[k] +  w_uop * score_u[k] ;		
			}
			
	  		postingsCache.clear();
			 		   	  
	  	}
		else if (dependency.equals("SD")){
			/*for (int i=0; i<phraseLength-1; i++) {
			for (int j=i+1; j<phraseLength; j++) {
				term1 = phraseTerms[i];
				term2 = phraseTerms[j];
				this.computeFDScore(term1, term2, scores, score_u, docids, 
						phraseTermWeights[i], phraseTermWeights[j], ngramC, index);
   			}
		}*/
		for (int i=0; i<phraseLength; i++){
			for (int j=0; j<opTerms.length; j++){
				term1 = phraseTerms[i];
				term2 = opTerms[j];
				if (term1.equals(term2))
					continue;
				term1 = phraseTerms[i];
				term2 = opTerms[j];
				this.computeFDScore(term1, term2, scores, score_u, docids, 
						phraseTermWeights[i], opTermScoreMap.get(opTerms[j]), ngramC, ngramLength, index);
			}
		}
	
		for (int k=0; k<docids.length; k++) {		
			scores[k] =  w_top * scores[k] +  w_oop * score_u[k] ;		
		}
		
  		postingsCache.clear();
		
		}
		else
		{
			System.err.println("WARNING: proximity.dependency.type not set. Set it to either FD or SD");
			return false;
		}
		//returning true, assuming that we have modified the scores of documents		
		return true;		
	}
}
