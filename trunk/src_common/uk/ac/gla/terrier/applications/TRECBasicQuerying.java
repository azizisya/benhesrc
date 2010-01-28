package uk.ac.gla.terrier.applications;

import uk.ac.gla.terrier.querying.BasicQueryExpansion;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.structures.trees.*;
import uk.ac.gla.terrier.utility.*;
import uk.ac.gla.terrier.matching.*;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.models.queryexpansion.QueryExpansionModel;

import java.io.*;
import java.util.HashSet;
import java.util.Vector;

/**
 * This class performs a batch mode retrieval for a set of queries.
 * Creation date: (09/07/2003 14:38:41)
 * @author Gianni Amati, Vassilis Plachouras
 */
public class TRECBasicQuerying { 

	/** The class that performs the actual matching. */
	protected BufferedMatching matching;
	
	/** The document index used by the BasicMatching class.*/
	protected DocumentIndex docIndex;
	
	protected final double DEFAULT_C_POST_EXPANSION = 7d;
	
	protected final double DEFAULT_B_POST_EXPANSION = 0.35;

	/** The lexicon used by the BasicMatching class.*/
	protected Lexicon lexicon;

	/** The inverted file used by the BasicMatching class.*/
	protected InvertedIndex invertedIndex;
	
	protected DirectIndex directIndex;

	/** The file to store the output to.*/
	protected PrintWriter resultFile;

	/** The weighting model used.*/
	protected WeightingModel wmodel;

	/** The stopword list.*/
	protected PorterStopPipeline pipe;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	protected WeightingModel qemodel;
	
	protected String[] wmodelNames;
	
	protected String[] qemodelNames;
	
	protected boolean REFORMULATE = false;
	
	public int REFORMULATE_TERMS = 
		Integer.parseInt(ApplicationSetup.getProperty("reformulate.terms", "5"));
	
	public int REFORMULATE_DOCUMENTS =
		Integer.parseInt(ApplicationSetup.getProperty("reformulate.documents", "5"));;
	
	protected Index index;
	
	public boolean QUERY_EXPANSION = false;
	
	protected BasicQueryExpansion queryExpansion;
	
	protected CollectionStatistics collSta;
	
	/** 
	 * The method - ie the weighting model and parameters.
	 * Examples:  <tt>TF_IDF</tt>, <tt>PL2c1.0</tt> 
	 */
	protected String method = null;
	
	/** The number of results to output. */
	protected static int RESULTS_LENGTH = Integer.parseInt(ApplicationSetup.getProperty("trec.output.format.length", "1000"));
	
	/** A TREC specific output field. */
	protected static String ITERATION =  ApplicationSetup.getProperty("trec.iteration", "Q");


	
	/**
	 * TRECBasicQuerying default constructor initialises the inverted index, the lexicon and the document index structures.
	 * Creation date: (09/07/2003 16:16:27)
	 */
	public TRECBasicQuerying(boolean reformulation, boolean expansion) {
		collSta = index.getCollectionStatistics();
		this.REFORMULATE = reformulation;
		this.QUERY_EXPANSION = expansion;
		long startLoading = System.currentTimeMillis();
		this.index = Index.createIndex();
		docIndex = index.getDocumentIndex();
		long endLoading = System.currentTimeMillis();
		System.err.println("time to load document index in memory: " + ((endLoading-startLoading)/1000.0D));
		lexicon = index.getLexicon();
		invertedIndex = index.getInvertedIndex();
		directIndex = index.getDirectIndex();
		if (debugging)
			System.out.println("Calling constructor of BufferedMatching...");
		matching = new BufferedMatching(index);
		if (debugging)
			System.out.println("Finished calling constructor of BufferedMatching...");
		this.pipe = new PorterStopPipeline();
		this.loadModels();
		this.queryExpansion = new BasicQueryExpansion(index);
	}
	/**
	 *  Closes the used structures.
	 */
	public void close() {
		invertedIndex.close();
		lexicon.close();
		docIndex.close();
	}
	
	public void setReformulateQuery(boolean flag){
		this.REFORMULATE = flag;
	}
	
