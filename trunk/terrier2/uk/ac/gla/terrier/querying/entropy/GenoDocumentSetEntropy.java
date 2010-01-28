/**
 * 
 */
package uk.ac.gla.terrier.querying.entropy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.SingleLineTRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * @author ben
 *
 */
public class GenoDocumentSetEntropy{
	
	/**
	 * Data structures of the span index
	 */
	protected Index spanIndex;
	protected DocumentIndex spanDocIndex;
	protected DirectIndex spanDirectIndex;
	protected InvertedIndex spanInvIndex;
	protected Lexicon spanLexicon;
	protected CollectionStatistics spanCollSta;
	
	protected THashMap<String,TIntHashSet> docnoSpanidsMap;
	
	
	/**
	 * The maximum percentage of documents that a query terms occurs in the collection, 
	 * so that the term is to be considered during the computation of the mean entropy.
	 */
	protected double MAX_PERCENTAGE = 0.20;
	
	public GenoDocumentSetEntropy(String spanIndexPath, String spanIndexPrefix){
		super();
		// initialise span indices
		
		spanIndex = Index.createIndex(spanIndexPath, spanIndexPrefix);
		spanDocIndex = spanIndex.getDocumentIndex();
		spanDirectIndex = spanIndex.getDirectIndex();
		spanInvIndex = spanIndex.getInvertedIndex();
		spanLexicon = spanIndex.getLexicon();
		spanCollSta = spanIndex.getCollectionStatistics();
	}
	
	public GenoDocumentSetEntropy(String spanIndexPath, String spanIndexPrefix, String docIdxOutputFilename, String resultFilename){
		super();
		// initialise span indices
		
		spanIndex = Index.createIndex(spanIndexPath, spanIndexPrefix);
		spanDocIndex = spanIndex.getDocumentIndex();
		spanDirectIndex = spanIndex.getDirectIndex();
		spanInvIndex = spanIndex.getInvertedIndex();
		spanLexicon = spanIndex.getLexicon();
		spanCollSta = spanIndex.getCollectionStatistics();
		
		docnoSpanidsMap = getSpanidsFromResultFile(docIdxOutputFilename, resultFilename);
	}
	/**
	 * 
	 * @param termids
	 * @param docno Docno in the document index.
	 * @return
	 */
	public double getMeanEntropy(int[] termids, String docno){
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.85d"));
		int[] spanids = docnoSpanidsMap.get(docno).toArray();
		return this.getMeanEntropy(termids, spanids, alpha);
	}
	
	/**
	 * 
	 * @param termids
	 * @param docnos Docnos in the document index.
	 * @return
	 */
	public double getMeanEntropy(int[] termids, String[] docnos){
		double entropy = 0;
		int effCounter = 0;
		for (int i=0; i<docnos.length; i++){
			int[] spanids = docnoSpanidsMap.get(docnos[i]).toArray();
			double thisEntropy = getMeanEntropy(termids, docnos[i]);
			//System.out.println("docno: "+docnos[i]+" has "+spanids.length+" spans.");
			if (thisEntropy > 0){
				entropy+=thisEntropy;
				effCounter++;
			}
		}
		return entropy/effCounter;
	}

