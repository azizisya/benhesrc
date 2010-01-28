/*
 * Created on 18 Aug 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.SingleLineTRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.FindTuples;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TupleQuery {
	protected FindTuples tupleFinder = new FindTuples(
			ApplicationSetup.getProperty("tuple.index.path", "/local/terrier/Indices/Wikipedia/2008/pfn/full/title/"),
			ApplicationSetup.getProperty("tuple.index.prefix", "data")
			);
	
	protected final int phraseQTWfnid = Integer.parseInt(ApplicationSetup.getProperty("proximity.qtw.fnid", "1"));
	
	protected Index index;
	protected DocumentIndex doi;
	protected long numTokens;
	protected long numDocs;
	protected double avgDocLen;
	protected double avgDocLen_SD;
	protected Lexicon lexicon;

	public TupleQuery() {
		super();
	} 
	
	/**
	 * Generate tuple queries out of one line queries.protected String[] phraseTerms;
	 * @param oneLineTopicFilename
	 * @param outputFilename
	 */
	public void generateTupleQuery(Index index, String oneLineTopicFilename, String outputFilename){
		SingleLineTRECQuery queries = new SingleLineTRECQuery(oneLineTopicFilename);
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			while (queries.hasMoreQueries()){
				String queryString = queries.nextQuery();
				String queryid = queries.getQueryId();
				TObjectDoubleHashMap<String> map = SingleLineTRECQuery.parseQueryStringWithWeights(queryString);
				String tupleQueryString = this.generateTupleQueryFromWikipedia(index, map);
				bw.write(queryid+" "+queryString+" "+tupleQueryString+ApplicationSetup.EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
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
	public String generateTupleQuery(Index index, TObjectDoubleHashMap<String> map) {
		boolean foundValidTuple = false;

		if (phraseQTWfnid < 1 || phraseQTWfnid > 4)
		{
			System.err.println("ERROR: Wrong function id specified for ProximityScoreModifierRF2008");
		}
		if (this.index == null){
			this.index = index;
			doi = index.getDocumentIndex();
			final CollectionStatistics collStats = index.getCollectionStatistics();
			numTokens = collStats.getNumberOfTokens();
			numDocs = (long)(collStats.getNumberOfDocuments());
			lexicon = index.getLexicon();
		}
		

		//the number of terms in the phrase
		//phraseTerms = terms.getTerms();
		String[] allTerms = 	map.keys(new String[map.size()]);
		
		TObjectDoubleHashMap<String> termHighWeightMap = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> termLowWeightMap = new TObjectDoubleHashMap<String>();
		
		for (String term : allTerms){
			LexiconEntry lexEntry = null;
			if ((lexEntry = lexicon.getLexiconEntry(term)) != null){
				if (lexEntry.TF < numDocs/5){
					double weight = map.get(term);
					if (weight >=1d)
						termHighWeightMap.put(term, weight);
					else
						termLowWeightMap.put(term, weight);
				}
			}
		}
		
		for (String term : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
			System.err.println("High weight phrase term: " + term);
		}
		if (termHighWeightMap.size() == 0)
			System.err.println("Warning: no high weight phrase term chosen.");
		
		for (String term : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
			System.err.println("Low weight phrase term: " + term);
		}
		if (termLowWeightMap.size() == 0)
			System.err.println("Warning: no low weight phrase term chosen.");
		StringBuilder sb = new StringBuilder();
		sb.append("TupleProximityScoreModifier [ ");
		String[] highWTerms = new String[termHighWeightMap.size()];
		int counter = 0;
		for (String term1 : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
			highWTerms[counter++] = term1;
			for (String term2 : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
				if (tupleFinder.findTuple(term1, term2)){
					double combinedPhraseQTWWeight;
					double phraseTermWeight1 = termHighWeightMap.get(term1);
					double phraseTermWeight2 = termLowWeightMap.get(term2);
					switch (phraseQTWfnid) {
						case 1: combinedPhraseQTWWeight = 0.5 * phraseTermWeight1 + 0.5 * phraseTermWeight2;
						break;
						case 2: combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
						break;
						case 3: combinedPhraseQTWWeight  = Math.min(phraseTermWeight1, phraseTermWeight2);
						break;
						case 4: combinedPhraseQTWWeight  = Math.max(phraseTermWeight1, phraseTermWeight2);
						break;
						default: combinedPhraseQTWWeight = 1.0d;
					}
					String newQueryString = "(" + term1 +" "+term2+ ")^"+Rounding.round(combinedPhraseQTWWeight, 4);
					System.out.println(newQueryString);
					sb.append(newQueryString+" ");
					foundValidTuple = true;
				}else{
					System.out.println("Tuple ("+term1+", "+term2+") ignored");
				}
			}
		}
		for (int i=0; i<highWTerms.length-1; i++){
			String term1 = highWTerms[i];
			for (int j=i+1; j<highWTerms.length; j++){
				String term2 = highWTerms[j];
				double combinedPhraseQTWWeight;
				double phraseTermWeight1 = termHighWeightMap.get(term1);
				double phraseTermWeight2 = termHighWeightMap.get(term2);
				switch (phraseQTWfnid) {
					case 1: combinedPhraseQTWWeight = 0.5 * phraseTermWeight1 + 0.5 * phraseTermWeight2;
					break;
					case 2: combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
					break;
					case 3: combinedPhraseQTWWeight  = Math.min(phraseTermWeight1, phraseTermWeight2);
					break;
					case 4: combinedPhraseQTWWeight  = Math.max(phraseTermWeight1, phraseTermWeight2);
					break;
					default: combinedPhraseQTWWeight = 1.0d;
				}
				String newQueryString = "(" + term1 +" "+term2+ ")^"+Rounding.round(combinedPhraseQTWWeight, 4);
				System.out.println(newQueryString);
				sb.append(newQueryString+" ");
				foundValidTuple = true;
			}
		}
		sb.append("]");
		if (!foundValidTuple)
			sb = new StringBuilder();
		return sb.toString();		
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
	public String generateTupleQueryFromWikipedia(Index index, TObjectDoubleHashMap<String> map) {
		boolean foundValidTuple = false;

		if (phraseQTWfnid < 1 || phraseQTWfnid > 4)
		{
			System.err.println("ERROR: Wrong function id specified for ProximityScoreModifierRF2008");
		}
		if (this.index == null){
			this.index = index;
			doi = index.getDocumentIndex();
			final CollectionStatistics collStats = index.getCollectionStatistics();
			numTokens = collStats.getNumberOfTokens();
			numDocs = (long)(collStats.getNumberOfDocuments());
			lexicon = index.getLexicon();
		}
		

		//the number of terms in the phrase
		//phraseTerms = terms.getTerms();
		String[] allTerms = 	map.keys(new String[map.size()]);
		
		TObjectDoubleHashMap<String> termHighWeightMap = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> termLowWeightMap = new TObjectDoubleHashMap<String>();
		
		for (String term : allTerms){
			LexiconEntry lexEntry = null;
			if ((lexEntry = lexicon.getLexiconEntry(term)) != null){
				if (lexEntry.TF < numDocs/5){
					double weight = map.get(term);
					if (weight >=1d)
						termHighWeightMap.put(term, weight);
					else
						termLowWeightMap.put(term, weight);
				}
			}
		}
		
		for (String term : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
			System.err.println("High weight phrase term: " + term);
		}
		if (termHighWeightMap.size() == 0)
			System.err.println("Warning: no high weight phrase term chosen.");
		
		for (String term : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
			System.err.println("Low weight phrase term: " + term);
		}
		if (termLowWeightMap.size() == 0)
			System.err.println("Warning: no low weight phrase term chosen.");
		StringBuilder sb = new StringBuilder();
		sb.append("TupleProximityScoreModifier [ ");
		String[] highWTerms = new String[termHighWeightMap.size()];
		int counter = 0;
		for (String term1 : (String[])termHighWeightMap.keys(new String[termHighWeightMap.size()])){
			highWTerms[counter++] = term1;
			for (String term2 : (String[])termLowWeightMap.keys(new String[termLowWeightMap.size()])){
				if (tupleFinder.findTuple(term1, term2)){
					double combinedPhraseQTWWeight;
					double phraseTermWeight1 = termHighWeightMap.get(term1);
					double phraseTermWeight2 = termLowWeightMap.get(term2);
					switch (phraseQTWfnid) {
						case 1: combinedPhraseQTWWeight = 0.5 * phraseTermWeight1 + 0.5 * phraseTermWeight2;
						break;
						case 2: combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
						break;
						case 3: combinedPhraseQTWWeight  = Math.min(phraseTermWeight1, phraseTermWeight2);
						break;
						case 4: combinedPhraseQTWWeight  = Math.max(phraseTermWeight1, phraseTermWeight2);
						break;
						default: combinedPhraseQTWWeight = 1.0d;
					}
					String newQueryString = "(" + term1 +" "+term2+ ")^"+Rounding.round(combinedPhraseQTWWeight, 4);
					System.out.println(newQueryString);
					sb.append(newQueryString+" ");
					foundValidTuple = true;
				}else{
					System.out.println("Tuple ("+term1+", "+term2+") ignored");
				}
			}
		}
		for (int i=0; i<highWTerms.length-1; i++){
			String term1 = highWTerms[i];
			for (int j=i+1; j<highWTerms.length; j++){
				String term2 = highWTerms[j];
				double combinedPhraseQTWWeight;
				double phraseTermWeight1 = termHighWeightMap.get(term1);
				double phraseTermWeight2 = termHighWeightMap.get(term2);
				switch (phraseQTWfnid) {
					case 1: combinedPhraseQTWWeight = 0.5 * phraseTermWeight1 + 0.5 * phraseTermWeight2;
					break;
					case 2: combinedPhraseQTWWeight = phraseTermWeight1 * phraseTermWeight2;
					break;
					case 3: combinedPhraseQTWWeight  = Math.min(phraseTermWeight1, phraseTermWeight2);
					break;
					case 4: combinedPhraseQTWWeight  = Math.max(phraseTermWeight1, phraseTermWeight2);
					break;
					default: combinedPhraseQTWWeight = 1.0d;
				}
				String newQueryString = "(" + term1 +" "+term2+ ")^"+Rounding.round(combinedPhraseQTWWeight, 4);
				System.out.println(newQueryString);
				sb.append(newQueryString+" ");
				foundValidTuple = true;
			}
		}
		sb.append("]");
		if (!foundValidTuple)
			sb = new StringBuilder();
		return sb.toString();		
	}
	
	public static void main(String[] args){
		if (args[0].equals("--generatetuplequery")){
			// --generatetuplequery indexPath indexPrefix onelinetopicfilename outputFilename
			TupleQuery apps = new TupleQuery();
			apps.generateTupleQuery(Index.createIndex(args[1], args[2]), args[3], args[4]);
		}
	}
	
}
