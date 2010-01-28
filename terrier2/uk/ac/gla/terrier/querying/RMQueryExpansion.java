package uk.ac.gla.terrier.querying;

import gnu.trove.THashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.Hiemstra_LM;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class RMQueryExpansion extends QueryExpansion {

	private Idf idf = new Idf();
	// docId -> termId -> tf(termId, docId)
	private TIntObjectHashMap<TIntIntHashMap> directCache = new TIntObjectHashMap<TIntIntHashMap>();
	
	/** list of positive feedback documents */
	private THashMap<String, TIntArrayList> feedbackMap;
	
	/**
	 * Builds a map of (pseudo-)relevant feedback documents for a given query.
	 * 
	 * @param filename The name of the (qrels) feedback file.
	 */
	private void loadFeedback(String filename) {
		try {
			
			feedbackMap = new THashMap<String, TIntArrayList>();
			
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
				// look up doc id
				int docId = documentIndex.getDocumentId(docNo);
				// grab relevance judgment of docno with respect to this topic
				boolean relevant = Integer.parseInt(pieces[3]) > 0;
				
				// add topic entry to the feedback map
				if (!feedbackMap.contains(topId)) {
					feedbackMap.put(topId, new TIntArrayList());
				}
				
				// add docno to the appropriate feedback list for this topic
				if (relevant) {
					feedbackMap.get(topId).add(docId);
				}
			}
			
			br.close();
			
		} catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public TIntIntHashMap getDirectList(int docId) {
		if (!directCache.contains(docId)) {
			TIntIntHashMap postings = new TIntIntHashMap();
			int[][] list = directIndex.getTerms(docId);
			int size = list[0].length;
			for (int i = 0; i < size; i++) {
				postings.put(list[0][i], list[1][i]);
				
			}
			directCache.put(docId, postings);
		}
		return directCache.get(docId);
	}
	
	private double lambda = Double.parseDouble(ApplicationSetup.getProperty("expansion.lavrenko.lambda", "0.6"));
	// P(w|m) = lambda * tf(w,m)/length(m) + (1-lambda) * tf(w,G)/length(G)
//	private double termScore(double documentTF, double documentLength, double collectionTF, double collectionLength) {
//		double lambda = Double.parseDouble(ApplicationSetup.getProperty("expansion.lavrenko.lambda", "0.6"));
//		return lambda * (documentTF / documentLength) + (1 - lambda) * (collectionTF / collectionLength);
//	}
//	
//	private double termScore_log(double documentTF, double documentLength, double collectionTF, double collectionLength) {
//		return lambda * (documentTF / documentLength) + (1 - lambda) * (collectionTF / collectionLength);
//	}
	
	Hiemstra_LM lm = new Hiemstra_LM();
	
	@Override
	public void expandQuery(MatchingQueryTerms query, ResultSet resultSet) {
		lambda = Double.parseDouble(ApplicationSetup.getProperty("expansion.lavrenko.lambda", "0.6"));
		lm.setParameter(lambda);
		String feedbackFilename = ApplicationSetup.getProperty("feedback.filename", null); 
		boolean pseudo = (feedbackFilename == null);
		
		if (!pseudo && feedbackMap == null) {
			loadFeedback(feedbackFilename);
		}

		// the number of term to re-weight (i.e. to do relevance feedback) is
		// the maximum between the system setting and the actual query length.
		// if the query length is larger than the system setting, it does not
		// make sense to do relevance feedback for a portion of the query. Therefore, 
		// we re-weight the number of query length of terms.
		int termLimit = Math.max(ApplicationSetup.EXPANSION_TERMS, query.length());
		if (ApplicationSetup.EXPANSION_TERMS == 0) {
			termLimit = 0;
		}

		TIntArrayList docIdList;
		// number of document in the feedback set
		int docCount = 0;
		if (pseudo) {
			docCount = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS, resultSet.getResultSize());
			docIdList = new TIntArrayList();
			docIdList.add(resultSet.getDocids(), 0, docCount);
		}
		else {
			// current topic id
			String topId = query.getQueryId();
			docIdList = feedbackMap.get(topId);
			
			docCount = docIdList.size();			
		}
		
		// array of (pseudo-)relevant documents doc ids
		int[] docIds = docIdList.toNativeArray();
		
		// return in case there is no (pseudo-)relevance feedback evidence for this query
		if (docCount == 0) {
			return;
		}
		
		// COMPUTATION OF TERM WEIGHTS

		// uniform document prior probability : P(m)
//		double docPrior = (double) 1 / docCount;
		double logDocPrior = idf.log(1.0d / (double) docCount);

//		double colLen = collStats.getNumberOfTokens();
		lm.setAverageDocumentLength(collStats.getAverageDocumentLength());
		lm.setKeyFrequency(1);
		lm.setNumberOfDocuments(collStats.getNumberOfDocuments());
		lm.setNumberOfTokens(collStats.getNumberOfTokens());
//		System.out.println(">>> CALCULATING TERM PRIOR PROBABILITIES");
		
		// term prior probabilities : P(w) = SUM_{m in M} P(w|m) * P(m)
		TIntDoubleHashMap termPriors = new TIntDoubleHashMap();
		
		// for each feedback document
		for (int docId : docIds) {
			TIntIntHashMap directList = getDirectList(docId);
			
			double docLen = documentIndex.getDocumentLength(docId);
			
			// for each term in the document
			for (int termId : directList.keys()) {
				if (!termPriors.contains(termId)) {
					termPriors.put(termId, 0);
				}
				
				lexicon.findTerm(termId);
			
				double docTF = directList.get(termId);
				//double colTF = lexicon.getTF();
				
				
				//double weight = termScore(docTF, docLen, colTF, colLen) * docPrior;
				double weight = lm.score(docTF, docLen, lexicon.getNt(), lexicon.getTF(), 1) + logDocPrior;
//				System.out.printf(" prior: %f %f %f %f %f\n", docTF, docLen, colTF, colLen, docPrior);
				
				// increment term prior probability
				termPriors.adjustOrPutValue(termId, weight, weight);
			}
		}
		
		// number of unique terms in the feedback set 
		int termCount = termPriors.size();

		ExpansionTerm[] expansionTerms= new ExpansionTerm[termCount];
		double normalizer = 0;
		int i = 0;
		
//		System.out.println(">>> CALCULATING TERM POSTERIOR PROBABILITIES");
		
		// for each candidate term in the feedback set
		for (int expId : termPriors.keys()) {
			lexicon.findTerm(expId);
//			double colTFexp = lexicon.getTF();
			
			ExpansionTerm expansionTerm = new ExpansionTerm(expId);
			String expToken = lexicon.getTerm();
			expansionTerm.setToken(expToken);
		
			double outer_product = 0;
			
			// for each term in the original query
			for (String queToken : query.getTerms()) {
				lexicon.findTerm(queToken);
				int queId = lexicon.getTermId();
//				double colTFque = lexicon.getTF();
				lm.setKeyFrequency(1);
				lm.setTermFrequency(lexicon.getTF());
				lm.setDocumentFrequency(lexicon.getNt());
				
				double sum = 0;
		
				// for each document in the feedback set
				for (int docId : docIds) {
					
					double docTFexp = getDirectList(docId).get(expId);
					double docTFque = getDirectList(docId).get(queId);					
					
					double docLen = documentIndex.getDocumentLength(docId);
					
					double logPmw = lm.score(docTFexp, docLen) + termPriors.get(expId) - logDocPrior;
					//double Pmw = termScore(docTFexp, docLen, colTFexp, colLen) * termPriors.get(expId) / docPrior; 
//					System.out.printf(">> %s %f %f %f %f %f %f %f\n", expToken, docTFexp, docLen, colTFexp, colLen, termPriors.get(expId), docPrior, Pmw);
					double logPqm = lm.score(docTFque, docLen);
//					System.out.printf("   %s %f %f %f %f %f\n", queToken, docTFque, docLen, colTFque, colLen, Pqm);
					
					sum += Math.pow(logPmw + logPqm, 2);
					
				}
				outer_product += idf.log(sum);
			}

			// term posterior probabilities (expansion weights)
			// P(w,Q) = P(w) * PROD_{q in Q} SUM_{m in M} P(m|w) * P(q|m)
			//        = P(w) * PROD_{q in Q} SUM_{m in M} [P(w|m) * P(w) / P(m)] * P(q|m)
			double weight = Math.pow(termPriors.get(expId) + outer_product, 2);
			normalizer += weight;
			
			expansionTerm.setWeightExpansion(weight);
			
//			System.out.println("> CANDIDATE: " + expansionTerm.getToken() + " : " + weight);
//			System.out.println("   " + termPriors.get(expId) + " * " + prod);
			
			expansionTerms[i++] = expansionTerm;
		}
		
		for (ExpansionTerm term : expansionTerms) {
			term.setWeightExpansion((double) term.getWeightExpansion() / normalizer);
		}
		
		// sorts array by expansion weights (ExpansionTerm implements Comparable)
		Arrays.sort(expansionTerms);

		// adds expanded terms to the query
		for (int j = 0; j < termLimit; j++) {
//			System.out.println(">>> ADDED EXPANSION TERM : " + expansionTerms[j].getToken() + "^" + expansionTerms[j].getWeightExpansion());
			query.setTermProperty(expansionTerms[j].getToken(), expansionTerms[j].getTermID());
			query.addTermPropertyWeight(expansionTerms[j].getToken(), expansionTerms[j].getWeightExpansion());
		}	
		
	}
	
}
