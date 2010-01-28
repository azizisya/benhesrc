/*
 * Created on 13-Jan-2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching;

import gnu.trove.TIntHashSet;

import java.util.Arrays;
import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.dsms.StaticScoreModifier;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.normalisation.Normalisation;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;

/**
 * @author vassilis, Ben He
 *
 * This class performs field-based normalisation of the term frequencies, 
 * according to the length of each field, as well as weighting of the 
 * normalised term frequencies, before adding them up to compute the 
 * score of a page.
 * 
 * @version $Revision: 1.11 $
 */
public class ThreeMatching extends Matching {
	
	public Normalisation[] normalisation = new Normalisation[3];

	//the statistics for the body
	public CollectionStatistics bodyColStats = null;
	//	new CollectionStatistics();

	//the statistics for the anchor text
	public CollectionStatistics anchorColStats = null;
	//	new CollectionStatistics();
	
	//the statistics for the title
	public CollectionStatistics titleColStats = null;
	//	new CollectionStatistics();
	
	public int[] termFreqs = null;
	
	public double globalAverageDocumentLength;
	
	public long globalNumberOfTokens;
	
	public Index index2; //anchor text
	
	public Index index3; //title
	
	protected boolean bodyEnabled = true;
	
	protected boolean atextEnabled = true;
	
	protected boolean titleEnabled = true;
		
	//public int normalisation = NORMALISATION2;
	
//	public static int NORMALISATION2 = 0; //the classic normalisation
//	public static int NORMALISATIONF = 1; //the normalisation of fields from BM25F
//	public static int NORMALISATIONB = 2; //the normalisation from Ben's SPIRE paper
		
	public boolean applySSA = false; 
	public boolean computeBooleanAnchorHits = false;
	
	public void setNormalisation(
			String[] normalisationNames, 
			double[] parameters, 
			double[] weights){
		String normalisationPrefix = "uk.ac.gla.terrier.matching.models.normalisation.Normalisation";
		for (int i = 0; i < normalisation.length; i++){
			if (normalisationNames[i].indexOf('.') < 0)
				normalisationNames[i] = normalisationPrefix + normalisationNames[i];
			try{
				System.err.println("name "+i+": " + normalisationNames[i]);
				normalisation[i] = (Normalisation)Class.forName(normalisationNames[i]).newInstance();
				normalisation[i].setParameter(parameters[i]);
				normalisation[i].setFieldWeight(weights[i]);
				System.err.println((i+1)+": "+normalisation[i].getInfo());
				normalisation[i].enableFieldRetrieval();
			}
			catch(Exception e){
				System.err.println("Exception occurs while initialising normalisation " +
						normalisationNames[i]);
				e.printStackTrace();
				System.exit(1);
			}			
		}
	}
	
