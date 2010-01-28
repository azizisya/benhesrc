package uk.ac.gla.terrier.structures;

import uk.ac.gla.terrier.utility.Rounding;

/**
 * This class encapsulates a weighted tuple of expansion terms.
 * 
 * @author rodrygo
 */
public class Tuple implements Comparable<Tuple> {
	
	private ExpansionTerm[] terms;
	private double weight;
	
	public Tuple(ExpansionTerm[] terms) {
		this.terms = terms;
	}

	public ExpansionTerm[] getTerms() {
		return terms;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;			
	}

	public int compareTo(Tuple o) {
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
	
	public String toString() {
		StringBuilder output = new StringBuilder();
		
		output.append("(");			
		for (int i = 0; i < terms.length; i++) {
			output.append(terms[i].getToken());
			if (i < terms.length - 1) {
				output.append(" ");
			}
		}
		output.append(")");
		
		if (weight != 1) {
			output.append("^" + Rounding.round(weight, 4));
		}

		return output.toString();
	}
	
}