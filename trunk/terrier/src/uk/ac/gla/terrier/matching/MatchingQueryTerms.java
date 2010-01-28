/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
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
 * The Original Code is MatchingQueryTerms.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.matching;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier;
import uk.ac.gla.terrier.matching.tsms.TermScoreModifier;
import uk.ac.gla.terrier.querying.parser.Query;
/**
 * Models a query used for matching documents. It is created
 * by creating an instance of this class, and then passing it as
 * an argument to the method obtainQueryTerms of a Query. It contains
 * the query terms, their weights, optionally the corresponding term 
 * identifiers and the assocciated term score modifiers. It also stores
 * the document score modifiers for the query.  
 * @author Vassilis Plachouras, Craig Macdonald.
 * @version $Revision: 1.1 $
 */
public class MatchingQueryTerms implements Serializable,Cloneable{
		
	/** The weight and the modifiers associated with a query term.*/
	protected static class QueryTermProperties implements Serializable{
		
		
		/** The weight of a query term. This is usually how many times the term occurred
		  * in the query, but sometime may be altered if a weight has been specified on the
		  * query term: eg QueryExpansion will do this, as will manually specifying a weight
		  * on the unparsed query (example <tt>term1 term2^3</tt>). */
		double weight;
		
		/** The term code (identifier) of the query term.*/
		int termCode;
		
		/** The term score modifiers associated with a particular query term.*/
		ArrayList<TermScoreModifier> modifiers = new ArrayList<TermScoreModifier>();
		
		/** An empty default constructor.*/
		public QueryTermProperties() {}

		/** 
		 * An constructor for setting the term code 
		 * of a query term.
		 * @param code int the term code of a query term. 
		 */
		public QueryTermProperties(int code) {
			termCode = code;
		}
		
		/** 
		 * A constructor for setting the weight of a term.
		 * @param w double the weight of a query term. 
		 */
		public QueryTermProperties(double w) {
			weight = w;
		}
		/**
		 * A constructor for setting a term score modifier for a term.
		 * @param tsm TermScoreModifier the modifier associated with a query term.
		 */
		public QueryTermProperties(TermScoreModifier tsm) {
			modifiers.add(tsm);
		}		
		
		/**
		 * A constructor for setting the weight and a 
		 * term score modifier for a term.
		 * @param w double the weight of a query term. 
		 * @param tsm TermScoreModifier the modifier associated with a query term.
		 */
		public QueryTermProperties(double w, TermScoreModifier tsm) {
			weight = w;
			modifiers.add(tsm);
		}
		
		/** 
		 * A constructor for setting the weight of a term
		 * and its term code.
		 * @param w double the weight of a query term. 
		 * @param code int the term code of a query term. 
		 */
		public QueryTermProperties(double w, int code) {
			weight = w;
			termCode = code;
		}
		
		/**
		 * A constructor for setting a term score modifier for a term 
		 * and its term code.
		 * @param tsm TermScoreModifier the modifier associated with a query term.
		 * @param code int the term code of a query term. 
		 */
		public QueryTermProperties(TermScoreModifier tsm, int code) {
			modifiers.add(tsm);
			termCode = code;
		}
		
		/**
		 * A constructor for setting a weight, a term score modifier 
		 * and the term code for a query term.
		 * @param tsm TermScoreModifier the modifier associated with a query term.
		 * @param code int the term code of a query term. 
		 */
		public QueryTermProperties(double w, TermScoreModifier tsm, int code) {
			weight = w;
			modifiers.add(tsm);
			termCode = code;
		}

		public Object clone()
		{
			QueryTermProperties newO = new QueryTermProperties(weight, termCode);
			for (TermScoreModifier tsm : modifiers)
				newO.modifiers.add((TermScoreModifier)(tsm.clone()));
			return (Object)newO;
		}

		public int hashCode()
		{
			int hashCodeValue = termCode;
			hashCodeValue += (new Double(weight)).hashCode();
			for (TermScoreModifier tsm : modifiers)
			{
				hashCodeValue += tsm.hashCode();
			}
			return hashCodeValue;
		}
		
	}
	
	/** The query ID, if provided */
	protected String queryId = null;
	
	/** A mapping from the string of a query term to its properties.*/
	protected HashMap<String,QueryTermProperties> termProperties = new HashMap<String,QueryTermProperties>();
	
