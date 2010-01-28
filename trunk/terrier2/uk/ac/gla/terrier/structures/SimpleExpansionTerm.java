package uk.ac.gla.terrier.structures;


/** 
 * This class implements a data structure 
 * for a term in the top-retrieved documents. 
 */
public class SimpleExpansionTerm implements Comparable<SimpleExpansionTerm> {
	
	/** The term ID. */
	protected int termid;
	/** The weight for query expansion. */
	protected double weight;
	/** The number of occurrences of the given term in the X top ranked documents. */
	
	/** 
	 * The constructor of ExpansionTerm. Once the term is found in a top-
	 * retrieved documents, we create a record for this term.
	 * @param termID int the ID of the term
	 * @param withinDocumentFrequency double the frequency of the term in 
	 *		a top-retrieved document
	 */
	
	public SimpleExpansionTerm(int termid, double weight) {
		this.termid = termid;
		this.weight = weight;
	}
		
	/**
	 * Returns the ID of the term. 
	 * @return int the term ID.
	 */
	public int getTermid(){
		return this.termid;
	}
	
	public double getWeight(){
		return this.getWeight();
	}
	
	public void setTermid(int id){
		this.termid = id;
	}
	
	public void setWeight(double value){
		this.weight = value;
	}

	public int compareTo(SimpleExpansionTerm o) {
		if (o.getWeight() > this.getWeight()) {
			return 1;
		}
		else if (o.getWeight() < this.getWeight()) {
			return -1;
		}
		else {
			return 0;
		}
	}
}