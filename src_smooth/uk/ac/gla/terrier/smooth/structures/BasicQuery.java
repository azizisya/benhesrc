/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
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
 * The Original Code is BasicQuery.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.structures;

import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.smooth.structures.trees.*;

/**
 * This class implements a data structure representing a queryTerms.
 * In this class, a queryTerms is represented by a DocumentTree containing
 * the queryTerms terms and the associated queryTerms number.
 * @author Ben He
 * @version $Revision: 1.2 $
 */
public class BasicQuery {

	/** The queryTerms terms. */
	protected TermTreeNode[] queryTerms;
	/** A Vector of query terms. */
	protected Vector vecQueryTerms;
	/** A string of query terms that are separated by spaces. */
	protected String queryTermString;

	/** The queryTerms number. */
	protected String queryid;
	/** a HashSet containing query terms. */
	protected HashSet termHashSet;
	
	/**
	 * Create an instance of BasicQuery for the given query terms and
	 * the query id.
	 * @param queryTerms The query terms.
	 * @param queryid The id of the query.
	 */
	public BasicQuery(TermTreeNode[] queryTerms, String queryid){
		this.queryTerms = queryTerms;
		this.queryid = queryid;
		this.initialise();
	}
	/**
	 * Create an instance of BasicQuery for the given query terms, query
	 * id, and the term pipeline. The query terms are processed through
	 * the term pipeline then.
	 * @param queryTerms A string of query terms that are separated by
	 * spaces.
	 * @param queryid The id of the query.
	 * @param pipe The term pipeline.
	 */
	public BasicQuery(String queryTerms, String queryid, Manager pipe){
		StringTokenizer stk = new StringTokenizer(queryTerms, "\" ");
		TermTree tree = new TermTree();
		while (stk.hasMoreTokens()){
			String term = stk.nextToken();
			term = pipe.pipelineTerm(term);
			if (term == null)
				continue;
			tree.insert(term);
		}
		this.queryTerms = tree.toArray();
		this.queryid = queryid;
		this.initialise();		
	}
	/**
	 * Create an instance of BasicQuery for a given string of query
	 * terms and query id.
	 * @param queryTerms A string of query terms that are separated
	 * by spaces.
	 * @param queryid The id of the query.
	 */
	public BasicQuery(String queryTerms, String queryid){
		StringTokenizer stk = new StringTokenizer(queryTerms, "\" ");
		TermTree tree = new TermTree();
		while (stk.hasMoreTokens()){
			String term = stk.nextToken();
			tree.insert(term);
		}
		this.queryTerms = tree.toArray();
		this.queryid = queryid;
		this.initialise();		
	}	
	/**
	 * Create an instance of BasicQuery for the given query terms in
	 * a TermTree and the query id.
	 * @param queryTerms A TermTree containing the query terms.
	 * @param queryid The query id.
	 */
	public BasicQuery(TermTree queryTerms, String queryid) {
		this.queryTerms = queryTerms.toArray();
		this.queryid = queryid;
		this.initialise();
	}
	/**
	 * It initialises the query term weight and other class members.
	 */
	public void initialise(){
		TermTreeNode[] terms = this.getQueryTerms();
		double maxFrequency = 0;
		this.queryTermString = "";

		for (int i = 0; i < terms.length; i++) {
			if (maxFrequency <= terms[i].frequency)
				maxFrequency = (double) terms[i].frequency;
		}
		this.termHashSet = new HashSet();
		this.vecQueryTerms = new Vector();
		for (int i = 0; i < terms.length; i++) {
			terms[i].normalisedFrequency =
				(double) terms[i].frequency / maxFrequency;
			this.termHashSet.add(terms[i].term);
			this.vecQueryTerms.addElement(terms[i]);
			this.queryTermString += (terms[i].term + " ");
		}
		this.queryTermString = this.queryTermString.trim();
		this.queryTerms = terms;
	}
	/**
	 * Get a string containing the query terms that are separated by
	 * spaces.
	 * @return A string containing the query terms that are separated by
	 * spaces.
	 */
	public String getQueryTermString(){
		return this.queryTermString;
	}
	/**
	 * Set the query terms.
	 * @param _queryTerms The query terms.
	 */
	public void setQueryTerms(TermTreeNode[] _queryTerms){
		this.queryTerms = _queryTerms;
		this.termHashSet = new HashSet();
		this.vecQueryTerms = new Vector();
		for (int i = 0; i < queryTerms.length; i++) {
			this.termHashSet.add(queryTerms[i].term);
			this.vecQueryTerms.addElement(queryTerms[i]);
		}
	}
	
	/**
	 * Returns the query length, i.e. the number of unique terms in the query.
	 * @return int the query length.
	 */
	public int getQueryLength(){
		return this.queryTerms.length;
	}