	/** 
	 * The document score modifiers associated with the query terms.
	 * It should contain the phrase score modifiers for example.
	 */
	protected ArrayList<DocumentScoreModifier> docScoreModifiers = new ArrayList<DocumentScoreModifier>();
	
	/** The original query as it came from the parser, in case any TSMs or DSMs
	 * wish to refer to it
	 */
	protected Query query; 

	/** Generate a MatchingQueryTerms object. Query id will be null. */
	public MatchingQueryTerms()
	{}

	/** Generate a MatchingQueryTerms object, with the specified query id.
	  * @param qid A string representation of the query id */
	public MatchingQueryTerms(String qid)
	{
		queryId = qid;
	}
	
	/**
	 * Adds a document score modifier for the query.
	 * @param dsm DocumentScoreModifier a document score modifier for 
	 *        the query.
	 */
	public void addDocumentScoreModifier(DocumentScoreModifier dsm) {
		docScoreModifiers.add(dsm);
	}
	
	/**
	 * Returns the document score modifiers used for the query.
	 * @return DocumentScoreModifiers[] an array of the registered
	 *         document score modifiers for the query. If there are 
	 *         no document score modifiers, then it returns null.
	 */
	public DocumentScoreModifier[] getDocumentScoreModifiers() {
		if (docScoreModifiers.size()>0)
			return docScoreModifiers.toArray(tmpDSM);
		return null;
	}
	
	/** Allows the manager to set the query that was used to
	 * query the system.
	 * @param q The Query, duh
	 */
	public void setQuery(Query q)
	{
		query = q;
	}

	/** Returns guess what?
	 * @return the query
	 */
	public Query getQuery()
	{
		return query;
	}

	/** Returns the query id specified when constructing this object.
	  * @return String query id, or null if none was specified. */
	public String getQueryId()
	{
		return queryId;
	}

	/** Sets the query id */
	public void setQueryId(String newId)
	{
		queryId = newId;
	}
	
	/** 
	 * Adds a term to the query.
	 * @param term String the term to add.
	 */
	public void setTermProperty(String term) {
		QueryTermProperties properties = termProperties.get(term);
		if (properties == null) {
			termProperties.put(term, new QueryTermProperties());
		}
	}
	
	/** 
	 * Adds a term to the query with a given weight.
	 * @param term String the term to add.
	 * @param weight double the weight of the added term.
	 */
	public void setTermProperty(String term, double weight) {
		QueryTermProperties properties = termProperties.get(term);
		if (properties == null) {
			termProperties.put(term, new QueryTermProperties(weight));
		} else {
			properties.weight = weight;
		}
	}
	
	/**
	 * Adds the given weight for an already existing term in the query.
	 * If the term does not exist, it is added to the arraylist, with weight w
	 * @param term String the term for which we add the weight.
	 * @param w double the added weight.
	 */
	public void addTermPropertyWeight(String term, double w) {
		QueryTermProperties properties = termProperties.get(term);
		if (properties == null) {
			termProperties.put(term, new QueryTermProperties(w));
		} else {
			properties.weight += w;
		}
	}
	
	/**
	 * Sets the term integer identifier for the given query term.
	 * @param term String the term for which the term identifier is set.
	 * @param code int the term identifier.
	 */
	public void setTermProperty(String term, int code) {
		QueryTermProperties properties = termProperties.get(term);
		if (properties == null) {
			termProperties.put(term, new QueryTermProperties(code));
		} else {
			properties.termCode = code;
		}
	}
	
	/**
	 * This method normalises the term weights by dividing each term weight
	 * by the maximum of the terms.
	 */
	public void normaliseTermWeights(){
		// obtain the maximum term weight
		double maxWeight = 0d;
		QueryTermProperties[] properties = 
			(QueryTermProperties[])termProperties.values().toArray(
					new QueryTermProperties[termProperties.size()]);
		for (int i = 0; i < properties.length; i++)
			maxWeight = Math.max(maxWeight, properties[i].weight);
		// normalise
		for (int i = 0; i < termProperties.size(); i++)
			properties[i].weight /= maxWeight;
	}
	
	/**
	 * Sets a term score modifier for the given query term.
	 * @param term String the term for which to add a term score modifier.
	 * @param tsm TermScoreModifier the term score modifier to apply for the given term.
	 */
	public void setTermProperty(String term, TermScoreModifier tsm) {
		QueryTermProperties properties = (QueryTermProperties)termProperties.get(term);
		if (properties == null) {
			termProperties.put(term, new QueryTermProperties(tsm));
		} else {
			properties.modifiers.add(tsm);
		}
	}
	
