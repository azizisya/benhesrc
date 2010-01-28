package uk.ac.gla.terrier.applications;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.StringUtility;
import uk.ac.gla.terrier.utility.TroveUtility;

public class ExtractInformativeTerms {
	
	protected Index index;
	
	protected DirectIndex directIndex;
	
	protected Lexicon lexicon;
	
	protected InvertedIndex invIndex;
	
	protected DocumentIndex docIndex;
	
	protected final String EOL = ApplicationSetup.EOL;
	
	protected CollectionStatistics collStats;
	
	
	public ExtractInformativeTerms() {
		this(Index.createIndex());
	}
	
	public ExtractInformativeTerms(Index index){
		this.index = index;
		directIndex = index.getDirectIndex();
		lexicon = index.getLexicon();
		invIndex = index.getInvertedIndex();
		docIndex = index.getDocumentIndex();
		collStats = index.getCollectionStatistics();
	}
	/**
	 * Extract the most informative terms from randomly selected documents and store them on disk.
	 * @param numberOfRandomDocs
	 * @param numberOfExtractedTerms
	 * @param qemodelName
	 * @param outputFilename
	 */
	public void printInformativeTermsFromRandomDocs(int numberOfRandomDocs, 
			int numberOfExtractedTerms, String qemodelName){
		ExpansionTerm[] topterms = this.getInformativeTermsFromRandomDocs(numberOfRandomDocs, 
				numberOfExtractedTerms, qemodelName);
		for (int i=0; i<topterms.length; i++){
				lexicon.findTerm(topterms[i].getTermID());
				System.out.print(lexicon.getTerm()+"^"+topterms[i].getWeightExpansion()+" ");
		}
		System.out.println();
	}
	/**
	 * Extract the most informative terms from randomly selected documents.
	 * @param numberOfRandomDocs The number of randomly selected documents.
	 * @param numberOfExtractedTerms The number of the most informative terms to be extracted.
	 * @param qemodelName The name of the QE model.
	 * @return
	 */
	public ExpansionTerm[] getInformativeTermsFromRandomDocs(int numberOfRandomDocs, 
			int numberOfExtractedTerms, String qemodelName){
		TIntHashSet docidSet = new TIntHashSet();
		int numberOfDocuments = collStats.getNumberOfDocuments();
		while (docidSet.size()<numberOfRandomDocs)
			docidSet.add((int)(Math.random()*(numberOfDocuments-1)));
		return getInformativeTerms(docidSet.toArray(), numberOfExtractedTerms, qemodelName);
	}
	
	
	/**
	 * Get informative terms from the specified documents.
	 * @param docids ids of the specified documents.
	 * @param numberOfExtractedTerms The number of most informative terms to be extracted.
	 * @param qemodelName The name of the QE model.
	 * @return The most informative terms.
	 */
	public ExpansionTerm[] getTerms(int[] docids, String qemodelName){
		// initiate QE model
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelName);
		
		// Get pseudoLength
		
		double pseudoLength = 0d;
		for (int i=0; i<docids.length; i++){
			pseudoLength += docIndex.getDocumentLength(docids[i]);
		}
		//System.out.println("Number of tokens in the document set: "+pseudoLength);
		// initiate expansionTerms
		int N = this.collStats.getNumberOfDocuments();
		
		int numberOfDocuments = collStats.getNumberOfDocuments();
		long numberOfTokens = collStats.getNumberOfTokens();
		double avl = collStats.getAverageDocumentLength();
		
