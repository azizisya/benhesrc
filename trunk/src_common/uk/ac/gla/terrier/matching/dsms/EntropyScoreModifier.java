/*
 * Created on 23 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.dsms;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.BlockDocumentSetEntropy;

public class EntropyScoreModifier implements DocumentScoreModifier {
	protected BlockDocumentSetEntropy entropyApp = null;
	
	protected String docidOutputFilename = ApplicationSetup.getProperty("docid.output.filename", "/users/grad/ben/tr.ben/uniworkspace/etc/disk12/docids_1_2_output.gz");

	public EntropyScoreModifier() {
		
	}
	/**
	 * Scores needs to be sorted in descending order.
	 * @param scores
	 */
	private void normaliseScores(double[] scores, int resultSize){
		double normaliser = scores[0];
		for (int i=0; i<resultSize; i++)
			scores[i] /= normaliser;
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "EntropyScoreModifer";}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		/**
		 * The alpha parameter used for the interpolation of scores
		 */
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("entropy.dsms.alpha", "1"));
		double beta = Double.parseDouble(ApplicationSetup.getProperty("entropy.dsms.beta", "1"));
		double jm_lambda = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.lambda", "0.85d"));
		int topX = Math.min(Integer.parseInt(ApplicationSetup.getProperty("entropy.dsms.topx", "1000")), resultSet.getExactResultSize());
		
		if (this.entropyApp==null)
			entropyApp = new BlockDocumentSetEntropy(index);
		int counter = 0;
		int[] docids = resultSet.getDocids();
		double[] entropies = new double[topX];
		// get termids of query terms
		Lexicon lexicon = index.getLexicon();
		String[] terms = queryTerms.getTerms();
		int[] termids = new int[terms.length];
		for (int i=0; i<termids.length; i++)
			termids[i] = lexicon.getLexiconEntry(terms[i]).termId;
		// remove terms with high document frequencies
		termids = entropyApp.filterQueryTerms(termids);
		// compute mean entropies
		for (int i=0; i<topX; i++)
			entropies[i] = entropyApp.getMeanEntropy(termids, docids[i], jm_lambda);
		// normalise both scores and entropies
		double[] scores = resultSet.getScores();
		//this.normaliseScores(scores, resultSet.getExactResultSize()); this.normaliseScores(entropies, topX);
		// combine
		for (int i=0; i<topX; i++)
			scores[i] = alpha*scores[i]+beta*entropies[i];
		System.err.println("alpha: "+alpha+", beta: "+beta+", modified score of "+topX+" documents.");
		return true;
	}

}
