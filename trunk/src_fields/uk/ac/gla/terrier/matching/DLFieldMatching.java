package uk.ac.gla.terrier.matching;

import gnu.trove.TIntHashSet;

import java.util.Arrays;

import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;

/**
 * @author vassilis
 * This class was used for the ECIR 2007 paper. It corresponds
 * to the model M_{D}L2.
 */
public class DLFieldMatching extends FieldMatching {

	
	public DLFieldMatching(Index[] i) {
		super(i);
	}

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
		
		for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
		{
			normalisation[indexNo].setAverageDocumentLength(fieldStats[indexNo].getAverageDocumentLength());
			System.out.println("Normalisation i"+indexNo + " " + normalisation[indexNo]);
		}
		
		final int TOTALNUMBERDOCUMENTS = fieldStats[0].getNumberOfDocuments();
		wmodel.setNumberOfDocuments(TOTALNUMBERDOCUMENTS);
		double averageDocumentLength = 
			TOTALNUMBERDOCUMENTS > 0 
			? sum(getNumberOfTokens())/(double)TOTALNUMBERDOCUMENTS 
			: 0;
		wmodel.setAverageDocumentLength(averageDocumentLength);
			System.err.println("avl: " + averageDocumentLength);
		

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
		
		final Lexicon[] lexicons = new Lexicon[NumberOfFields];
		final InvertedIndex[] invIndexes = new InvertedIndex[NumberOfFields];
		for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
		{
			lexicons[indexNo] = indices[indexNo].getLexicon();
			invIndexes[indexNo] = indices[indexNo].getInvertedIndex();
		}

		System.err.println("Processing query in DLFieldMatching.");
		for (int i = 0; i < queryLength; i++)
		{
			final String currentTerm = queryTermStrings[i];
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
						System.err.println("lexicon "+indexNo);
						System.out.println("term : " + lex.getTerm());
						System.out.println("code : " + lex.getTermId());
						System.out.println("tf : " + lex.getTF());
						System.out.println("nt : " + lex.getNt());
					}
					else
					{
						System.out.println("lexicon "+indexNo+": not found");
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
			System.err.println("qtw: " + queryTerms.getTermWeight(currentTerm));
			
			
			//check if the IDF is very low.
			if (IGNORE_LOW_IDF_TERMS==true && docIndex.getNumberOfDocuments() < lexicon.getTF()) {
				System.err.println("query term " + queryTermStrings[i] + " has low idf - ignored from scoring.");
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
			System.err.println(" with " + numMatchingDocsForThisTerm + " documents (TF is " + TFInCollection + ").");
			System.err.println("Nt: " + numMatchingDocsForThisTerm);
			
			double length = 0;

			double[] termScores = new double[allPointersDocids.length];
			double[] p = new double[NumberOfFields];
			double totalNumberOfTokens = 0.0d;
			for (int j=0; j<NumberOfFields; j++) {
				//p[j] = fieldStats[j].getNumberOfTokens() / fieldStats[j].getNumberOfDocuments();
				//totalNumberOfTokens += fieldStats[j].getNumberOfTokens();
			}
			
			double weight[] = new double[NumberOfFields];
			
			
			for (int j=0; j<NumberOfFields; j++) {
				p[j] = 1.0d / (fieldStats[j].getNumberOfDocuments() * NumberOfFields);
				//p[j] = p[j] / totalNumberOfTokens;
				String propertyKey = "p."+(j+1);
				String propertyValue = ApplicationSetup.getProperty(propertyKey, "1.0d");
				weight[j] = Double.parseDouble(propertyValue);
				System.err.println("p."+(j+1) + " = " + weight[j]);
				p[j] = p[j] / weight[j];
				System.err.println("p."+(j+1)+" = " + p[j]);
			}

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

				double q = 1.0d;
				double TF = TFInCollection;
				double tf_q = TF;
				final double log2 = Math.log(2.0d);
				//double score = -TF * Math.log(TF)/log2 + TF - Math.log(TF)/log2/2.0d - Math.log(2.0d*Math.PI)/log2/2.0d;
				//double score = (NumberOfFields/2.0d)*Math.log(2.0d*Math.PI*TF)/log2;
				//System.err.println("number of Fields: " + NumberOfFields);
				//System.err.println("TF: " + TF + " score: " + score);
				
				//find the number of fields containing the query term
				int fieldsWithTerm = 0;
				for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
				{
					if (termFoundInFields[indexNo] && (Arrays.binarySearch(pointers[indexNo][0],currentDocid)) >= 0) {
						fieldsWithTerm++;
					}
				}

				double score = (fieldsWithTerm/2.0d)*Math.log(2.0d*Math.PI*TF)/log2;
				for(int indexNo=0;indexNo<NumberOfFields;indexNo++)
				{
					//determine the length of the document in that field
					lengths[indexNo] = indices[indexNo].getDocumentIndex().getDocumentLength(currentDocid);
					//update the known length of this document
					allPointersLengths[j] += lengths[indexNo];
					int tmpIndex = -1;
					if (termFoundInFields[indexNo] && (tmpIndex = Arrays.binarySearch(pointers[indexNo][0],currentDocid)) >= 0) {
						frequencies[indexNo] = pointers[indexNo][1][tmpIndex];

						//normalise the term frequency from that field
						if(normalisation[indexNo] != null && frequencies[indexNo] != 0)
							frequencies[indexNo] = normalisation[indexNo].normalise(
								frequencies[indexNo], 
								lengths[indexNo], 
								lexicons[indexNo].getTF());
						allPointersFreqs[j] += frequencies[indexNo];
						p[indexNo] = 1.0d / (fieldStats[0].getNumberOfDocuments() * fieldsWithTerm * weight[indexNo]);
						double tf = frequencies[indexNo];
						tf_q -= tf;
						q -= p[indexNo];
						double tmp = tf * Math.log(tf / (TF*p[indexNo]))/log2 + Math.log(tf/TF)/(2.0d*log2);

						if (tf > 0.0d) 
							score += tmp;
						if (Double.isNaN(score)) {
							System.err.println("NaN found for docid " + currentDocid + " tf: " + tf + "field: " + indexNo);
						}		
					}
				}			

				score += tf_q * Math.log(tf_q / (TF*q))/log2 + Math.log(tf_q/TF)/(2.0d*log2);
				termScores[j] = score / (1.0d + TF - tf_q);	
				termScores[j] = queryTerms.getTermWeight(currentTerm) * termScores[j];
			}
			
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

		System.out.println("score of 1st document ("+docids[0]+") : " + scores[0]);
		
		
		
		//modifyScores(query, docids, scores);
		//application dependent modification of scores
		//of documents for a query
		//sorting the result set after applying each DSM
		for (int t = 0; t < numOfDocModifiers; t++) {
			if (((DocumentScoreModifier)documentModifiers.get(t)).modifyScores(index, queryTerms, resultSet))
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
	
}