		ExpansionTerms expansionTerms = new ExpansionTerms(
				numberOfDocuments,
				numberOfTokens,
				avl,
				pseudoLength, 
				lexicon);
		//System.out.print("Initialising ExpansionTerms...");
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<docids.length; i++){
			int[][] terms = directIndex.getTerms(docids[i]);
			if (terms == null)
				continue;
			else
				for (int j = 0; j < terms[0].length; j++){
					expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
					termidSet.addAll(terms[0]);
				}
		}
		if (docids.length==1)
			expansionTerms.setEXPANSION_MIN_DOCUMENTS(1);
		
	//	System.out.println("Done.");
		// assign weights and get the term weights
		//System.out.print("Assigning term weights...");
		TIntObjectHashMap<ExpansionTerm> termMap = expansionTerms.getExpandedTermHashSet(termidSet.size(), qemodel);
		ExpansionTerm[] expandedTerms = new ExpansionTerm[termMap.size()];
		int i = 0;
		for (Integer k : termMap.keys()){
			expandedTerms[i++] = termMap.get(k);
		}
		Arrays.sort(expandedTerms);
		for (i=0; i<expandedTerms.length; i++)
			expandedTerms[i].setToken(lexicon.getLexiconEntry(expandedTerms[i].getTermID()).term);
		//System.out.println("Done.");
		return expandedTerms;
	}
	
	/**
	 * Get informative terms from the specified documents.
	 * @param docids ids of the specified documents.
	 * @param numberOfExtractedTerms The number of most informative terms to be extracted.
	 * @param qemodelName The name of the QE model.
	 * @return The most informative terms.
	 */
	public ExpansionTerm[] getInformativeTerms(int[] docids, int numberOfExtractedTerms, String qemodelName){
		// initiate QE model
		WeightingModel qemodel = WeightingModel.getWeightingModel(qemodelName);
		
		// Get pseudoLength
		
		double pseudoLength = 0d;
		for (int i=0; i<docids.length; i++){
			pseudoLength += docIndex.getDocumentLength(docids[i]);
		}
		//System.out.println("Number of tokens in the document set: "+pseudoLength);
		// initiate expansionTerms
		int N = this.collStats.getNumberOfDocuments();
		
		int numberOfDocuments = collStats.getNumberOfDocuments();
		long numberOfTokens = collStats.getNumberOfTokens();
		double avl = collStats.getAverageDocumentLength();
		
		ExpansionTerms expansionTerms = new ExpansionTerms(
				numberOfDocuments,
				numberOfTokens,
				avl,
				pseudoLength, 
				lexicon);
		//System.out.print("Initialising ExpansionTerms...");
		for (int i=0; i<docids.length; i++){
			int[][] terms = directIndex.getTerms(docids[i]);
			if (terms == null)
				continue;
			else
				for (int j = 0; j < terms[0].length; j++){
					expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
				}
		}
		if (docids.length==1)
			expansionTerms.setEXPANSION_MIN_DOCUMENTS(1);
	//	System.out.println("Done.");
		// assign weights and get the term weights
		//System.out.print("Assigning term weights...");
		TIntObjectHashMap<ExpansionTerm> termMap = expansionTerms.getExpandedTermHashSet(numberOfExtractedTerms, qemodel);
		ExpansionTerm[] expandedTerms = new ExpansionTerm[termMap.size()];
		int i = 0;
		for (Integer k : termMap.keys()){
			expandedTerms[i++] = termMap.get(k);
		}
		Arrays.sort(expandedTerms);
		for (i=0; i<expandedTerms.length; i++)
			expandedTerms[i].setToken(lexicon.getLexiconEntry(expandedTerms[i].getTermID()).term);
		//System.out.println("Done.");
		qemodel = null;
		return expandedTerms;
	}
	
	public void extractInfomativeTermsAndWrite(String feedbackFilename, String outputFilename, String qeModelname, int numberOfExtractedTerms){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(feedbackFilename);
		String[] qids = qrels.getQueryids(); Arrays.sort(qids);
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<qids.length; i++){
			System.out.println(">>>>>Processing query "+qids[i]);
			String[] relDocidStrings = qrels.getRelevantDocumentsToArray(qids[i]);
			String[] nonrelDocidStrings = qrels.getNonRelevantDocumentsToArray(qids[i]);
			int[] docids = new int[relDocidStrings.length+nonrelDocidStrings.length];
			int counter = 0;
			for (int j=0; j<relDocidStrings.length; j++)
				docids[counter++] = Integer.parseInt(relDocidStrings[j]);
			for (int j=0; j<nonrelDocidStrings.length; j++)
				docids[counter++] = Integer.parseInt(nonrelDocidStrings[j]);
			// get expansion terms from all feedback documents
			System.out.print("Expanded from all feedback documents...");
			ExpansionTerm[] terms = this.getInformativeTerms(docids, numberOfExtractedTerms, qeModelname);
			Arrays.sort(terms);
			buf.append(qids[i]);
			for (int j=0; j<docids.length; j++)
				buf.append(" "+docids[j]);
			buf.append(" :");
			for (int j=0; j<terms.length; j++)
				buf.append(" "+terms[j].getTermID()+"^"+terms[j].getWeightExpansion());
			buf.append(ApplicationSetup.EOL);
			System.out.println("Done.");
			// get expansion terms from each individual feedback document
			for (int docid : docids){
				System.out.print("Expanded from document "+docid+"...");
				int[] fbdocids = {docid};
				terms = this.getInformativeTerms(fbdocids, numberOfExtractedTerms, qeModelname);
				Arrays.sort(terms);
				buf.append(qids[i]+" "+docid+" :");
				for (int j=0; j<terms.length; j++)
					buf.append(" "+terms[j].getTermID()+"^"+terms[j].getWeightExpansion());
				buf.append(ApplicationSetup.EOL);
				System.out.println("Done.");
			}
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Saved in file "+outputFilename);
	}
	
	public void extractInfomativeTermsForCoTraining(String resultFilename, String outputFilename, String qeModelname, int numberOfExtractedTerms){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		String[] qids = results.getQueryids(); Arrays.sort(qids);
		// StringBuffer buf = new StringBuffer();
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			for (int i=0; i<qids.length; i++){
				System.out.println(">>>>>Processing query "+qids[i]);
				int[] docids = TroveUtility.stringArrayToIntArray(results.getDocnoSet(qids[i]));
				// get expansion terms from all retrieved documents
				TIntHashSet termidSet = new TIntHashSet();
				System.out.print("Expand from all retrieved documents...");
				ExpansionTerm[] terms = this.getInformativeTerms(docids, numberOfExtractedTerms, qeModelname);
				Arrays.sort(terms);
				bw.write(qids[i]);
				for (int j=0; j<docids.length; j++)
					bw.write(" "+docids[j]);
				bw.write(" :");
				for (int j=0; j<terms.length; j++){
					termidSet.add(terms[j].getTermID());
					bw.write(" "+terms[j].getTermID()+"^"+terms[j].getWeightExpansion());
				}
				bw.write(ApplicationSetup.EOL);
				System.out.println("Done.");
				
				// get expansion terms from each individual feedback document
				for (int docid : docids){
					System.out.print("Expanded from document "+docid+"...");
					int[] fbdocids = {docid};
					terms = this.getTerms(fbdocids, qeModelname);
					if (terms.length == 0)
						continue;
					Arrays.sort(terms);
					bw.write(qids[i]+" "+docid+" :");
					for (int j=0; j<terms.length; j++)
						if (termidSet.contains(terms[j].getTermID()))
							bw.write(" "+terms[j].getTermID()+"^"+terms[j].getWeightExpansion());
					bw.write(ApplicationSetup.EOL);
					System.out.println("Done.");
					fbdocids = null; terms = null;
				}
				termidSet.clear(); termidSet = null;
				if (i%10 == 0)
					System.gc();
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Saved in file "+outputFilename);
	}
	
	public void extractInfomativeTermsAndWrite(
			String bestFeedbackFilename,
			String feedbackFilename, 
			String outputFilename, 
			String qeModelname, 
			int numberOfExtractedTerms){
		TRECQrelsInMemory bestQrels = new TRECQrelsInMemory(bestFeedbackFilename);
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(feedbackFilename);
		String[] qids = qrels.getQueryids(); Arrays.sort(qids);
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);			
			for (int i=0; i<qids.length; i++){
				// StringBuffer buf = new StringBuffer();
				System.out.println(">>>>>Processing query "+qids[i]);
				String[] docidStrings = bestQrels.getRelevantDocumentsToArray(qids[i]);
				int[] bestDocids = StringUtility.stringsToInts(docidStrings);
				// get expansion terms from all feedback documents
				System.out.print("Expanded from best feedback documents...");
				ExpansionTerm[] terms = this.getInformativeTerms(bestDocids, numberOfExtractedTerms, qeModelname);
				Arrays.sort(terms);
				bw.write(qids[i]);
				for (int j=0; j<bestDocids.length; j++)
					bw.write(" "+bestDocids[j]);
				bw.write(" :");
				for (int j=0; j<terms.length; j++)
					bw.write(" "+terms[j].getTermID()+"^"+terms[j].getWeightExpansion());
				bw.write(ApplicationSetup.EOL);
				System.out.println("Done.");
				// get expansion terms from each individual feedback document
				String[] relDocidStrings = qrels.getRelevantDocumentsToArray(qids[i]);
				String[] nonrelDocidStrings = qrels.getNonRelevantDocumentsToArray(qids[i]);
				int[] docids = new int[relDocidStrings.length+nonrelDocidStrings.length];
				int counter = 0;
				for (int j=0; j<relDocidStrings.length; j++)
					docids[counter++] = Integer.parseInt(relDocidStrings[j]);
				for (int j=0; j<nonrelDocidStrings.length; j++)
					docids[counter++] = Integer.parseInt(nonrelDocidStrings[j]);
				for (int docid : docids){
					System.out.print("Expanded from document "+docid+"...");
					int[] fbdocids = {docid};
					terms = this.getInformativeTerms(fbdocids, numberOfExtractedTerms, qeModelname);
					Arrays.sort(terms);
					bw.write(qids[i]+" "+docid+" :");
					for (int j=0; j<terms.length; j++)
						bw.write(" "+terms[j].getTermID()+"^"+terms[j].getWeightExpansion());
					bw.write(ApplicationSetup.EOL);
					System.out.println("Done.");
					terms = null;
					fbdocids = null;
				}
				// bw.write(buf.toString());
				relDocidStrings = null; nonrelDocidStrings = null; docidStrings = null; bestDocids = null;
				if (i%10 == 0)
					System.gc();
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Saved in file "+outputFilename);
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(args[0]);
		if (args[0].equals("--extractinfotermsfromrandomdocs")){
			// --extractinfotermsfromrandomdocs numberOfRandomDocs numberOfExtractedTerms qemodelName
			ExtractInformativeTerms app = new ExtractInformativeTerms();
			app.printInformativeTermsFromRandomDocs(Integer.parseInt(args[1]),
					Integer.parseInt(args[2]), args[3]);
		}else if (args[0].equals("--extractandwriteinfotermsfromfbdocs")){
			// --extractandwriteinfotermsfromfbdocs feedbackFilename, outputFilename, qeModelname, numberOfExtractedTerms
			ExtractInformativeTerms app = new ExtractInformativeTerms();
			app.extractInfomativeTermsAndWrite(args[1], args[2], args[3], Integer.parseInt(args[4]));
		}else if (args[0].equals("--extractandwriteinfoterms")){
			System.out.println("1");
			// --extractandwriteinfoterms bestFeedbackFilename feedbackFilename, outputFilename, qeModelname, numberOfExtractedTerms
			ExtractInformativeTerms app = new ExtractInformativeTerms();
			app.extractInfomativeTermsAndWrite(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
		}else if (args[0].equals("--extractinfotermsforcotraining")){
			// --extractinfotermsforcotraining feedbackFilename, outputFilename, qeModelname
			ExtractInformativeTerms app = new ExtractInformativeTerms();
			app.extractInfomativeTermsForCoTraining(args[1], args[2], args[3], Integer.parseInt(args[4]));
		}
		else{
			System.out.println("Usage: ");
			System.out.println("--extractinfotermsfromrandomdocs numberOfRandomDocs numberOfExtractedTerms qemodelName");
		}

	}

}
