package uk.ac.gla.terrier.querying.termselector;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;


import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.proximity.NGramCounter;
import uk.ac.gla.terrier.matching.models.proximity.ProximityModel;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Distance;
import uk.ac.gla.terrier.utility.Rounding;

public class GeometricTermSelector extends TermSelector {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	protected int EXPANSION_MIN_DOCUMENTS;
	
	protected double avgNgramLength;
	
	protected ProximityModel proxModel;
	
	protected int[] bgDocids;
	
	protected double w_roc = Double.parseDouble(ApplicationSetup.getProperty("proximity.w_roc", "1d"));
	
	protected TIntObjectHashMap<int[][]> postingCache;
	
	// candidate termid -> set of termids of phrase terms
	protected TIntObjectHashMap<TIntHashSet> proxTermMap;
	
	protected boolean CombinedQE = Boolean.parseBoolean(
			ApplicationSetup.getProperty("proximity.qe.combined", "false"));
	
	protected boolean ThreadedNgramCounting = Boolean.parseBoolean(ApplicationSetup.getProperty("proximity.count.threaded", "false"));
	
	
	
	protected int bgSize = Integer.parseInt(ApplicationSetup.getProperty("proximity.bg.size", "0"));

	public GeometricTermSelector(Index index) {
		super(index);
		this.EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup.getProperty("expansion.mindocuments","2"));
		proxModel = ProximityModel.getDefaultProximityModel();
	}

	public GeometricTermSelector() {
		this.EXPANSION_MIN_DOCUMENTS = Integer.parseInt(ApplicationSetup.getProperty("expansion.mindocuments","2"));
		proxModel = ProximityModel.getDefaultProximityModel();
	}

	@Override
	public void assignTermWeights(ResultSet resultSet, int feedbackSetSize,
			WeightingModel QEModel, Lexicon bgLexicon) {
		int[] docids = resultSet.getDocids();
		feedbackSetSize = Math.min(feedbackSetSize, docids.length);
		this.setResultSet(resultSet);
			
		assignTermWeights(Arrays.copyOf(docids, feedbackSetSize), QEModel, bgLexicon);

	}
	
	protected void mergeWithRocchioTermSelector(int[] fbDocids, WeightingModel qemodel, Lexicon bgLexicon){
		TermSelector selector = TermSelector.getTermSelector("RocchioTermSelector", index);
		String[] termStrings = new String[this.originalQueryTermidSet.size()];
		int[] termids = this.originalQueryTermidSet.toArray();
		for (int i=0; i<this.originalQueryTermidSet.size(); i++)
			termStrings[i] = lexicon.getLexiconEntry(termids[i]).term;
		selector.setOriginalQueryTerms(termStrings);
		selector.setResultSet(this.resultSet);
		selector.assignTermWeights(fbDocids, qemodel, bgLexicon);
		termids = null;
		ExpansionTerm[] expTerms = selector.getMostWeightedTerms(selector.getNumberOfUniqueTerms());
		for (ExpansionTerm expTerm : expTerms){
			if (termMap.containsKey(expTerm.getTermID())){
				termMap.get(expTerm.getTermID()).setWeightExpansion(termMap.get(expTerm.getTermID()).getWeightExpansion()+
						w_roc*expTerm.getWeightExpansion());
			}
		}
	}
	
	protected void getTerms(int[] docids, TIntHashSet phraseTermidSet, int wSize){
		if (wSize==0){
			this.getTerms(docids);
			return;
		}
		this.feedbackSetLength = 0;
		termMap = new TIntObjectHashMap<ExpansionTerm>();
		this.proxTermMap = new TIntObjectHashMap<TIntHashSet>();
		for (int docid : docids) {
			int[][] terms = di.getTerms(docid);
			if (terms == null)
				logger.warn("document "+"("+docid+") not found");
			else{
				feedbackSetLength += terms[0].length;
				// map from position to termid
				TIntIntHashMap map = new TIntIntHashMap();
				int blockIdx = 0;
				TIntHashSet phraseTermPosSet = new TIntHashSet(); 
				TIntIntHashMap termidFreqMap = new TIntIntHashMap();
				for (int j=0; j<terms[0].length; j++){
					int termid = terms[0][j];
					termidFreqMap.put(termid, terms[1][j]);
					int blockFreq = terms[3][j];
					terms[3][j] = blockIdx;
					for (int k=0; k<blockFreq; k++){
						if (phraseTermidSet.contains(termid))
							phraseTermPosSet.add(terms[4][blockIdx]);
						map.put(terms[4][blockIdx++], termid);
					}
				}
				
				// find termids close to original query terms
				for (int pos : phraseTermPosSet.toArray()){
					int phraseTermid = map.get(pos);
					for (int i=pos-wSize+1; i<pos+wSize; i++){
						if (i<0)
							continue;
						int termid = map.get(i);
						this.insertTerm(termid, termidFreqMap.get(termid));
						if (proxTermMap.get(termid) == null){
							TIntHashSet tmpSet = new TIntHashSet();
							tmpSet.add(phraseTermid);
							proxTermMap.put(termid, tmpSet);
						}
					}
				}
				phraseTermPosSet.clear(); phraseTermPosSet = null;
				termidFreqMap.clear(); termidFreqMap = null;
				map.clear(); map = null;
			}
			terms = null;
		}
	}
	
	public void assignTermWeights(TIntIntHashMap[] termidFreqMap, WeightingModel QEModel, 
			TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap){
		// TODO: to implement
	}


	@Override
	public void assignTermWeights(int[] fbDocids, WeightingModel QEModel,
			Lexicon bgLexicon) {
		TIntHashSet phraseTermidSet = new TIntHashSet();
		int numberOfDocuments = index.getCollectionStatistics().getNumberOfDocuments();
		// accept only terms appear in less than or equal to 5% of documents in the collection. 
		for (int termid : this.originalQueryTermidSet.toArray()){
			int docFrequency = bgLexicon.getLexiconEntry(termid).n_t;
			if ((double)docFrequency/numberOfDocuments <= 0.05d)
				phraseTermidSet.add(termid);
			else
				logger.debug("term "+bgLexicon.getLexiconEntry(termid).term+" ignored for high document frequency.");
		}
		if (phraseTermidSet.size() == 0){
			logger.debug("All query terms are ignored for high document frequency.");
			return;
		}
		for (int termid : phraseTermidSet.toArray())
			logger.debug("phrase term: "+bgLexicon.getLexiconEntry(termid).term);
		final int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length","40"));
		this.getTerms(fbDocids, phraseTermidSet, ngramLength);
		bgDocids = null;
		int [] docids = resultSet.getDocids();
		if (docids.length > bgSize && bgSize != 0){
			bgDocids = new int[bgSize];
			System.arraycopy(docids, 0, bgDocids, 0, bgSize);
		}else{
			bgDocids = docids;
		}
		final double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c","0.10d"));
		QEModel.setParameter(ngramC);
		int[] termids = termMap.keys();
		logger.debug("windowsize: "+ngramLength+", ngramC: "+ngramC+", model: "+QEModel.getInfo());
		logger.debug("number of candidate terms: "+termMap.size());
		
		for (int termid : termids){
			double score = 0d;
			
			int docFrequency = bgLexicon.getLexiconEntry(termid).n_t;
			if ((double)docFrequency/numberOfDocuments > 0.05d)
				termMap.get(termid).setWeightExpansion(0d);
			else if (termMap.get(termid).getDocumentFrequency()<this.EXPANSION_MIN_DOCUMENTS)
				termMap.get(termid).setWeightExpansion(0d);
			else{
				if (ngramLength>1){
					for (int qtermid : this.proxTermMap.get(termid).toArray()){
						if (termid == qtermid)
							continue;
						// only the original query term with the closest geometric association to the candidate term counts
						score = Math.max(score, computeFDScore(QEModel, termid, qtermid, fbDocids, bgDocids, ngramC, ngramLength));
					}
				}else{
					for (int qtermid : this.originalQueryTermidSet.toArray()){
						if (termid == qtermid)
							continue;
						// only the original query term with the closest geometric association to the candidate term counts
						score = Math.max(score, computeFDScore(QEModel, termid, qtermid, fbDocids, bgDocids, ngramC, ngramLength));
					}
				}
				termMap.get(termid).setWeightExpansion(score);
			}
		}
		
		Object[] obj = termMap.getValues();
		ExpansionTerm[] terms = new ExpansionTerm[obj.length];
		for (int i=0; i<terms.length; i++)
			terms[i] = (ExpansionTerm)obj[i];
		Arrays.sort(terms);
		double normalizer = terms[0].getWeightExpansion();
		logger.debug("term with the maximum weight: " + bgLexicon.getLexiconEntry(terms[0].getTermID()).term+
				", normalizer: " + Rounding.toString(normalizer, 4));
		if (normalizer>0d){
			for (ExpansionTerm term : terms){
				term.setWeightExpansion(term.getWeightExpansion()/normalizer);
			}
		}else{/*
			for (int termid : termMap.keys()){
				ExpansionTerm term = termMap.get(termid);
				logger.debug(term.getTermID()+", "+term.getWithinDocumentFrequency()+", "+term.getDocumentFrequency()+", "+term.getWeightExpansion());
			}*/
		}
		if (postingCache!=null){
			this.postingCache.clear(); this.postingCache = null; 
		}
		System.gc();
		if (this.CombinedQE){
			logger.debug("Merge with Rocchio's QE");
			this.mergeWithRocchioTermSelector(fbDocids, QEModel, bgLexicon);
		}
	}
	
	protected double computeFDScore(WeightingModel model, int termid1, int termid2, int[] fbDocids, int[] bgDocids, double ngramC, int ngramLength){
	 	
	 	this.avgNgramLength = index.getCollectionStatistics().getAverageDocumentLength()-ngramLength;
	
		//System.err.println("term 1 and 2: " + term1 + " ("+termid1+") " + term2 + " ("+termid2+")");
	 	
	 	
		int[][] postings1 = this.getPostings(termid1, index);
		if (this.postingCache == null){
			this.postingCache = new TIntObjectHashMap<int[][]>();
		}
		int[][] postings2 = null;
		if (postingCache.containsKey(termid2)){
			postings2 = postingCache.get(termid2);
		}else{
			postings2 = this.getPostings(termid2, index);
			postingCache.put(termid2, postings2);
		}
		
/*		int[] postings14 = postings1[4];
		int[] postings24 = postings2[4];*/
		//find the documents that contain term1 and term2
		final int postings1Length = postings1[0].length;
		final int postings2Length = postings2[0].length;
		
		
		TIntHashSet bgDocidSet = new TIntHashSet(bgDocids);
		bgDocidSet.addAll(fbDocids);
		for (int docid : postings1[0])
			if (!bgDocidSet.contains(docid))
				bgDocidSet.remove(docid);
		for (int docid : postings2[0])
			if (!bgDocidSet.contains(docid))
				bgDocidSet.remove(docid);
		// cooccurredDocidSet.retainAll(postings2[0]);
		
		// for (int docid : docids){
			// logger.debug("doc "+docid+" is in: "+cooccurredDocidSet.contains(docid));
		//}
	
		TIntDoubleHashMap ngramFrequencies = new TIntDoubleHashMap();
		int ngramDocumentFrequency = 0;
		double ngramCollectionFrequency = 0;
		
		if (this.ThreadedNgramCounting){
			TIntIntHashMap docidLengthMap = new TIntIntHashMap();
			DocumentIndex docidx = index.getDocumentIndex();
			for (int docid : bgDocidSet.toArray())
				docidLengthMap.put(docid, docidx.getDocumentLength(docid));
		
			bgDocidSet = null;
			
			
			
			NGramCounter counter = new NGramCounter();
			counter.count(postings1, postings2, ngramLength, docidLengthMap, proxModel);
			
			ngramFrequencies = counter.getNGramFrequencies();
			counter.getNgramDocumentFrequency();
			counter.getNgramCollectionFrequency();
		}else{
			for (int docid : bgDocidSet.toArray()) {
				
				int pos1 = Arrays.binarySearch(postings1[0], docid);
				int pos2 = Arrays.binarySearch(postings2[0], docid);
				if (pos1<0 || pos2<0)
				// if (!bgDocidSet.contains(docid))
					continue;
				//processed++;
				
				//find the places where the terms co-occur closely together
				int start1 = postings1[3][pos1];
				int end1 = pos1==postings1Length-1 ? postings1[4].length : (postings1[3][pos1+1]);
			
				int start2 = postings2[3][pos2];
	   			int end2 = pos2==postings2Length-1 ? postings2[4].length : postings2[3][pos2+1];
	
		
				final int docLength = index.getDocumentIndex().getDocumentLength(docid);	
				final double matchingNGrams = proxModel.getNGramFrequency(postings1[4], start1, end1, postings2[4], start2, end2, ngramLength, docLength);
	   			//if we found matching ngrams, we score them
	   			if (matchingNGrams > 0) {
	   				ngramDocumentFrequency++;
	   				ngramCollectionFrequency+=matchingNGrams;
	   			}
	   			ngramFrequencies.put(docid, matchingNGrams);
			}
			bgDocidSet = null;
		}
		if (ngramDocumentFrequency!=0){
			int pseudoSetNgramFrequency = 0;
			for (int docid : fbDocids){
				pseudoSetNgramFrequency += ngramFrequencies.get(docid);
			}
			
			
			if (pseudoSetNgramFrequency == 0){
				return 0d;
			}
		}
		
		// assign scores
		double[] scores = new double[fbDocids.length];
		this.assignScores(model, scores, fbDocids, ngramLength, ngramC, ngramFrequencies, ngramCollectionFrequency, ngramDocumentFrequency);
		double score = 0d;
		
		/*if (this.EXPANSION_MIN_DOCUMENTS < docids.length){
			int counter = 0;
			for (double value : scores)
				if (value>0){
					if (++counter==this.EXPANSION_MIN_DOCUMENTS){
						score=Statistics.mean(scores);
						break;
					}
				}
		}else*/
			score=Statistics.mean(scores);
			
		postings1 = null;// postings2 = null; 
		scores = null;
		ngramFrequencies.clear(); ngramFrequencies = null;
		System.gc();
		return score;
	}
	
	protected int[][] getPostings(int termid, Index index) {
		
		int[][] postings = null;
		boolean[] flag = {true, false, false, true, true};
		postings = index.getInvertedIndex().getDocuments(termid, flag);
			
		//replace the block frequencies with the index of the blocks in the array
		final int docFrequency = postings[0].length;
		int blockFrequencySum = 0;
		int tmp;
		for (int i = 0; i<docFrequency; i++) {
			tmp = postings[3][i];
			postings[3][i] = blockFrequencySum;
			blockFrequencySum += tmp;
		}
		flag = null;
		return postings;
	}
	
	protected void assignScores(WeightingModel model, double[] scores, int[] docids, int ngramLength, double ngramC, TIntDoubleHashMap ngramFreqMap, double ngramCollFreq, int ngramDocFreq){
		int n = docids.length;
		model.setAverageDocumentLength(this.avgNgramLength);
		model.setParameter(ngramC);
		model.setTermFrequency(ngramCollFreq);
		model.setDocumentFrequency(ngramDocFreq);
		model.setNumberOfTokens(this.index.getCollectionStatistics().getNumberOfTokens());
		model.setKeyFrequency(1);
		
		if (ngramDocFreq == 0){
			Arrays.fill(scores, 0d);
			return;
		}
		
		for (int k=0; k<n; k++){
			final int docLength = index.getDocumentIndex().getDocumentLength(docids[k]);
			final int numberOfNGrams = (docLength>0 && docLength < ngramLength)? 1 : docLength - ngramLength + 1;
			final double matchingNGrams = ngramFreqMap.get(docids[k]);
										
			if (matchingNGrams != 0){
				final double score = model.score(matchingNGrams, numberOfNGrams);
				scores[k] = score;
				// logger.debug("score="+score);
			}
		}
	}

}