 	/**
 	 * Returns a PrintWriter used to store the results.
 	 * @param predefinedName java.lang.String a non-standard prefix for the result file.
 	 * @return a handle used as a destination for storing results.
 	 */ 
	public PrintWriter getResultFile(String predefinedName) {

		PrintWriter resultFile = null;
		
		File fx = new File(ApplicationSetup.TREC_RESULTS);
        if (!fx.exists())
            fx.mkdir();

        fx = new File(ApplicationSetup.TREC_RESULTS, "querycounter");
        int counter = 0;
        if (!fx.exists()) {
            try {
                BufferedWriter bufw = new BufferedWriter(new FileWriter(fx));
                bufw.write(counter + ApplicationSetup.EOL);
                bufw.close();
            } catch (IOException ioe) {
                System.err.println("Input/Output exception while defining querycounter. Stack trace follows.");
                ioe.printStackTrace();
                System.exit(1);
            }
        } else
            try {
                BufferedReader buf = new BufferedReader(new FileReader(fx));
                String s = buf.readLine();
                counter = (new Integer(s)).intValue();
                counter++;
                buf.close();
                BufferedWriter bufw = new BufferedWriter(new FileWriter(fx));
                bufw.write(counter + ApplicationSetup.EOL);
                bufw.close();
            } catch (Exception e) {
                System.err.println(e + "defining querycounter");
                System.exit(1);
            }
        try {
	        String prefix = null;
	        if (predefinedName==null || predefinedName.equals(""))
	        	prefix = ApplicationSetup.TREC_MODELS;
	        else
	        	prefix = predefinedName;
	        int queryType = SystemUtility.queryType();
	        prefix = prefix + "_q" + queryType;
	        if (REFORMULATE)
	        	prefix = prefix + "_" + REFORMULATE_TERMS + "_" + REFORMULATE_DOCUMENTS;
	        if (QUERY_EXPANSION)
	        	prefix = prefix + "_" + qemodel.getInfo();
	        resultFile =
                	new PrintWriter(
	                	new BufferedWriter(
		                	new FileWriter(
			                	new File(ApplicationSetup.TREC_RESULTS, prefix + "_" + counter + ApplicationSetup.TREC_RESULTS_SUFFIX))));
        } catch (IOException e) {
            System.err.println("Input/Output exception while creating the result file. Stack trace follows.");
            e.printStackTrace();
            System.exit(1);
        }

        return resultFile;
    }
	
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * Creation date: (17/07/2003 11:26:24)
	 * @param queryId java.lang.String The query number
	 * @param queryTerms uk.ac.gla.terrier.structures.trees.TreeNode[] The query terms
	 */
	public void reformulateQuery(BasicQuery query) {
		//at this stage, the only matching function available is the pure content retreival.
		//Later, classes for query expansion, and link analysis should be added here. 
		//This is a matter of architecture
		
			matching.setModel(wmodel);
			TermTreeNode[] topTerms = null;
			if (query.getQueryLength() > this.REFORMULATE_TERMS)
				topTerms = query.termsWithHighestIdf(this.REFORMULATE_TERMS, lexicon);
			else
				topTerms = (TermTreeNode[])query.getQueryTerms().clone();
			//TermTreeNode[] topTerms = query.termsWithHighestFrequency(this.REFORMULATE_TERMS);
			//TermTreeNode[] topTerms = (TermTreeNode[])query.getQueryTerms().clone();
			BasicQuery topTermsQuery = new BasicQuery(topTerms, query.getQueryNumber());
			matching.basicMatch(topTermsQuery);
			// insert terms in the top-ranked documents into expansionTerms
			ResultSet resultSet = matching.getResultSet();
			int topX = Math.min(this.REFORMULATE_DOCUMENTS, resultSet.getExactResultSize());
			if (topX == 0)
				return;
			int[] docids = resultSet.getDocids();
			double[] scores = resultSet.getScores();
			int totalLength = 0;
			for (int i = 0; i < topX; i++)
				totalLength += docids[i];
			ExpansionTerms expansionTerms = new ExpansionTerms(collSta, totalLength,
					lexicon); 
			System.out.println("top ranked documents:");
			for (int i = 0; i < topX; i++) {
				System.out.println((i+1) + ": " + docIndex.getDocumentNumber(docids[i]) +
						" with score " + Rounding.toString(scores[i], 4));
				int[][] terms = directIndex.getTerms(docids[i]);
				for (int j = 0; j < terms[0].length; j++)
					expansionTerms.insertTerm(terms[0][j], (double)terms[1][j]);
			}
			
			// weight the other query terms
			WeightingModel qemodel = null;
			String qeModelName = "uk.ac.gla.terrier.matching.models.Bo1";
			qemodel = WeightingModel.getWeightingModel(qeModelName);
			TermTreeNode[] queryTerms = query.getQueryTerms();
			HashSet hashSet = new HashSet();
			for (int i = 0; i < topTerms.length; i++)
				hashSet.add(topTerms[i].term);
			expansionTerms.assignWeights(qemodel);
			for (int i = 0; i < queryTerms.length; i++){
				//if (!hashSet.contains(queryTerms[i].term)){
				double original = queryTerms[i].normalisedFrequency;
				double weight = expansionTerms.getExpansionWeight(queryTerms[i].term) * queryTerms[i].frequency;
				if (weight <= 0d){
					System.out.println("term " + queryTerms[i].term + 
							"'s weight <= 0d");
					Idf idf = new Idf();
					if (lexicon.findTerm(queryTerms[i].term)){
						weight = idf.log(1+
									(double)lexicon.getTF() /
									collSta.getNumberOfTokens()) *
									queryTerms[i].frequency;
					}
					else{
						System.out.println("term not found in lexicon.");
						continue;
					}
				}
//				if (hashSet.contains(queryTerms[i].term))
//					queryTerms[i].normalisedFrequency += weight;
//				else
				queryTerms[i].normalisedFrequency += weight;
				System.out.println("reset term " + queryTerms[i].term + "'s " +
						"normalisedFrequency from " +
						Rounding.toString(original, 4) +
						" to " + Rounding.toString(queryTerms[i].normalisedFrequency, 4));
				//}
			}
		
//		else{
//			System.out.println("Query is too short. Do not apply query reformulation.");
//		}
	}
	
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * Creation date: (17/07/2003 11:26:24)
	 * @param queryId java.lang.String The query number
	 * @param queryTerms uk.ac.gla.terrier.structures.trees.TreeNode[] The query terms
	 */
	public void processQuery(BasicQuery query) {
		//at this stage, the only matching function available is the pure content retreival.
		//Later, classes for query expansion, and link analysis should be added here. 
		//This is a matter of architecture
		matching.setModel(wmodel);
		matching.basicMatch(query);
		this.printResults(resultFile, matching.getResultSet(), query.getQueryNumber());
		//outputFormatter.printResults(queryId, matching.getResultSet(), resultFile, matching.getInfo(), null);
	}

