package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntIntHashMap;
import java.util.Arrays;
import java.util.ArrayList;
import uk.ac.gla.terrier.statistics.GammaFunction;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Distance;
import uk.ac.gla.terrier.utility.FindTuples;
import uk.ac.gla.terrier.structures.DocumentIndex;


/**
 * Modifies the scores of the documents using n-grams, approximating
 * the dependence of terms between documents.
 * <p>
 * implement as a document score modifier, similarly to PhraseScoreModifier
 *
 * For each pair of query terms
 * do
 *	for each document
 *	do
 *		find number of times for which the terms co-occur in an n-gram and compute using the binomial and the laplace aftereffect
 * 		add the score to the corresponding document
 *	done
 * done
 *
 * <p>
 * <b>Properties</b>
 * <ul>
 * <li><tt>proximity.dependency.type</tt> - one of SD, FD for sequential dependence or full dependence</li>
 * <li><tt>proximity.ngram.length</tt> - proxmity windows for FD, in tokens</li>
 * <li><tt>proximity.ngram.SD.length</tt> - proximity windows for SD, in tokens</li>
 * <li><tt>proximity.w_t</tt> - weight of unigram in combination, defaults 1.0d</li>
 * <li><tt>proximity.w_o</tt> - weight of SD in combination, default 1.0d</li>
 * <li><tt>proximity.w_u</tt> - weight of FD in combination, default 1.0d</li>
 * <li><tt>proximity.high.weight.terms.only</tt> - only consider query terms with weight &gt;=1.0d. This maybe useful
 * for applications with QE or collection enrichment enabled. defaults to false.</li>
 * <li><tt>proximity.qtw.fnid</tt> - combination function to combine the qtws of two terms involved in a phrase. See below.</li>
 * </ul>
 * <p><b>QTW Combination Functions</b>
 * <ol>
 * <li><tt>1</tt>: phraseQTW = 0.5 * (qtw1 + qtw2)</li>
 * <li><tt>2</tt>: phraseQTW = qtw1 * qtw2</li>
 * <li><tt>3</tt>: phraseQTW = min(qtw1, qtw2)</li>
 * <li><tt>4</tt>: phraseQTW = max(qtw1, qtw2)</li>
 * </ol>
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class ProximityScoreModifierRF2008 extends ProximityScoreModifierTREC2008 {
	protected FindTuples tupleFinder = new FindTuples(
			ApplicationSetup.getProperty("tuple.index.path", "/local/terrier/Indices/Wikipedia/2008/pfn/full/title/"),
			ApplicationSetup.getProperty("tuple.index.prefix", "data")
			);
	
	public Object clone()
	{
		return new ProximityScoreModifierRF2008(phraseTerms);
	}

	protected String[] phraseTerms;

	public ProximityScoreModifierRF2008() {
		super();
	} 
	public ProximityScoreModifierRF2008(final String[] pTerms) {
		super(pTerms);
		phraseTerms = pTerms;
	}

	public ProximityScoreModifierRF2008(final String[] pTerms, boolean r) {
		this(pTerms);
	}

	/**
	 * Returns the name of the modifier.
	 * 
	 * @return String the name of the modifier.
	 */
	public String getName() {
		return "ProximityScoreModifierRF2008";
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
			System.err.println("ERROR: Wrong function id specified for ProximityScoreModifierRF2008");
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
		
		w_t = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_t","1.0d"));

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
		
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","1.0d"));
		
		if (dependency.equals("FD")){
			w_u = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_u","1.0d"));
			System.err.println("w_t: "+w_t+", w_u: "+w_u+", fnid: "+phraseQTWfnid+", ngramc: "+ngramC);
		}
		else if (dependency.equals("SD")){
			w_o = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_o","1.0d"));
			System.err.println("w_t: "+w_t+", w_o: "+w_o+", fnid: "+phraseQTWfnid+", ngramc: "+ngramC);
		}

		if (dependency.equals("FD"))
		{
			for (int i=0; i<phraseLength-1; i++) {
				for (int j=i+1; j<phraseLength; j++) {
					term1 = phraseTerms[i];
					term2 = phraseTerms[j];
					if (tupleFinder.findTuple(term1, term2)){
						System.out.println("Tuple ("+term1+", "+term2+") is valid");
						this.computeFDScore(term1, term2, scores, score_u, docids, 
							phraseTermWeights[i], phraseTermWeights[j], ngramC, ngramLength, index);
					}else{
						System.out.println("Tuple ("+term1+", "+term2+") ignored from querying");
					}
	   			}
			}
		
			for (int k=0; k<docids.length; k++) {		
				scores[k] =  w_t * scores[k] +  w_u * score_u[k] ;		
			}
			
	  		postingsCache.clear();
			 		   	  
	  	}
		else if (dependency.equals("SD")){
			for (int i=0; i<phraseLength-1; i++) {
			for (int j=i+1; j<phraseLength; j++) {
				term1 = phraseTerms[i];
				term2 = phraseTerms[j];
				if (tupleFinder.findTuple(term1, term2)){
					System.out.println("Tuple ("+term1+", "+term2+") is valid");
					this.computeFDScore(term1, term2, scores, score_u, docids, 
							phraseTermWeights[i], phraseTermWeights[j], ngramC, ngramLength, index);
				}else{
					System.out.println("Tuple ("+term1+", "+term2+") ignored from querying");
				}
   			}
		}
	
		for (int k=0; k<docids.length; k++) {		
			scores[k] =  w_t * scores[k] +  w_o * score_u[k] ;		
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
