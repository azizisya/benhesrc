package uk.ac.gla.terrier.querying.termselector;

import java.util.Arrays;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public abstract class TermSelector {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	protected TIntObjectHashMap<ExpansionTerm> termMap; // mapping from termid to expansion terms
	
	protected TIntHashSet originalQueryTermidSet;
	
	protected Index index;
	
	protected Lexicon lexicon;
	
	protected DirectIndex di;
	
	protected InvertedIndex ii;
	
	protected int feedbackSetLength;
	
	protected int feedbackSetSize;
	
	protected ResultSet resultSet;
	
	protected THashMap<String, String> metaMap = new THashMap<String, String>();
	
	public TermSelector(Index index) {
		this.setIndex(index);
	}
	
	public TermSelector(){
		
	}
	
	public void setIndex(Index index){
		this.index = index;
		lexicon = index.getLexicon();
		di = index.getDirectIndex();
		ii = index.getInvertedIndex();
	}
	
	public void setResultSet(ResultSet results){
		this.resultSet = results;
	}
	
	public ExpansionTerm[] getMostWeightedTerms(int numberOfExpandedTerms){
		if (termMap==null){
			ExpansionTerm[] expTerms = {};
			return expTerms;
		}
		int n = Math.min(numberOfExpandedTerms, termMap.size());
		boolean conservativeQE = (numberOfExpandedTerms==0 && this.originalQueryTermidSet != null);
		THashSet<ExpansionTerm> tSet = new THashSet<ExpansionTerm>();
		Object[] obj = termMap.getValues();
		int len = obj.length;
		ExpansionTerm[] terms = new ExpansionTerm[len];
		for (int i=0; i<len; i++)
			terms[i] = (ExpansionTerm)obj[i];
		Arrays.sort(terms);
		if (!conservativeQE){
			for (int i=0; i<n; i++)
				if (terms[i].getWeightExpansion()>0d)
					tSet.add(terms[i]);
		}else{
			for (int i=0; i<len; i++)
				if (this.originalQueryTermidSet.contains(terms[i].getTermID())){
					tSet.add(terms[i]);
					if (tSet.size() == this.originalQueryTermidSet.size())
						break;
				}
		}
		return (ExpansionTerm[])tSet.toArray(new ExpansionTerm[tSet.size()]);
	}
	
	public TIntObjectHashMap<ExpansionTerm> getMostWeightedTermsInHashMap(int numberOfExpandedTerms){
		TIntObjectHashMap<ExpansionTerm> tMap = new TIntObjectHashMap<ExpansionTerm>();
		if (termMap==null)
			return tMap;
		int n = Math.min(numberOfExpandedTerms, termMap.size());
		boolean conservativeQE = (numberOfExpandedTerms==0 && this.originalQueryTermidSet != null);
		Object[] obj = termMap.getValues();
		int len = obj.length;
		ExpansionTerm[] terms = new ExpansionTerm[obj.length];
		for (int i=0; i<len; i++)
			terms[i] = (ExpansionTerm)obj[i];
		Arrays.sort(terms);
		if (!conservativeQE){
			for (int i=0; i<n; i++)
				if (terms[i].getWeightExpansion()>0d)
					tMap.put(terms[i].getTermID(), terms[i]);
		}else{
			for (int i=0; i<len; i++)
				if (this.originalQueryTermidSet.contains(terms[i].getTermID())){
					tMap.put(terms[i].getTermID(), terms[i]);
					if (tMap.size() == this.originalQueryTermidSet.size())
						break;
				}
		}
		return tMap;
	}
	
	public int getNumberOfUniqueTerms(){
		int nTerms = 0;
		if (termMap!=null)
			nTerms = termMap.size();
		return nTerms;
	}
	
	public void setMetaInfo(String property, String value){
		metaMap.put(property, value);
	}
	
	protected void getTerms(int[] docids){
		this.feedbackSetLength = 0;
		this.feedbackSetSize = 0;
		termMap = new TIntObjectHashMap<ExpansionTerm>();
		for (int docid : docids) {
			int[][] terms = di.getTerms(docid);
			if (terms == null)
				logger.warn("document "+"("+docid+") not found");
			else{
				this.feedbackSetSize++;
				feedbackSetLength += terms[0].length;
				for (int j = 0; j < terms[0].length; j++)
					this.insertTerm(terms[0][j], (double)terms[1][j]);
			}
		}
	}
	
	protected TIntIntHashMap extractTerms(int docid){
		TIntIntHashMap termidFreqMap = new TIntIntHashMap();
		int[][] terms = di.getTerms(docid);
		if (terms == null)
			logger.warn("document "+"("+docid+") not found");
		else{
			for (int j = 0; j < terms[0].length; j++)
				termidFreqMap.put(terms[0][j], terms[1][j]);
		}
		return termidFreqMap;
	}
	
	protected void getTerms(TIntIntHashMap[] termidFreqMaps){
		// logger.debug("termidFreqMaps.length: "+termidFreqMaps.length);
		// logger.debug("termidFreqMaps[0].size(): "+termidFreqMaps[0].size());
		this.feedbackSetLength = 0;
		termMap = new TIntObjectHashMap<ExpansionTerm>();
		for (TIntIntHashMap map : termidFreqMaps){
			if (map.size()!=0)
				this.feedbackSetSize++;
			int[] termids = map.keys();
			for (int termid : termids){
				final int freq = map.get(termid);
				if (freq>0){
					feedbackSetLength += freq;
					this.insertTerm(termid, (double)freq);
				}
			}
		}
		// logger.debug("termMap.size(): "+termMap.size());
	}
	
	/**
 	* Add a term in the X top-retrieved documents as a candidate of the 
	* expanded terms.
 	* @param termID int the integer identifier of a term
 	* @param withinDocumentFrequency double the within document 
 	*		frequency of a term
 	*/
	protected void insertTerm(int termID, double withinDocumentFrequency) {
		final ExpansionTerm et = termMap.get(termID);
		if (et == null)
			termMap.put(termID, new ExpansionTerm(termID, withinDocumentFrequency));
		else
			et.insertRecord(withinDocumentFrequency);
	}
	
	public static TermSelector getDefaultTermSelector(Index index){
		String prefix = "uk.ac.gla.terrier.querying.termselector.";
		String name = ApplicationSetup.getProperty("term.selector.name", 
				"uk.ac.gla.terrier.querying.termselector.RocchioTermSelector");
		if (name.indexOf('.')<0)
			name = prefix.concat(name);
		TermSelector selector = null;
		try{
			selector = (TermSelector)Class.forName(name).newInstance();
		}catch(Exception e){
			logger.warn("Error while initializing TermSelector "+name);
			e.printStackTrace();
		}
		selector.setIndex(index);
		return selector;
	}
	
	public static TermSelector getTermSelector(String name, Index index){
		String prefix = "uk.ac.gla.terrier.querying.termselector.";
		if (name.indexOf('.')<0)
			name = prefix.concat(name);
		TermSelector selector = null;
		try{
			selector = (TermSelector)Class.forName(name).newInstance();
		}catch(Exception e){
			logger.warn("Error while initializing TermSelector "+name);
			e.printStackTrace();
		}
		selector.setIndex(index);
		return selector;
	}
	
	public abstract void assignTermWeights(ResultSet resultSet, int feedbackSetSize, WeightingModel QEModel, Lexicon bgLexicon);
	
	
	public abstract void assignTermWeights(int[] docids, WeightingModel QEModel, Lexicon bgLexicon);
	
	public abstract void assignTermWeights(TIntIntHashMap[] termidFreqMap, WeightingModel QEModel, 
		TIntIntHashMap bgTermidFreqMap, TIntIntHashMap bgTermidDocfreqMap);
	
	public void setOriginalQueryTermids(int[] termids){
		originalQueryTermidSet = new TIntHashSet();
		originalQueryTermidSet.addAll(termids);
	}
	
	public void setOriginalQueryTerms(String[] termStrings){
		this.originalQueryTermidSet = new TIntHashSet();
		for (String term : termStrings){
			LexiconEntry le = lexicon.getLexiconEntry(term);
			if (le!=null)
				originalQueryTermidSet.add(le.termId);
		}
	}
}
