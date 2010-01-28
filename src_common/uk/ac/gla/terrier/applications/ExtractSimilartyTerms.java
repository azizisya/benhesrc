/*
 * Created on 26 Nov 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.util.Arrays;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import uk.ac.gla.terrier.statistics.Information;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;

public class ExtractSimilartyTerms {
	protected Index index;
	protected DirectIndex directIndex;
	protected Lexicon lexicon;
	protected InvertedIndex invIndex;
	protected CollectionStatistics collStat;

	public ExtractSimilartyTerms() {
		super();
		index = Index.createIndex();
		directIndex = index.getDirectIndex();
		invIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		collStat = index.getCollectionStatistics();
	}
	/**
	 * @param termid The id of the term for which similar terms are to be found.
	 * @return A hashmap mapping from ids of similar terms to weights.
	 */
	public TIntDoubleHashMap findSimilarTerms(int termid, int numberOfExtractedTerms){
		lexicon.findTerm(termid);
		int DFx = lexicon.getNt();
		int N = collStat.getNumberOfDocuments();
		// find documents (set D) containing the term
		TIntHashSet docidSetX = new TIntHashSet(invIndex.getDocuments(termid)[0]);// X the target term 
		// get all terms in D
		int[] docidsX = docidSetX.toArray();
		TIntHashSet termidSetY = new TIntHashSet();
		for (int i=0; i<docidsX.length; i++){
			termidSetY.addAll(directIndex.getTerms(docidsX[i])[0]);			
		}
		int[] termidsY = termidSetY.toArray();
		double[] MI = new double[termidsY.length];// mutual information
		termidSetY.clear(); termidSetY = null; termidSetY = new TIntHashSet();
		// for each term in D
		for (int i=0; i<termidsY.length; i++){
			lexicon.findTerm(termidsY[i]);
			int DFy = lexicon.getNt();
			// get docidsY
			int[] docidsY = invIndex.getDocuments(termidsY[i])[0];
			int DFyx = 0;
			// count the overlap
			for (int j=0; j<docidsY.length; j++)
				if (docidSetX.contains(docidsY[j]))
					DFyx++;
			// filter out terms that appear in less than half of the documents in D
			if (DFyx*2<DFx)
				MI[i] = 0d;
			else{
				// compute MI
				double Px = (double)DFx/N;
				double Py = (double)DFy/N;
				double Py_x = (double)DFyx/DFx;
				MI[i] = Information.mutualInformation(Px, Py, Py_x, 2d);
			}
			docidSetX.clear(); docidSetX=null;
			docidsY=null;
		}
		// sort and return the terms
		short[] dummy = new short[MI.length];
		Arrays.fill(dummy, (short)1);
		uk.ac.gla.terrier.utility.HeapSort.descendingHeapSort(MI, termidsY, dummy);
		TIntDoubleHashMap map = new TIntDoubleHashMap();
		for (int i=0; i<numberOfExtractedTerms; i++)
			if (MI[i]<0)
				break;
			else
				map.put(termidsY[i], MI[i]);
		return map;
	}
}