	/**
	 * Entropy is computed for each termid over a set of spans.
	 * For each pair of (termid, spanid), a prob(t, span) is computed.
	 * Such probability is smoothed by JM smoothing. Thus, the final prob is:
	 * p(t | span) = alpha * p_{ml}(t | span) + (1 - alpha) * p(t | all spanids)
	 * The Entropy for a pair of (termid, spanids[]) is given by:
	 * Entropy(termid, spanids[])=-\sum_{spanid}\log_2(p(t|span))
	 * This method returns the mean of all Entropy(termid, spanids[]) for given termids.
	 * @param termids
	 * @param spanids
	 * @param alpha
	 * @return
	 */
	public double getMeanEntropy(int[] termids, int[] spanids, double alpha){
		TIntHashSet termidSet = new TIntHashSet();
		termidSet.addAll(termids);
		//int[][] freqs = new int[termids.length][spanids.length];
		/**
		 * Mapping from termid to a mapping from spanid to frequency
		 */
		TIntObjectHashMap<TIntIntHashMap> freqMap = new TIntObjectHashMap<TIntIntHashMap>();
		int[] spanLengths = new int[spanids.length];
		int length = 0;
		// map from termid to freq in all spans
		TIntIntHashMap termidFreqMap = new TIntIntHashMap();
		/*System.err.println("spanids.length: "+spanids.length);
		for (int i=0; i<spanids.length; i++){
			System.err.println("span "+(i+1)+": "+spanids[i]+", "+spanDocIndex.getDocumentLength(spanids[i]));
		}
		System.err.println("termids.length: "+termids.length);
		for (int i=0; i<termids.length; i++){
			System.err.println("term "+(i+1)+": "+termids[i]+", "+spanLexicon.getLexiconEntry(termids[i]).term);
		}*/
		int effCounter = 0;
		TIntHashSet effSpanidSet = new TIntHashSet();
		for (int i=0; i<spanids.length; i++){
			//int counter = 0;
			spanLengths[i] = spanDocIndex.getDocumentLength(spanids[i]);
			if (spanLengths[i] <= 0)
				continue;
			effCounter++;
			length+=spanLengths[i];
			effSpanidSet.add(spanids[i]);
			int[][] terms = spanDirectIndex.getTerms(spanids[i]);
			//System.err.println("span: "+spanDocIndex.getDocumentNumber(spanids[i])+", terms[0].length: "+terms[0].length);
			try{	
				for (int j=0; j<terms[0].length; j++){
					if (termidSet.contains(terms[0][j])){
						//freqs[counter++][i] = terms[1][j];
						if (!freqMap.containsKey(terms[0][j]))
							freqMap.put(terms[0][j], new TIntIntHashMap());
						freqMap.get(terms[0][j]).put(spanids[i], terms[1][j]);
						//System.err.println("put "+spanLexicon.getLexiconEntry(terms[0][j]).term+", "+terms[1][j]);
						termidFreqMap.adjustOrPutValue(terms[0][j], terms[1][j], terms[1][j]);
					}//else
						//System.err.println("ignore "+spanLexicon.getLexiconEntry(terms[0][j]).term);
				}
			}catch(NullPointerException e){
				System.err.println("spanLength: "+spanDocIndex.getDocumentLength(spanids[i]));
				// System.err.println("spanNo: "+spanDocIndex.getDocumentNumber(spanids[i]));
				System.err.println("spanid: "+spanids[i]);
				e.printStackTrace();
				System.exit(1);
			}
		}
		// compute probs
		double[] entropies = new double[termids.length];
		//spanids = effSpanidSet.toArray();
		for (int i=0; i<termids.length; i++){
			LexiconEntry lexEntry = spanLexicon.getLexiconEntry(termids[i]);
			if (termidFreqMap.get(termids[i])<3){
				entropies[i] = 0;
				continue;
			}
			double[] probs = new double[spanids.length];
			TIntIntHashMap map = freqMap.get(termids[i]);
			if (map == null)
				continue;
			// double maxEntropy = Statistics.maxEntropy(spanids.length, termidFreqMap.get(termids[i]));
			for (int j=0; j<spanids.length; j++){
				int tf = map.get(spanids[j]);
				//probs[j] = alpha*tf/spanLengths[j]+(1-alpha)*termidFreqMap.get(termids[i])/length;
				probs[j] = alpha*tf/termidFreqMap.get(termids[i])+(1-alpha)*termidFreqMap.get(termids[i])/lexEntry.TF;
				if (probs[j] <= 0)
					System.err.println("freq: "+termidFreqMap.get(termids[i])+", prob: "+probs[j]);
			}
			entropies[i] = Statistics.entropy(probs);///maxEntropy;
			if (entropies[i] <= 0d){
				System.err.println("entropies["+i+"]: "+entropies[i]);
				for (int t=0; t<probs.length; t++)
					System.err.println("probs["+t+"]: "+probs[t]);
			}
		}
		return Statistics.mean(entropies);
	}
	