	public ThreeMatching(
			String pathBody, String prefixBody,
			String pathAtext, String prefixAtext,
			String pathTitle, String prefixTitle){
		super(Index.createIndex(pathBody, prefixBody));
		globalNumberOfTokens = 0L;
		if (prefixAtext.equalsIgnoreCase("null"))
			index2 = null;
		else
			index2 = Index.createIndex(pathAtext, prefixAtext);
		if (prefixTitle.equalsIgnoreCase("null"))
			index3 = null;
		else
			index3 = Index.createIndex(pathTitle, prefixTitle);
		computeBooleanAnchorHits = (new Boolean(ApplicationSetup.getProperty("compute.anchorhits","false"))).booleanValue();
		applySSA = (new Boolean(ApplicationSetup.getProperty("apply.ssa", "false"))).booleanValue();
		if (applySSA)
			this.addDocumentScoreModifier(new StaticScoreModifier());

			bodyColStats = index.getCollectionStatistics();// .initialise(
					//pathBody +
					//ApplicationSetup.FILE_SEPARATOR + 
					//prefixBody + 
					//ApplicationSetup.LOG_SUFFIX);
			System.err.println("colStats1");
			System.err.println("# docs: " + bodyColStats.getNumberOfDocuments());
			System.err.println("avg doc len: " + bodyColStats.getAverageDocumentLength());
			globalNumberOfTokens += bodyColStats.getNumberOfTokens();
			////////////////////////////////////////////////////////////////////////////////
			if (index2!=null){
				anchorColStats = index2.getCollectionStatistics();//.initialise(
						//pathAtext +
						//ApplicationSetup.FILE_SEPARATOR + 
						//prefixAtext + 
						//ApplicationSetup.LOG_SUFFIX);
				System.err.println("colStats2");
				System.err.println("# docs: " + anchorColStats.getNumberOfDocuments());
				System.err.println("avg doc len: " + anchorColStats.getAverageDocumentLength());
				globalNumberOfTokens += anchorColStats.getNumberOfTokens();
			}
			/////////////////////////////////////////////////////////////////////////////////
			if (index3!=null){
				titleColStats = index3.getCollectionStatistics();//.initialise(
						//pathTitle +
						//ApplicationSetup.FILE_SEPARATOR + 
						//prefixTitle +
						//ApplicationSetup.LOG_SUFFIX);
				System.err.println("colStats3");
				System.err.println("# docs: " + titleColStats.getNumberOfDocuments());
				System.err.println("avg doc len: " + titleColStats.getAverageDocumentLength());
				globalNumberOfTokens += titleColStats.getNumberOfTokens();
			}
			
			globalAverageDocumentLength = (double)globalNumberOfTokens/bodyColStats.getNumberOfDocuments();
					
	}
	
	public ThreeMatching(Index i1, Index i2, Index i3) {
		super(i1);
		index2 = i2;
		index3 = i3;
		System.err.println("body index:" + index);
		System.err.println("atext index:" + index2);
		System.err.println("title index:" + index3);
		globalNumberOfTokens = 0L;
		computeBooleanAnchorHits = (new Boolean(ApplicationSetup.getProperty("compute.anchorhits","false"))).booleanValue();
		applySSA = (new Boolean(ApplicationSetup.getProperty("apply.ssa", "false"))).booleanValue();
		if (applySSA)
			this.addDocumentScoreModifier(new StaticScoreModifier());

			bodyColStats = i1.getCollectionStatistics();//.initialise(
				//	ApplicationSetup.makeAbsolute(
				//	ApplicationSetup.getProperty("terrier.index.path",ApplicationSetup.TERRIER_INDEX_PATH), ApplicationSetup.TERRIER_VAR) +
				//	ApplicationSetup.FILE_SEPARATOR + 
				//	ApplicationSetup.TERRIER_INDEX_PREFIX + 
				//	ApplicationSetup.LOG_SUFFIX);
			System.err.println("colStats1");
			System.err.println("# docs: " + bodyColStats.getNumberOfDocuments());
			System.err.println("avg doc len: " + bodyColStats.getAverageDocumentLength());
			globalNumberOfTokens += bodyColStats.getNumberOfTokens();
			/////////////////////////////////////////////////////////////////////////////////
			if (index2!=null){
				anchorColStats =i2.getCollectionStatistics();//.initialise(
					//	ApplicationSetup.makeAbsolute(ApplicationSetup.getProperty("terrier.index.path2",ApplicationSetup.TERRIER_INDEX_PATH), ApplicationSetup.TERRIER_VAR) +
					//	ApplicationSetup.FILE_SEPARATOR + 
					//	ApplicationSetup.getProperty("terrier.index.prefix2", ApplicationSetup.TERRIER_INDEX_PREFIX) + 
					//	ApplicationSetup.LOG_SUFFIX);
				System.err.println("colStats2");
				System.err.println("# docs: " + anchorColStats.getNumberOfDocuments());
				System.err.println("avg doc len: " + anchorColStats.getAverageDocumentLength());
				globalNumberOfTokens += anchorColStats.getNumberOfTokens();
			}
			/////////////////////////////////////////////////////////////////////////////////
			if (index3!=null){
				titleColStats = i3.getCollectionStatistics();//.initialise(
					//	ApplicationSetup.makeAbsolute(ApplicationSetup.getProperty("terrier.index.path3",ApplicationSetup.TERRIER_INDEX_PATH), ApplicationSetup.TERRIER_VAR) +
					//	ApplicationSetup.FILE_SEPARATOR + 
					//	ApplicationSetup.getProperty("terrier.index.prefix3", ApplicationSetup.TERRIER_INDEX_PREFIX) +
					//	ApplicationSetup.LOG_SUFFIX);
				System.err.println("colStats3");
				System.err.println("# docs: " + titleColStats.getNumberOfDocuments());
				System.err.println("avg doc len: " + titleColStats.getAverageDocumentLength());
				globalNumberOfTokens += titleColStats.getNumberOfTokens();
			}
			globalAverageDocumentLength = (double)globalNumberOfTokens/bodyColStats.getNumberOfDocuments();
	}
	
