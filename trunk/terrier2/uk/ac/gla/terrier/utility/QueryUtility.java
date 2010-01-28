package uk.ac.gla.terrier.utility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TObjectDoubleHashMap;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.TRECQuery;
/**
 * This class provides methods for query related utilities. 
 * @author ben
 *
 */
public class QueryUtility {
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
	
	public static void TRECQueryToOneLineQuery(String trecTopicFilename, String outputFilename){
		TRECQuery queries = new TRECQuery(trecTopicFilename);
		String[] qids = queries.getQueryids();
		Arrays.sort(qids);
		StringBuilder buf = new StringBuilder();
		for (int i=0; i<qids.length; i++){
			buf.append(qids[i]+" "+queries.getQuery(qids[i]));
			if (i!=qids.length-1)
				buf.append(ApplicationSetup.EOL);
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString()+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Get the query parser that is being used.
	 * @return The query parser that is being used.
	 */
	public static TRECQuery getQueryParser()
	{
		String TopicsParser = ApplicationSetup.getProperty("trec.topics.parser", "TRECQuery");	
		String topicsFile = null;
		TRECQuery rtr = null;
		try{
			Class queryingClass	= Class.forName(
				TopicsParser.indexOf('.') > 0 
				? TopicsParser
				: "uk.ac.gla.terrier.structures."+TopicsParser);
			if ((topicsFile = ApplicationSetup.getProperty("trec.topics",null)) != null)
			{
				Class[] types = {String.class};
				Object[] params = {topicsFile};
				rtr = (TRECQuery)
					queryingClass
					.getConstructor(types)
					.newInstance(params)
					;
			}
			else
			{
				rtr = (TRECQuery) queryingClass.newInstance();
			}
		//} catch (ClassNotFoundException cnfe) {

		} catch (Exception e) {
			System.err.println("Error instantiating topic file tokeniser called "+ TopicsParser);
			e.printStackTrace();
		}
		return rtr;
	}
	/**
	 * Decompose a one-line query string to terms and their weights.
	 * @param queryString
	 * @param map
	 */
	public static void decomposeOneLineQuery(String queryString, TObjectDoubleHashMap<String> map){
		String[] tokens = null;
		if (ApplicationSetup.getProperty("SingleLineTRECQuery.queryid.exists", "true").equals("true"))
			tokens = queryString.substring(queryString.indexOf(' ')+1, queryString.length()).replaceAll("\\^", " ").split(" ");
		else
			tokens = queryString.replaceAll("\\^", " ").split(" ");
		//System.out.println(queryString);
		//System.out.println(tokens.length);
		if (tokens.length % 2 == 0)
			for (int i=0; i<tokens.length; i+=2){
				double weight = Double.parseDouble(tokens[i+1]);
				map.adjustOrPutValue(tokens[i], weight, weight);
			}
		else
			for (int i=1; i<tokens.length; i+=2){
				double weight = Double.parseDouble(tokens[i+1]);
				map.adjustOrPutValue(tokens[i], weight, weight);
			}
	}
	/**
	 * Remove terms that appear in more than a given percentage of documents in the collection.
	 * @param termids
	 * @return
	 */
	public static int[] filterTerms(int[] termids, Lexicon lexicon, int numberOfDocuments, double ratio){
		int upperDF = (int)((double)numberOfDocuments*ratio);
		TIntHashSet termidSet = new TIntHashSet();
		for (int i=0; i<termids.length; i++){
			LexiconEntry entry = lexicon.getLexiconEntry(termids[i]); 
			if (entry.n_t<=upperDF)
				termidSet.add(termids[i]);
		}
		return termidSet.toArray();
	}
}