	public void printMeanEntropy(String topicFilename, String resultFilename, String docIdxOutputFilename, int topX){
		SingleLineTRECQuery queries = new SingleLineTRECQuery(topicFilename);
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.85d"));
		THashMap<String, TIntHashSet> docmap = this.getSpanidsFromResultFile(docIdxOutputFilename, resultFilename);
		String[] queryids = results.getQueryids();
		Arrays.sort(queryids);
		double[] meanEntropies = new double[queryids.length];
		Manager manager = new Manager(null);
		for (int i=0; i<queryids.length; i++){
			String[] docnos = results.getRetrievedDocnos(queryids[i], topX);
			double entropy = 0d;
			int[] termids = queryStringToTermids(queries.getQuery(queryids[i]), manager, spanLexicon);
			/**
			if (termids.length > this.NUM_TERMS_LOW_DF){
				// get the terms with the lowest document frequencies
				double[] DF = new double[termids.length];
				int[] localTermids = (int[])termids.clone();
				for (int j=0; j<termids.length; j++){
					LexiconEntry lexEntry = spanLexicon.getLexiconEntry(termids[j]);
					if (lexEntry!=null){
						DF[j] = lexEntry.n_t;
					}else
						DF[j] = spanCollSta.getNumberOfDocuments();
				}
				short[] dummy = new short[termids.length];
				Arrays.fill(dummy, (short)1);
				uk.ac.gla.terrier.utility.HeapSort.ascendingHeapSort(DF, localTermids, dummy, this.NUM_TERMS_LOW_DF);
				termids = Arrays.copyOfRange(localTermids, 0, this.NUM_TERMS_LOW_DF);
				// if a term occurs in more than 5% of documents in the collection, remove it from computation
				TIntHashSet localTermidSet = new TIntHashSet();
				localTermidSet.addAll(termids);
				for (int j=termids.length-1; j>=0; j--){
					// if all query terms occur in more than 5% of docmuments in the collection, keep the term with the lowest DF
					if (DF[j] > spanCollSta.getNumberOfDocuments()/20 && localTermidSet.size()>1){
						localTermidSet.remove(termids[j]);
					}else
						break;
				}
			}*/
			
			// remove terms that occur in more than given percentage of spans in the span collection
			double[] DF = new double[termids.length];
			int[] localTermids = (int[])termids.clone();
			for (int j=0; j<termids.length; j++){
				LexiconEntry lexEntry = spanLexicon.getLexiconEntry(termids[j]);
				if (lexEntry!=null){
					DF[j] = lexEntry.n_t;
				}else
					DF[j] = spanCollSta.getNumberOfDocuments();
			}
			short[] dummy = new short[termids.length];
			Arrays.fill(dummy, (short)1);
			uk.ac.gla.terrier.utility.HeapSort.ascendingHeapSort(DF, localTermids, dummy);
			TIntHashSet localTermidSet = new TIntHashSet();
			localTermidSet.addAll(termids);
			for (int j=termids.length-1; j>=0; j--){
				// if all query terms occur in more than 5% of docmuments in the collection, keep the term with the lowest DF
				if (DF[j] > this.MAX_PERCENTAGE*spanCollSta.getNumberOfDocuments() 
						&& localTermidSet.size()>1){
					localTermidSet.remove(termids[j]);
				}else
					break;
			} 
			termids = localTermidSet.toArray();
			System.err.println("query terms with lowest document frequencies: ");
			for (int termid : termids)
				System.err.print("("+spanLexicon.getLexiconEntry(termid).term+", "+termid+") ");
			System.err.println();
			
			// entropy is compute for termids over each doc.
			for (int j=0; j<docnos.length; j++){
				TIntHashSet spanidSet = new TIntHashSet();
				spanidSet.addAll(docmap.get(docnos[j]).toArray());
				// get rid of empty spans
				for (int spanid : spanidSet.toArray()){
					if (spanDocIndex.getDocumentLength(spanid) <= 0)
						spanidSet.remove(spanid);
				}
				entropy += this.getMeanEntropy(termids, spanidSet.toArray(), alpha);
			}
			entropy /= docnos.length;
			System.out.println("query "+queryids[i]+": "+entropy);
			meanEntropies[i] = entropy;
		}
		System.out.println("Mean entropy: "+Statistics.mean(meanEntropies));
	}
	
