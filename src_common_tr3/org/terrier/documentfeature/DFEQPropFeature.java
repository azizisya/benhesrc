package org.terrier.documentfeature;

import org.terrier.structures.Index;
import org.terrier.utility.IndexUtility;
import gnu.trove.TIntObjectHashMap;

/**
 * Number of documents in the collection containing each of the expansion terms and all original 
 * query terms (DFEQ): see if the co-occurrance of the expansion terms  with the original query 
 * terms in the feedback document is by chance or not.
 * @author ben
 *
 */
public class DFEQPropFeature extends DFEQFeature {
	
	public int featureId;
	
	public DFEQPropFeature(Index index){
		super(index);
		this.featureId = 10;		
	}
	
	public String getInfo(){
		return "DEFQPropFeature";
	}

	@Override
	public void extractDocumentFeature(int docid, String queryid,
			int[] queryTermids, TIntObjectHashMap featureMap) {
		if (CACHED){
			TIntObjectHashMap<Double> map = this.cache.get(queryid);
			if (map!=null)
				featureMap.put(featureId, map.get(docid));
		}else{
			/**
			int[][] terms = directIndex.getTerms(docid);
			int[] oneDocid = {docid};
			if (terms==null)
				return;
			ExpansionTerm[] expterms = QueryExpansion.expandFromDocuments(oneDocid, 
					null, terms[0].length, 
					directIndex, docIndex, collStats, lexicon, model);
			Arrays.sort(expterms);
			TIntHashSet expTermidSet = new TIntHashSet();
			TIntHashSet originalTermidSet = new TIntHashSet();
			originalTermidSet.addAll(queryTermids);
			for (int i=0; i<expterms.length; i++){
				if (!originalTermidSet.contains(expterms[i].getTermID()))
					expTermidSet.add(expterms[i].getTermID());
			}
			int[] expTermids = QueryUtility.filterTerms(expTermidSet.toArray(), lexicon, collStats.getNumberOfDocuments(), 0.05);
			*/
			
			int[] expTermids = entries.get(queryid).get(docid).keys();
			
			
			// get co-occurrences
			int overlap = 0;
			int[] toRetain = IndexUtility.getCooccurredDocuments(queryTermids, index.getInvertedIndex(), index.getLexicon());
			//System.out.println("Count");
			for (int i=0; i<expTermids.length; i++){
				int coo = IndexUtility.getCooccurrenceInCollection(expTermids[i], 
						toRetain, index.getInvertedIndex(), index.getLexicon(), collStats.getNumberOfDocuments());
				overlap += coo;
			}
			featureMap.put(featureId, (double)overlap/(expTermids.length*toRetain.length));
		}
	}
	
	
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			DFEQPropFeature app = new DFEQPropFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename, app.featureId);
		}
	}
	
}
