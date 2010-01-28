package uk.ac.gla.terrier.querying;
/** @author Craig Macdonald, Vassilis Plachouras
  * @version $Revision: 1.1 $
  */
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.links.MetaIndex;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class Decorate implements PostProcess, PostFilter {
	/** Logging error messages */
	private static Logger logger = Logger.getRootLogger();
	
	/** 
	 * The cache used for the meta data. Implements a 
	 * Least-Recently-Used policy for retaining the most 
	 * recently accessed metadata. It is provided by the 
	 * manager.
	 */ 
	protected LRUMap metaCache = null;
	
	/** The meta index server. It is provided by the manager. */
	protected MetaIndex metaIndex = null;
	
	/** The document index used in the decoration process */
	protected DocumentIndex docIndex = null;

	//hand controlled, see properties
	protected boolean show_docid_early = Boolean.parseBoolean(
		ApplicationSetup.getProperty("postproccess.decorate.show_docid_early", "false"));
	/* set postproccess.decorate.show_docid_early if scope filter is enabled */
	
	protected boolean show_url_early = Boolean.parseBoolean(
		ApplicationSetup.getProperty("postproccess.decorate.show_url_early", "true"));
	/* set postproccess.decorate.show_docid_early if scope filter is enabled */

	//init at post process, postfilter stage
	protected boolean show_snippet = false;
	protected boolean show_title = false;
	protected boolean show_docid = false;
	protected boolean show_url = false;
	
	protected static boolean enableQueryTermsHighlight = Boolean.parseBoolean(ApplicationSetup.getProperty("enable.queryterms.highlight","true"));
	
	public Decorate() {	}
	
	public void new_query(Manager m, SearchRequest q, ResultSet rs)
	{
		show_snippet = checkControl("show_snippet", q);
		show_title = checkControl("show_title", q);
		show_docid = checkControl("show_docid", q);
		show_url = checkControl("show_url", q);
		if (show_docid)
			docIndex = m.getIndex().getDocumentIndex();
		metaIndex = (MetaIndex)m.getIndex().getIndexStructure("meta");
		metaCache = (LRUMap) m.getIndex().getIndexStructure("metacache");

		//preparing the query terms for highlighting
		String original_q = q.getOriginalQuery();
		if (original_q == null)
		{
			return;
		}
		String[] qTerms;
		qTerms = original_q.trim().toLowerCase().split("\\s+");
	
		boolean atLeastOneTermToHighlight = false;
				String pattern = "";
		if (qTerms.length>0 ) {
				pattern = "(\\b)(";
				if (!qTerms[0].contains(":")) {
						String qTerm = qTerms[0].replaceAll("\\W+", "");
						pattern += qTerm;
						atLeastOneTermToHighlight = true;
				} else if (!(qTerms[0].startsWith("group:") || qTerms[0].startsWith("related:"))) {
						String qTerm = qTerms[0].substring(qTerms[0].indexOf(':')+1).replaceAll("\\W+","");
						pattern += qTerm;
						atLeastOneTermToHighlight = true;
				}
		}

		for (int i=1; i<qTerms.length; i++) {
				if (!qTerms[i].contains(":")) {
						String qTerm = qTerms[i].replaceAll("\\W+","");
						if (atLeastOneTermToHighlight) {
								pattern += "|" + qTerm;
						} else {
								pattern += qTerm;
						}
						atLeastOneTermToHighlight = true;
				} else if (!(qTerms[i].startsWith("group:") || qTerms[0].startsWith("related:"))) {
						String qTerm = qTerms[i].substring(qTerms[i].indexOf(':')+1).replaceAll("\\W+","");
						if (atLeastOneTermToHighlight) {
								pattern += "|" + qTerm;
						} else {
								pattern += qTerm;
						}
						atLeastOneTermToHighlight = true;
				}
		}

		if (qTerms.length>0)
				pattern += ")(\\b)";

		if (!atLeastOneTermToHighlight) {
				pattern = ("(\\b)()(\\b)");
		}
		highlight = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

	}

	//a regular expression that detects the existence of a 
	//control character or a non-visible character in a string
	protected Pattern controlNonVisibleCharacters = Pattern.compile("[\\p{Cntrl}\uFFFD]|[^\\p{Graph}\\p{Space}]");
	
	//the matcher that corresponds to the above regular expression, initialised
	//for an empty string. This variable is defined in order to avoid creating
	//a new object every time it is required to check for and remove control characters, or non-visible characters.
	protected Matcher controlNonVisibleCharactersMatcher = controlNonVisibleCharacters.matcher("");
	
	protected Pattern highlight;
	
	protected String[] qTerms;
	
	//the metadata keys
	private static final String[] keys = new String[] {"url", "abstract", "title"};
	
	//the regular expression for splitting the text into sentences
	private static Pattern sentencePattern = Pattern.compile("\\.\\s+|!\\s+|\\|\\s+|\\?\\s+");
	
	//the regular expression for removing common endings from words - similar to very light stemming
	private static Pattern removeEndings = Pattern.compile("ing$|es$|s$");
	
	//decoration at the postfilter stage
	public byte filter(Manager m, SearchRequest q, ResultSet rs, int DocAtNumber, int DocNo)
	{		
		String originalQuery = q.getOriginalQuery();
		//highlight query terms only for the queries, 
		//which are not about finding related documents
		//this check may be removed, because the preparation of the 
		//query terms for highlighting takes into account
		//the related: operator
		boolean highlightQueryTerms = true;
		if (originalQuery.matches("\\brelated:\\d+\\b") || //check if the query uses the related: operator 
			!enableQueryTermsHighlight || //check if the highlighting of terms is enabled
			highlight.pattern().equals("(\\b)()(\\b)")) //check if there are no query terms to highlight
			highlightQueryTerms = false;
		
		String[] qTerms = q.getOriginalQuery().replaceAll(" \\w+\\p{Punct}\\w+ "," ").toLowerCase().split(" ");
		
		String[] metadata = null;
		int tmpSentence;
		double tmpScore;
		synchronized(metaCache) {
			try {
				Integer DocNoObject = new Integer(DocNo);
				if (metaCache.containsKey(DocNoObject))
						metadata = (String[]) metaCache.get(DocNoObject);
				else {
					metadata = metaIndex.getItems(keys, DocNo);
					metaCache.put(DocNoObject,metadata);
				}
			} catch(IOException ioe) {} 
		}
	
		if (show_snippet)
		{
			String extract = metadata[1];
			String[] sentences = sentencePattern.split(extract, 50); //use 50 sentences at most
			double[] sentenceScores = new double[sentences.length]; 
			int frsSentence = -1;
			int sndSentence = -1;
			int top1Sentence = 0;
			int top2Sentence = 0;
			double max1Score = -1;
			double max2Score = 0;
			final int qTermsLength = qTerms.length;
			for (int i=0; i<qTermsLength; i++) {
				qTerms[i] = removeEndings.matcher(qTerms[i]).replaceAll("");
			}
			String lowerCaseSentence;
			int sentenceLength;
			final int sentencesLength = sentences.length;

			for (int i=0; i<sentencesLength; i++) {
				
				lowerCaseSentence = sentences[i].toLowerCase();
				sentenceLength=sentences[i].length();
				if (sentenceLength < 20 || sentenceLength > 250) {
					for (int j=0; j<qTermsLength; j++) {
						if (lowerCaseSentence.indexOf(qTerms[j])>=0) {
							sentenceScores[i]+=1.0d + sentenceLength / (20.0d + sentenceLength);
						}
					}

					
				} else {
					for (int j=0; j<qTermsLength; j++) {
						if (lowerCaseSentence.indexOf(qTerms[j])>=0) {
							sentenceScores[i]+=qTerms[j].length() + sentenceLength / (1.0d + sentenceLength);
						}
					}					
				}
								
				//do your best to get at least a second sentence for the snippet, 
				//after having found the first one
				if (frsSentence > -1 && sndSentence == -1 && sentenceLength > 5) {
					sndSentence = i;
				}

				//do your best to get at least one sentence for the snippet
				if (frsSentence == -1 && sentenceLength > 5) { 
					frsSentence = i;
				}

				if (max2Score < sentenceScores[i]) {
					max2Score = sentenceScores[i];
					top2Sentence = i;
					//logger.debug("top 2 sentence is " + i);
					if (max2Score > max1Score) {
						tmpScore = max1Score; max1Score = max2Score; max2Score = tmpScore;
						tmpSentence = top1Sentence; top1Sentence = top2Sentence; top2Sentence = tmpSentence;
					}
				}

			}
			int lastIndexOfSpace = -1;
			String sentence="";
			String secondSentence="";
			String snippet = "";
			if (max1Score == -1) {
				if (frsSentence>=0) {
					sentence = sentences[frsSentence];
					if (sentence.length() > 100) {
						lastIndexOfSpace = sentence.substring(0, 100).lastIndexOf(" ");
						sentence = sentence.substring(0, lastIndexOfSpace > 0 ? lastIndexOfSpace : 100);
					}
				}
				
				if (sndSentence>=0) {
					secondSentence = sentences[sndSentence];
					if (secondSentence.length() > 100) {
						lastIndexOfSpace = secondSentence.substring(0, 100).lastIndexOf(" ");
						secondSentence = secondSentence.substring(0, lastIndexOfSpace>0 ? lastIndexOfSpace : 100);
					}					
				}
				
				if (frsSentence >=0 && sndSentence >= 0) 
					snippet = sentence.trim() + "..." + secondSentence.trim();
				else if (frsSentence >= 0 && sndSentence<0) 
					snippet = sentence.trim();
				
			} else if (sentences[top1Sentence].length()<100 && top1Sentence!=top2Sentence) {
				sentence = sentences[top1Sentence];
				if (sentence.length() > 100) {
					lastIndexOfSpace = sentence.substring(0, 100).lastIndexOf(" ");
					sentence = sentence.substring(0, lastIndexOfSpace > 0 ? lastIndexOfSpace : 100);
				}
								
				secondSentence = sentences[top2Sentence];
				if (secondSentence.length() > 100) {
					lastIndexOfSpace = secondSentence.substring(0, 100).lastIndexOf(" ");
					secondSentence = secondSentence.substring(0, lastIndexOfSpace>0 ? lastIndexOfSpace : 100);
				}
				snippet = sentence.trim() + "..." + secondSentence.trim();
			} else {
				sentence = sentences[top1Sentence];
				if (sentence.length()>200) {
					lastIndexOfSpace = sentence.substring(0, 200).lastIndexOf(" ");
					sentence = sentence.substring(0, lastIndexOfSpace > 0 ? lastIndexOfSpace : 200);
				}
				snippet = sentence.trim();
			}
			
			//checking and removing any control characters
			controlNonVisibleCharactersMatcher.reset(snippet);
			snippet = controlNonVisibleCharactersMatcher.replaceAll("");
			
			//String escapedSnippet = StringEscapeUtils.escapeHtml(snippet);
			//if (highlightQueryTerms)
			//	escapedSnippet = highlight.matcher(escapedSnippet).replaceAll("$1<b>$2</b>$3");
			if (highlightQueryTerms)
				snippet = highlight.matcher(snippet).replaceAll("$1<b>$2</b>$3");
			//else 
			//	snippet = StringEscapeUtils.escapeHtml(snippet);
			
			//change by Vassilis, 20/09/2006
			//disabling the xml escaping in order to move the data into ![CDATA[ ]]
			//the code was:
			//rs.addMetaItem("snippet", DocAtNumber, StringEscapeUtils.escapeXml(snippet));
			//and now it is:
			//snippet.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]","");
			rs.addMetaItem("snippet", DocAtNumber, snippet);
			
		}
		if (show_title)
		{
			String title = metadata[2];
			
			//checking and removing any control characters
			controlNonVisibleCharactersMatcher.reset(title);
			title = controlNonVisibleCharactersMatcher.replaceAll("");
			title = (highlightQueryTerms)
				  ? highlight.matcher(metadata[2]).replaceAll("$1<b>$2</b>$3") 
				  : metadata[2];
			
			
			//String escapedTitle = StringEscapeUtils.escapeHtml(metadata[2]);
			//String title = (highlightQueryTerms)
			//			 ? highlight.matcher(escapedTitle).replaceAll("$1<b>$2</b>$3") 
			//			 : escapedTitle;
			//String title = (highlightQueryTerms) 
			//		? highlight.matcher(metadata[2]).replaceAll("$1<b>$2</b>$3") 
			//		: metadata[2];

			//change by Vassilis, 20/09/2006
			//disabling the xml escaping in order to move the data into ![CDATA[ ]]
			//the code was:
			//rs.addMetaItem("title", DocAtNumber, StringEscapeUtils.escapeXml(title));
			//and now is:
			//title.replaceAll("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFFF]","");
			rs.addMetaItem("title", DocAtNumber, title);
			
		}
		if (!show_docid_early && show_docid)
		{
			rs.addMetaItem("docid", DocAtNumber, docIndex.getDocumentNumber(DocNo));
		}
		if (!show_url_early && show_url)
		{

			rs.addMetaItem("url", DocAtNumber, StringEscapeUtils.escapeXml(metadata[0]));
		}
		
		return FILTER_OK;
	}

	/** decoration at the postprocess stage. only decorate if required for future postfilter or postprocesses.
	  * @param manager The manager instance handling this search session.
	  * @param q the current query being processed
	  */
	public void process(Manager manager, SearchRequest q)
	{
		ResultSet rs = q.getResultSet();
		new_query(manager, q, rs);
		int docids[] = rs.getDocids();
		int resultsetsize = docids.length;
		if (show_url_early && show_url)
		{
			String[] urls = new String[resultsetsize];
			String[] metadata = null;
			logger.debug("early url decoration");
			synchronized(metaCache) {
				try {
					for(int i=0;i<resultsetsize;i++) {
						Integer DocNoObject = new Integer(docids[i]);
						if (metaCache.containsKey(DocNoObject))
								metadata = (String[]) metaCache.get(DocNoObject);
						else {
							metadata = metaIndex.getItems(keys, docids[i]);
							metaCache.put(DocNoObject,metadata);
						}
						urls[i] = metadata[0];
					}
					rs.addMetaItems("url", urls);
				
				}catch(IOException ioe) {}
			}  
			

		}

		if (show_docid_early && show_docid)
		{
			String[] documentids = new String[resultsetsize];
			for(int i=0;i<resultsetsize;i++)
			{
				documentids[i] = docIndex.getDocumentNumber(docids[i]);
			}

			System.err.println("Decorating with docnos for "+documentids.length + "result");
			if (documentids.length > 0)
				System.err.println("\tFirst docno is "+documentids[0]);
			rs.addMetaItems("docid", documentids);
		}
	}

	protected boolean checkControl(String control_name, SearchRequest srq)
	{
		String controlValue = srq.getControl(control_name);
		if (controlValue.length() == 0)
			return false;
		if (controlValue.equals("0") || controlValue.toLowerCase().equals("off")
			|| controlValue.toLowerCase().equals("false"))
			return false;
		return true;
	}
	
	/**
	 * Returns the name of the post processor.
	 * @return String the name of the post processor.
	 */
	public String getInfo()
	{
		return "Decorate";
	}
}