	/** 
	 * Prints the results for the given search request, 
	 * using the specified destination. 
	 * @param pw PrintWriter the destination where to save the results.
	 * @param q SearchRequest the object encapsulating the query and the results.
	 */
	public void printResults(PrintWriter pw, ResultSet set, String queryid) {
		int[] docids = set.getDocids();
		double[] scores = set.getScores();
		int minimum = RESULTS_LENGTH;
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length
		if (minimum > set.getResultSize())
			minimum = set.getResultSize();
		String iteration = ITERATION + "0";
		
		String queryIdExpanded = queryid + " " + iteration + " ";
		String methodExpanded = " " + method + "\n";
		StringBuffer sbuffer = new StringBuffer();
		//even though we have single-threaded usage
		//in mind, the synchronized makes code faster
		//since each sbuffer.append() call does not
		//try to obtain a lock.
		synchronized(sbuffer) {
			//the results are ordered in desc	eding order
			//with respect to the score. 
			int start = 0;
			int end = minimum;
			for (int i = start; i < end; i++) {
				sbuffer.append(queryIdExpanded);
				sbuffer.append(docIndex.getDocumentNumber(docids[i]));
				sbuffer.append(" ");
				sbuffer.append(i);
				sbuffer.append(" ");
				sbuffer.append(scores[i]);
				sbuffer.append(methodExpanded);
			}
			pw.write(sbuffer.toString());
		}
		pw.flush();
	}
	
