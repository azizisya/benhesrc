/*
 * Created on 23 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.dsms;

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

public class SpamFilterScoreModifier implements DocumentScoreModifier {
	THashSet<String> spamDocnoSet;
	TIntHashSet spamDocidSet;
	boolean docidInitialised = false;

	public SpamFilterScoreModifier() {
		this.loadDocumentFilter();
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "SpamFilterScoreModifer";}
	
	private void loadDocumentFilter(){
		System.out.println("loading documents...");
		spamDocnoSet = new THashSet<String>();
		String filterFilename = ApplicationSetup.getProperty("spam.filter.filename", "");
		try{
			BufferedReader br = Files.openFileReader(filterFilename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				spamDocnoSet.add(str);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(spamDocnoSet.size()+" docnos loaded.");
	}
	
	private void convertDocnosToDocids(Index index){
		DocumentIndex docIndex = index.getDocumentIndex();
		spamDocidSet = new TIntHashSet();
		Object[] docnos = this.spamDocnoSet.toArray();
		int n = docnos.length;
		for (int i=0; i<n; i++){
			int docid = docIndex.getDocumentId((String)docnos[i]);
			if (docid>=0)
				this.spamDocidSet.add(docid);
		}
		this.docidInitialised = true;
		spamDocnoSet.clear();
		spamDocnoSet = null;
	}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		int counter = 0;
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		final int docCount = docids.length;
		if (!docidInitialised)
			this.convertDocnosToDocids(index);
		for (int i=0; i<docCount; i++){
			if (spamDocidSet.contains(docids[i])){
				scores[i] = 0.0d;
				counter++;
			}
		}
		System.out.println(counter+" documents are filtered.");
		return true;
	}

}