	public int[] anchorOccurrences = null;
	
	public int[] titleOccurences = null;
	
	public boolean optimiseK_1 = false;
	public double original_K_1 = 0.0;
	
	public void initialise() {
		super.initialise();
		if (anchorOccurrences == null && index2!=null) {
			anchorOccurrences = new int[anchorColStats.getNumberOfDocuments()];
			Arrays.fill(anchorOccurrences, 0);
		}
		if (titleOccurences == null && index3!=null) {
			titleOccurences = new int[titleColStats.getNumberOfDocuments()];
		}
			
		optimiseK_1 = (new Boolean(ApplicationSetup.getProperty("optimisek1","false"))).booleanValue();
		original_K_1 = Double.parseDouble(ApplicationSetup.getProperty("originalk1","1.2d"));
		
	}
	
	
	
	/**
	 * Get number of documents in each field.
	 * @return A 3-dimensional array. [body, atext, title]
	 */
	public int[] getNumberOfDocuments(){
		int[] docs = new int[3];
		Arrays.fill(docs, 0);
		docs[0] = bodyColStats.getNumberOfDocuments();
		if (index2!=null)
			docs[1] = anchorColStats.getNumberOfDocuments();
		if (index3!=null)
			docs[2] = titleColStats.getNumberOfDocuments();		
		return docs;
	}
	/**
	 * Get number of tokens in each field.
	 * @return
	 */
	public long[] getNumberOfTokens(){
		long[] tokens = new long[3];
		Arrays.fill(tokens, 0);
		tokens[0] = bodyColStats.getNumberOfTokens();
		if (index2!=null)
			tokens[1] = anchorColStats.getNumberOfTokens();
		if (index3!=null)
			tokens[2] = titleColStats.getNumberOfTokens();		
		return tokens;
	}
	/**
	 * Get number of unique terms in each field.
	 * @return
	 */
	public long[] getNumberOfUniqueTerms(){
		long[] terms = new long[3];
		Arrays.fill(terms, 0);
		terms[0] = bodyColStats.getNumberOfUniqueTerms();
		if (index2!=null)
			terms[1] = anchorColStats.getNumberOfUniqueTerms();
		if (index3!=null)
			terms[2] = titleColStats.getNumberOfUniqueTerms();		
		return terms;
	}
	/**
	 * 
	 * @return
	 */
	public long[] getNumberOfPointers(){
		long[] pointers = new long[3];
		Arrays.fill(pointers, 0);
		pointers[0] = bodyColStats.getNumberOfPointers();
		if (index2!=null)
			pointers[1] = anchorColStats.getNumberOfPointers();
		if (index3!=null)
			pointers[2] = titleColStats.getNumberOfPointers();		
		return pointers;
	}
	
