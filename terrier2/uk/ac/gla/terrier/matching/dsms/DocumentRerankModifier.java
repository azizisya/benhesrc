/*
 * Created on 23 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.dsms;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * Fix the ranking of relevant documents in the qrels.
 * @author ben
 *
 */
public class DocumentRerankModifier implements DocumentScoreModifier {
	
	TRECResultsInMemory results;
	
	TRECQrelsInMemory qrels;

	public DocumentRerankModifier() {
		this.loadDocumentFilter();
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "DocumentFilterModifier";}
	
	private void loadDocumentFilter(){
		String filterFilename = ApplicationSetup.getProperty("feedback.filename", "");
		String referenceResultFilename = ApplicationSetup.getProperty("reference.results.filename", "");
		results = new TRECResultsInMemory(referenceResultFilename);
		qrels = new TRECQrelsInMemory(filterFilename);
	}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		if (ApplicationSetup.getProperty("querying.lastpostprocess",null) != null)
			return false;
		int counter = 0;
		final String queryId = queryTerms.getQueryId();
		THashSet<String> relDocnoSet = qrels.getRelevantDocuments(queryId);
		String[] retDocnos = results.getDocnoSet(queryId);
		if (relDocnoSet==null || retDocnos==null)
			return false;
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		final int docCount = docids.length;
		int maxScore = resultSet.getExactResultSize(); 
		DocumentIndex docIndex = index.getDocumentIndex();
		for (int i=0; i<docCount; i++){
			String docno = docIndex.getDocumentNumber(docids[i]);
			if (relDocnoSet.contains(""+docids[i])){
				int rank = results.getRank(queryId, docno);
				scores[i]=(double)maxScore-(double)rank+0.5;
				System.out.println("docno: "+docno+", rank: "+rank);
				counter++;
			}else
				scores[i]=maxScore-i;
		}
		System.out.println(counter+" documents are reordered.");
		return true;
	}

}
