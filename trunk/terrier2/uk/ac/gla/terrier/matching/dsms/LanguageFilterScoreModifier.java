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
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class LanguageFilterScoreModifier implements DocumentScoreModifier {
	// THashSet<String> filteredDocnoSet;
	TIntHashSet filteredDocidSet;
	// boolean docidInitialised = false;
	THashSet<String> acceptedLangSet;

	public LanguageFilterScoreModifier() {
		this.loadDocumentFilter();
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "LanguageFilterScoreModifer";}
	
	private void loadDocumentFilter(){
		System.out.print("Initialising language filter...");
		filteredDocidSet = new TIntHashSet();
		acceptedLangSet = new THashSet<String>();
		String[] acceptedLangs = ApplicationSetup.getProperty("lang.accept", "english,unknown").split(",");
		for (int i=0; i<acceptedLangs.length; i++){
			acceptedLangSet.add(acceptedLangs[i]);
			System.out.print(acceptedLangs[i]+" ");
		}
		String filterFilename = ApplicationSetup.getProperty("lang.filter.filename", "");
		try{
			BufferedReader br = Files.openFileReader(filterFilename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				String[] tokens = str.replaceAll("\\[", " ").replaceAll("\\]", " ").replaceAll(",", " ").split(" ");
				boolean accept = false;
				for (int i=1; i<tokens.length; i++){
					if (acceptedLangSet.contains(tokens[i])){
						accept = true;
						break;
					}
				}
				if (!accept)
					filteredDocidSet.add(Integer.parseInt(tokens[0]));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(filteredDocidSet.size()+" docnos loaded.");
	}
	
/*	private void convertDocnosToDocids(Index index){
		filteredDocidSet = new TIntHashSet();
		
		
		Object[] docnos = filteredDocnoSet.toArray();
		int n = docnos.length;
		DocumentIndex docIndex = index.getDocumentIndex();
		for (int i=0; i<n; i++){
			int docid = docIndex.getDocumentId((String)docnos[i]);
			if (docid>=0)
				filteredDocidSet.add(docid);
		}
		
		DocumentIndexInputStream docIndexIS = new DocumentIndexInputStream(index.getPath(), index.getPrefix());
		try{
			int counter = 0;
			while (docIndexIS.readNextEntry()!=-1){
				if (filteredDocnoSet.contains(docIndexIS.getDocumentNumber())){
					filteredDocidSet.add(docIndexIS.getDocumentId());
					if (++counter == filteredDocnoSet.size())
						break;
				}
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		docIndexIS.close();
		
		docidInitialised = true;
		filteredDocnoSet.clear();
		filteredDocnoSet = null;
	}
*/	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		int counter = 0;
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		final int docCount = docids.length;
		//if (!docidInitialised)
			//this.convertDocnosToDocids(index);
		for (int i=0; i<docCount; i++){
			if (filteredDocidSet.contains(docids[i])){
				scores[i] = 0.0d;
				counter++;
			}
		}
		System.out.println(counter+" documents are filtered.");
		return true;
	}

}
