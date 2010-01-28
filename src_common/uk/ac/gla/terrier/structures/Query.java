/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is Query.java code.
 *
 * The Initial Developer of the Original Code is the University of Glasgow.
 * Portions created by the Initial Developer are Copyright (C) 2004, 2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> (original author) 
 */
package uk.ac.gla.terrier.structures;

import java.io.Serializable;

import uk.ac.gla.terrier.structures.trees.*;

/**
 * This class implements a data structure representing a query.
 * In this class, a query is represented by a DocumentTree containing
 * the query terms and the associated query number.
 * @author Ben He
 * @version $Revision: 1.1 $
 * @deprecated 
 */
public class Query implements Serializable{

	/** The query terms. */
	protected TermTreeNode[] queryTerms;

	/** The query number. */
	protected String queryNo;
	
	/** Creates a new instance of Query */
	public Query(TermTreeNode[] query, String queryNo){
		this.queryTerms = query;
		this.queryNo = queryNo;
		TermTreeNode[] terms = this.getQueryTerms();

		double maxFrequency = 0;

		for (int i = 0; i < terms.length; i++) {
			if (maxFrequency <= terms[i].frequency)
				maxFrequency = (double) terms[i].frequency;

		}

		for (int i = 0; i < terms.length; i++) {
			terms[i].normalisedFrequency =
				(double) terms[i].frequency / maxFrequency;
		}

		this.queryTerms = terms;
	}
	

	/** Creates a new instance of Query */
	public Query(TermTree query, String queryNo) {
		this.queryTerms = query.toArray();
		this.queryNo = queryNo;
		TermTreeNode[] terms = this.getQueryTerms();

		double maxFrequency = 0;

		for (int i = 0; i < terms.length; i++) {
			if (maxFrequency <= terms[i].frequency)
				maxFrequency = (double) terms[i].frequency;

		}

		for (int i = 0; i < terms.length; i++) {
			terms[i].normalisedFrequency =
				(double) terms[i].frequency / maxFrequency;
		}

		this.queryTerms = terms;

	}
	
	/**
	 * Returns the query length, ie the number of non-stopwords in the query
	 * @return int the query length
	 */
	public int getQueryLength(){
		return this.queryTerms.length;
	}

	/**
	 * Returns the query number
	 * @return String the query number
	 */
	public String getQueryNumber() {
		return this.queryNo;
	}
	
	public String[] getQueryTermStrings(){
		String[] terms = new String[queryTerms.length];
		for (int i = 0; i < terms.length; i++)
			terms[i] = queryTerms[i].term;
		return terms;
	}
	
	/**
	 * Returns a representation of the query as a TermTreeNode array 
	 * @return TermTreeNode[] an array of TermTreeNode
	 */
	public TermTreeNode[] getQueryTerms() {
		return this.queryTerms;
	}
}
