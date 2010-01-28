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

import java.io.BufferedReader;
import java.io.IOException;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class QrelsFilterModifier implements DocumentScoreModifier {
	TRECQrelsInMemory qrels;

	public QrelsFilterModifier() {
		this.loadDocumentFilter();
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "DocumentFilterModifier";}
	
	private void loadDocumentFilter(){
		String filterFilename = ApplicationSetup.getProperty("filter.filename", "");
		qrels = new TRECQrelsInMemory(filterFilename);
	}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		if (ApplicationSetup.getProperty("querying.lastpostprocess",null) != null)
			return false;
		int counter = 0;
		final String queryId = queryTerms.getQueryId();
		THashSet<String> filterDocnoSet = qrels.getRelevantDocuments(queryId);
		if (filterDocnoSet==null)
			return false;
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		final int docCount = docids.length;
		DocumentIndex docIndex = index.getDocumentIndex();
		for (int i=0; i<docCount; i++){
			if (filterDocnoSet.contains(""+docids[i])){
				scores[i] = 0.0d;
				counter++;
			}
		}
		System.out.println(counter+" documents are filtered.");
		return true;
	}

}
