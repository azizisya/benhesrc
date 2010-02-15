package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.dsms.DistanceModifier;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.models.smoothing.JMSmoothing;
import uk.ac.gla.terrier.models.smoothing.SmoothingMethod;
import uk.ac.gla.terrier.statistics.CosineSimilarity;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.DataUtility;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.utility.IndexUtility;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TroveUtility;
import uk.ac.gla.terrier.utility.io.RandomDataInput;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

public class Blog06 {
	
	public static void replaceScores(String srcFilename, String dstResFilename, String outputFilename){
		try{
			// load 2500 values from src
			
			
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			BufferedReader br = Files.openFileReader(dstResFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				String qid = stk.nextToken();
				String q0 = stk.nextToken();
				String docid = stk.nextToken();
				int rank = Integer.parseInt(stk.nextToken());
				double score = Double.parseDouble(stk.nextToken());
				String tag = stk.nextToken();
				// double newScore = results.getScores(qid)[rank];
			}
			br.close();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void checkRelDistribution(String qrelsFilename, String outputFilename){
		String feedPLMapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+
				"docid2feedid.txt.gz";
		// load docid -> feedid map
		TIntIntHashMap map = new TIntIntHashMap();
		map = DataUtility.loadIntIntHashMap(feedPLMapFilename, 0, 1);
		int[] docids = DataUtility.loadIntHashSet(qrelsFilename, 2).toArray();
		TIntIntHashMap feedRelMap = new TIntIntHashMap();
		for (int docid : docids){
			feedRelMap.adjustOrPutValue(map.get(docid), 1, 1);
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			int counter = 0;
			for (int feedid : feedRelMap.keys()){
				bw.write(feedid+" "+feedRelMap.get(feedid)+ApplicationSetup.EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		/**
		int[] feedSize = feedRelMap.getValues();
		Arrays.sort(feedSize);
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			int counter = 0;
			for (int i=feedSize.length-1; i>=0; i--)
				bw.write((counter++)+" "+feedSize[i]+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}	*/
	}
	
	public static void runOverBaselinePostLM(String resultFilename, String wlogFilename,
            double initBeta, double interval, int nTrials,
            String outputPrefix){
	    // load results
	    TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
	    // load wlog
	    TIntDoubleHashMap docidScoreMap = DataUtility.loadIntDoubleHashMap(wlogFilename, 0, 1);
	    // run
	    for (int i=0; i<nTrials; i++){
	            double beta = initBeta + interval*i;
	            String outputFilename = outputPrefix+Rounding.toString(beta, 2)+".gz";
	            try{
	            		System.out.print("running with beta="+beta+"...");
	                    runOverBaseline(results, docidScoreMap, beta, outputFilename);
	                    System.out.println("Done. Saved at "+outputFilename);
	            }catch(IOException ioe){
	                    ioe.printStackTrace();
	                    System.exit(1);
	            }
	    }
	}
	
	public static void runOverBaselineFeedLM(String resultFilename, String postwFilename,
			String feedwFilename,
            double initBeta, double interval, int nTrials,
            String outputPrefix){
	    // load results
	    TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
	    // load wlog
	    TIntDoubleHashMap map = DataUtility.loadIntDoubleHashMap(feedwFilename, 0, 1);
	    TIntDoubleHashMap docidScoreMap = new TIntDoubleHashMap();
        // convert feedScore map to docScore map
        String feedPLMapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+
			"docid2feedid.txt.gz";
        TIntIntHashMap docFeedidMap = DataUtility.loadIntIntHashMap(feedPLMapFilename, 0, 1);
        TIntIntHashMap feedSizeMap = DataUtility.loadIntSizeHashMap(feedPLMapFilename, 1);
        for (int docid : docFeedidMap.keys()){
        		int feedid = docFeedidMap.get(docid);
                docidScoreMap.put(docid, map.get(docFeedidMap.get(docid))/feedSizeMap.get(feedid));
        }
        map.clear(); docFeedidMap.clear(); map = null; docFeedidMap = null;
        map = DataUtility.loadIntDoubleHashMap(postwFilename, 0, 1);
        for (int docid : docidScoreMap.keys())
        	docidScoreMap.adjustValue(docid, map.get(docid));
	    // run
	    for (int i=0; i<nTrials; i++){
	            double beta = initBeta + interval*i;
	            String outputFilename = outputPrefix+Rounding.toString(beta, 2)+".gz";
	            try{
	            		System.out.print("running with beta="+beta+"...");
	                    runOverBaseline(results, docidScoreMap, beta, outputFilename);
	                    System.out.println("Done. Saved at "+outputFilename);
	            }catch(IOException ioe){
	                    ioe.printStackTrace();
	                    System.exit(1);
	            }
	    }
	}
	
	public static void runOverBaseline(TRECResultsInMemory results, 
			TIntDoubleHashMap map, 
			double beta, 
			String outputFilename)
    throws IOException{
            String[] qids = results.getQueryids();
            Arrays.sort(qids);
            BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
            for (String qid : qids){
                    int[] docids = TroveUtility.stringArrayToIntArray(results.getRetrievedDocnos(qid));
                    double[] scores = results.getScores(qid);
                    int N = docids.length;
                    for (int i=0; i<N; i++){
                            double addendum = map.get(docids[i]);
                            if (!Double.isNaN(addendum))
                                    scores[i] = beta*scores[i]+addendum;
                            // bw.write(qid+" Q0 "+docids[i]+" "+i+" "+scores[i]+" tag"+ApplicationSetup.EOL);
                    }
                    short[] dummy = new short[N];
                    HeapSort.descendingHeapSort(scores, docids, dummy);
                    for (int i=0; i<N; i++)
                    	bw.write(qid+" Q0 "+docids[i]+" "+i+" "+scores[i]+" tag"+ApplicationSetup.EOL);
            }
            bw.close();
    }
	
	public static void docid2FeedidInResults(String resultFilename, String outputFilename){
		try{
			String feedPLMapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+
				"docid2feedid.txt.gz";
			// load docid -> feedid map
			TIntIntHashMap map = new TIntIntHashMap();
			BufferedReader br = Files.openFileReader(feedPLMapFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				map.put(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
			}
			br.close();
			br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int feedid = map.get(Integer.parseInt(tokens[2]));
				bw.write(tokens[0]+" "+tokens[1]+" "+feedid+" "+tokens[3]+" "+tokens[4]+" "+tokens[5]+ApplicationSetup.EOL);
			}
			bw.close();
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void computeSentimentWeights(String qrelsFilename, String qemodelName, String outputFilename){
		// get all documents with relevance grade >= 2
		TIntHashSet docidSet = new TIntHashSet();
		System.out.print("Loading qrels file "+qrelsFilename+"...");
		try{
			BufferedReader br = Files.openFileReader(qrelsFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				if (Integer.parseInt(tokens[3])>=2)
					docidSet.add(Integer.parseInt(tokens[2]));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+docidSet.size()+" docids loads.");
		// compute KL or other sort of weights for each term
		System.out.print("Extracting informative terms using "+qemodelName+"...");
		Index index = Index.createIndex();
		ExtractInformativeTerms app = new ExtractInformativeTerms(index);
		ExpansionTerm[] terms = app.getInformativeTerms(docidSet.toArray(), 10000, qemodelName);
		System.out.println("Done. "+terms.length+" terms extracted.");
		// sort and output
		System.out.print("Flush to disk...");
		Arrays.sort(terms);
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			for (int i=0; i<terms.length; i++)
				bw.write(terms[i].getTermID()+" "+terms[i].getToken()+" "+terms[i].getWeightExpansion()+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+terms.length+" info terms stored at "+outputFilename);
	}
	
	public static void convertfeedPLMapFile(){
		String filename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"feed2docno.txt.gz";
		String outputFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"docid2feedid.txt.gz";
		String feedidFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"feedidmap.txt.gz";
		String docidxFilename = "/home/ben/tr.ben/indices/Blog06/Stopwords_PorterStemmer_blocks/data.docid";
		
		/**
		 * Convert docnos to docids
		 */	
		TObjectIntHashMap<String> docmap = new TObjectIntHashMap<String>();
		try{
			System.err.println("Loading docidx ...");
			DocumentIndex docidx = new DocumentIndex(docidxFilename);
			int N = docidx.getNumberOfDocuments();
			byte[] buffer = new byte[docidx.DOCNO_BYTE_LENGTH];
			RandomDataInput docIndex = Files.openFileRandom(docidxFilename);
			int docid = 0; int docLength = 0; String docno = null; 
			long endOffset = 0L; byte endBitOffset = 0;
			for (int i = 0; i < N; i++) {
				docIndex.seek(i * docidx.entryLength);
				docid = docIndex.readInt();
				docLength = docIndex.readInt();
				docIndex.readFully(buffer, 0, docidx.DOCNO_BYTE_LENGTH);
				docno = new String(buffer).trim();
				endOffset = docIndex.readLong();
				endBitOffset = docIndex.readByte();
				docmap.put(docno, docid);
			}
			docIndex.close(); docidx.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// load feedback file and convert
		try{
			BufferedWriter bw1 = (BufferedWriter)Files.writeFileWriter(feedidFilename);
			BufferedWriter bw2 = (BufferedWriter)Files.writeFileWriter(outputFilename);
			System.err.println("Converting docnos to docids for "+filename+"...");
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			THashSet<String> feednoSet = new THashSet<String>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String feedno = tokens[0]; String docno = tokens[1];
				feednoSet.add(feedno);
				bw1.write(1+" "+feedno+ApplicationSetup.EOL);
				bw2.write(docmap.get(docno)+" "+(feednoSet.size()-1)+ApplicationSetup.EOL);
			}
			br.close(); bw1.close(); bw2.close();
			System.out.println("Done. Saved in "+outputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static double getFeedLM(
			SmoothingMethod smoothing, 
			int[] termids, double[] qtw, int[] docids, int[] freqinColl,
			DirectIndex di, 
			DocumentIndex docidx, 
			Lexicon lex){
		int length = 0;
		for (int docid : docids){
			int doclength = docidx.getDocumentLength(docid);
			if (doclength > 0)
				length += doclength;
		}
		
		int[][] tf = new int[termids.length][docids.length];
		
		// for each docid
		double feedLogGenerProb = 0;
		for (int i=0; i<docids.length; i++){
			int[] tfs = IndexUtility.getTermFrequenciesFromDirect(di, docids[i], termids);
			for (int j=0; j<tfs.length; j++)
				tf[j][i] = tfs[j];
		}
		// compute LM
		for (int i=0; i<termids.length; i++){
			// get tf and TF in the feed
			try{
				smoothing.setFreqInColl(freqinColl[i]);
			}catch(ArrayIndexOutOfBoundsException e){
				System.err.println("freqinColl.length: "+freqinColl.length+", termids.length: "+termids.length+", i:"+i);
				e.printStackTrace();
				System.exit(1);
			}
			int TF = Statistics.sum(tf[i]);
			// build LM
			feedLogGenerProb += qtw[i]*Idf.log(smoothing.smooth(TF, length));
		}
		return feedLogGenerProb;
	}
	
	public static double getFeedWeight(
			WeightingModel model, 
			int[] termids, double[] qtw, int[] docids, int[] freqinColl,
			DirectIndex di, 
			DocumentIndex docidx, 
			Lexicon lex){
		int length = 0;
		for (int docid : docids){
			int doclength = docidx.getDocumentLength(docid);
			if (doclength > 0)
				length += doclength;
		}
		
		int[][] tf = new int[termids.length][docids.length];
		
		// for each docid
		double feedW = 0;
		for (int i=0; i<docids.length; i++){
			int[] tfs = IndexUtility.getTermFrequenciesFromDirect(di, docids[i], termids);
			for (int j=0; j<tfs.length; j++)
				tf[j][i] = tfs[j];
		}
		// compute feed weight
		for (int i=0; i<termids.length; i++){
			// get tf and TF in the feed
			model.setTermFrequency(freqinColl[i]);
			model.setKeyFrequency(qtw[i]);
			int TF = Statistics.sum(tf[i]);
			// build LM
			feedW += model.score((double)TF, (double)length);
		}
		return feedW;
	}
	
	public static double getPostLM(
			SmoothingMethod smoothing, 
			int[] termids, double[] qtw, int docid, int[] freqinColl,
			DirectIndex di, 
			DocumentIndex docidx, 
			Lexicon lex){
		int length = docidx.getDocumentLength(docid);
		double postLM = 0;
		int[] tf = IndexUtility.getTermFrequenciesFromDirect(di, docid, termids);
		// compute LM
		for (int i=0; i < termids.length; i++){
			// get tf and TF in the feed
			int termid = termids[i];
			smoothing.setFreqInColl(freqinColl[i]);
			// build LM
			postLM += qtw[i]*Idf.log(smoothing.smooth(tf[i], length));
		}
		return postLM;
	}
	
	public static double getPostWeight(
			WeightingModel model, 
			int[] termids, double[] qtw, int[] freqinColl, int docid,
			DirectIndex di, 
			DocumentIndex docidx, 
			Lexicon lex){
		int length = docidx.getDocumentLength(docid);
		double postW = 0;
		int[] tf = IndexUtility.getTermFrequenciesFromDirect(di, docid, termids);
		// compute LM
		for (int i=0; i < termids.length; i++){
			// get tf and TF in the feed
			int termid = termids[i];
			model.setTermFrequency(freqinColl[i]); model.setKeyFrequency(qtw[i]);
			// build LM
			postW += model.score((double)tf[i], (double)length);
		}
		return postW;
	}
	
	public static double getPostHiddenLM(SmoothingMethod smoothing, int feedSize, double feedLM){
		return Idf.log(1d/feedSize)+feedLM;
	}
	
	/**
	 * 
	 * @param wordlistFilename
	 * @param topX
	 * @param outputPrefix = /home/ben/tr.ben/uniworkspace/etc/Blog06/06.qrels.opinion.KL.feedLM.top
	 */
	public static void computeFeedSentLM(String wordlistFilename, int beginX, int endX, int intX, 
			String outputPrefix){
		// load top ranked words
		System.out.print("Loading sentiment words...");
		int n = (endX-beginX)/intX + 1;
		
		int[] termids = new int[endX];
		double[] qtw = new double[endX];
		try{
			BufferedReader br = Files.openFileReader(wordlistFilename);
			String line = null;
			int counter = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				termids[counter] = Integer.parseInt(tokens[0]);
				qtw[counter] = Double.parseDouble(tokens[2]);
				counter++;
				if (counter==endX)
					break;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+termids.length+" words loaded.");
		// load feedid to docid map
		String idmapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"docid2feedid.txt.gz";
		
		Index index = Index.createIndex();
		DirectIndex di = index.getDirectIndex();
		DocumentIndex docidx = index.getDocumentIndex();
		Lexicon lex = index.getLexicon();
		
		int[] freqinColl = new int[endX];
		for (int i=0; i<endX; i++){
			freqinColl[i] = lex.getLexiconEntry(termids[i]).TF;
			// System.err.println("freqinColl["+i+"]="+freqinColl[i]);
		}
		
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.8"));
		JMSmoothing smoothing = new JMSmoothing(index.getCollectionStatistics().getNumberOfTokens(), alpha);
		// for each feed
		try{
			BufferedWriter[] bw = new BufferedWriter[n];
			int X = beginX;
			for (int i=0; i<n; i++){
				bw[i] = (BufferedWriter)Files.writeFileWriter(outputPrefix+X+".gz");
				X+=intX;
			}
			BufferedReader br = Files.openFileReader(idmapFilename);
			String line = null;
			int feedid = 0;
			TIntHashSet docidSet = new TIntHashSet();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = Integer.parseInt(tokens[0]);
				int currentFeedid = Integer.parseInt(tokens[1]);
				if (currentFeedid == feedid)
					docidSet.add(docid);
				else{
					System.out.print(feedid+": ");
					X = beginX;
					for (int i=0; i<n; i++){
						int[] termidsX = new int[X];
						int[] freqinCollX = new int[X];
						double[] qtwX = new double[X];
						System.arraycopy(termids, 0, termidsX, 0, X);
						System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
						System.arraycopy(qtw, 0, qtwX, 0, X);
						double feedLogGenerProb = getFeedLM(smoothing, termidsX, qtwX, docidSet.toArray(), freqinCollX, di, docidx, lex);
						// build LM
						System.out.print(feedLogGenerProb+" ");
						// write
						bw[i].write(feedid+" "+feedLogGenerProb+ApplicationSetup.EOL);
						X+=intX; 
						termidsX = null; freqinCollX = null;
					}
					feedid = currentFeedid;
					docidSet.clear(); docidSet.add(docid);
					System.out.println();
					System.gc();
				}
			}
			br.close();
			System.out.print(feedid+": ");
			X = beginX;
			for (int i=0; i<n; i++){
				int[] termidsX = new int[X];
				int[] freqinCollX = new int[X];
				double[] qtwX = new double[X];
				System.arraycopy(termids, 0, termidsX, 0, X);
				System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
				System.arraycopy(qtw, 0, qtwX, 0, X);
				double feedLogGenerProb = getFeedLM(smoothing, termidsX, qtwX, docidSet.toArray(), freqinCollX, di, docidx, lex);
				// build LM
				System.out.print(feedLogGenerProb+" ");
				// write
				bw[i].write(feedid+" "+feedLogGenerProb+ApplicationSetup.EOL);
				X+=intX; 
				termidsX = null; freqinCollX = null;
			}
			docidSet.clear();
			System.out.println();
			for (int i=0; i<n; i++)
				bw[i].close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Saved at "+outputPrefix);
	}
	
	/**
	 * 
	 * @param wordlistFilename
	 * @param topX
	 * @param outputFilename
	 */
	public static void computePostHiddenSentLM(String feedLMFilename, String outputFilename){
		// load feed LM
		System.out.print("Loading feed sentiment language model...");
		TIntDoubleHashMap feedLMMap = new TIntDoubleHashMap();
		try{
			BufferedReader br = Files.openFileReader(feedLMFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				feedLMMap.put(Integer.parseInt(tokens[0]), Double.parseDouble(tokens[1]));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+feedLMMap.size()+" feed sentiment LMs loaded.");
		// load feedid to docid map
		String idmapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"docid2feedid.txt.gz";
		
		Index index = Index.createIndex();
		DirectIndex di = index.getDirectIndex();
		DocumentIndex docidx = index.getDocumentIndex();
		Lexicon lex = index.getLexicon();
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.8"));
		JMSmoothing smoothing = new JMSmoothing(index.getCollectionStatistics().getNumberOfTokens(), alpha);
		// for each feed
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			BufferedReader br = Files.openFileReader(idmapFilename);
			String line = null;
			int feedid = 0;
			TIntHashSet docidSet = new TIntHashSet();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int currentFeedid = Integer.parseInt(tokens[1]);
				if (currentFeedid == feedid)
					docidSet.add(Integer.parseInt(tokens[0]));
				else{
					// compute LM for each docid in the feed
					for (int docid : docidSet.toArray()){
						System.out.print(docid+": ");
						double docLM = getPostHiddenLM(smoothing, docidSet.size(), feedLMMap.get(feedid));
						// build LM
						System.out.println(docLM);
						// write
						bw.write(docid+" "+docLM+ApplicationSetup.EOL);
					}
					feedid = currentFeedid;
					docidSet.clear(); docidSet.add(Integer.parseInt(tokens[0]));
					System.gc();
				}
			}
			br.close();
			// compute LM for each docid in the feed
			for (int docid : docidSet.toArray()){
				System.out.print(docid+": ");
				double docLM = getPostHiddenLM(smoothing, docidSet.size(), feedLMMap.get(feedid));
				// build LM
				System.out.println(docLM);
				// write
				bw.write(docid+" "+docLM+ApplicationSetup.EOL);
			}
			docidSet.clear(); 
			System.gc();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Saved at "+outputFilename);
	}
	
	/**
	 * 
	 * @param wordlistFilename
	 * @param topX
	 * @param outputFilename
	 */
	public static void computePostSentLM(String wordlistFilename, 
			int beginX, int endX, int intX, String outputPrefix){
		// load top ranked words
		System.out.print("Loading sentiment words...");
		int n = (endX-beginX)/intX + 1;
		int[] termids = new int[endX];
		double[] qtw = new double[endX];
		try{
			BufferedReader br = Files.openFileReader(wordlistFilename);
			String line = null;
			int counter = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				termids[counter] = Integer.parseInt(tokens[0]);
				qtw[counter] = Double.parseDouble(tokens[2]);
				counter++;
				if (counter==endX)
					break;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+termids.length+" words loaded.");
		// load feedid to docid map
		String idmapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"docid2feedid.txt.gz";
		
		Index index = Index.createIndex();
		DirectIndex di = index.getDirectIndex();
		DocumentIndex docidx = index.getDocumentIndex();
		Lexicon lex = index.getLexicon();
		
		int[] freqinColl = new int[endX];
		for (int i=0; i<endX; i++)
			freqinColl[i] = lex.getLexiconEntry(termids[i]).TF;
		
		double alpha = Double.parseDouble(ApplicationSetup.getProperty("jm.smoothing.alpha", "0.8"));
		JMSmoothing smoothing = new JMSmoothing(index.getCollectionStatistics().getNumberOfTokens(), alpha);
		// for each feed
		try{
			BufferedWriter[] bw = new BufferedWriter[n];
			int X = beginX;
			for (int i=0; i<n; i++){
				bw[i] = (BufferedWriter)Files.writeFileWriter(outputPrefix+X+".gz");
				X+=intX;
			}
			BufferedReader br = Files.openFileReader(idmapFilename);
			String line = null;
			int feedid = 0;
			TIntHashSet docidSet = new TIntHashSet();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int currentFeedid = Integer.parseInt(tokens[1]);
				if (currentFeedid == feedid)
					docidSet.add(Integer.parseInt(tokens[0]));
				else{
					// compute LM for each docid in the feed
					for (int docid : docidSet.toArray()){
						System.out.print(docid+": ");
						X = beginX;
						for (int i=0; i<n; i++){
							int[] termidsX = new int[X]; int[] freqinCollX = new int[X];
							double[] qtwX = new double[X];
							System.arraycopy(termids, 0, termidsX, 0, X);
							System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
							System.arraycopy(qtw, 0, qtwX, 0, X);
							double docLM = getPostLM(smoothing, 
									termidsX, qtwX, docid, freqinCollX,
									di, 
									docidx, 
									lex);
							// build LM
							System.out.print(docLM+" ");
							// write
							bw[i].write(docid+" "+docLM+ApplicationSetup.EOL);
							X+=intX;
							termidsX = null; freqinCollX = null;
						}
						System.out.println();
					}
					feedid = currentFeedid;
					docidSet.clear(); docidSet.add(Integer.parseInt(tokens[0]));
					System.gc();
				}
			}
			br.close();
//			 compute LM for each docid in the feed
			for (int docid : docidSet.toArray()){
				System.out.print(docid+": ");
				X = beginX;
				for (int i=0; i<n; i++){
					int[] termidsX = new int[X]; int[] freqinCollX = new int[X];
					double[] qtwX = new double[X];
					System.arraycopy(termids, 0, termidsX, 0, X);
					System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
					System.arraycopy(qtw, 0, qtwX, 0, X);
					double docLM = getPostLM(smoothing, 
							termidsX, qtwX, docid, freqinCollX,
							di, 
							docidx, 
							lex);
					// build LM
					System.out.print(docLM+" ");
					// write
					bw[i].write(docid+" "+docLM+ApplicationSetup.EOL);
					X+=intX;
					termidsX = null; freqinCollX = null;
				}
				System.out.println();
			}
			docidSet.clear(); 
			System.gc();
			for (int i=0; i<n; i++)
				bw[i].close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Saved at "+outputPrefix);
	}
	
	/**
	 * 
	 * @param wordlistFilename
	 * @param topX
	 * @param outputFilename
	 */
	public static void computeFeedSentWeight(String wordlistFilename,
			int beginX, int endX, int intX, 
			String modelname, double c, 
			String outputPrefix){
		// load top ranked words
		System.out.print("Loading sentiment words...");
		int[] termids = new int[endX];
		double[] qtw = new double[endX];
		int n = (endX-beginX)/intX+1;
		try{
			BufferedReader br = Files.openFileReader(wordlistFilename);
			String line = null;
			int counter = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				termids[counter] = Integer.parseInt(tokens[0]);
				qtw[counter++] = Double.parseDouble(tokens[2]);
				if (counter==endX)
					break;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+termids.length+" words loaded.");
		// load feedid to docid map
		String idmapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"docid2feedid.txt.gz";
		
		Index index = Index.createIndex();
		DirectIndex di = index.getDirectIndex();
		DocumentIndex docidx = index.getDocumentIndex();
		Lexicon lex = index.getLexicon();
		
		int[] freqinColl = new int[endX];
		for (int i=0; i<endX; i++)
			freqinColl[i] = lex.getLexiconEntry(termids[i]).TF;
		
		WeightingModel model = WeightingModel.getWeightingModel(modelname);
		model.setNumberOfTokens(index.getCollectionStatistics().getNumberOfTokens());
		model.setAverageDocumentLength(index.getCollectionStatistics().getAverageDocumentLength());
		model.setNumberOfDocuments(index.getCollectionStatistics().getNumberOfDocuments());
		model.setNumberOfPointers(index.getCollectionStatistics().getNumberOfPointers());
		model.setNumberOfUniqueTerms(index.getCollectionStatistics().getNumberOfUniqueTerms());
		model.setParameter(c);
		
		// for each feed
		try{
			BufferedWriter[] bw = new BufferedWriter[n];
			int X = beginX;
			for (int i=0; i<n; i++){
				bw[i] = (BufferedWriter)Files.writeFileWriter(outputPrefix+X+".gz");
				X+=intX;
			}
			BufferedReader br = Files.openFileReader(idmapFilename);
			String line = null;
			int feedid = 0;
			TIntHashSet docidSet = new TIntHashSet();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = Integer.parseInt(tokens[0]);
				int currentFeedid = Integer.parseInt(tokens[1]);
				if (currentFeedid == feedid)
					docidSet.add(docid);
				else{
					System.out.print(feedid+": ");
					X = beginX;
					for (int i=0; i<n; i++){
						int[] termidsX = new int[X]; double[] qtwX = new double[X];
						int[] freqinCollX = new int[X];
						System.arraycopy(termids, 0, termidsX, 0, X);
						System.arraycopy(qtw, 0, qtwX, 0, X);
						System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
						double feedLogGenerProb = getFeedWeight(model, termidsX, qtwX, docidSet.toArray(), freqinCollX, di, docidx, lex);
						// build LM
						System.out.print(feedLogGenerProb+" ");
						// write
						bw[i].write(feedid+" "+feedLogGenerProb+ApplicationSetup.EOL);
						X+=intX;
						termidsX = null; freqinCollX = null; qtwX = null;
					}
					feedid = currentFeedid;
					System.out.println();
					docidSet.clear(); docidSet.add(docid);
					System.gc();
				}
			}
			br.close();
			System.out.print(feedid+": ");
			X = beginX;
			for (int i=0; i<n; i++){
				int[] termidsX = new int[X]; double[] qtwX = new double[X];
				int[] freqinCollX = new int[X];
				System.arraycopy(termids, 0, termidsX, 0, X);
				System.arraycopy(qtw, 0, qtwX, 0, X);				 
				System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
				for (int j=0; j<X; j++)
					freqinCollX[j] = freqinColl[j];
				double feedLogGenerProb = getFeedWeight(model, termidsX, qtwX, docidSet.toArray(), freqinCollX, di, docidx, lex);
				// build LM
				System.out.print(feedLogGenerProb+" ");
				// write
				bw[i].write(feedid+" "+feedLogGenerProb+ApplicationSetup.EOL);
				X+=intX;
				termidsX = null; freqinCollX = null; qtwX = null;
			}
			for (int i=0; i<n; i++)
				bw[i].close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Saved at "+outputPrefix);
	}
	
	/**
	 * 
	 * @param wordlistFilename
	 * @param topX
	 * @param outputFilename
	 */
	public static void computePostSentWeight(String wordlistFilename, 
			int beginX, int endX, int intX, 
			String modelname, double c, 
			String outputPrefix){
		// load top ranked words
		System.out.print("Loading sentiment words...");
		int[] termids = new int[endX];
		double[] qtw = new double[endX];
		int n = (endX-beginX)/intX+1;
		try{
			BufferedReader br = Files.openFileReader(wordlistFilename);
			String line = null;
			int counter = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				termids[counter] = Integer.parseInt(tokens[0]);
				qtw[counter++] = Double.parseDouble(tokens[2]);
				if (counter==endX)
					break;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+termids.length+" words loaded.");
		// load feedid to docid map
		String idmapFilename = ApplicationSetup.TERRIER_ETC+ApplicationSetup.FILE_SEPARATOR+"VLDB"+ApplicationSetup.FILE_SEPARATOR+"docid2feedid.txt.gz";
		
		Index index = Index.createIndex();
		DirectIndex di = index.getDirectIndex();
		DocumentIndex docidx = index.getDocumentIndex();
		Lexicon lex = index.getLexicon();
		
		int[] freqinColl = new int[endX];
		for (int i=0; i<endX; i++)
			freqinColl[i] = lex.getLexiconEntry(termids[i]).TF;
		
		WeightingModel model = WeightingModel.getWeightingModel(modelname);
		model.setNumberOfTokens(index.getCollectionStatistics().getNumberOfTokens());
		model.setAverageDocumentLength(index.getCollectionStatistics().getAverageDocumentLength());
		model.setNumberOfDocuments(index.getCollectionStatistics().getNumberOfDocuments());
		model.setNumberOfPointers(index.getCollectionStatistics().getNumberOfPointers());
		model.setNumberOfUniqueTerms(index.getCollectionStatistics().getNumberOfUniqueTerms());
		model.setParameter(c);
		
		// for each feed
		try{
			BufferedWriter[] bw = new BufferedWriter[n];
			int X = beginX;
			for (int i=0; i<n; i++){
				bw[i] = (BufferedWriter)Files.writeFileWriter(outputPrefix+X+".gz");
				X+=intX;
			}
			BufferedReader br = Files.openFileReader(idmapFilename);
			String line = null;
			int feedid = 0;
			TIntHashSet docidSet = new TIntHashSet();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int currentFeedid = Integer.parseInt(tokens[1]);
				if (currentFeedid == feedid)
					docidSet.add(Integer.parseInt(tokens[0]));
				else{
					// compute LM for each docid in the feed
					for (int docid : docidSet.toArray()){
						System.out.print(docid+": ");
						X = beginX;
						for (int i=0; i<n; i++){
							int[] termidsX = new int[X]; double[] qtwX = new double[X];
							int[] freqinCollX = new int[X];
							System.arraycopy(termids, 0, termidsX, 0, X);
							System.arraycopy(qtw, 0, qtwX, 0, X);							 
							System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
							double docLM = getPostWeight(model, 
									termidsX, qtwX, freqinCollX, docid,
									di, 
									docidx, 
									lex);
							// build LM
							System.out.print(docLM+" ");
							// write
							bw[i].write(docid+" "+docLM+ApplicationSetup.EOL);
							X+=intX;
							termidsX = null; freqinCollX = null; qtwX = null;
						}
						System.out.println();
					}
					feedid = currentFeedid;
					docidSet.clear(); docidSet.add(Integer.parseInt(tokens[0]));
					System.gc();
				}
			}
			br.close();
//			 compute LM for each docid in the feed
			for (int docid : docidSet.toArray()){
				System.out.print(docid+": ");
				X = beginX;
				for (int i=0; i<n; i++){
					int[] termidsX = new int[X]; double[] qtwX = new double[X];
					int[] freqinCollX = new int[X];
					System.arraycopy(termids, 0, termidsX, 0, X);
					System.arraycopy(qtw, 0, qtwX, 0, X);
					System.arraycopy(freqinColl, 0, freqinCollX, 0, X);
					double docLM = getPostWeight(model, 
							termidsX, qtwX, freqinCollX, docid,
							di, 
							docidx, 
							lex);
					// build LM
					System.out.print(docLM+" ");
					// write
					bw[i].write(docid+" "+docLM+ApplicationSetup.EOL);
					X+=intX;
					termidsX = null; freqinCollX = null; qtwX = null;
				}
				System.out.println();
			}
			docidSet.clear(); 
			System.gc();
			for (int i=0; i<n; i++)
				bw[i].close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		index.close();
		System.out.println("Done. Saved at "+outputPrefix);
	}
	
	public static void getTopicalTermSimilarity(String filename1, String filename2, int topX){
		TIntDoubleHashMap map1 = DataUtility.loadIntDoubleHashMap(filename1, 0, 2, topX);
		TIntDoubleHashMap map2 = DataUtility.loadIntDoubleHashMap(filename2, 0, 2, topX);
		double sim = CosineSimilarity.cosine(map1, map2);
		System.out.println("similarity: "+sim);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--convertfeeddocmap"))
		// --convertfeeddocmap 
			Blog06.convertfeedPLMapFile();
		else if (args[0].equals("--computesentimentweights")){
			// --computesentimentweights qrelsFilename qemodelName outputFilename
			Blog06.computeSentimentWeights(args[1], args[2], args[3]);
		}else if (args[0].equals("--computefeedsentlm")){
			// --computefeedsentlm wordlistFilename beginX endX intX outputPrefix
			Blog06.computeFeedSentLM(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
		}else if (args[0].equals("--docid2feedidinresults")){
			// --docid2feedidinresults resultFilename outputFilename
			Blog06.docid2FeedidInResults(args[1], args[2]);
		}else if (args[0].equals("--computeposthiddenlm")){
			// --computeposthiddenlm feedLMFilename outputFilename
			Blog06.computePostHiddenSentLM(args[1], args[2]);
		}else if (args[0].equals("--computepostsentlm")){
			// --computepostsentlm wordlistFilename beginX endX intX outputPrefix
			Blog06.computePostSentLM(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), 
					Integer.parseInt(args[4]), args[5]);
		}else if (args[0].equals("--computepostsentweight")){
			// --computepostsentweight wordlistFilename, beginX, endX, intX, modelname, c, outputPrefix
			Blog06.computePostSentWeight(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
					Integer.parseInt(args[4]), args[5], Double.parseDouble(args[6]), args[7]);
		}else if (args[0].equals("--computefeedsentweight")){
			// --computefeedsentweight wordlistFilename, beginX, endX, iniX, modelname, c, outputPrefix
			Blog06.computeFeedSentWeight(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]),
					Integer.parseInt(args[4]), args[5], Double.parseDouble(args[6]), args[7]);
		}else if (args[0].equals("--runoverbaselinepostlm")){
            // --runoverbaselinepostlm resultFilename, wlogFilename, initBeta, interval, nTrials, outputPrefix
            Blog06.runOverBaselinePostLM(args[1], args[2], Double.parseDouble(args[3]), Double.parseDouble(args[4]),
                            Integer.parseInt(args[5]), args[6]);
		}else if (args[0].equals("--runoverbaselinefeedlm")){
            // --runoverbaselinepostlm resultFilename, postwFilename, feedwFilename, initBeta, interval, nTrials, outputPrefix
			Blog06.runOverBaselineFeedLM(args[1], args[2], args[3], Double.parseDouble(args[4]), Double.parseDouble(args[5]), 
					Integer.parseInt(args[6]), args[7]);
		}else if (args[0].equals("--checkreldistr")){
			// --checkreldistr qrelsFilename, outputFilename
			Blog06.checkRelDistribution(args[1], args[2]);
		}else if (args[0].equals("--getcos")){
			// --getcos filename1, filename2, topX
			Blog06.getTopicalTermSimilarity(args[1], args[2], Integer.parseInt(args[3]));
		}else if (args[0].equals("--test")){
			int[] values = DataUtility.loadInt(args[1], 1);
			int topx = Integer.parseInt(args[2]);
			int[] buf = new int[topx];
			System.arraycopy(values, 0, buf, 0, topx);
			int sum = Statistics.sum(buf);
			System.out.println(sum+", "+(double)sum/25842);
		}


	}

}
