/**
 * 
 */
package org.terrier.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.terrier.querying.Manager;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.InvertedIndex;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.QueryUtility;
import org.terrier.utility.Rounding;

import gnu.trove.TDoubleArrayList;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;
import org.terrier.evaluation.TRECQrelsInMemory;
import org.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.statistics.Statistics;

/**
 * @author ben
 *
 */
public class BlockDocumentSetEntropy{
	/**
	 * Data structures of the document index
	 */
	protected Index index;
	protected DocumentIndex docIndex;
	protected DirectIndex directIndex;
	protected InvertedIndex invIndex;
	protected Lexicon lexicon;
	protected CollectionStatistics collSta;
	
	/**
	 * The maximum percentage of documents that a query terms occurs in the collection, 
	 * so that the term is to be considered during the computation of the mean entropy.
	 */
	protected double MAX_PERCENTAGE = 0.20;
	/**
	 * The number of spans per document.
	 */
	protected final int NUMBER_OF_SPANS_PER_DOC = Integer.parseInt(ApplicationSetup.getProperty("number.of.spans.per.doc", "10"));
	//protected final int NUMBER_OF_TERMS_PER_SPAN = 50;
	
	public BlockDocumentSetEntropy(Index _index){
		super();
		index = _index;
		docIndex = index.getDocumentIndex();
		directIndex = index.getDirectIndex();
		invIndex = index.getInvertedIndex();
		lexicon = index.getLexicon();
		collSta = index.getCollectionStatistics();
	}
	
	public BlockDocumentSetEntropy(String docIndexPath, String docIndexPrefix){
		this(Index.createIndex(docIndexPath, docIndexPrefix));
	}

