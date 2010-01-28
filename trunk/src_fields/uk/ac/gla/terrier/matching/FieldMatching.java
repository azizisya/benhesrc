/*
 * Created on 13-Jan-2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching;

import gnu.trove.TIntHashSet;

import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.models.normalisation.Normalisation;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;

/**
 * @author vassilis
 *
 * This class performs field-based normalisation of the term frequencies, 
 * according to the length of each field, as well as weighting of the 
 * normalised term frequencies, before adding them up to compute the 
 * score of a page.
 * 
 * @version $Revision: 1.7 $
 */
public class FieldMatching extends Matching {

	protected final boolean WEIGHTINGMODEL_REQUIRES_DOCLEN;

	/** The number of fields in use */
	protected int NumberOfFields;
	
	/** the logger for this class */
	private static Logger logger = Logger.getLogger("field");
	
	/** the normalisation that should be use for each field */
	protected Normalisation[] normalisation;
	
	/** the global statistics for each field */
	protected CollectionStatistics[] fieldStats;
	
	/** the indices for each field */
	protected Index indices[];
	protected Lexicon lexicons[];
	protected InvertedIndex invIndexes[];
	protected DocumentIndex docIndexes[];
	
	protected int[] termFreqs = null;
	
	/** The body querye expansion weight. */
	protected double[] QEWeights;
	

	public void setNormalisation(
			String[] normalisationNames, 
			double[] parameters, 
			double[] weights){
		String normalisationPrefix = "uk.ac.gla.terrier.matching.models.normalisation.Normalisation";
		for (int i = 0; i < NumberOfFields; i++){
			if (normalisationNames[i].indexOf('.') < 0)
				normalisationNames[i] = normalisationPrefix + normalisationNames[i];
			try{
				normalisation[i] = (Normalisation)Class.forName(normalisationNames[i]).newInstance();
				normalisation[i].setParameter(parameters[i]);
				normalisation[i].setFieldWeight(weights[i]);
				/*if (logger.isDebugEnabled()){
					logger.debug("Normalisation "+(i+1)+": "+normalisation[i].getInfo());
				}*/
				normalisation[i].enableFieldRetrieval();
			}
			catch(Exception e){
				logger.fatal("Exception occurs while initialising normalisation " +
						normalisationNames[i]);
				e.printStackTrace();
				System.exit(1);
			}			
		}
	}
	
	public FieldMatching(Index[] i) {
		super(i[0]);		
		NumberOfFields = i.length;
		indices = i;
		if (logger.isInfoEnabled())
			logger.info("initialisating statistics and tf normalisation for "+NumberOfFields+" fields");
		normalisation = new Normalisation[NumberOfFields];
		fieldStats = new CollectionStatistics[NumberOfFields];
		for(int indexNo = 0; indexNo < NumberOfFields;indexNo++)
		{
			fieldStats[indexNo]  = i[indexNo].getCollectionStatistics();
			if (logger.isInfoEnabled()){
				logger.info("colStats"+indexNo);
				logger.info("# docs: " + fieldStats[indexNo].getNumberOfDocuments());
				logger.info("avg doc len: " + fieldStats[indexNo].getAverageDocumentLength());
			}
		}
		
		lexicons = new Lexicon[NumberOfFields];
		invIndexes = new InvertedIndex[NumberOfFields];
		docIndexes = new DocumentIndex[NumberOfFields];
		for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
		{
			lexicons[indexNo] = indices[indexNo].getLexicon();
			invIndexes[indexNo] = indices[indexNo].getInvertedIndex();
			docIndexes[indexNo] = indices[indexNo].getDocumentIndex();
		}
		
		QEWeights = new double[NumberOfFields];
		Arrays.fill(QEWeights, 1d);
		
		WEIGHTINGMODEL_REQUIRES_DOCLEN = (new Boolean(ApplicationSetup.getProperty("weighting.model.requires.doclen","false")).booleanValue());
	}
	
	public FieldMatching() {
		WEIGHTINGMODEL_REQUIRES_DOCLEN = (new Boolean(ApplicationSetup.getProperty("weighting.model.requires.doclen","false")).booleanValue());
	}
	
	
	public void initialise() {
		super.initialise();
	}
	
	public void enableFieldRetrieval(){
		for (int i=0; i<NumberOfFields; i++)
			this.normalisation[i].enableFieldRetrieval();
	}
	
