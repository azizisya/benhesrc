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
package uk.ac.gla.terrier.querying;
import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.termselector.TermSelector;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
/**
 * Implements automatic query expansion as PostFilter that is applied to the resultset
 * after 1st-time matching using explicit relevance information.
 * <B>Controls</B>
 * <ul><li><tt>qemodel</tt> : The query expansion model used for Query Expansion. 
 * Defauls to Bo1.</li></ul>
 * <B>Properties</B>
 * <ul><li><tt>expansion.terms</tt> : The maximum number of most weighted terms in the 
 * pseudo relevance set to be added to the original query. The system performs a conservative
 * query expansion if this property is set to 0. A conservation query expansion only reweighs
 * the original query terms without adding new terms to the query.</li>
 * <li><tt>expansion.documents</tt> : The number of top documents from the 1st pass 
 * retrieval to use for QE. The query is expanded from this set of docuemnts, also 
 * known as the relevance set.</li>
 * </ul>
 * @version $Revision: 1.1 $
 * @author Ben He
 */
public class ExplicitQueryExpansion extends QueryExpansion {
	protected static Logger logger = Logger.getRootLogger();
	/**
	 * Mapping from query id to identifiers of the positive feedback documents.
	 */
	protected THashMap<String, TIntHashSet> queryidRelDocumentMap;
	
	protected String[] queryids;
	/**
	* The default constructor of QueryExpansion.
	*/
	public ExplicitQueryExpansion() {
		super();
		String feedbackFilename = ApplicationSetup.getProperty("feedback.filename",
				ApplicationSetup.TERRIER_ETC+
				ApplicationSetup.FILE_SEPARATOR+"feedback");
		this.loadFeedbackInformation(feedbackFilename);
	}
	
	protected void loadFeedbackInformation(String filename){
		try{
			queryidRelDocumentMap = new THashMap<String, TIntHashSet>();
			BufferedReader br = Files.openFileReader(filename);
			//THashSet<String> queryids = new THashSet<String>();
			String line = null;
			String currentQueryid = "1st";
			TIntHashSet docidSet = new TIntHashSet();
			while ((line=br.readLine())!=null){
				line=line.trim();
				if (line.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(line);
				int[] relDocids = new int[stk.countTokens()-1];
				String queryid = stk.nextToken();
				stk.nextToken();// skip 0
				int docid = Integer.parseInt(stk.nextToken());
				int relevance = Integer.parseInt(stk.nextToken());
				
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					if (!queryidRelDocumentMap.containsKey(currentQueryid)){
						queryidRelDocumentMap.put(currentQueryid, new TIntHashSet(docidSet.toArray()));
					}
					currentQueryid = queryid;
					docidSet = new TIntHashSet();
				}
				if (relevance > 0) {docidSet.add(docid);}
			}
			if (!queryidRelDocumentMap.containsKey(currentQueryid)){
				queryidRelDocumentMap.put(currentQueryid, new TIntHashSet(docidSet.toArray()));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
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
		int[] relDocids = null;
		if (queryidRelDocumentMap.get(queryid)==null || queryidRelDocumentMap.get(queryid).toArray().length == 0){
			//logger.info("No relevant document found for feedback.");
			// return;
			/**
			 * An alternate option is to do psuedo relevance feedback
			 */
			logger.info("No relevant document found for feedback. PRF is applied.");
			int[] docids = resultSet.getDocids();
			int expDocs = Math.min(ApplicationSetup.EXPANSION_DOCUMENTS, resultSet.getExactResultSize());
			relDocids = new int[expDocs];
			System.arraycopy(docids, 0, relDocids, 0, expDocs);
		}else
			relDocids = queryidRelDocumentMap.get(queryid).toArray();
		// int relDocidsCount = relDocids.length;
		/* if (relDocidsCount == 0)
			return; */
		int[] docIDs = relDocids;
		
		ExpansionTerm[] expTerms = null;
		TermSelector selector = TermSelector.getDefaultTermSelector(index);
		/*if (QEModel.PerDocQE)
			expTerms = this.expandPerDoc(docIDs, query, numberOfTermsToReweight, index, QEModel, selector);
		else*/
			expTerms = this.expandFromDocuments(docIDs, query, numberOfTermsToReweight, index, QEModel, selector);	
		this.mergeWithExpandedTerms(expTerms, query);
	}
}
