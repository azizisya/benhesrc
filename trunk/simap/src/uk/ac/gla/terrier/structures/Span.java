/*
 * Created on 25 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.structures;

import java.util.Arrays;

import gnu.trove.TIntIntHashMap;
import uk.ac.gla.terrier.querying.Manager;

public class Span implements Comparable<Span>{
	protected String docno;
	
	protected String rankby = "score";
	
	public String getRankby() {
		return rankby;
	}

	public void setRankby(String rankby) {
		this.rankby = rankby;
	}

	protected int offset = 0;
	
	protected int byteLength = -1;
	
	protected String spanString;
	
	protected double spanScore = 0;
	/**
	 * TXT: text
	 * REF: reference
	 * NOI: noise
	 */
	protected String type = "TXT";
	
	protected String section = "TXT";
	
	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		if (section.toLowerCase().equals("materials") || section.toLowerCase().equals("methods"))
			this.section = "MATERIALS AND METHODS";
		else if (section.toLowerCase().equals("433ults"))
			this.section = "RESULTS";
		else if (section.toLowerCase().equals("abstract"))
			this.section = "ABSTRACT/INTRODUCTION";
		else if (section.toLowerCase().equals("experimental"))
			this.section = "EXPERIMENTAL PROCEDURES";
		else
			this.section = section;
	}

	protected int spanLength = -1;
	
	protected TIntIntHashMap termFreqMap = null;

	public Span(String docno, String spanString) {
		super();
		this.docno = docno;
		this.spanString = spanString;
	}

	public Span(String docno, int offset, int byteLength, String spanString) {
		super();
		this.docno = docno;
		this.offset = offset;
		this.byteLength = byteLength;
		this.spanString = spanString;
	}
	
	public int getFrequency(int termid){
		return termFreqMap.get(termid);
	}
	
	public boolean isInformative(){
		// A span is non-informative when
		boolean informative = true;
		// it has less than 50 chars
		if (spanString.length()<50){
			return false;
		}
		// more than 30% percent of chars are non-letter/digit
		int counter = 0;
		for (int i=0; i<spanString.length(); i++){
			if (Character.isLetterOrDigit(spanString.charAt(i)))
				counter++;
		}
		if ((double)counter/spanString.length()<0.70)
			return false;
		return informative;
	}
	
	public int compareTo(Span o) {
		if (rankby.equalsIgnoreCase("score")){
			if (o.getSpanScore() > this.getSpanScore()) {
				return 1;
			}
			else if (o.getSpanScore() < this.getSpanScore()) {
				return -1;
			}
			else {
				return 0;
			}
		}else if (rankby.equalsIgnoreCase("offset")){
			if (o.getOffset() > this.getOffset()) {
				return -1;
			}
			else if (o.getOffset() < this.getOffset()) {
				return 1;
			}
			else {
				return 0;
			}
		}
		return 0;
	}
	
	public void tokenise(Manager manager){
		spanLength = 0;
		termFreqMap = new TIntIntHashMap();
		String spanString = getSpanString();
		StringBuilder sb = new StringBuilder();
		Lexicon lexicon = manager.getIndex().getLexicon();
		for (int j=0; j<spanString.length(); j++){
			char ch = spanString.charAt(j);
			if (Character.isLetterOrDigit(ch)){
				sb.append(ch);
			}else if (j>0){
				String term = sb.toString().toLowerCase();
				sb = new StringBuilder();
				term = manager.pipelineTerm(term);
				if (term!=null){
					//System.out.println(term);
					term = term.trim();
					if (term.length()==0){
						continue;
					}
					LexiconEntry lexEntry = null;
					if ((lexEntry=lexicon.getLexiconEntry(term))==null){
						continue;
					}
					else{
						termFreqMap.adjustOrPutValue(lexEntry.termId, 1, 1);
						spanLength++;
					}
				}
			}
		}
	}
	
	public void dumpTermFreqMap(Lexicon lexicon){
		int[] termids = termFreqMap.keys();
		Arrays.sort(termids);
		System.out.println(">>>Dumping span: "+getSpanString());
		System.out.println("Document number: "+docno+", type: "+type+", length: "+spanLength);
		for (int i=0; i<termids.length; i++){
			LexiconEntry lexEntry = lexicon.getLexiconEntry(termids[i]);
			System.out.println("term "+(i+1)+": "+lexEntry.term+", "+termFreqMap.get(termids[i]));
		}
	}
	
	public void autoSetType(){
		if (!isInformative())
			this.setType("NOI");
	}

	/**
	 * @return the docno
	 */
	public String getDocno() {
		return docno;
	}

	/**
	 * @param pmid the pmid to set
	 */
	public void setDocno(String docno) {
		this.docno = docno;
	}

	/**
	 * @return the offset
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @param offset the offset to set
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * @return the length
	 */
	public int getByteLength() {
		return byteLength;
	}

	/**
	 * @param length the length to set
	 */
	public void setByteLength(int length) {
		this.byteLength = length;
	}

	/**
	 * @return the spanString
	 */
	public String getSpanString() {
		return spanString;
	}

	/**
	 * @param spanString the spanString to set
	 */
	public void setSpanString(String spanString) {
		this.spanString = spanString;
	}

	/**
	 * @return the spanScore
	 */
	public double getSpanScore() {
		return spanScore;
	}

	/**
	 * @param spanScore the spanScore to set
	 */
	public void setSpanScore(double spanScore) {
		this.spanScore = spanScore;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the termFreqMap
	 */
	public TIntIntHashMap getTermFreqMap() {
		return termFreqMap;
	}

	/**
	 * @return the spanLength
	 */
	public int getSpanLength() {
		return spanLength;
	}
}