	public Index getSpanIndex(){
		return this.spanIndex;
	}
	
	public int[] filterQueryTerms(int[] termids){
		// remove terms that occur in more than given percentage of spans in the span collection
		double[] DF = new double[termids.length];
		int[] localTermids = (int[])termids.clone();
		for (int j=0; j<termids.length; j++){
			LexiconEntry lexEntry = spanLexicon.getLexiconEntry(termids[j]);
			if (lexEntry!=null){
				DF[j] = lexEntry.n_t;
			}else
				DF[j] = spanCollSta.getNumberOfDocuments();
		}
		short[] dummy = new short[termids.length];
		Arrays.fill(dummy, (short)1);
		uk.ac.gla.terrier.utility.HeapSort.ascendingHeapSort(DF, localTermids, dummy);
		TIntHashSet localTermidSet = new TIntHashSet();
		localTermidSet.addAll(termids);
		for (int j=termids.length-1; j>=0; j--){
			// if all query terms occur in more than a certain percentage of docmuments in the collection, keep the term with the lowest DF
			if (DF[j] > this.MAX_PERCENTAGE*spanCollSta.getNumberOfDocuments() 
					&& localTermidSet.size()>1){
				localTermidSet.remove(termids[j]);
			}else
				break;
		} 
		return localTermidSet.toArray();
	}
	