	/**
	 * Entropy is computed for each termid over a set of segmentations of documents.
	 * For each pair of (termid, span), a prob(t, span) is computed.
	 * Such probability is smoothed by JM smoothing. Thus, the final prob is:
	 * p(t | span) = alpha * p_{ml}(t | span) + (1 - alpha) * p(t | d)
	 * The Entropy for a pair of (termid, d) is given by:
	 * Entropy(termid, d)=-\sum_{span}\log_2(p(t|span))
	 * This method returns the mean of all Entropy(termid, d) for given termids.
	 * @param termids
	 * @param docid
	 * @param alpha
	 * @return
	 */
	public double getMeanEntropy(int[] termids, int docid, double alpha){
		if (docid<0)
			return 0;
		TIntHashSet termidSet = new TIntHashSet();
		termidSet.addAll(termids);
		TIntIntHashMap termidFreqMap = new TIntIntHashMap();
		// Get borders of spans of the document
		// each bin i starts (includes) at position borders[i-1], and ends (excludes) at position borders[i]
		// namely borders[i-1] <= position < borders[i]
		int[][] pointers = null;
		int docLength = 0;
		try{
			pointers = directIndex.getTerms(docid);
			docLength = docIndex.getDocumentLength(docid);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
			
		if (docLength <= 0)
			return 0;
		int numberOfSpans = this.NUMBER_OF_SPANS_PER_DOC;
			//(docLength%this.NUMBER_OF_TERMS_PER_SPAN==0)?
				//(docLength/this.NUMBER_OF_TERMS_PER_SPAN):(docLength/this.NUMBER_OF_TERMS_PER_SPAN+1);
		int[] borders = new int[numberOfSpans+1];
		borders[0] = 0; borders[numberOfSpans] = docLength;
		int NUMBER_OF_TERMS_PER_SPAN = docLength / numberOfSpans;
		for (int i=1; i<borders.length; i++)
			borders[i] = borders[i-1]+NUMBER_OF_TERMS_PER_SPAN*i;
		// get frequencies of each term in spans of the document
		/**
		 * Mapping from termid to a mapping from spanid to frequency
		 */
		TIntObjectHashMap<TIntIntHashMap> freqMap = new TIntObjectHashMap<TIntIntHashMap>();
		int blockIndex = 0;
		for (int i=0; i<pointers[0].length; i++){
			int termid = pointers[0][i];
			if (pointers[1][i]!=pointers[3][i]){
				System.err.println("Warning: pointers[1][i]!=pointers[3][i]");
			}
			if (!termidSet.contains(termid)){
				blockIndex += pointers[3][i];
				continue;
			}
			freqMap.put(termid, new TIntIntHashMap());
			termidFreqMap.put(termid, pointers[1][i]);
			// get the blocks of the term
			int[] blocks = new int[pointers[3][i]];
			for (int j=0; j<pointers[3][i]; j++)
				blocks[j] = pointers[4][blockIndex++];
			Arrays.sort(blocks);
			// count frequencies of the term in spans
			for (int j=0; j<pointers[3][i]; j++){
				for (int spanid=1; spanid<=numberOfSpans; spanid++){
					if (blocks[j]>=borders[spanid-1] && blocks[j]<borders[spanid]){
						freqMap.get(termid).adjustOrPutValue(spanid, 1, 1);
						break;
					}
				}
			}
		}
		// compute probs
		double[] entropies = new double[termids.length];
		double[] infoPriors = getInfoPrior(termids, lexicon, collSta.getNumberOfTokens());
		infoPriors = Statistics.normaliseMax(infoPriors);
		for (int i=0; i<termids.length; i++){
			if (termidFreqMap.get(termids[i])<3){
				entropies[i] = 0;
				continue;
			}
			LexiconEntry lexEntry = (LexiconEntry)lexicon.getLexiconEntry(termids[i]).getValue();
			double[] probs = new double[numberOfSpans];
			TIntIntHashMap map = freqMap.get(termids[i]);
			if (map == null)
				continue;
			for (int j=0; j<numberOfSpans; j++){
				int spanLength = (j==numberOfSpans-1)?
						(borders[j+1]-borders[j]):(docLength/numberOfSpans);
				int tf = 0;
				tf = map.get(j+1);
				/**
				 * Apply Laplace smoothing
				 */
				probs[j] = (double)(tf+1)/(termidFreqMap.get(termids[i])+this.NUMBER_OF_SPANS_PER_DOC);
				//probs[j] = alpha*tf / termidFreqMap.get(termids[i])+(1-alpha)*termidFreqMap.get(termids[i])/lexEntry.TF;
					//alpha*tf/spanLength+(1-alpha)*termidFreqMap.get(termids[i])/docLength;
				//lexEntry.TF/collSta.getNumberOfTokens();
				/*if (probs[j] <= 0)
					System.err.println("freq: "+termidFreqMap.get(termids[i])+", prob: "+probs[j]);
					*/
			}
			// entropies[i] = infoPriors[i]*Statistics.entropy(probs);///maxEntropy;
			entropies[i] = Statistics.entropy(probs);
			if (entropies[i] <= 0d){
				System.err.println("entropies["+i+"]: "+entropies[i]);
				for (int t=0; t<probs.length; t++)
					System.err.println("probs["+t+"]: "+probs[t]);
					
			}
		}
		double meanEntropy = Statistics.mean(entropies);
		//if (meanEntropy<=0){
			System.err.println("mean entropy: "+meanEntropy);
			for (int i=0; i<entropies.length; i++)
				System.err.println("entropies["+i+"]="+entropies[i]);
		//}
		return meanEntropy;
	}
	
	public int[] filterQueryTerms(int[] termids){
		// remove terms that occur in more than given percentage of spans in the span collection
		double[] DF = new double[termids.length];
		int[] localTermids = (int[])termids.clone();
		for (int j=0; j<termids.length; j++){
			LexiconEntry lexEntry = (LexiconEntry)lexicon.getLexiconEntry(termids[j]).getValue();
			DF[j] = (lexEntry==null)?(collSta.getNumberOfDocuments()):(1d*lexEntry.getDocumentFrequency());
		}
		short[] dummy = new short[termids.length];
		Arrays.fill(dummy, (short)1);
		org.terrier.utility.HeapSort.ascendingHeapSort(DF, localTermids, dummy);
		TIntHashSet localTermidSet = new TIntHashSet();
		localTermidSet.addAll(termids);
		for (int j=termids.length-1; j>=0; j--){
			// if all query terms occur in more than a certain percentage of docmuments in the collection, keep the term with the lowest DF
			if (DF[j] > this.MAX_PERCENTAGE*collSta.getNumberOfDocuments() 
					&& localTermidSet.size()>1){
				localTermidSet.remove(termids[j]);
			}else
				break;
		} 
		return localTermidSet.toArray();
	}
	
	public void printEntropyForResults(String resultFilename, String docIdxOutputFilename){
		TRECQuery queries = QueryUtility.getQueryParser();
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.lambda", "0.85d"));
		TObjectIntHashMap<String> docmap = IndexUtility.getDocidsFromFile(docIdxOutputFilename, resultFilename, 2);
		String[] queryids = results.getQueryids();
		Arrays.sort(queryids);
		double[] meanEntropies = new double[queryids.length];
		String[] pipes = {"Stopwords", "PorterStemmer"};
		for (int i=0; i<queryids.length; i++){
			String[] docnos = results.getRetrievedDocnos(queryids[i]);
			double[] scores = results.getScores(queryids[i]);
			double entropy = 0d;
			int[] termids = QueryUtility.queryStringToTermids(queries.getQuery(queryids[i]), pipes, lexicon);
			termids = this.filterQueryTerms(termids);
			
			// entropy is compute for termids over each doc.
			for (int j=0; j<docnos.length; j++){
				entropy = this.getMeanEntropy(termids, docmap.get(docnos[j]), alpha);
				System.out.println("SCORE_ENTR "+docnos[j]+" "+Rounding.round(scores[j], 4)+" "+Rounding.toString(entropy, 4)+" "+queryids[i]);
			}
			//entropy /= docnos.length;
			//meanEntropies[i] = entropy;
		}
		//System.out.println("Mean entropy: "+Statistics.mean(meanEntropies));
	}
	/**
	 * InfoPrior=-log_2(TF/NumberOfTokens)
	 * @param termid
	 * @param lexicon
	 */
	public static double getInfoPrior(LexiconEntry lexEntry, long numberOfTokens){
		return -Math.log((double)lexEntry.getFrequency()/numberOfTokens)/Math.log(2d);
	}
	
	/**
	 * InfoPrior=-log_2(TF/NumberOfTokens)
	 * @param termid
	 * @param lexicon
	 */
	public static double[] getInfoPrior(int[] termids, Lexicon lexicon, long numberOfTokens){
		double[] infoPriors = new double[termids.length];
		for (int i=0; i<termids.length; i++){
			LexiconEntry lexEntry = (LexiconEntry)lexicon.getLexiconEntry(termids[i]).getValue();
			infoPriors[i] = getInfoPrior(lexEntry, numberOfTokens);
		}
		return infoPriors;
	}
	
	public void printMeanEntropy(String resultFilename, String docIdxOutputFilename, int topX){
		TRECQuery queries = QueryUtility.getQueryParser();
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.85d"));
		TObjectIntHashMap<String> docmap = IndexUtility.getDocidsFromFile(docIdxOutputFilename, resultFilename, 2);
		String[] queryids = results.getQueryids();
		Arrays.sort(queryids);
		double[] meanEntropies = new double[queryids.length];
		String[] pipes = {"Stopwords", "PorterStemmer"};
		for (int i=0; i<queryids.length; i++){
			String[] docnos = results.getRetrievedDocnos(queryids[i], topX);
			double entropy = 0d;
			int[] termids = QueryUtility.queryStringToTermids(queries.getQuery(queryids[i]), pipes, lexicon);
			
			// remove terms that occur in more than given percentage of spans in the span collection
			termids = this.filterQueryTerms(termids);
			System.err.println("query terms with lowest document frequencies: ");
			for (int termid : termids)
				System.err.print("("+(String)lexicon.getLexiconEntry(termid).getKey()+", "+termid+") ");
			System.err.println();
			
			// entropy is compute for termids over each doc.
			for (int j=0; j<docnos.length; j++){
				entropy += this.getMeanEntropy(termids, docmap.get(docnos[j]), alpha);
			}
			entropy /= docnos.length;
			System.out.println("query "+queryids[i]+": "+entropy);
			meanEntropies[i] = entropy;
		}
		System.out.println("Mean entropy: "+Statistics.mean(meanEntropies));
	}
	
	public void printMeanEntropyForOneDoc(String resultFilename, String docIdxOutputFilename, int rank){
		TRECQuery queries = QueryUtility.getQueryParser();
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.85d"));
		TObjectIntHashMap<String> docmap = IndexUtility.getDocidsFromFile(docIdxOutputFilename, resultFilename, 2);
		String[] queryids = results.getQueryids();
		Arrays.sort(queryids);
		double[] meanEntropies = new double[queryids.length];
		String[] pipes = {"Stopwords", "PorterStemmer"};
		for (int i=0; i<queryids.length; i++){
			String[] docnos = {results.getRetrievedDocnos(queryids[i])[rank]};
			double entropy = 0d;
			int[] termids = QueryUtility.queryStringToTermids(queries.getQuery(queryids[i]), pipes, lexicon);
			
			// remove terms that occur in more than given percentage of spans in the span collection
			termids = this.filterQueryTerms(termids);
			System.err.println("query terms with lowest document frequencies: ");
			for (int termid : termids)
				System.err.print("("+(String)lexicon.getLexiconEntry(termid).getKey()+", "+termid+") ");
			System.err.println();
			
			// entropy is compute for termids over each doc.
			for (int j=0; j<docnos.length; j++){
				entropy += this.getMeanEntropy(termids, docmap.get(docnos[j]), alpha);
			}
			entropy /= docnos.length;
			System.out.println("query "+queryids[i]+": "+entropy);
			meanEntropies[i] = entropy;
		}
		System.out.println("Mean entropy: "+Statistics.mean(meanEntropies));
	}
	
	
	
	public static void printPrecisionOfRankX(String qrelsFilename, String resultFilename, int rank){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		THashSet<String> queryidsInResults = new THashSet<String>();
		for (int i=0; i<results.getQueryids().length; i++)
			queryidsInResults.add(results.getQueryids()[i]);
		THashSet<String> queryidsInQrels = new THashSet<String>();
		for (int i=0; i<qrels.getQueryids().length; i++)
			queryidsInQrels.add(qrels.getQueryids()[i]);
		queryidsInResults.retainAll(queryidsInQrels);
		String[] queryids = (String[])queryidsInResults.toArray(new String[queryidsInResults.size()]);
		Arrays.sort(queryids);
		int relCounter = 0;
		for (int i=0; i<queryids.length; i++){
			String docno = results.getRetrievedDocnos(queryids[i])[rank];
			if (qrels.isRelevant(queryids[i], docno)){
				System.out.println("query "+queryids[i]+", docno: "+docno+", relevant");
				relCounter++;
			}else
				System.out.println("query "+queryids[i]+", docno: "+docno+", irrelevant");
		}
		System.out.println(queryids.length+" queries in total. "+relCounter+" queries has relevant doc retrieved at rank "+rank);
		System.out.println("Precision of rank "+rank+": "+(double)relCounter/queryids.length);
	}
	
	
	
	protected double getEntropies(
			String[] pipes, String query, String[] docnos, TObjectIntHashMap<String> docmap, double alpha){
		double entropy = 0d;
		int[] termids = QueryUtility.queryStringToTermids(query, pipes, lexicon);
		termids = this.filterQueryTerms(termids);
			
		// entropy is compute for termids over each doc.
		int effCounter = 0;
		for (int j=0; j<docnos.length; j++){
			//System.out.print("docid: "+docmap.get(docnos[j])+", docno: "+docnos[j]);
			double value = this.getMeanEntropy(termids, docmap.get(docnos[j]), alpha);
			//System.out.println(", entropy: "+value);
			if (value > 0){
				entropy += value;
				effCounter++;
			}
		}
		return entropy/effCounter;
	}
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--printmeanentropies")){
			// --printmeanentropies indexpath indexprefix resultFilename docidxoutputFilename topX 
			BlockDocumentSetEntropy app = new BlockDocumentSetEntropy(args[1], args[2]);
			app.printMeanEntropy(args[3], args[4], Integer.parseInt(args[5]));
		}else if (args[0].equals("--printmeanentropiesforithdoc")){
			// --printmeanentropies indexpath indexprefix resultFilename docidxoutputFilename rank 
			BlockDocumentSetEntropy app = new BlockDocumentSetEntropy(args[1], args[2]);
			app.printMeanEntropyForOneDoc(args[3], args[4], Integer.parseInt(args[5]));
		}else if (args[0].equals("--printpreofrank")){
			// --printpreofrank qrelsfilename resultFilename rank
			BlockDocumentSetEntropy.printPrecisionOfRankX(args[1], args[2], Integer.parseInt(args[3]));
		}else if (args[0].equals("--printentropyforresults")){
			// --printmeanentropies indexpath indexprefix resultFilename docidxoutputFilename
			BlockDocumentSetEntropy app = new BlockDocumentSetEntropy(args[1], args[2]);
			app.printEntropyForResults(args[3], args[4]);
		}
	}

}
