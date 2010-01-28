/*
 * Created on 7 Aug 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.structures;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.utility.SentenceParser;

public class GenoArticle {
	protected char[] docString;
	protected Span[] spans;
	protected String docno = null;
	protected int docLength;
	protected TIntIntHashMap termidFreqMap;
	
	protected static final String[] refIdentifiers = {"references", "acknowledgments", 
			"received for publication", "literature cited"};
	
	/**
	 * @return the docString
	 */
	public char[] getDocString() {
		return docString;
	}

	/**
	 * @param docString the docString to set
	 */
	public void setDocString(char[] docString) {
		this.docString = docString;
	}

	/**
	 * @return the docno
	 */
	public String getDocno() {
		return docno;
	}

	/**
	 * @param docno the docno to set
	 */
	public void setDocno(String docno) {
		this.docno = docno;
	}

	/**
	 * @return the spans
	 */
	public Span[] getSpans() {
		return spans;
	}

	/**
	 * @return the docLength
	 */
	public int getDocLength() {
		return docLength;
	}

	public GenoArticle(char[] chars, Manager manager, String docno){
		this.docString = chars;
		this.docno = docno;
		// split into spans
		spans = GenoArticle.getSentencesFromText(docString, docno);
		// set span types
		for (int i=0; i<spans.length; i++)
			spans[i].autoSetType();
		// identify references
		GenoArticle.lableReferences(spans);
		// tokenise spans
		docLength = 0;
		for (int i=0; i<spans.length; i++){
			spans[i].tokenise(manager);
			docLength += spans[i].getSpanLength();
		}
		this.termidFreqMap = new TIntIntHashMap();
		GenoArticle.mergeTermFreqMap(spans, termidFreqMap);
	}
	
	public static void mergeTermFreqMap(Span[] spans, TIntIntHashMap map){
		for (int i=0; i<spans.length; i++){
			TIntIntHashMap spanMap = spans[i].getTermFreqMap();
			int[] termids = spanMap.keys();
			for (int j=0; j<termids.length; j++){
				int tf = spanMap.get(termids[j]);
				map.adjustOrPutValue(termids[j], tf, tf);
			}
		}
	}
	
	public TIntIntHashMap getTxtSpanStats(){
		ArrayList<Span> spanList = new ArrayList<Span>();
		for (int i=0; i<spans.length; i++)
			if(spans[i].type.equals("TXT"))
				spanList.add(spans[i]);
		Span[] txtSpans = spanList.toArray(new Span[spanList.size()]);
		TIntIntHashMap map = new TIntIntHashMap();
		mergeTermFreqMap(txtSpans, map);
		return map;
	}
	
	public void mergeInto(TIntIntHashMap map){
		for (int k : termidFreqMap.keys()){
			int tf = termidFreqMap.get(k);
			map.adjustOrPutValue(k, tf, tf);
		}
	}
	
	public int[][] getTerms(){
		int[][] terms = new int[2][termidFreqMap.size()];
		terms[0] = termidFreqMap.keys();
		terms[1] = termidFreqMap.getValues();
		return terms;
	}
	
	public static void lableReferences(Span[] spans){
		TIntHashSet refSpanSet = new TIntHashSet();
		GenoArticle.findReferencePositions(spans, refSpanSet);
		int[] refSpans = refSpanSet.toArray();
		if (refSpans!=null && refSpans.length>0){
			Arrays.sort(refSpans);
			int lastRefSpan = refSpans[refSpans.length-1];
			for (int i=0; i<spans.length; i++){
				spans[i].autoSetType();
				if ((refSpanSet.contains(i)||i>lastRefSpan)&&lastRefSpan > (int)(0.75d*spans.length)){
					spans[i].setType("REF");
				}
			}
		}
	}
	
	public void dumpSpans(Lexicon lexicon){
		for (int i=0; i<spans.length; i++)
			spans[i].dumpTermFreqMap(lexicon);
	}
	
	protected static int[] findReferencePositions(Span[] spans){
		TIntHashSet posSet = new TIntHashSet();
		for (int i=0; i<spans.length; i++){
			int idx = -1;
			String spanString = spans[i].getSpanString().toLowerCase();
			if (spanString.length()==0)
				continue;
			for (int j=0; j<refIdentifiers.length; j++){
				idx = spanString.indexOf(refIdentifiers[j]);
				if (idx >=0 )
					break;
			} 
			if (idx>=0)
				posSet.add(i);
		}
		int[] pos = posSet.toArray(); 
		Arrays.sort(pos);
		return pos;
	}
	
	protected static void findReferencePositions(Span[] spans, TIntHashSet refSpanSet){
		for (int i=0; i<spans.length; i++){
			int idx = -1;
			String spanString = spans[i].getSpanString().toLowerCase();
			if (spanString.length()==0)
				continue;
			for (int j=0; j<refIdentifiers.length; j++){
				idx = spanString.indexOf(refIdentifiers[j]);
				if (idx >=0 )
					break;
			} 
			if (idx>=0)
				refSpanSet.add(i);
		}
	}
	
	public static Span[] getSentencesFromText(char[] text, String docno){
		/**
		String[] sentences = SentenceParser.splitIntoSentences(text);
		if (sentences.length == 0)
			return null;
		Span[] spans = new Span[sentences.length];		
		for (int i=0; i<spans.length; i++)
			spans[i] = new Span(docno, sentences[i]);
		return spans;
		*/
		Span[] spans = SentenceParser.splitIntoSpans(text, docno);
		return spans;
	}

	/**
	 * @return the termidFreqMap
	 */
	public TIntIntHashMap getTermidFreqMap() {
		return termidFreqMap;
	}
}
