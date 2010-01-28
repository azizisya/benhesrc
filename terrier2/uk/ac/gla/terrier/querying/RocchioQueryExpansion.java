package uk.ac.gla.terrier.querying;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * 
 * 
 * @author rodrygo
 */
public class RocchioQueryExpansion extends QueryExpansion {

	/** Lists of feedback documents mapped to each topic */
	private THashMap<String, Feedback> feedbackMap;
	private double alpha;
	private double beta;
	private double gamma;
	private boolean EXPAND_NEG_TERMS = Boolean.parseBoolean(ApplicationSetup.getProperty("expand.negative.terms", "false"));
	private boolean EXPAND_POS_TERMS = Boolean.parseBoolean(ApplicationSetup.getProperty("expand.positive.terms", "true"));

	/**
	 * Class for encapsulating feedback documents for a given topic.
	 * 
	 * @author rodrygo
	 */
	private class Feedback {
		/** list of positive feedback documents */
		private TIntHashSet positiveDocs;
		/** list of negative feedback documents */
		private TIntHashSet negativeDocs;
		
		public Feedback() {
			positiveDocs = new TIntHashSet();
			negativeDocs = new TIntHashSet();
		}
		
		public TIntHashSet getPositiveDocs() {
			return positiveDocs;
		}
		
		public TIntHashSet getNegativeDocs() {
			return negativeDocs;
		}
	}
	
	public RocchioQueryExpansion() {
		this.alpha = Double.parseDouble(ApplicationSetup.getProperty("rocchio_alpha", "1d"));
		this.beta = Double.parseDouble(ApplicationSetup.getProperty("rocchio_beta", "0.75d"));
		this.gamma = Double.parseDouble(ApplicationSetup.getProperty("rocchio_gamma", "0.15d"));
		loadFeedback(ApplicationSetup.getProperty("feedback.filename", ApplicationSetup.TERRIER_ETC + ApplicationSetup.FILE_SEPARATOR + "feedback"));
	}
	