	/**
	 * Sets the weight and a term score modifier for the given query term.
	 * @param term String the term for which we set the properties.
	 * @param weight int the weight of the query term.
	 * @param tsm TermScoreModifier the term score modifier applied for the query term.
	 */
	public void setTermProperty(String term, double weight, TermScoreModifier tsm) {
		QueryTermProperties properties = (QueryTermProperties)termProperties.get(term);
		if (properties == null) {
			termProperties.put(term, new QueryTermProperties(weight, tsm));
		} else {
			properties.weight = weight;
			properties.modifiers.add(tsm);
		}
	}
	
	/**
	 * Returns the assocciated weight of the given query term.
	 * @param term String the query term for which the weight is returned.
	 * @return double the weight of the given query term. If the term is not part
	 *         of the query, then it returns 0.
	 */
	public double getTermWeight(String term) {
		QueryTermProperties tp = (QueryTermProperties)termProperties.get(term);
		if (tp!=null)
			return tp.weight;
		return 0.0d;
	}
	/**
	 * Returns the associated weights of the given query terms.
	 * @return double The weights of the given terms in a double array.
	 */
	public double[] getTermWeights(){
		double[] tws = new double[this.length()];
		for (int i = 0; i < tws.length; i++)
			tws[i] = this.getTermWeight(this.getTerms()[i]);
		return tws;
	}
	
	/**
	 * Returns the assocciated code of the given query term.
	 * @param term String the query term for which the weight is returned.
	 * @return int the term code of the given query term, or -1 if the term
	 *         does not appear in the query.
	 */
	public int getTermCode(String term) {
		QueryTermProperties tp = (QueryTermProperties)termProperties.get(term);
		if (tp!=null)
			return tp.termCode;
		return -1;
	}
	
	/** 
	 * Returns the term score modifiers assocciated with the given query term.
	 * @param term String a query term.
	 * @return TermScoreModifiers[] the term score modifiers associated with
	 *         the given query term, or null if the query term is not part 
	 *         of the query. 
	 */
	public TermScoreModifier[] getTermScoreModifiers(String term) {
		QueryTermProperties tp = (QueryTermProperties)termProperties.get(term);
		if (tp!=null)
			return (TermScoreModifier[])tp.modifiers.toArray(tmpTSM);
		return null;
	}
	/**
	 * Returns the terms of the query. 
	 * @return String[] an array of the query terms, or null if the query 
	 *         does not contain any terms.
	 */
	/*public String[] getTerms() {
		Set keySet = termProperties.keySet();
		if (keySet.size()>0)
			return (String[])keySet.toArray(tmpString);
		return null;
	}*/
	
	//
	/** Currently uses Array.sort(Object[]), will retain order of query terms
	// in future. TODO return and rewrite this method */
	public String[] getTerms() {
		Set<String> keySet = termProperties.keySet();
		if (keySet.size()>0)
		{
			String[] terms = keySet.toArray(tmpString);
			Arrays.sort(terms);
			return terms;	
		}
		return null;
	}
	
	/**
	 * Returns the number of unique terms in the query.
	 * @return int the number of unique terms in the query.
	 */
	public int length() {
		return termProperties.size();
	}

	/** Performs a deep clone of this object, and all objects it contains. This allows a MQT to be copied,
	  * and changed without affecting the original object. */
	public Object clone()
	{
		MatchingQueryTerms newMQT = new MatchingQueryTerms(this.queryId);
		//copy queryID, Strings are immutable
		//clone query term properties
		for (String term : termProperties.keySet().toArray(tmpString))
		{
			newMQT.termProperties.put(term, (QueryTermProperties)(this.termProperties.get(term).clone()));
		}
		for (DocumentScoreModifier dsm : docScoreModifiers)
		{
			newMQT.docScoreModifiers.add( (DocumentScoreModifier)(dsm.clone()));
		}
		//clone query		
		newMQT.query = (Query)this.query.clone();
		return newMQT;
	}
	
	/* 
	 * The following attributes are used for creating arrays of the correct type.
	 */
	private static final TermScoreModifier[] tmpTSM = new TermScoreModifier[0];
	private static final DocumentScoreModifier[] tmpDSM = new DocumentScoreModifier[0];
	private static final String[] tmpString = new String[0];
	
}