	public void disableFieldRetrieval(){
		for (int i=0; i<NumberOfFields; i++)
			this.normalisation[i].disableFieldRetrieval();
	}
	
	
	/**
	 * Get number of documents in each field.
	 * @return An array containing the number of documents in each field
	 */
	public int[] getNumberOfDocuments(){
		int[] docs = new int[NumberOfFields];
		Arrays.fill(docs, 0);
		for(int i=0;i<NumberOfFields;i++)
		{
			if(fieldStats != null)
				docs[i] = fieldStats[i].getNumberOfDocuments();
		}
		return docs;
	}
	
	/**
	 * Get number of unique terms in each field. This is exactly the size of each lexicon.
	 * @return An array containing the number of unique terms in each field
	 */
	public long[] getNumberOfUniqueTerms()
	{
		long[] terms = new long[NumberOfFields];
		Arrays.fill(terms, 0);
		for(int i=0;i<NumberOfFields;i++)
		{
			if(fieldStats != null)
				terms[i] = fieldStats[i].getNumberOfUniqueTerms();
		}
		return terms;
	}
	/**
	 * Get number of tokens in each field.
	 * @return An array containing the number of tokens in each field
	 */
	public long[] getNumberOfTokens(){
		long[] tokens = new long[NumberOfFields];
		Arrays.fill(tokens, 0);
		for(int i=0;i<NumberOfFields;i++)
		{
			if(fieldStats != null)
				tokens[i] = fieldStats[i].getNumberOfTokens();
		}
		return tokens;
	}
	
	/**
	 * Get the number of pointers in each field 
	 * @return An array containing the number of pointers in each field
	 */
	public long[] getNumberOfPointers(){
		long[] pointers = new long[3];
		Arrays.fill(pointers, 0);
		for(int i=0;i<NumberOfFields;i++)
		{
			if(fieldStats != null)
				pointers[i] = fieldStats[i].getNumberOfPointers();
		}
		return pointers;
	}

