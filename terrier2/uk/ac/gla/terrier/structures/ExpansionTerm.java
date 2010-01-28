package uk.ac.gla.terrier.structures;


/** 
 * This class implements a data structure 
 * for a term in the top-retrieved documents. 
 */
public class ExpansionTerm implements Comparable<ExpansionTerm> {
	
	
	/** The term string. */
	protected String token;
	/** The term ID. */
	protected int termID;
	/** The weight for query expansion. */
	protected double weightExpansion;
	/** The number of occurrences of the given term in the X top ranked documents. */
	protected double withinDocumentFrequency;
	
	/** The document frequency of the term in the X top ranked documents. */
	protected int documentFrequency;
	
	/** 
	 * The constructor of ExpansionTerm. Once the term is found in a top-
	 * retrieved documents, we create a record for this term.
	 * @param termID int the ID of the term
	 * @param withinDocumentFrequency double the frequency of the term in 
	 *		a top-retrieved document
	 */
	
	public ExpansionTerm(int termID) {
		this.termID = termID;
	}
	
	public ExpansionTerm(int termID, double withinDocumentFrequency) {
		this.termID = termID;
		this.withinDocumentFrequency = withinDocumentFrequency;
		this.documentFrequency = 1;
		this.weightExpansion = 0;
	}
		
	/**
	 * Returns the ID of the term. 
	 * @return int the term ID.
	 */
	public int getTermID(){
		return this.termID;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	/** 
	 * If the term is found in another top-retrieved document, we increase
	 * the frequency and the document frequency of the term.
	 * @param withinDocumentFrequency double the frequency of the term
	 *		in the corresponding top-retrieved document.
	 */
	public void insertRecord(double withinDocumentFrequency){
		this.withinDocumentFrequency += withinDocumentFrequency;
		this.documentFrequency++;
	}
	/** 
	 * Sets the expansion weight of the term.
	 * @param weightExpansion double the expansion weight of the term.
	 */
	public void setWeightExpansion(double weightExpansion){
		this.weightExpansion = weightExpansion;
	}
	
	/** 
	 * The method returns the document frequency of term in the top-retrieved
	 * documents.
	 * @return int The document frequency of term in the top-retrieved
	 *		 documents.
	 */
	public int getDocumentFrequency(){
		return this.documentFrequency;
	}
	/** 
	 * The method returns the expansion weight of the term.
	 * @return double The expansion weight of the term.
	 */
	public double getWeightExpansion(){
		return this.weightExpansion;
	}
	
	/** 
	 * The method returns the frequency of the term in the X top-retrieved
	 * documents.
	 * @return double The expansion weight of the term.
	 */
	public double getWithinDocumentFrequency(){
		return this.withinDocumentFrequency;
	}

	public int compareTo(ExpansionTerm o) {
//		System.out.println(o.getWeightExpansion() + " vs " + this.getWeightExpansion());
		if (o.getWeightExpansion() > this.getWeightExpansion()) {
			return 1;
		}
		else if (o.getWeightExpansion() < this.getWeightExpansion()) {
			return -1;
		}
		else {
			return 0;
		}
	}
}