	private void loadFeedback(String filename) {		
		logger.debug("Loading feedback information from "+filename+"...");
		try {
			feedbackMap = new THashMap<String, Feedback>();
			
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			// for each line in the feedback (qrels) file
			while ((line = br.readLine()) != null){
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				
				// split line into space-separated pieces
				String[] pieces = line.split("\\s+");
				
				// grab topic id
				String topId = pieces[0];		
				// grab docno
				String docNo = pieces[2];
				// grab relevance judgment of docno with respect to this topic
				boolean relevant = Integer.parseInt(pieces[3]) > 0;
				
				// add topic entry to the feedback map
				if (!feedbackMap.contains(topId)) {
					feedbackMap.put(topId, new Feedback());
				}
				
				// add docno to the appropriate feedback list for this topic
				if (relevant) {
					feedbackMap.get(topId).getPositiveDocs().add(Integer.parseInt(docNo));
				}
				else {
					feedbackMap.get(topId).getNegativeDocs().add(Integer.parseInt(docNo));
				}
			}
			
			br.close();
			
		} catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Re-weigh original query terms and possibly expand them with the highest
	 * scored terms in the relevant set considered according to the Rocchio's
	 * algorithm.
	 */
	public void expandQuery(MatchingQueryTerms query, ResultSet resultSet) {
		this.alpha = Double.parseDouble(ApplicationSetup.getProperty("rocchio_alpha", "1d"));
		this.beta = Double.parseDouble(ApplicationSetup.getProperty("rocchio_beta", "0.75d"));
		this.gamma = Double.parseDouble(ApplicationSetup.getProperty("rocchio_gamma", "0.15d"));
		if (QEModel.PARAMETER_FREE)
			logger.debug("Parameter-free query expansion");
		logger.debug("alpha: "+alpha+", beta: "+beta+", gamma: "+gamma);
		
		// the number of term to re-weight (i.e. to do relevance feedback) is
		// the maximum between the system setting and the actual query length.
		// if the query length is larger than the system setting, it does not
		// make sense to do relevance feedback for a portion of the query. Therefore, 
		// we re-weight the number of query length of terms.
		/**
		 * 2008/07/29, Ben wrote: this criteria has been revoked. Query terms can be reweighed no matter how
		 * long the query is.
		 */
		//int numberOfTermsToReweight = Math.max(ApplicationSetup.EXPANSION_TERMS, query.length());
		//if (ApplicationSetup.EXPANSION_TERMS == 0) {
			//numberOfTermsToReweight = 0;
		//}

		// current topic id
		String topId = query.getQueryId();
		// get docnos from the positive feedback documents for this query
		int[] positiveDocids = feedbackMap.get(topId).getPositiveDocs().toArray();
		// get docnos from the negative feedback documents for this query
		int[] negativeDocids = feedbackMap.get(topId).getNegativeDocs().toArray();

		// if there is no positive feedback for this query
		if (positiveDocids.length == 0) {
			logger.info("No relevant document found for feedback.");
			return;
			/**
			 * An alternate option is to do psuedo relevance feedback
			 */
		}
		
		int positiveCount = positiveDocids.length;
		int negativeCount = negativeDocids.length;
		
		System.out.println("# POSITIVE DOCS: " + positiveCount);
		System.out.println("# NEGATIVE DOCS: " + negativeCount);
		
		// return in case there is no (pseudo-)relevance feedback evidence for this query
		if (positiveCount == 0 && negativeCount == 0) {
			return;
		}

		// --------------------------------------------------------------------		
		// COMPUTATION OF POSITIVE WEIGHTS ------------------------------------
		// --------------------------------------------------------------------
		
		// get total number of tokens in positive documents
		double positiveDocLength = 0;
		for (int i = 0; i < positiveCount; i++){
			positiveDocLength += documentIndex.getDocumentLength(positiveDocids[i]);
		}

		
		ExpansionTerms positiveCandidateTerms = new ExpansionTerms(collStats, positiveDocLength, lexicon);
		// get all terms in positive documents as candidate expansion terms
		// for each positive feedback document
		for (int i = 0; i < positiveCount; i++) {
			int[][] terms = directIndex.getTerms(positiveDocids[i]);
			if (terms == null) {
				logger.warn("document "+documentIndex.getDocumentLength(positiveDocids[i]) + "("+positiveDocids[i]+") not found");
			}
			else {
				// for each term in the document
				for (int j = 0; j < terms[0].length; j++) {
					// add term id, term frequency to the list of candidate expansion terms
					positiveCandidateTerms.insertTerm(terms[0][j], (double)terms[1][j]);
				}
			}
		}
		
		System.out.println("# UNIQUE TERMS IN POSITIVE DOCS: " + positiveCandidateTerms.getNumberOfUniqueTerms());
		
		// mark original query terms in the set of candidate expansion terms
		positiveCandidateTerms.setOriginalQueryTerms(query);
		// get list of all candidate expansion terms in positive documents with their respective expansion weights
		TIntObjectHashMap<ExpansionTerm> positiveQueryTerms = 
			positiveCandidateTerms.getExpandedTermHashSet(positiveCandidateTerms.getNumberOfUniqueTerms(), QEModel);

		// --------------------------------------------------------------------
		// COMPUTATION OF NEGATIVE WEIGHTS ------------------------------------
		// --------------------------------------------------------------------
		
		// get total number of tokens in negative documents
		double negativeDocLength = 0;
		for (int i = 0; i < negativeCount; i++){
				negativeDocLength += documentIndex.getDocumentLength(negativeDocids[i]);
		}

		
		ExpansionTerms negativeCandidateTerms = new ExpansionTerms(collStats, negativeDocLength, lexicon);
		// get all terms in negative documents as candidate expansion terms
		// for each negative feedback document
		for (int i = 0; i < negativeCount; i++) {
			int[][] terms = directIndex.getTerms(negativeDocids[i]);
			if (terms == null) {
				logger.warn("document "+documentIndex.getDocumentLength(negativeDocids[i]) + "("+negativeDocids[i]+") not found");
			}
			else {
				// for each term in the document
				for (int j = 0; j < terms[0].length; j++) {
					// add term id, term frequency to the list of candidate expansion terms
					negativeCandidateTerms.insertTerm(terms[0][j], (double)terms[1][j]);
				}
			}
		}
		
		System.out.println("# UNIQUE TERMS IN NEGATIVE DOCS: " + negativeCandidateTerms.getNumberOfUniqueTerms());
		
		// mark original query terms in the set of candidate expansion terms
		negativeCandidateTerms.setOriginalQueryTerms(query);
		// get list of all candidate expansion terms in negative documents with their respective expansion weights
		TIntObjectHashMap<ExpansionTerm> negativeQueryTerms = 
			negativeCandidateTerms.getExpandedTermHashSet(negativeCandidateTerms.getNumberOfUniqueTerms(), QEModel);
		
		
		// --------------------------------------------------------------------
		// COMBINED WEIGHTS ---------------------------------------------------
		// --------------------------------------------------------------------
		
		// temporary structure for merging positiveQueryTerms and negativeQueryTerms
		TIntObjectHashMap<ExpansionTerm> queryTerms = new TIntObjectHashMap<ExpansionTerm>();
		
		// put all positive query term ids
		for (Integer k : positiveQueryTerms.keys()) {
			queryTerms.put(k, null);
		}
		// put all negative query term ids
		for (Integer k : negativeQueryTerms.keys()) {
			queryTerms.put(k, null);
		}
		
		System.out.println("# UNIQUE TERMS IN ALL DOCS: " + queryTerms.size());
		
		for (Integer k : queryTerms.keys()) {	
			lexicon.findTerm(k);
			double queWeight = query.getTermWeight(lexicon.getTerm());
			double posWeight = positiveQueryTerms.contains(k) ? positiveQueryTerms.get(k).getWeightExpansion() : 0;
			double negWeight = negativeQueryTerms.contains(k) ? negativeQueryTerms.get(k).getWeightExpansion() : 0;
			
			ExpansionTerm t = new ExpansionTerm(k);
			t.setWeightExpansion(alpha * queWeight + beta * posWeight - gamma * negWeight);
			///t.setWeightExpansion(alpha * queWeight + posWeight + negWeight);
			//if (queWeight!=0)
				//System.out.println("queWeight: "+queWeight + ", posWeight: "+posWeight + ", negWeight: "+negWeight);
			queryTerms.put(k, t);
			if (positiveQueryTerms.containsKey(k))				
				positiveQueryTerms.put(k, t);
			if (negativeQueryTerms.containsKey(k))				
				negativeQueryTerms.put(k, t);
		}
		
		// convert merging structure into array
		//ExpansionTerm[] termArr = queryTerms.values().toArray(new ExpansionTerm[queryTerms.size()]);
		// sort array by expansion weights (ExpansionTerm implements Comparable)
		//Arrays.sort(termArr);
		String[] originalQueryTerms = query.getTerms();
		THashSet<String> queryTermSet = new THashSet<String>();
		for (int i=0; i<originalQueryTerms.length; i++)
			queryTermSet.add(originalQueryTerms[i]);
		// for each of the top numberOfTermsToReweight expanded terms
		if (EXPAND_POS_TERMS){
			/**
			 * 2008/07/30 Ben: Assign weights for positive and negative document sets
			 * seperately.
			 */
			
			ExpansionTerm[] posTermArr = new ExpansionTerm[positiveQueryTerms.size()];
			int posCounter = 0;
			for (int k : positiveQueryTerms.keys())
				posTermArr[posCounter++] = positiveQueryTerms.get(k);
			Arrays.sort(posTermArr);
			int numberOfTermsToReweigh = Math.min(posTermArr.length, ApplicationSetup.EXPANSION_TERMS);
			for (int i = 0; i < numberOfTermsToReweigh; i++){
				// add final expanded term as a query term
				int termid = posTermArr[i].getTermID();
				lexicon.findTerm(termid);
				
				query.setTermProperty(lexicon.getTerm(), 
						posTermArr[i].getWeightExpansion());
			
				if(logger.isDebugEnabled()){
					logger.debug("term " + lexicon.getTerm()
				 		+ " appears in expanded query with normalised weight: "
						+ Rounding.toString(query.getTermWeight(lexicon.getTerm()), 4));
				}
				if (queryTermSet.contains(lexicon.getTerm()) &&
            			numberOfTermsToReweigh<posTermArr.length)
            		numberOfTermsToReweigh++;
				else
					queryTermSet.add(lexicon.getTerm());
				//System.out.println(">> expanded term " + lexicon.getTerm() + " with Rocchio weight " + Rounding.toString(termArr[i].getWeightExpansion(), 4));
			}
		}
		if (EXPAND_NEG_TERMS){
			
			//ExpansionTerm[] negTermArr = (ExpansionTerm[])negativeQueryTerms.getValues();
			ExpansionTerm[] negTermArr = new ExpansionTerm[negativeQueryTerms.size()];
			int negCounter = 0;
			for (int k : negativeQueryTerms.keys())
				negTermArr[negCounter++] = negativeQueryTerms.get(k);	
			Arrays.sort(negTermArr);
			int numberOfTermsToReweigh = Math.min(negTermArr.length, ApplicationSetup.EXPANSION_TERMS/2);
			/*
			 * 2008/07/30 Ben:
			 * Expand the query with terms from the negative document set. The number of expanded terms is half of
			 * that from the positive document set.
			 */
			for (int i=negTermArr.length-1; i>=negTermArr.length-numberOfTermsToReweigh; i--){
				// add final expanded term as a query term
            	lexicon.findTerm(negTermArr[i].getTermID());
            	// Only terms with negative weights are added.
            	if (!queryTermSet.contains(lexicon.getTerm()) && negTermArr[i].getWeightExpansion() < 0){
            		query.setTermProperty(lexicon.getTerm(), negTermArr[i].getWeightExpansion());

            		if(logger.isDebugEnabled()){
            				logger.debug("term " + lexicon.getTerm()
            						+ " appears in expanded query with normalised weight: "
            						+ Rounding.toString(query.getTermWeight(lexicon.getTerm()), 4));
            		}
            		queryTermSet.add(lexicon.getTerm());
            	}
            	else if (numberOfTermsToReweigh<negTermArr.length)
            		numberOfTermsToReweigh++;
            		
            	//System.out.println(">> expanded term " + lexicon.getTerm() + " with Rocchio weight " + Rounding.toString(termArr[i].getWeightExpansion(), 4));
			}
		}
	}

}
