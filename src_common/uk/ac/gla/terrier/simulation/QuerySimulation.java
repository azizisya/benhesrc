/*
 * Created on 2005-2-16
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.simulation;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import uk.ac.gla.terrier.applications.ExtractInformativeTerms;
import uk.ac.gla.terrier.applications.TRECQuerying;
import uk.ac.gla.terrier.matching.BufferedMatching;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.querying.Request;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class QuerySimulation {
	
	protected WeightingModel wmodel;
	
	protected WeightingModel qemodel;
	
	protected DocumentIndex docIndex;
	
	protected InvertedIndex invIndex;
	
	protected Lexicon lexicon;
	
	protected DirectIndex directIndex;
	
	protected TFRanking tfRanking;
	
	protected BufferedMatching matching;
	
	protected CollectionStatistics collStat;
	
	protected TRECQuerying querying;
	
	protected ExtractInformativeTerms extract;
	
	public QuerySimulation(){
		this(Index.createIndex());
	}
	
	public QuerySimulation(Index index){
		docIndex = index.getDocumentIndex();
		invIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		directIndex = index.getDirectIndex();
		collStat = index.getCollectionStatistics();
		this.setModels("PL2", "Bo1", 7d);
		this.tfRanking = new TFRanking(index);
		matching = new BufferedMatching(index);
		matching.setModel(this.wmodel);
		querying = new TRECQuerying(index);
		extract = new ExtractInformativeTerms(index);
	}
	
	public String oneStepSimulation(int minLength, int maxLength){
		int queryLength = minLength + (int)(Math.random()*(maxLength-minLength));
		return oneStepSimulation(queryLength);
	}
	
	public String oneStepSimulation(int queryLength){
		return this.concatQueryTerms(this.extractInformativeTerms(queryLength));
	}
	
	public String twoStepSimulation(int minLength, int maxLength){
		int queryLength = minLength + (int)(Math.random()*(maxLength-minLength));
		return twoStepSimulation(queryLength);
	}
	
	public String twoStepSimulation(int queryLength){
		// extract terms using a random seed-term and then obtain the new seed-term
		lexicon.findTerm(extractInformativeTerms(1)[0].getTermID());
		String newSeedTerm = lexicon.getTerm();
		// using the new seed-term to extract other composing terms of the
		// simulated query
		ExpansionTerm[] extractedTerms = this.extractInformativeTerms(newSeedTerm,
				queryLength - 1);
		Arrays.sort(extractedTerms);
		// the simulated query consists of the new seed-term and the extracted terms
		// in the second extraction
		//ExpansionTerm[] queryTerms = new ExpansionTerm[queryLength];
		//queryTerms[0] = new TermTreeNode(newSeedTerm);
		//for (int i = 0; i < queryLength; i++){
			//queryTerms[i] = new ExpansionTerm[i];
		//}//
		
		return concatQueryTerms(extractedTerms);
	}
	
	protected String concatQueryTerms(ExpansionTerm[] expTerms){
		StringBuilder buf = new StringBuilder();
		for (int i=0; i<expTerms.length; i++){
			LexiconEntry lexEntry = lexicon.getLexiconEntry(expTerms[i].getTermID());
			buf.append(lexEntry.term+"^"+expTerms[i].getWeightExpansion()+" ");
		}
		return buf.toString().trim();
	}
	
	public void setModels(String wModelName, String qeModelName, double parameter){
		if (wModelName.lastIndexOf('.') < 0)
			wModelName = "uk.ac.gla.terrier.matching.models.".concat(wModelName);
		if (qeModelName.lastIndexOf('.') < 0)
			qeModelName = "uk.ac.gla.terrier.matching.models.".concat(qeModelName);
		try{
			this.wmodel = (WeightingModel)Class.forName(wModelName).newInstance();
			this.wmodel.setParameter(parameter);
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println("Error occurs while creating the weighting model.");
			System.exit(1);
		}
		try{
			this.qemodel = WeightingModel.getWeightingModel(qeModelName);
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println("Error occurs while creating the query expansion model.");
			System.exit(1);
		}
	}
	
	public ExpansionTerm[] extractInformativeTerms(int numberOfExtractedTerms){
		int numberOfTopDocs = ApplicationSetup.EXPANSION_DOCUMENTS;
		String queryid = "extraction";
		int documentFrequency = 0;
		int initialSeedTermid = 0;
		//System.out.println("start extracting...");
		while (documentFrequency < numberOfTopDocs){
			//System.out.println("selecting a random term...");
			initialSeedTermid = this.tfRanking.getValidRandomTermId();
			//System.out.println("initial seed termid: " + initialSeedTermid);
			lexicon.findTerm(initialSeedTermid);
			documentFrequency = lexicon.getNt();
		}
		String seedTerm = lexicon.getTerm();
		//System.out.println("seed-term: " + seedTerm);
		//TermTreeNode[] singleTerm = {new TermTreeNode(seedTerm)};
		//BasicQuery query = new BasicQuery(singleTerm, queryid);
		String query = seedTerm+"^1.0000";
		//System.out.println("matching for seed-term...");
		//matching.basicMatch(query);
		//ResultSet resultSet = matching.getResultSet();
		ResultSet resultSet = querying.processQuery(queryid, query).getResultSet();
		int[] docidsTmp = resultSet.getDocids();
		
		int[] docids = new int[numberOfTopDocs];
		for (int i = 0; i < numberOfTopDocs; i++)
			docids[i] = docidsTmp[i];
		THashSet<String> excludedTerms = new THashSet<String>();
		excludedTerms.add(seedTerm);
		return this.extractInformativeTerms(docids, numberOfExtractedTerms, excludedTerms);
	}
	
	public ExpansionTerm[] extractInformativeTerms(String seedTerm, int numberOfExtractedTerms){
		String queryid = this.getRandomQueryId();
		Request q = (Request)querying.processQuery(queryid, seedTerm);
		ResultSet resultSet = q.getResultSet();
		int[] docidsTmp = resultSet.getDocids();
		int numberOfTopDocs = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS,
				resultSet.getExactResultSize());
		int[] docids = new int[numberOfTopDocs];
		for (int i = 0; i < numberOfTopDocs; i++)
			docids[i] = docidsTmp[i];
		THashSet<String> excludedTerms = new THashSet<String>();
		excludedTerms.add(seedTerm);
		return extract.getInformativeTerms(docids, numberOfExtractedTerms, ApplicationSetup.getProperty("trec.qemodel", "Bo1"));
	}
	
	public ExpansionTerm[] extractInformativeTerms(int[] docids
			, int numberOfExtractedTerms, THashSet<String> excludedTerms){
		double totalDocumentLength = 0;
		int effDocuments = docids.length; 
		for (int i = 0; i < effDocuments; i++)
			totalDocumentLength += docIndex.getDocumentLength(docids[i]);
		ExpansionTerms expansionTerms =
			new ExpansionTerms(collStat, totalDocumentLength, lexicon);
		
		for (int i = 0; i < effDocuments; i++) {
			int[][] terms = directIndex.getTerms(docids[i]);
			for (int j = 0; j < terms[0].length; j++)
				expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
		}
		TIntObjectHashMap<ExpansionTerm> expTermSet = expansionTerms.getExpandedTermHashSet(
				numberOfExtractedTerms+excludedTerms.size(), qemodel);
		ExpansionTerm[] expTerms = new ExpansionTerm[expTermSet.size()];
		int counter = 0;
		for (Integer k : expTermSet.keys()){
			expTerms[counter++] = expTermSet.get(k);
		}
		Arrays.sort(expTerms);
		ExpansionTerm[] termsToReturn = new ExpansionTerm[numberOfExtractedTerms];
		counter = 0;
		for (int i=0; i<expTerms.length; i++){
			LexiconEntry lexEntry = lexicon.getLexiconEntry(expTerms[i].getTermID());
			if (!excludedTerms.contains(lexEntry.term))
				termsToReturn[counter++] = expTerms[i];
		}
		return termsToReturn;
	}
	
	/**
	 * Get a queryid that is randomly chosen and unduplicatable.
	 * @return
	 */
	private String getRandomQueryId(){
		return new String(""+System.currentTimeMillis());
	}

	public static void main(String[] args) {
		QuerySimulation simulation = new QuerySimulation();
	 	String query = simulation.twoStepSimulation(10);
	 	System.err.println(query);
	}
}
