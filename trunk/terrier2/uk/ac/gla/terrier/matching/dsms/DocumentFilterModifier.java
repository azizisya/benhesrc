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

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class DocumentFilterModifier implements DocumentScoreModifier {
	THashMap<String, TIntHashSet> queryidDocidsMap;

	public DocumentFilterModifier() {
		this.loadDocumentFilter();
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "DocumentFilterModifier";}
	
	private void loadDocumentFilter(){
		System.out.println("loading documents...");
		queryidDocidsMap = new THashMap<String, TIntHashSet>();
		String filterFilename = ApplicationSetup.getProperty("filter.filename", "");
		/**
		 * Filter is changed to qrels format: any document in the qrels, no matter relevant
		 * or not, is going to be removed from results.
		 */
		/*try{
			BufferedReader br = Files.openFileReader(filterFilename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				String[] tokens = str.split("\\s* \\s*");
				TIntHashSet docidSet = new TIntHashSet(); 
				for (int i=1; i<tokens.length; i++)
					docidSet.add(Integer.parseInt(tokens[i]));
				queryidDocidsMap.put(tokens[0], docidSet);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}*/
	}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		if (ApplicationSetup.getProperty("querying.lastpostprocess",null) != null)
			return false;
		int counter = 0;
		final String queryId = queryTerms.getQueryId();
		if (queryidDocidsMap.get(queryId)==null)
			return false;
		TIntHashSet filterDocids = (TIntHashSet)queryidDocidsMap.get(queryId);
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		final int docCount = docids.length;
		DocumentIndex docIndex = index.getDocumentIndex();
		for (int i=0; i<docCount; i++){
			if (filterDocids.contains(docids[i])){
				scores[i] = 0.0d;
				counter++;
			}
		}
		System.out.println(counter+" documents are filtered.");
		return true;
	}

}