	public static boolean bodyOnlyScores = (new Boolean(ApplicationSetup.getProperty("body.only.scores","false"))).booleanValue();

	
	public void match(String queryNumber, MatchingQueryTerms queryTerms) {
		//the first step is to initialise the arrays of scores and document ids.
		initialise();

		//load in the dsms
		DocumentScoreModifier[] dsms; int NumberOfQueryDSMs = 0;
		dsms = queryTerms.getDocumentScoreModifiers();
		if (dsms!=null)
			NumberOfQueryDSMs = dsms.length;
		
		//and prepare for the tsms
		TermScoreModifier[] tsms; int NumberOfQueryTSMs = 0;
		normalisation[0].setAverageDocumentLength(bodyColStats.getAverageDocumentLength());
		if (index2!=null)
			normalisation[1].setAverageDocumentLength(anchorColStats.getAverageDocumentLength());
		if (index3!=null)
			normalisation[2].setAverageDocumentLength(titleColStats.getAverageDocumentLength());
		System.out.println(normalisation[0].getInfo());
		if (index2!=null)
			System.out.println(normalisation[1].getInfo());
		if (index3!=null)
			System.out.println(normalisation[2].getInfo());




		String[] queryTermStrings = queryTerms.getTerms();
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize(bodyColStats.getNumberOfDocuments());
			resultSet.setResultSize(bodyColStats.getNumberOfDocuments());
			return;
		}

		//in order to save the time from references to the arrays, we create local references
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		short[] occurences = resultSet.getOccurrences();
		
		wmodel.setNumberOfTokens(sum(getNumberOfTokens()));
		//assumed indices are aligned
		wmodel.setNumberOfDocuments((double)bodyColStats.getNumberOfDocuments());
		wmodel.setAverageDocumentLength(globalAverageDocumentLength);
		//approximation
		wmodel.setNumberOfUniqueTerms((double)collectionStatistics.getNumberOfUniqueTerms());	
		
		//the number of documents with non-zero score.
		numberOfRetrievedDocuments = 0;
		
		//the pointers read from the inverted file
		int[][] pointers1 = null;
		int[][] pointers2 = null;
		int[][] pointers3 = null;
		
		//the number of term score modifiers
		int numOfTermModifiers = termModifiers.size();
		
		//the number of document score modifiers
		int numOfDocModifiers = documentModifiers.size();
		
		int numberOfModifiedDocumentScores =0;
		