	public void loadModels(){
		try{
			BufferedReader methodsFile = new BufferedReader(
					new FileReader(ApplicationSetup.TREC_MODELS));
			String methodName;
			Vector names = new Vector();
			while ((methodName = methodsFile.readLine()) != null){
				if (!methodName.startsWith("#"))
					names.addElement(methodName);
			}	
			methodsFile.close();
			this.wmodelNames = (String[])names.toArray(new String[names.size()]);
			
			methodsFile = new BufferedReader(
					new FileReader(ApplicationSetup.EXPANSION_MODELS));
			names = new Vector();
			if (this.QUERY_EXPANSION){
				while ((methodName = methodsFile.readLine()) != null){
					methodName = methodName.trim();
					System.out.println(methodName);
					if (methodName.length() == 0)
						continue;
					if (!methodName.startsWith("#"))
						names.addElement(methodName);
				}	
				methodsFile.close();
				this.qemodelNames = (String[])names.toArray(new String[names.size()]);
			}
			else{
				this.qemodelNames = new String[1];
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Performs the matching using the specified weighting model 
	 * from the setup and possibly a combination of evidence mechanism.
	 * It parses the file with the queries (the name of the file is defined
	 * in the address_query file), creates the file of results, and for each
	 * query, gets the relevant documents, scores them, and outputs the results
	 * to the result file.
	 * @param betaParameter the value of beta
	 */
	public void query(double betaParameter) {
		TRECQuery queries = new TRECQuery();
		
		for (int i = 0; i < wmodelNames.length; i++) {
			String methodName = wmodelNames[i];
			//creating the weighting model, according to the value setup in the properties file
			try {
				wmodel = (WeightingModel) Class.forName(methodName).newInstance();
				method = wmodel.getInfo();
				wmodel.setParameter(betaParameter);
				matching.setModel(wmodel);
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			} 
			for (int j = 0; j < qemodelNames.length; j++){
				if (this.QUERY_EXPANSION){
					try {
						qemodel = WeightingModel.getWeightingModel(qemodelNames[j]);
						method += " " + qemodel.getQEInfo();
						qemodel.setAverageDocumentLength(collSta.getAverageDocumentLength());
						qemodel.setNumberOfTokens(collSta.getNumberOfTokens());
					} catch(Exception e) {
						e.printStackTrace();
						System.err.println("qemodelNames.length: " + qemodelNames.length);
						System.err.println("qemodelNames[j]: " + qemodelNames[j]);
						System.exit(1);
					} 
				}
                       
				//	the file to store the results to.
				
				resultFile = getResultFile(wmodel.getInfo());
				while (queries.hasMoreQueries()) {
					wmodel.setParameter(betaParameter);
					System.out.println("weighting model: " + wmodel.getInfo());
					BasicQuery query = new BasicQuery(queries.nextQuery(),
							queries.getQueryId(), pipe);
					System.err.println("processing query " + query.getQueryNumber());
					if (this.REFORMULATE&&this.QUERY_EXPANSION){
						TermTreeNode[] originalQueryTerms = 
							(TermTreeNode[])query.getQueryTerms().clone();
						System.out.println("query reformulation enabled.");
						this.reformulateQuery(query);
						System.out.println("reformulated query:");
						query.dumpQuery();
						
						if (query.getQueryLength() < ApplicationSetup.EXPANSION_TERMS){
							queryExpansion.setQueryExpansionModel(qemodel);
							matching.basicMatch(query);
							query.setQueryTerms(originalQueryTerms);
							queryExpansion.expandQuery(query, matching.getResultSet());
							System.out.println("Expanded query:");
							query.dumpQuery();
						}
						else{
							System.out.println("query is too long. do not apply query expansion.");
						}
						
//						if (wmodel.getInfo().startsWith("BM25"))
//							wmodel.setParameter(this.DEFAULT_B_POST_EXPANSION);
//						else{
//							wmodel.setParameter(this.DEFAULT_C_POST_EXPANSION);
//						}
						
					}
					else{
						if (this.REFORMULATE){
							System.out.println("query reformulation enabled.");
							this.reformulateQuery(query);
							System.out.println("reformulated query:");
							query.dumpQuery();
						}
						if (this.QUERY_EXPANSION){
							if (query.getQueryLength() < ApplicationSetup.EXPANSION_TERMS){
								queryExpansion.setQueryExpansionModel(qemodel);
		//						matching.basicMatch(
		//								query.getQueryNumber(),
		//								query.termsWithHighestIdf(this.REFORMULATE_TERMS, lexicon)
		//								);
								matching.basicMatch(query);
								queryExpansion.expandQuery(query, matching.getResultSet());
								System.out.println("Expanded query:");
								query.dumpQuery();
								if (wmodel.getInfo().startsWith("BM25"))
									wmodel.setParameter(this.DEFAULT_B_POST_EXPANSION);
								else{
									wmodel.setParameter(this.DEFAULT_C_POST_EXPANSION);
								}
							}else{
								System.out.println("query is too long. do not apply query expansion.");
							}
							
						}
					}
					
						
					//process the query
					long processingStart = System.currentTimeMillis();
					processQuery(query);
					long processingEnd = System.currentTimeMillis();
					System.out.println("time to process query: " + ((processingEnd - processingStart)/1000.0D));
				}
				resultFile.close();
			}
		}
		
	}
	
	public static void main(String[] args){
		// -ret <parameter> <expansion> <reformulation>
		double parameter = Double.parseDouble(args[1]);
		boolean expansion = new Boolean(args[2]).booleanValue();
		boolean reformulation = new Boolean(args[3]).booleanValue();
		System.out.println("Calling the constructor...");
		TRECBasicQuerying querying = new TRECBasicQuerying(reformulation, expansion);
//		if (args.length > 2){
//			querying.setReformulateQuery(Boolean.valueOf(args[2]).booleanValue());
//			System.out.println("set " + querying.REFORMULATE);
//			for (int term = 3; term <= 10; term++)
//				for (int docs = 3; docs <= 10; docs++){
//					querying.REFORMULATE_TERMS = term;
//					querying.REFORMULATE_DOCUMENTS = docs;
//					querying.query(parameter);
//				}
//		}
//		else
//			querying.query(parameter);
//		}
		System.out.println("staring querying process...");
		querying.query(parameter);
	}
}
