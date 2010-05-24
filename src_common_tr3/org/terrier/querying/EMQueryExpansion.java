	/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is ExplicitQueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package org.terrier.querying;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.terrier.learning.RFTrainer;
import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.querying.termselector.TermSelector;
import org.terrier.structures.ExpansionTerm;
import org.terrier.utility.ApplicationSetup;

import org.terrier.evaluation.TRECQrelsInMemory;
/**
 * Global document features are applied for the classification.
 * Top-returned documents are used as positive examples and the least ranked ones are used as negative examples.
 * @version $Revision: 1.1 $
 * @author Ben He
 */
public class EMQueryExpansion extends QueryExpansion {
	protected static Logger logger = Logger.getRootLogger();
	
	/** The number of results to output. */
	protected static int RESULTS_LENGTH = Integer.parseInt(ApplicationSetup.getProperty("trec.output.format.length", "1000"));
	
	protected RFTrainer trainer;
	/** Number of initial positive examples. */
	protected int initPos = Integer.parseInt(ApplicationSetup.getProperty("cot.init.pos", "3"));
	/** Number of initial negative examples.  */
	protected int initNeg = Integer.parseInt(ApplicationSetup.getProperty("cot.init.neg", "10"));
	
	protected int candRankLimit = Integer.parseInt(ApplicationSetup.getProperty("cot.cand.limit", "50"));
	
	protected TRECQrelsInMemory qrels;
	
	protected TRECQrelsInMemory qeQrels;
	
	/**
	* The default constructor of QueryExpansion.
	*/
	public EMQueryExpansion() {
		super();
		String trainerName = ApplicationSetup.getProperty("rf.trainer.name", "uk.ac.gla.terrier.learning.EMTrainer");
		String methodName = ApplicationSetup.getProperty("weka.classifier.name", "weka.classifiers.functions.Logistic");
		String args = ApplicationSetup.getProperty("weka.classifier.args", "").replaceAll(",", " ");
		qrels = new TRECQrelsInMemory(ApplicationSetup.getProperty("qrels.filename", "must be given"));
		qeQrels = new TRECQrelsInMemory(ApplicationSetup.getProperty("qeQrels.filename", "must be given"));
		
		logger.debug("trainer: "+trainerName);
		logger.debug("method: "+methodName);
		
		try{
			trainer = (RFTrainer)Class.forName(trainerName)
					.getConstructor(new Class[]{String.class, String.class, TRECQrelsInMemory.class})
					.newInstance(new Object[]{methodName, args, qeQrels});
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		// trainer = new EMTrainer(methodName, args, qrels);
		/*
		String feedbackFilename = ApplicationSetup.getProperty("feedback.filename",
				ApplicationSetup.TERRIER_ETC+
				ApplicationSetup.FILE_SEPARATOR+"feedback");
		this.loadFeedbackInformation(feedbackFilename);
		*/
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
		if (maximum-initPos-initNeg <= 0){
			logger.debug("Too few returned documents. Peform normal PRF.");
			int effDocuments = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS, docids.length);
			docIDs = Arrays.copyOf(docids, effDocuments);
		}else{
				
			int[] posDocids = new int[initPos];
			int[] negDocids = new int[initNeg];
			System.arraycopy(docids, 0, posDocids, 0, initPos);
			System.arraycopy(docids, maximum-initNeg-1, negDocids, 0, initNeg);
			int limit = Math.min(this.candRankLimit, maximum);
			int[] retDocids = new int[maximum];
			System.arraycopy(docids, 0, retDocids, 0, maximum);
			TIntHashSet retDocidSet = new TIntHashSet(docids);
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
				logger.debug("Document "+docid+" ranked at "+idx+" added to the feedback set. Rel: "+qeQrels.isRelevant(queryid, ""+docid));
			}
			fbDocidSet.clear(); fbDocidSet = null; retDocidSet.clear(); retDocidSet = null;
			docidRankMap.clear(); docidRankMap = null; posCandSet.clear(); posCandSet = null; posCand = null;
		}
		
		TermSelector selector = TermSelector.getDefaultTermSelector(lastIndex);
		selector.mergeWithQuery(QEModel, query, numberOfTermsToReweight);
		
		ExpansionTerm[] expTerms = null;
		/*if (QEModel.PerDocQE)
			expTerms = this.expandPerDoc(feedbackDocIDs, query, numberOfTermsToReweight, index, QEModel, selector);
		else*/
		//	expTerms = this.expandFromDocuments(docIDs, query, numberOfTermsToReweight, lastIndex, QEModel, selector);
		// selector.m.mergeWithExpandedTerms(expTerms, query);
		
	}
	
}