		//for each query term in the query
		final int queryLength = queryTermStrings.length;
		for (int i = 0; i < queryLength; i++) {
			//we seek the query term in the lexicon
			boolean found = lexicon.findTerm(queryTermStrings[i]);
			boolean found2 = false;
			if (index2 != null)
				found2=index2.getLexicon().findTerm(queryTermStrings[i]);
			boolean found3 = false;
			if (index3 != null)
				found3 = index3.getLexicon().findTerm(queryTermStrings[i]);
			
			//and if it is not found, we continue with the next term
			if (!(found || found2 || found3))
				continue;
			//because when the TreeNode is created, the term code assigned is taken from
			//the TermCodes class, the assigned term code is only valid during the indexing
			//process. Therefore, at this point, the term code should be updated with the one
			//stored in the lexicon file.	
			queryTerms.setTermProperty(queryTermStrings[i], lexicon.getTermId());

//			System.err.println("" + (i + 1) + ": " + queryTermStrings[i].trim() + 
//					"(" + lexicon.getTermId() + ", " 
//						+ index2.getLexicon().getTermId() + ", "
//						+ index3.getLexicon().getTermId() + ")");

			if (found) {
				System.out.println("lexicon 1");
				System.out.println("term : " + lexicon.getTerm());
				System.out.println("code : " + lexicon.getTermId());
				System.out.println("tf : " + lexicon.getTF());
				System.out.println("nt : " + lexicon.getNt());				
			} else {
				System.out.println("lexicon 1: not found");
			}
			if (found2) {
				System.out.println("lexicon 2");
				System.out.println("term : " + index2.getLexicon().getTerm());
				System.out.println("code : " + index2.getLexicon().getTermId());
				System.out.println("tf : " + index2.getLexicon().getTF());
				System.out.println("nt : " + index2.getLexicon().getNt());	
			} else {
				System.out.println("lexicon 2: not found");
			}
			if (found3) {
				System.out.println("lexicon 3");
				System.out.println("term : " + index3.getLexicon().getTerm());
				System.out.println("code : " + index3.getLexicon().getTermId());
				System.out.println("tf : " + index3.getLexicon().getTF());
				System.out.println("nt : " + index3.getLexicon().getNt());				
			} else {
				System.out.println("lexicon 3: not found");
			}

			//the weighting model is prepared for assigning scores to documents
			wmodel.setKeyFrequency(queryTerms.getTermWeight(queryTermStrings[i]));
			System.err.println("qtw: " + queryTerms.getTermWeight(queryTermStrings[i]));
		
			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
				System.err.println("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			
			//the postings are beign read from the inverted file.
			//wmodel.
			//wmodel.setTermFrequency((double)lexicon.getTF());
			double TFInCollection = 0.0d;
			if (found) {
				TFInCollection+=lexicon.getTF();
				pointers1 = invertedIndex.getDocuments(queryTerms.getTermCode(queryTermStrings[i]));
			}
			else
				System.err.println(queryTermStrings[i]+" not found in body");
			if (found2) {
				TFInCollection+=index2.getLexicon().getTF();
				pointers2 = index2.getInvertedIndex().getDocuments(index2.getLexicon().getTermId());
			}
			else
				System.err.println(queryTermStrings[i]+" not found in anchor text");
			//not adding the frequency from the title, because it 
			//is already counted in the body of the document.
			if (found3) {
				TFInCollection+=index3.getLexicon().getTF();
				pointers3 = index3.getInvertedIndex().getDocuments(index3.getLexicon().getTermId());
			}
			else
				System.err.println(queryTermStrings[i]+" not found in title");
			wmodel.setTermFrequency(TFInCollection);
			
			TIntHashSet allPointers = null;
			if (found) {
				allPointers = new TIntHashSet(pointers1[0]);
				if (found2)
					allPointers.addAll(pointers2[0]);
				if (found3)
					allPointers.addAll(pointers3[0]);
			} else if (found2) {
				allPointers = new TIntHashSet(pointers2[0]);
				if (found)
					allPointers.addAll(pointers1[0]);
				if (found3)
					allPointers.addAll(pointers3[0]);
			} else if (found3) {
				allPointers = new TIntHashSet(pointers3[0]);
				if (found) 
					allPointers.addAll(pointers1[0]);
				if (found2) 
					allPointers.addAll(pointers2[0]);
			}

			int[] allPointersDocids = allPointers.toArray();
			double[] allPointersFreqs = new double[allPointersDocids.length];
			//wmodel.setDocumentFrequency((double)lexicon.getNt());
			wmodel.setDocumentFrequency((double)allPointersDocids.length);
			System.err.println(" with " + allPointersDocids.length + " documents (TF is " + TFInCollection + ").");
			System.err.println("Nt: " + allPointersDocids.length);
			int tmpDocid;
			int tmpIndex1 = -1;
			int tmpIndex2 = -1;
			int tmpIndex3 = -1;

			double titleFrequency = 0;
			double anchorFrequency = 0;
			double bodyFrequency = 0;
			
			double titleLength = 0;
			double anchorLength = 0;
			double bodyLength = 0;
			double length = 0;
			Idf idf = new Idf();
			for (int j=0; j<allPointersFreqs.length; j++) {
				tmpDocid = allPointersDocids[j];
				titleFrequency = anchorFrequency = bodyFrequency = 0;

				bodyLength = docIndex.getDocumentLength(tmpDocid);
				if (found2)
					anchorLength = index2.getDocumentIndex().getDocumentLength(tmpDocid);
				if (found3)
					titleLength = index3.getDocumentIndex().getDocumentLength(tmpDocid);
				
				if (found && (tmpIndex1 = Arrays.binarySearch(pointers1[0],tmpDocid)) >= 0) {
					bodyFrequency = pointers1[1][tmpIndex1];
				}

				if (found2 && (tmpIndex2 = Arrays.binarySearch(pointers2[0],tmpDocid)) >= 0) {
					anchorFrequency = pointers2[1][tmpIndex2];
				}
				
				if (found3 && (tmpIndex3 = Arrays.binarySearch(pointers3[0], tmpDocid)) >= 0) {
					titleFrequency = pointers3[1][tmpIndex3];
				}
				
				if (normalisation[0]!=null && bodyFrequency!=0){
					bodyFrequency = normalisation[0].normalise(
							bodyFrequency, 
							bodyLength, 
							lexicon.getTF());
				}
				
				if (normalisation[1]!=null&&anchorFrequency!=0){
					anchorFrequency = normalisation[1].normalise(
							anchorFrequency, 
							anchorLength, 
							index2.getLexicon().getTF());
				}
				
				if (normalisation[2]!=null&&titleFrequency!=0){
					titleFrequency = normalisation[2].normalise(
							titleFrequency, 
							titleLength, 
							index3.getLexicon().getTF());
				}
				
				
				allPointersFreqs[j] =bodyFrequency + 
				  titleFrequency + 
				  anchorFrequency;
				//String tmpDocno = index.getDocumentIndex().getDocumentNumber(allPointersDocids[j]);
//				if (tmpDocno.equals("WTX064-B48-188"))	
//					System.err.println("freq in WTX064-B48-188: "+allPointersFreqs[j]);				  
				//System.err.println(allPointersFreqs[j] + " " + bodyFrequency + " " + titleFrequency + " " + anchorFrequency);
			}
			
			double[] termScores = new double[allPointersDocids.length];
			
			//assign scores to documents for a term
			assignScores(termScores, allPointersDocids, allPointersFreqs);
			
			//int[][] pointers = new int[][] {allPointersDocids, allPointersFreqs};
			
			//application dependent modification of scores
			//of documents for a term
//			numberOfModifiedDocumentScores = 0;
//			for (int t = 0; t < numOfTermModifiers; t++)
//				((TermScoreModifier)termModifiers.get(t)).modifyScores(termScores, pointers);
//			//application dependent modification of scores
//			//of documents for a term. These are predefined by the query
//			tsms = queryTerms.getTermScoreModifiers(queryTermStrings[i]);
//			if (tsms!=null) {
//				for (int t=0; t<tsms.length; t++)
//					if (tsms[t]!=null)
//						tsms[t].modifyScores(termScores, pointers);
//			}

			
			//finally setting the scores of documents for a term
			//a mask for setting the occurrences
			short mask = 0;
			if (i<16)
				mask = (short)(1 << i);
			
			int docid;
			int[] pointers10 = allPointersDocids;
			//int[] pointers11 = pointers[1];
			final int numberOfPointers = pointers10.length;
			for (int k = 0; k < numberOfPointers; k++) {
				docid = pointers10[k];
				if ((scores[docid] == 0.0d) && (termScores[k] > 0.0d)) {
					numberOfRetrievedDocuments++;
				} else if ((scores[docid] > 0.0d) && (termScores[k] < 0.0d)) {
					numberOfRetrievedDocuments--;
				}
				scores[docid] += termScores[k];
				occurences[docid] |= mask;
			}
		}

		//sort in descending score order the top RETRIEVED_SET_SIZE documents
		long sortingStart = System.currentTimeMillis();

		//we need to sort at most RETRIEVED_SET_SIZE, or if we have retrieved
		//less documents than RETRIEVED_SET_SIZE then we need to find the top 
		//numberOfRetrievedDocuments.
		int set_size = Math.min(RETRIEVED_SET_SIZE, numberOfRetrievedDocuments);
		if (set_size == 0) 
			set_size = numberOfRetrievedDocuments;
		
		//sets the effective size of the result set.
		resultSet.setExactResultSize(numberOfRetrievedDocuments);
		
		//sets the actual size of the result set.
		resultSet.setResultSize(set_size);
		
		
		HeapSort.descendingHeapSort(scores, docids, occurences, set_size);
		long sortingEnd = System.currentTimeMillis();
		//output results
		System.err.println("number of retrieved documents: " + numberOfRetrievedDocuments);
		System.err.println("time to sort: " + ((sortingEnd - sortingStart) / 1000.0D));

		System.err.println("score of 1st document ("+docids[0]+") : " + scores[0]);
		
		
		//modifyScores(query, docids, scores);
		//application dependent modification of scores
		//of documents for a query
		//sorting the result set after applying each DSM
		for (int t = 0; t < numOfDocModifiers; t++) {
			if (((DocumentScoreModifier)documentModifiers.get(t)).modifyScores(index, queryTerms, resultSet));
				HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}

		//application dependent modification of scores
		//of documents for a query, defined by this query
		for (int t = NumberOfQueryDSMs-1; t >= 0; t--) {
			dsms[t].modifyScores(index, queryTerms, resultSet);
			HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		}

		System.out.println("score of 1st document ("+docids[0]+") : " + scores[0]);
		
	}
	
