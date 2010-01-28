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
 * The Original Code is ExplicitFieldQueryExpansion.java.
 *
 * The Original Code is Copyright (C) 2004-2007 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.parser.SingleTermQuery;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.utility.ApplicationSetup;
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
public class ExplicitFieldQueryExpansion extends FieldQueryExpansion {
	protected static Logger logger = Logger.getRootLogger();
	
	protected THashMap queryidRelDocumentMap;
	
	protected String[] queryids;
	/**
	* The default constructor of QueryExpansion.
	*/
	public ExplicitFieldQueryExpansion() {
		super();
		String feedbackFilename = ApplicationSetup.getProperty("feedback.filename",
				ApplicationSetup.TERRIER_ETC+
				ApplicationSetup.FILE_SEPARATOR+"feedback");
		this.loadRelevanceInformation(feedbackFilename);
	}
	
	private void loadRelevanceInformation(String filename){
		try{
			queryidRelDocumentMap = new THashMap();
			BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
			THashSet queryids = new THashSet();
			String line = null;
			while ((line=br.readLine())!=null){
				line=line.trim();
				if (line.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(line);
				String[] relDocnos = new String[stk.countTokens()-1];
				String queryid = stk.nextToken();
				queryids.add(queryid);
				for (int i=0; i<relDocnos.length; i++)
					relDocnos[i]=stk.nextToken();
				this.queryidRelDocumentMap.put(queryid, relDocnos);
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

		// get docids of the feedback documents
		String queryid = query.getQueryId();
		String[] relDocnos = (String[])queryidRelDocumentMap.get(queryid);
		if (relDocnos==null){
			int[] docids = resultSet.getDocids();
			relDocnos = new String[ApplicationSetup.EXPANSION_DOCUMENTS];
			for (int i=0; i<relDocnos.length; i++)
				relDocnos[i] = documentIndex[0].getDocumentNumber(docids[i]);
		}
		int relDocnosCount = relDocnos.length;
		if (relDocnosCount == 0)
			return;
		int[] docIDs = new int[relDocnosCount];
		for (int i=0; i<relDocnosCount; i++)
			docIDs[i] = documentIndex[0].getDocumentId(relDocnos[i]); 
		
		double totalDocumentLength = 0;
		
		int effDocuments = docIDs.length;
		for (int i = 0; i < effDocuments; i++){
			totalDocumentLength += this.getDocumentLength(docIDs[i]);
			if(logger.isDebugEnabled()){
				logger.debug((i+1)+": " + relDocnos[i]+
						" ("+docIDs[i]+")");
			}
		}
		ExpansionTerms expansionTerms = new ExpansionTerms(
				numberOfDocuments, 
				numberOfTokens, 
				averageDocumentLength, 
				totalDocumentLength, 
				globalLexicon);
		for (int i = 0; i < effDocuments; i++) {
			// GLOBAL termids of terms in the pseudo relevance set
			TIntHashSet termidSet = new TIntHashSet();
			TIntDoubleHashMap termidFrequencyMap = new TIntDoubleHashMap();
			
			this.getTerms(docIDs[i], termidSet, termidFrequencyMap);
			int[] termids = termidSet.toArray();
			int termCount = termids.length;
			for (int j=0; j<termCount; j++){
				expansionTerms.insertTerm(termids[j], termidFrequencyMap.get(termids[j]));
				//System.out.println("termid "+termids[j]+", tf "+termidFrequencyMap.get(termids[j]));
			}
		}
		expansionTerms.setOriginalQueryTerms(query);
		SingleTermQuery[] expandedTerms =
			expansionTerms.getExpandedTerms(numberOfTermsToReweight, QEModel);
		for (int i = 0; i < expandedTerms.length; i++){
			SingleTermQuery expandedTerm = expandedTerms[i];
			query.addTermPropertyWeight(expandedTerm.getTerm(), expandedTerm.getWeight());
			if(logger.isDebugEnabled()){
				logger.debug("term " + expandedTerms[i].getTerm()
				 	+ " appears in expanded query with normalised weight: "
					+ Rounding.toString(query.getTermWeight(expandedTerms[i].getTerm()), 4));
			}
		}
			

	}
}
