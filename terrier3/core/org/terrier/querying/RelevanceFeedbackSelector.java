/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is RelevanceFeedbackSelector.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.querying;
import gnu.trove.THashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.terrier.structures.Index;
import org.terrier.structures.MetaIndex;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
/** Selects feedback documents from a qrels file, using the query id. NB, will select
  * all documents, irrespective of relevance.
  * <p>
  * <b>Properties:</b>
  * <ul><li><tt>qe.feedback.filename</tt> - filename of qrels file to use for feedback.</li>
  * </ul>
  * @since 3.0
  * @author Craig Macdonald 
  */
public class RelevanceFeedbackSelector extends FeedbackSelector
{
	protected static final Logger logger = Logger.getLogger(RelevanceFeedbackSelector.class);
	protected MetaIndex metaIndex;
	protected THashMap<String, List<FeedbackWithDocno>> queryidRelDocumentMap;

	static class FeedbackWithDocno extends FeedbackDocument
	{
		String docno;

		
		@Override
		public boolean equals(Object y) {
			if (! (y instanceof FeedbackDocument))
				return false;
			FeedbackWithDocno o = (FeedbackWithDocno)y;
			if (docno.equals(o.docno))
				return true;
			return super.docid == o.docid;
		}

		@Override
		public int hashCode() {
			return docno.hashCode();
		}
	}

	public RelevanceFeedbackSelector()
	{	
		String feedbackFilename = ApplicationSetup.getProperty("qe.feedback.filename",
				ApplicationSetup.TERRIER_ETC+
				ApplicationSetup.FILE_SEPARATOR+"feedback");
		this.loadRelevanceInformation(feedbackFilename);
	}

	public void setIndex(Index index){
		metaIndex = index.getMetaIndex();
	}

	public FeedbackDocument[] getFeedbackDocuments(Request request)
	{
		// get docids of the feedback documents
		String queryid = request.getQueryID();
		List<FeedbackWithDocno> list = queryidRelDocumentMap.get(queryid);
		//deal with undefined case
		if (list == null)
			return null;
		//dela with empty case
		if (list.size() == 0)
			return new FeedbackDocument[0];
		final List<FeedbackDocument> rtrList = new ArrayList<FeedbackDocument>(list.size());
		for(FeedbackWithDocno doc: list)
		{
			try{
				doc.docid = metaIndex.getDocument("docno", doc.docno);
			} catch (IOException ioe) {
				logger.warn("IOException while looking for docid for feedback document "+doc.docno+" of query "+ request.getQueryID(), ioe);
			}
			if (doc.docid < 0)
			{
				logger.warn("Could not find docid for feedback document "+doc.docno+" of query "+ request.getQueryID());
				continue;
			}
			doc.score = -1;
			doc.rank = -1;
			logger.info("("+(rtrList.size()+1)+") Feedback document:"+doc.docno);
			rtrList.add(doc);
		}
		logger.info("Found "+(rtrList.size())+" feedback documents for query "+request.getQueryID());
		return rtrList.toArray(new FeedbackDocument[0]);
	}

	private void loadRelevanceInformation(String filename){
		logger.info("Loading relevance feedback assessments from "+ filename);
		try{
			queryidRelDocumentMap = new THashMap<String, List<FeedbackWithDocno>>();
			BufferedReader br = Files.openFileReader(filename);
			//THashSet<String> queryids = new THashSet<String>();
			String line = null;
			int assessmentsCount =0;
			while ((line=br.readLine())!=null){
				line=line.trim();
				if (line.length()==0)
					continue;
				String[] parts = line.split("\\s+");
				FeedbackWithDocno doc = new FeedbackWithDocno();
				doc.docno = parts[2];
				doc.relevance = Byte.parseByte(parts[3]);
				
				List<FeedbackWithDocno> list = queryidRelDocumentMap.get(parts[0]);
				if (list == null)
				{
					queryidRelDocumentMap.put(parts[0], list = new ArrayList<FeedbackWithDocno>());
				}
				list.add(doc);
				assessmentsCount++;
			}
			br.close();
			logger.info("Total "+ assessmentsCount+ " assessments found");
		}catch(IOException ioe){
			logger.error("Problem loading relevance feedback assessments from "+ filename, ioe);
		}
	}
}