	public void assignScores(double[] scores, int[] docids, double[] freqs) {	

		final int numOfPointers = docids.length;

		//for each document that contains 
		//the query term, the score is computed.
		double frequency;
		int docLength;
		double score;
		//System.err.println("###In assign scores###");
		if (bodyOnlyScores) {
			wmodel.setDocumentFrequency(lexicon.getNt());
			wmodel.setTermFrequency(lexicon.getTF());
			int counter = 0;
			for (int j = 0; j < numOfPointers; j++) {
				//compute the score
				score = wmodel.score(freqs[j], 0);
				//increase the number of retrieved documents if the
				//previous score was zero and the added score is positive
				//sometimes negative scores occur due to very low
				//probabilities
				if (score > 0)
					scores[j] = score;
				else
					counter++;
			}
			System.out.println("assign scores: # of docs with <0 score = " + counter);
			
		} else {
			//System.err.println("ready to go into loop");
			for (int j = 0; j < numOfPointers; j++) {
				//compute the score
				score = wmodel.score(freqs[j], 0);
				String tmpDocno = index.getDocumentIndex().getDocumentNumber(docids[j]);
//				if (tmpDocno.equals("WTX064-B48-188"))	
//					System.err.println("score of WTX064-B48-188: "+score);
				//System.err.println(wmodel+" "+wmodel.getInfo() + ": score: " + score + ", freq: " + freqs[j]);
				//increase the number of retrieved documents if the
				//previous score was zero and the added score is positive
				//sometimes negative scores occur due to very low
				//probabilities
				if (score > 0) {
					scores[j] = score;				
					//System.err.println("score: " + score);
				}
			}
		}
	}
	
    public static double sum(double[] arr)
    {
        final int l = arr.length;
        double s= 0;
        for(int i=0;i<l;i++)
        {
            s+=arr[i];
        }
        return s;
    }

    public static long sum(long[] arr)
    {
        final int l = arr.length;
        long s=0;
        for(int i=0;i<l;i++)
        {
            s+=arr[i];
        }
        return s;
    }

    public static int sum(int[] arr)
    {
        final int l = arr.length;
        int s=0;
        for(int i=0;i<l;i++)
        {
            s+=arr[i];
        }
        return s;
    }


}