	/**
	 * Returns the queryTerms number
	 * @return String the queryTerms number
	 */
	public String getQueryNumber() {
		return this.queryid;
	}
	/**
	 * Get the query terms in an string array.
	 * @return The query terms.
	 */
	public String[] getQueryTermStrings(){
		String[] terms = new String[queryTerms.length];
		for (int i = 0; i < terms.length; i++)
			terms[i] = queryTerms[i].term;
		return terms;
	}
	/**
	 * Get the terms with the highest idf (lowest document frequency).
	 * @param topX The number of terms to be returned.
	 * @param lexicon The lexicon that is used to obtain the document frequency
	 * of each query term.
	 * @return The query terms with the highest idf.
	 */
	public TermTreeNode[] termsWithHighestIdf(int topX, Lexicon lexicon){
		if (topX >= queryTerms.length)
			return (TermTreeNode[])queryTerms.clone();
		TermTreeNode[] returnedTerms = new TermTreeNode[topX];
		TermTreeNode[] clone = (TermTreeNode[])queryTerms.clone();
		int[] df = new int[queryTerms.length];
		int[] indice = new int[queryTerms.length];
		for (int i = 0; i < indice.length; i++){
			indice[i] = i;
			lexicon.findTerm(queryTerms[i].term);
			df[i] = lexicon.getNt();
		}
		int counter = 0;
		for (int i = 0; i < indice.length-1; i++){
			int top = i;
			for (int j = i+1; j < indice.length; j++){
				if (df[indice[top]] > df[indice[j]])
					top = j;
			}
			returnedTerms[counter++] = clone[indice[top]];
			// swap
			int temp = indice[i];
			indice[i] = indice[top];
			indice[top] = temp;
			
			if (counter == topX)
				break;
		}
		return returnedTerms;
	}
	/**
	 * Get the query terms with the lowest frequency in the collection.
	 * @param topX The number of terms to be returned.
	 * @return The query terms with the lowest collection frequency.
	 */
	public TermTreeNode[] termsWithLowestFrequency(int topX){
		if (topX >= queryTerms.length)
			return (TermTreeNode[])queryTerms.clone();
		TermTreeNode[] returnedTerms = new TermTreeNode[topX];
		TermTreeNode[] clone = (TermTreeNode[])queryTerms.clone();
		int[] frequency = new int[queryTerms.length];
		int[] indice = new int[queryTerms.length];
		for (int i = 0; i < indice.length; i++){
			indice[i] = i;
			frequency[i] = queryTerms[i].frequency;
		}
		int counter = 0;
		for (int i = 0; i < indice.length-1; i++){
			int top = i;
			for (int j = i+1; j < indice.length; j++){
				if (queryTerms[indice[top]].frequency > queryTerms[indice[j]].frequency)
					top = j;
			}
			returnedTerms[counter++] = clone[indice[top]];
			// swap
			int temp = indice[i];
			indice[i] = indice[top];
			indice[top] = temp;
			
			if (counter == topX)
				break;
		}
		return returnedTerms;
	}
	/**
	 * Print query terms and their normalised frequencies (term weights).
	 *
	 */
	public void dumpQuery(){
		System.out.println("query id: " + this.queryid);
		for (int i = 0; i < queryTerms.length; i++){
			System.out.println((i+1) + ": " + queryTerms[i].term +
					", normalisedFrequency: " + 
					uk.ac.gla.terrier.utility.Rounding.toString(queryTerms[i].normalisedFrequency, 4)
					);
		}
	}
	/**
	 * Insert a term into the query.
	 * @param term The inserted term.
	 */
	public void addTermTreeNode(TermTreeNode term){
		if (!termHashSet.contains(term.term)){
			this.vecQueryTerms.addElement(term);
			this.queryTerms = (TermTreeNode[])vecQueryTerms.toArray(
					new TermTreeNode[vecQueryTerms.size()]);
		}
		else{
			for (int i = 0; i < queryTerms.length; i++){
				if (queryTerms[i].term.equals(term.term)){
					queryTerms[i].normalisedFrequency += term.normalisedFrequency;
					break;
				}
			}
		}
	}
	
	/**
	 * Remove terms with zero or negative weights from the qeury.
	 *
	 */
	public void shrinkQuery(){
		for (int i = 0; i <queryTerms.length; i++){
			if (queryTerms[i].normalisedFrequency <= 0){
				termHashSet.remove(queryTerms[i].term);
				vecQueryTerms.remove(queryTerms[i]);
			}
		}
		this.queryTerms = (TermTreeNode[])vecQueryTerms.toArray(
				new TermTreeNode[vecQueryTerms.size()]);
	}
	
	/**
	 * Check if a given is a query term.
	 * @param term The given term.
	 * @return True is the given term is a query terms, false otherwise.
	 */
	public boolean isQueryTerm(String term){
		if (this.termHashSet.contains(term))
			return true;
		else
			return false;
	}

	/**
	 * Remove the given term from the query.
	 * @param term The given term.
	 */
	public void removeQueryTerm(TermTreeNode term){
		vecQueryTerms.remove(term);
		termHashSet.remove(term.term);
		this.queryTerms = (TermTreeNode[])vecQueryTerms.toArray(
				new TermTreeNode[vecQueryTerms.size()]);
	}
	
	/**
	 * Returns a representation of the queryTerms as a TermTreeNode array 
	 * @return TermTreeNode[] an array of TermTreeNode
	 */
	public TermTreeNode[] getQueryTerms() {
		return this.queryTerms;
	}
}
