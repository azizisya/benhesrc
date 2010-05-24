package org.terrier.querying;

import java.util.Arrays;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.querying.termselector.TermSelector;
import org.terrier.structures.ExpansionTerm;
import org.terrier.utility.ApplicationSetup;

import org.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.utility.TroveUtility;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

/**
 * Global document features are applied for the classification.
 * Top-returned documents are used as positive examples and the least ranked ones are used as negative examples.
 * @version $Revision: 1.1 $
 * @author Ben He
 */
public class EMExplicitQueryExpansion extends EMQueryExpansion {
	/** map from query id to positive docids */
	private THashMap<String, int[]> posDocMap;
	/** map from query id to negative docids */
	private THashMap<String, int[]> negDocMap;

	public EMExplicitQueryExpansion() {
		super();
		String feedbackFilename = ApplicationSetup.getProperty("feedback.filename",
				ApplicationSetup.TERRIER_ETC+
				ApplicationSetup.FILE_SEPARATOR+"feedback");
		this.loadFeedbackDocuments(feedbackFilename);
	}
	
	private void loadFeedbackDocuments(String filename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(filename);
		String[] qids = qrels.getQueryids();
		posDocMap = new THashMap<String, int[]>();
		negDocMap = new THashMap<String, int[]>();
		for (String qid : qids){
			int[] posDocids = TroveUtility.stringArrayToIntArray(qrels.getRelevantDocumentsToArray(qid));
			int[] negDocids = TroveUtility.stringArrayToIntArray(qrels.getNonRelevantDocumentsToArray(qid));
			posDocMap.put(qid, posDocids);
			negDocMap.put(qid, negDocids);
		}
	}
	
	/**
 	* This method implements the functionality of expanding a query.
 	* @param query MatchingQueryTerms the query terms of 
 	*		the original query.
 	* @param resultSet CollectionResultSet the set of retrieved 
 	*		documents from the first pass retrieval.
 	*/
	public void expandQuery(MatchingQueryTerms query, ResultSet resultSet) {
		// the number of term to re-weight (i.e. to do relevance feedback) is
		// the maximum between the system setting and the actual query length.
		// if the query length is larger than the system setting, it does not
		// make sense to do relevance feedback for a portion of the query. Therefore, 
		// we re-weight the number of query length of terms.
		int numberOfTermsToReweight = Math.max(ApplicationSetup.EXPANSION_TERMS, 
				query.length());
		if (ApplicationSetup.EXPANSION_TERMS == 0)
			numberOfTermsToReweight = 0;

		// get docids of the feedback documents.
		// pseudo relevance set is replaced by the docs specified in the feedback file.
		String queryid = query.getQueryId();
		
		
		// do EM training to obtain feedback documents
		final int maximum = 
			RESULTS_LENGTH > resultSet.getResultSize() || RESULTS_LENGTH == 0
			? resultSet.getResultSize()
			: RESULTS_LENGTH;
		int[] docIDs = null; int[] docids = resultSet.getDocids();
		
		int[] posDocids = posDocMap.get(query.getQueryId());
		/** 2009/09/23: negative examples are taken from the least ranked documents. */
		int[] negDocids = negDocMap.get(query.getQueryId());
	
		// int[] negDocids = new int[initNeg];
		// System.arraycopy(docids, maximum-initNeg-1, negDocids, 0, initNeg);
		
		if (posDocids == null){
			logger.debug("No feedback documents. Peform normal PRF.");
			int effDocuments = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS, docids.length);
			docIDs = Arrays.copyOf(docids, effDocuments);
		}else if (negDocids == null){
			logger.debug("No negative feedback documents given. Use least ranked documents for training.");
			negDocids = new int[initNeg];
			System.arraycopy(docids, maximum-initNeg-1, negDocids, 0, initNeg);
		}else{
			int limit = Math.min(this.candRankLimit, maximum);
			int[] retDocids = new int[maximum];
			System.arraycopy(docids, 0, retDocids, 0, maximum);
			TIntHashSet retDocidSet = new TIntHashSet(retDocids);
			retDocidSet.removeAll(posDocids); retDocidSet.removeAll(negDocids);
			// retDocids = retDocidSet.toArray();
			TIntIntHashMap docidRankMap = new TIntIntHashMap();
			for (int i=0; i<limit; i++)
				docidRankMap.put(docids[i], i);
			
			int[] posCand = new int[limit];
			System.arraycopy(docids, 0, posCand, 0, limit);
			TIntHashSet posCandSet = new TIntHashSet(posCand);
			posCandSet.removeAll(posDocids); posCandSet.removeAll(negDocids);
			
			// sort retDocids with ranking
			retDocids = new int[retDocidSet.size()];
			int counter = 0;
			for (int i=0; i<docids.length; i++){
				if (retDocidSet.contains(docids[i])){
					retDocids[counter++] = docids[i];
					if (counter == retDocids.length)
						break;
				}
			}
			
			docIDs = trainer.run(queryid, posDocids, negDocids, retDocids, posCandSet, ApplicationSetup.TREC_RESULTS).get(0);
			TIntHashSet fbDocidSet = new TIntHashSet(docIDs);
			fbDocidSet.removeAll(posDocids);
			for (int docid : fbDocidSet.toArray()){
				int idx = docidRankMap.get(docid);
				logger.debug("Document "+docid+" ranked at "+idx+" added to the feedback set. Rel: "+qrels.isRelevant(queryid, ""+docid));
			}
		}
		
		TermSelector selector = TermSelector.getDefaultTermSelector(lastIndex);
		selector.mergeWithQuery(QEModel, query, numberOfTermsToReweight);
		
		// ExpansionTerm[] expTerms = null;
		/*if (QEModel.PerDocQE)
			expTerms = this.expandPerDoc(feedbackDocIDs, query, numberOfTermsToReweight, index, QEModel, selector);
		else*/
			//expTerms = this.expandFromDocuments(docIDs, query, numberOfTermsToReweight, lastIndex, QEModel, selector);
		// this.mergeWithExpandedTerms(expTerms, query);
	}
}
