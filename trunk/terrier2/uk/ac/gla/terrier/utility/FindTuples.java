/*
 * Created on 14 Aug 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.utility;

import java.io.BufferedReader;
import java.io.IOException;

import gnu.trove.TIntHashSet;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.SingleLineTRECQuery;

public class FindTuples {
	protected Index index;
	
	protected InvertedIndex  invIndex;
	
	protected Lexicon lexicon;
	
	public FindTuples(Index index){
		this.index = index;
		this.invIndex = index.getInvertedIndex();
		this.lexicon = index.getLexicon();
	}
	
	public FindTuples(String indexPath, String indexPrefix){
		this(Index.createIndex(indexPath, indexPrefix));
	}
	
	public boolean findTuple(String term1, String term2){
		TIntHashSet docidSet = new TIntHashSet();
		int[] docids = null;
		int counter = 0;
		if ((docids=getDocids(term1))!=null){
			counter+=docids.length;
			docidSet.addAll(docids);
		}
		if ((docids=getDocids(term2))!=null){
			counter+=docids.length;
			docidSet.addAll(docids);
		}
		return docidSet.size()<counter;
	}
	
	private int[] getDocids(String term){
		LexiconEntry lexEntry = lexicon.getLexiconEntry(term);
		if (lexEntry!=null)
			return invIndex.getDocuments(lexEntry)[0];
		return null;
	}
	
	public void findTuples(String queryString){
		queryString = SingleLineTRECQuery.stripWeights(queryString);
		String[] terms = queryString.split(" ");
		findTuples(terms);
	}
	
	public void findTuplesFromOneLineQueries(String queryFilename){
		try{
			BufferedReader br = Files.openFileReader(queryFilename);
			String query = null;
			while ((query=br.readLine())!=null){
				System.out.println("query: "+query);
				this.findTuples(query);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
	}
	
	public void findTuples(String[] terms){
		for (int i=0; i<terms.length-1; i++)
			for (int j=i+1; j<terms.length; j++){
				if (findTuple(terms[i], terms[j]))
					System.out.println("("+terms[i]+", "+terms[j]+") TRUE!");
				else
					System.out.println("("+terms[i]+", "+terms[j]+") FALSE!");
			}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--findtuplesfromonelinequeries")){
			// --findtuplesfromonelinequeries indexpath indexprefix topicfilename
			FindTuples app = new FindTuples(args[1], args[2]);
			app.findTuplesFromOneLineQueries(args[3]);
		}
	}

}