	private static void dumpArray(final int[] arr)
	{
		final int l = arr.length;
		for(int i=0;i<l;i++)
			System.err.print(arr[i]+",");
		System.err.println();
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
	
	
	public void match(String queryNumber, MatchingQueryTerms queryTerms) {
		//the first step is to initialise the arrays of scores and document ids.
		initialise();
		/*if (FilterApplied){
			docidsToFilter = (TIntHashSet)queryidFilterDocidMap.get(queryTerms.getQueryId());
			if (docidsToFilter==null)
				docidsToFilter = new TIntHashSet();
		}*/

		//load in the dsms
		DocumentScoreModifier[] dsms; int NumberOfQueryDSMs = 0;
		dsms = queryTerms.getDocumentScoreModifiers();
		if (dsms!=null)
			NumberOfQueryDSMs = dsms.length;
		
		//and prepare for the tsms
		TermScoreModifier[] tsms; int NumberOfQueryTSMs = 0;
		
		for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
		{
			normalisation[indexNo].setAverageDocumentLength(fieldStats[indexNo].getAverageDocumentLength());
			//System.out.println("Normalisation "+indexNo + " " + normalisation[indexNo]);
		}
		
		final int TOTALNUMBERDOCUMENTS = fieldStats[0].getNumberOfDocuments();
		wmodel.setNumberOfDocuments(TOTALNUMBERDOCUMENTS);
		if (logger.isDebugEnabled())
			logger.debug("Number of documents in the collection: "+TOTALNUMBERDOCUMENTS);
		double averageDocumentLength = 
			TOTALNUMBERDOCUMENTS > 0 
			? sum(getNumberOfTokens())/(double)TOTALNUMBERDOCUMENTS 
			: 0;
		wmodel.setAverageDocumentLength(averageDocumentLength);
		if (logger.isDebugEnabled())
			logger.debug("Average document length in the collection: "+averageDocumentLength);
		

		String[] queryTermStrings = queryTerms.getTerms();
		//check whether we need to match an empty query.
		//if so, then return the existing result set.
		if (MATCH_EMPTY_QUERY && queryTermStrings.length == 0) {
			resultSet.setExactResultSize(TOTALNUMBERDOCUMENTS);
			resultSet.setResultSize(TOTALNUMBERDOCUMENTS);
			return;
		}

		//in order to save the time from references to the arrays, we create local references
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		short[] occurences = resultSet.getOccurrences();
		
		
		
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
	
		for (int i = 0; i < queryLength; i++)
		{
			final String currentTerm = queryTermStrings[i];
			if (logger.isDebugEnabled())
				logger.debug("query term "+(i+1)+": "+currentTerm);
			//we seek the query term in the lexicon
			
			//records which fields this term was found in
			boolean termFoundInFields[] = new boolean[NumberOfFields];
			/* records if this term was found in any fields, used to shortcut out of this
			 * term if it is not found anywhere in the collection (ie in no fields) */
			boolean termFoundOnce = false;
			Arrays.fill(termFoundInFields, false);
			//check to see if the term exists in each field
			for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
			{
				if (indices[indexNo] != null)
				{
					final Lexicon lex = lexicons[indexNo];
					termFoundInFields[indexNo] = lex.findTerm(currentTerm);
					termFoundOnce |= termFoundInFields[indexNo];
					if (termFoundInFields[indexNo])
					{
						/*if (logger.isDebugEnabled()){
							logger.debug("found in lexicon "+indexNo);
							//logger.debug("term : " + lex.getTerm());
							//logger.debug("code : " + lex.getTermId());
							logger.debug("TF : " + lex.getTF());
							logger.debug("Nt : " + lex.getNt());
						}*/
					}
					else
					{
						if (logger.isDebugEnabled())
							logger.debug("lexicon "+indexNo+": not found");
					}
				}
			}
			
			//and if it is not found, we continue with the next term - no need to progress 
			//with this term
			if (!termFoundOnce)
				continue;
				
			//because when the TreeNode is created, the term code assigned is taken from
			//the TermCodes class, the assigned term code is only valid during the indexing
			//process. Therefore, at this point, the term code should be updated with the one
			//stored in the lexicon file.	
			//TODO is this termID actually used?
			queryTerms.setTermProperty(queryTermStrings[i], lexicons[0].getTermId());


			//the weighting model is prepared for assigning scores to documents
			wmodel.setKeyFrequency(queryTerms.getTermWeight(currentTerm));
			//if (logger.isDebugEnabled())
				//logger.debug("qtw: " + queryTerms.getTermWeight(currentTerm));
			
			
			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
				if (logger.isDebugEnabled())
					logger.debug("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
				continue;
			}
			
			//the postings are beign read from the inverted file.
			//wmodel.
			//wmodel.setTermFrequency((double)lexicon.getTF());
			double TFInCollection = 0.0d;
			int[][][] pointers = new int[NumberOfFields][][];
			TIntHashSet allDocids = new TIntHashSet(lexicons[0].getTF());
			for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
			{
				if(! termFoundInFields[indexNo])
					continue;
				//System.err.println("Reading postings for index "+indexNo);
				TFInCollection += lexicons[indexNo].getTF();
				pointers[indexNo] = invIndexes[indexNo].getDocuments(lexicons[indexNo].getTermId());
				allDocids.addAll(pointers[indexNo][0]);
				//System.err.println("f="+indexNo+",termid="+lexicons[indexNo].getTermId());
				//dumpArray(pointers[indexNo][0]);
			}
			
			wmodel.setTermFrequency(TFInCollection);
			
			int[] allPointersDocids = allDocids.toArray();
			final int numMatchingDocsForThisTerm = allPointersDocids.length;
			allDocids = null;
			double[] allPointersFreqs = new double[numMatchingDocsForThisTerm];
			int[] allPointersLengths = new int[numMatchingDocsForThisTerm];
			wmodel.setDocumentFrequency((double)numMatchingDocsForThisTerm);
			//System.err.println(" with " + numMatchingDocsForThisTerm + " documents (TF is " + TFInCollection + ").");
			//if (logger.isDebugEnabled())
				//logger.debug("Nt: " + numMatchingDocsForThisTerm+", TF: "+TFInCollection);
			
			double length = 0;
			for (int j=0; j<numMatchingDocsForThisTerm; j++)
			//for each document that matches in at least one field
			{
				final int currentDocid = allPointersDocids[j];
				allPointersLengths[j]=0;
				int lengths[] = new int[NumberOfFields];
				double frequencies[] = new double[NumberOfFields];
				Arrays.fill(frequencies,0.0d);
				Arrays.fill(lengths,0);
				
				int thisDocumentTotalLength = 0;
				
				/* okay, optimisations of time can be made if the actual weighting model does
				 * not require the document length. Normally it does not, as document length 
				 * is used by the normalisation components */
				if (WEIGHTINGMODEL_REQUIRES_DOCLEN)
				{
					for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
					{
						/* we need to retrieve the length of the document as, the total length of the document
						 * MAY be required during Scoring. Quite often it will not be, as document length is
						 * usually taken into account by the normalisation component, which is being done separately
						 * here. */
						 
						//determine the length of the document in that field
						lengths[indexNo] = indices[indexNo].getDocumentIndex().getDocumentLength(currentDocid);
						//update the known length of this document
						allPointersLengths[j] += lengths[indexNo];
						int tmpIndex = -1;
						if (termFoundInFields[indexNo] && (tmpIndex = Arrays.binarySearch(pointers[indexNo][0],currentDocid)) >= 0)
						{	
							frequencies[indexNo] = pointers[indexNo][1][tmpIndex];
							//normalise the term frequency from that field
							if(normalisation[indexNo] != null && frequencies[indexNo] != 0)
								frequencies[indexNo] = normalisation[indexNo].normalise(
									frequencies[indexNo], 
									lengths[indexNo], 
									lexicons[indexNo].getTF());
							allPointersFreqs[j] += frequencies[indexNo];
						}
					}
				}
				else
				{	/* weighting model does not require document length to be calculated, so do not calculate
					 * retrieve the field length for documents that do not match the query term in that field */ 
					for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
					{
						int tmpIndex = -1;
						if (termFoundInFields[indexNo] && (tmpIndex = Arrays.binarySearch(pointers[indexNo][0],currentDocid)) >= 0)
						{
							//find the length of the document in that field
							lengths[indexNo] = indices[indexNo].getDocumentIndex().getDocumentLength(currentDocid);
							
							frequencies[indexNo] = pointers[indexNo][1][tmpIndex];
							//normalise the term frequency from that field
							if(normalisation[indexNo] != null && frequencies[indexNo] != 0)
								frequencies[indexNo] = normalisation[indexNo].normalise(
									frequencies[indexNo], 
									lengths[indexNo], 
									lexicons[indexNo].getTF());
							allPointersFreqs[j] += frequencies[indexNo];
						}
					}
				}
				
			}
			
			
			double[] termScores = new double[allPointersDocids.length];
			
			//assign scores to documents for a term
			assignScores(termScores, allPointersDocids, allPointersFreqs, allPointersLengths);
			
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
		
		// apply document filter
		/*if (FilterApplied){
			int N = scores.length;
			for (int i=0; i<N; i++)
				if (docidsToFilter.contains(docids[i])){
					if (logger.isDebugEnabled())
						logger.debug("Document "+docids[i]+" ("+docIndex.getDocumentNumber(docids[i])+
								") with score "+scores[docids[i]]+" is filtered."
								);
					scores[docids[i]] = 0.0d;
				}
		}*/
		
		//sets the actual size of the result set.
		resultSet.setResultSize(set_size);
		
		HeapSort.descendingHeapSort(scores, docids, occurences, set_size);
		long sortingEnd = System.currentTimeMillis();
		if (logger.isInfoEnabled())
			logger.info("time to sort: " + ((sortingEnd - sortingStart) / 1000.0D));
			
		//output results
		if (logger.isDebugEnabled()){
			logger.debug("number of retrieved documents: " + numberOfRetrievedDocuments);
			logger.debug("score of 1st document ("+docids[0]+") : " + scores[0]);
		}
		
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
		//for (int t = NumberOfQueryDSMs-1; t >= 0; t--) {
			//dsms[t].modifyScores(index, queryTerms, resultSet);
			//HeapSort.descendingHeapSort(scores, docids, occurences, resultSet.getResultSize());
		//}
		if (logger.isDebugEnabled())
			logger.debug("score of 1st document ("+docids[0]+") : " + scores[0]);
		
	}
	
	/** For each documents that contains the query term, the score is computed, and placed in the 
	  * scores array
	  * @param scores Output - the computed scores of the documents
	  * @param docids Input - the docids of the documents that are being scored
	  * @param freqs Input - the term frequencies of the documents that are being scores
	  * @param lengths Input - the lengths of the documents being scored
	  */ 
	public void assignScores(double[] scores, final int[] docids, final double[] freqs, final int[] lengths)
	{	
		final int numOfPointers = docids.length;

		//for each document that contains 
		//the query term, the score is computed.
		double score;
		
		//check to see if the weighting model requires the total document length to have
		//been calculated.
		if (WEIGHTINGMODEL_REQUIRES_DOCLEN)
		{
			for (int j = 0; j < numOfPointers; j++) {
				//compute the score
				score = wmodel.score(freqs[j], lengths[j]);
				//increase the number of retrieved documents if the
				//previous score was zero and the added score is positive
				//sometimes negative scores occur due to very low probabilities
				if (score > 0) {
					scores[j] = score;				
					//System.err.println("score: " + score);
				}
			}
		}
		else
		{
			for (int j = 0; j < numOfPointers; j++) {
				//compute the score
				score = wmodel.score(freqs[j], 0);
				//increase the number of retrieved documents if the
				//previous score was zero and the added score is positive
				//sometimes negative scores occur due to very low probabilities
				if (score > 0) {
					scores[j] = score;				
					//System.err.println("score: " + score);
				}
			}
		}
	}
}
