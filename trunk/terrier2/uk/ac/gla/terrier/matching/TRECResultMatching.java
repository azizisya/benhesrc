package uk.ac.gla.terrier.matching;
import java.io.IOException;
import java.io.BufferedReader;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.structures.Index;


/** Implements a Matching class, where all returned results are retrieved from a TREC compatible results file. Such a results file
  * is compatible with trec_eval. In particular, the format is 
  * <pre>queryID Q0 docno score rank label</pre>
  * <p><b>Properties:</b><ul>
  * <li><tt>trecresults.matching.file</tt> - path to the TREC formatted results file.</li>
  * <tt>trecresults.matching.scores</tt> - whether scores should be parsed. Some TREC submitting systems do not appear to use
  * scores assignments compatible with Java's Double.parseDouble() - for these run, the scores should be ignored.
  * @version $Revision: 1.1 $
  * @author Craig Macdonald
  */
public class TRECResultMatching extends Matching
{
	/** number of errors to allow when reading a results file to become fatal */
	static final int MAX_ERRORS = 20;
	/** filename of .res file containing ranking of docs for the required queries */
	String resultsFilename = ApplicationSetup.getProperty("trecresults.matching.file",null);
	/** whether the scores in the .res file should be parsed */
	final boolean parseScore = Boolean.parseBoolean(ApplicationSetup.getProperty("trecresults.matching.scores", "false"));

	/** file being read */
	BufferedReader resultsFile = null;
	/** number of errors so far when reading this file */
	int errorCount = 0;
	
	// info about current line
	String currentLine = null;
	String currentQueryId = null;
	double score = 0.0d;
	int docid = -1;
	String docno = null;
	int rank = 0;
	int lineNumber = 0;


	public TRECResultMatching(Index index)
	{ 
		super(index);
		openFile();
		loadLine();
	}

	/** open the results file. normally following by loadLine */
	protected void openFile()
	{
		try{
			resultsFile = Files.openFileReader(resultsFilename);
		} catch (IOException ioe) {
			logger.error(ioe);
		}
		errorCount = 0;
		logger.debug("TRECResultMatching opened "+ resultsFilename + " for matching results");
	}
	
	/** close the results file */
	protected void closeFile()
	{
		try{
			resultsFile.close();
		} catch (IOException ioe) {
			logger.error(ioe);
		}
	}

	/** read a line of the results file into memory */
	protected boolean loadLine()
	{
		//try to read a line from the result file
		try{
			currentLine = resultsFile.readLine();
		} catch (IOException ioe){ 
			logger.warn("Problem reading matching results file at line " + lineNumber, ioe);
			return false;
		}
		
		//EOF was reached. Assume end of results file, and reopen for future run
		if (currentLine == null) 
		{
			closeFile();
			openFile();
			loadLine();
			return false;
		}
		
		//parse the current line
		lineNumber++;
		currentLine = currentLine.trim();
		String parts[] = currentLine.split("\\s+");
		if (parts.length != 6)
		{
			logger.warn(lineNumber+": Invalid line format, not 6 parts : \""+currentLine+"\"");
			if (++errorCount < MAX_ERRORS)
				return loadLine();
			return false;
		}
		currentQueryId = parts[0];
		docno = parts[2];
		docid = docIndex.getDocumentId(docno);
		if (docid == -1)
		{
			logger.warn(lineNumber+": Docno not found: \""+docno+"\"");
			if (++errorCount < MAX_ERRORS)
				return loadLine();
			return false;
		}
		rank = Integer.parseInt(parts[3]);
		//parsing of scores is optional - in case of invalid scores
		if (parseScore)
			score = Double.parseDouble(parts[4]);
		return true;
	}
	
	/** Initialise for running a query. resultSet will be re-inited to full size */
	protected void initialise() {	
		resultSet = new CollectionResultSet(collectionStatistics.getNumberOfDocuments());
		resultSet.initialise();
	}

	/** Match document to query, for specified queryNumber */
	public void match(String queryNumber, MatchingQueryTerms queryTerms)
	{
		
		//the number of document score modifiers
		int numOfDocModifiers = documentModifiers.size();
		
		initialise();
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		int resultSet_rank = 0;

		// search for the current query in the result file
		while(! currentQueryId.equals(queryNumber) )
		{
			if (! loadLine())
				return;
		}
		
		// populate the resultset for the required query
		while(currentQueryId.equals(queryNumber))
		{
			docids[resultSet_rank] = docid;
			scores[resultSet_rank] = score;	
			if (! loadLine())
				break;
			resultSet_rank++;
		}
		
		//crop the result set to the correct size
		resultSet.setExactResultSize(resultSet_rank+1);
		resultSet.setResultSize(resultSet_rank+1);
		resultSet = resultSet.getResultSet(0, resultSet_rank+1);
		docids = resultSet.getDocids();
		scores = resultSet.getScores();
		
		/*application dependent modification of scores
		of documents for a query, based on a static set by the client code
		sorting the result set after applying each DSM*/
		for (int t = 0; t < numOfDocModifiers; t++) {
			if (documentModifiers.get(t).modifyScores(index, queryTerms, resultSet)) {
				HeapSort.descendingHeapSort(scores, docids, resultSet.getOccurrences(), resultSet.getResultSize());
			}				
		}
		
		logger.debug("TRECResultMatching ranked "+ (resultSet_rank+1) +" documents in response to query "+queryNumber);
	}
}