	public void printMeanEntropyForOneDoc(String topicFilename, String resultFilename, String docIdxOutputFilename, int rank){
		SingleLineTRECQuery queries = new SingleLineTRECQuery(topicFilename);
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.85d"));
		THashMap<String, TIntHashSet> docmap = this.getSpanidsFromResultFile(docIdxOutputFilename, resultFilename);
		String[] queryids = results.getQueryids();
		Arrays.sort(queryids);
		double[] meanEntropies = new double[queryids.length];
		Manager manager = new Manager(null);
		for (int i=0; i<queryids.length; i++){
			String[] docnos = {results.getRetrievedDocnos(queryids[i])[rank]};
			double entropy = 0d;
			int[] termids = queryStringToTermids(queries.getQuery(queryids[i]), manager, spanLexicon);
			
			// remove terms that occur in more than given percentage of spans in the span collection
			double[] DF = new double[termids.length];
			int[] localTermids = (int[])termids.clone();
			for (int j=0; j<termids.length; j++){
				LexiconEntry lexEntry = spanLexicon.getLexiconEntry(termids[j]);
				if (lexEntry!=null){
					DF[j] = lexEntry.n_t;
				}else
					DF[j] = spanCollSta.getNumberOfDocuments();
			}
			short[] dummy = new short[termids.length];
			Arrays.fill(dummy, (short)1);
			uk.ac.gla.terrier.utility.HeapSort.ascendingHeapSort(DF, localTermids, dummy);
			TIntHashSet localTermidSet = new TIntHashSet();
			localTermidSet.addAll(termids);
			for (int j=termids.length-1; j>=0; j--){
				// if all query terms occur in more than 5% of docmuments in the collection, keep the term with the lowest DF
				if (DF[j] > this.MAX_PERCENTAGE*spanCollSta.getNumberOfDocuments() 
						&& localTermidSet.size()>1){
					localTermidSet.remove(termids[j]);
				}else
					break;
			} 
			termids = localTermidSet.toArray();
			System.err.println("query terms with lowest document frequencies: ");
			for (int termid : termids)
				System.err.print("("+spanLexicon.getLexiconEntry(termid).term+", "+termid+") ");
			System.err.println();
			
			// entropy is compute for termids over each doc.
			for (int j=0; j<docnos.length; j++){
				TIntHashSet spanidSet = new TIntHashSet();
				spanidSet.addAll(docmap.get(docnos[j]).toArray());
				// get rid of empty spans
				for (int spanid : spanidSet.toArray()){
					if (spanDocIndex.getDocumentLength(spanid) <= 0)
						spanidSet.remove(spanid);
				}
				entropy += this.getMeanEntropy(termids, spanidSet.toArray(), alpha);
			}
			entropy /= docnos.length;
			System.out.println("query "+queryids[i]+": "+entropy);
			meanEntropies[i] = entropy;
		}
		System.out.println("Mean entropy: "+Statistics.mean(meanEntropies));
	}
	/**
	 * Tokenise a given query string and get the corresponding termids from lexicon. All tokens
	 * are processed through the system term pipelines.
	 * @param query
	 * @param manager
	 * @param lexicon
	 * @return
	 */
	public static int[] queryStringToTermids(String query, Manager manager, Lexicon _lexicon){
		for (int i=0; i<query.length(); i++)
			if (!Character.isLetterOrDigit(query.charAt(i)) && !Character.isSpaceChar(query.charAt(i)))
				query.replace(query.charAt(i), ' ');
		String[] tokens = query.split(" ");
		THashSet<String> termSet = new THashSet<String>();
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<tokens.length; i++){
			String term = tokens[i].trim().toLowerCase();
			term = manager.pipelineTerm(term);
			if (term!=null && term.trim().length() > 0){
				if (!termSet.contains(term)){
					try{
						LexiconEntry lexEntry = _lexicon.getLexiconEntry(term);
						if (lexEntry!=null){
							termSet.add(term);
							termidSet.add(lexEntry.termId);
						}
					}catch(StringIndexOutOfBoundsException e){
						System.err.println("query: "+query);
						System.err.println("term: "+term);
						e.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
		termSet.clear(); termSet = null;
		//System.err.println("query string: "+query);
		//for (int termid : termidSet.toArray())
		//	System.err.print("("+_lexicon.getLexiconEntry(termid).term+", "+termid+") ");
		//System.err.println();
		return termidSet.toArray();
	}
	
	/**
	 * Tokenise a given query string and get the corresponding termids from lexicon. All tokens
	 * are processed through the system term pipelines.
	 * @param query
	 * @param manager
	 * @param lexicon
	 * @return
	 */
	public static int[] queryStringsToTermids(String[] terms, Lexicon _lexicon){
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<terms.length; i++){
			LexiconEntry lexEntry = _lexicon.getLexiconEntry(terms[i]);
			if (lexEntry!=null){
				//System.err.println("term: "+terms[i]+", id: "+lexEntry.termId);
				termidSet.add(lexEntry.termId);
			}
		}
		return termidSet.toArray();
	}
	
	protected THashMap<String, TIntHashSet> getSpanidsFromResultFile(String docIdxOutputFilename, String resultFilename){
		// load docnos in result file
			// initialise a mapping from docnos to spanids
		THashMap<String, TIntHashSet> docmap = new THashMap<String, TIntHashSet>();
		try{
			System.err.println("Check docnos in "+resultFilename+ "...");
			BufferedReader br = Files.openFileReader(resultFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String docno = tokens[2];
				docmap.put(docno, new TIntHashSet());
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println(docmap.size()+" docnos loaded from "+resultFilename);
		// read through docid output file and find corresponding spanids
		try{
			System.err.println("Loading docidx ...");
			THashSet<String> processedDocnoSet = new THashSet<String>();
			BufferedReader br = Files.openFileReader(docIdxOutputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(",");
				if (tokens.length!=5)
					continue;
				String spanno = tokens[2].trim();
				String docno = spanno.split("-")[0];
				if (docmap.containsKey(docno)){
					docmap.get(docno).add(Integer.parseInt(tokens[0].trim()));
					processedDocnoSet.add(docno);
				}else if (processedDocnoSet.size() == docmap.size())
					break;
			}
			br.close();
			System.out.println(docmap.size()+" entries loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return docmap;
	}
	
	public void convertResultsWithDocid(String docIdxOutputFilename, String resultFilename, String outputFilename){
		THashMap<String, TIntHashSet> docmap = this.getSpanidsFromResultFile(docIdxOutputFilename, resultFilename);	
		// write to output file
		try{
			System.err.println("Converting docnos to docids for "+resultFilename+"...");
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			StringBuffer buf = new StringBuffer();
			for (String docno : docmap.keySet()){
				int[] spanids = docmap.get(docno).toArray();
				Arrays.sort(spanids);
				buf.append(docno);
				for (int i=0; i<spanids.length; i++)
					buf.append(" "+spanids[i]);
				buf.append(ApplicationSetup.EOL);
			}
			bw.write(buf.toString());
			bw.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		// load feedback file and convert
	}
	
	/**
	public void printMeanEntropy(String resultFilename, int topX){
		TObjectDoubleHashMap<String> queryidEntropyMap = new TObjectDoubleHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String currentQueryid = "1st";
			String line = null;
			TIntHashSet docidSet = new TIntHashSet();
			int counter = 0;
			ArrayList<Double> entropies = new ArrayList<Double>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String queryid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				if (currentQueryid.equals("1st")){
					currentQueryid = queryid;
				}else if (!currentQueryid.equals(queryid)){
					int[] docids = docidSet.toArray();
					double entropy = this.getEntropyForTopTerms(docids);
					//System.err.println(entropy);
					queryidEntropyMap.put(currentQueryid, entropy);
					entropies.add(entropy);
					docidSet = new TIntHashSet();
					currentQueryid = queryid;
					counter = 0;
				}
				if (counter < topX)
					docidSet.add(docid);
				counter++;
			}
			System.err.print(currentQueryid+": ");
			int[] docids = docidSet.toArray();
			//for (int i=0; i<docids.length; i++)
				//System.err.print(docIndex.getDocumentNumber(docids[i])+", ");
			double entropy = getEntropy(docids);
			//System.err.println(entropy);
			queryidEntropyMap.put(currentQueryid, entropy);
			entropies.add(entropy);
			docidSet = new TIntHashSet();
			br.close();
			Object[] obj = entropies.toArray(new Double[entropies.size()]);
			double[] values = new double[entropies.size()];
			for (int i=0; i<values.length; i++)
				values[i] = ((Double)obj[i]).doubleValue();
			//System.err.println("Mean: "+Statistics.mean(values));
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return queryidEntropyMap;
	}*/
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--convertresultdocnostospanids")){
			// --convertresultdocnostospanids spanindexpath spanindexprefix docidxoutputFilename resultFilename, outputFilename 
			GenoDocumentSetEntropy app = new GenoDocumentSetEntropy(args[1], args[2]);
			app.convertResultsWithDocid(args[3], args[4], args[5]);
		}else if (args[0].equals("--printmeanentropies")){
			// --printmeanentropies spanindexpath spanindexprefix topicFilename resultFilename docidxoutputFilename topX 
			GenoDocumentSetEntropy app = new GenoDocumentSetEntropy(args[1], args[2]);
			app.printMeanEntropy(args[3], args[4], args[5], Integer.parseInt(args[6]));
		}else if (args[0].equals("--printmeanentropiesforithdoc")){
			// --printmeanentropies spanindexprefix topicFilename resultFilename docidxoutputFilename rank 
			GenoDocumentSetEntropy app = new GenoDocumentSetEntropy(args[1], args[2]);
			app.printMeanEntropyForOneDoc(args[3], args[4], args[5], Integer.parseInt(args[6]));
		}

	}

}
