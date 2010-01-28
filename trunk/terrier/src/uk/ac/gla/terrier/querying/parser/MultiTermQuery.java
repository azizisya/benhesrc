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
 * The Original Code is MultiTermQuery.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying.parser;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.HashSet;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.terms.TermPipelineAccessor;
/**
 * Represents a query consisting of more than one terms or 
 * other sub-queries, qualified with field, requirement or 
 * phrase operators.
 * @author Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class MultiTermQuery extends Query {
	
	/** 
	 * A list for holding the sub-queries. A LinkedList is ideal as we
	 * have no need to access numbered sub-query elements of the query - 
	 * only be able to iterate throught them.
	 * 
	 */
	protected LinkedList<Query> v = null;
	
	/** A default constructor.*/
	public MultiTermQuery() {
		v = new LinkedList<Query>();
	}
		
	/** 
	 * Returns the number of terms, or subqueries.
	 * @return int the number of terms, or subqueries of this query.
	 */
	public int getNumberOfTerms() {
		return v.size();
	}

	public Object clone()
	{
		MultiTermQuery mtq = (MultiTermQuery)super.clone();
		mtq.v = new LinkedList<Query>();
		for(Query child: v)
		{
			mtq.v.add((Query)(child.clone()));
		}
		return (Object)mtq;
	}

	/*
	 * Adds a single query term to the query.
	 * @param term String the query term.
	 */
	public void add(final String term) {
		if (term != null) v.addLast(new SingleTermQuery(term));
	}
	/**
	 * Adds a subquery to this query.
	 * @param query Query a subquery.
	 */
	public void add(final Query query) {
		if (query != null) v.addLast(query);
	}
	/**
	 * Returns a string representation of the query.
	 * @return String the string of this query.
	 */
	public String toString() {
		StringBuilder output = new StringBuilder();
		
		int queryLength = v.size();
		for (Query child: v)
		{
			output.append(child);
			if(--queryLength > 0)
				output.append(" ");
		}
		return output.toString();
	}
	
	/**
	 * Applies the given term pipeline to the query terms
	 * and sub-queries that construct this query.
	 * @param tpa TermPipelineAccessor the object that gives access
	 *		to a term pipeline.
	 * @return boolean true if the query is not empty, otherwise returns false.
	 */
	public boolean applyTermPipeline(final TermPipelineAccessor tpa)
	{
		boolean Alive = false;
		//cannot use foreach, as elements need to be removed
		final Iterator it = v.iterator();
		while (it.hasNext())
		{
			boolean rtr = ((Query)it.next()).applyTermPipeline(tpa);
			Alive |= rtr;
			if (! rtr)
			{
				it.remove();
			}
		}
		return Alive;
	}
	/**
	 * Prepares the query for matching by transforming
	 * the query objects to a set of query terms.
	 * @param terms MatchingQueryTerms the object which holds the 
	 *		query terms and their modifiers.
	 */
	public void obtainQueryTerms(final MatchingQueryTerms terms) {
		Iterator it = v.iterator();
		while (it.hasNext()) {
			((Query)it.next()).obtainQueryTerms(terms);
		}
	}
	
	/**
	 * Prepares the query for matching by transforming
	 * the query objects to a set of query terms.
	 * @param terms MatchingQueryTerms the object which holds the 
	 *		query terms and their modifiers.
	 * @param required boolean indicates whether the field query
	 *		is required or not.	 
	 */
	public void obtainQueryTerms(final MatchingQueryTerms terms, final boolean required) {
		Iterator it = v.iterator();
		while (it.hasNext()) {
			((Query)it.next()).obtainQueryTerms(terms, required);
		}
	}
	
	/**
	 * Adds all the subqueries and single-term queries to 
	 * a given array list.
	 * @param alist ArrayList the array list in which all the 
	 *		query terms and sub-queries are stored.
	 */
	protected void getTerms(final ArrayList alist) {
		Iterator it = v.iterator();
		while (it.hasNext()) {
			((Query)it.next()).getTerms(alist);
		}
	}
	/** Checks all child objects to see if they are FieldQuery objects, they are
	  * each called to see if the are controls. Child objects which return true
	  * are removed from the tree. If all child objects return true then this 
	  * object is also considered dead, and should be removed. 
	  * @param allowed The HashSet of control names that are allowed to be set.
	  * @param controls The Hashtable into which child objects much add 
	  *        found controls 
	  * @return true if this object should be considered dead 
	  *         (no longer part of the query), false otherwise.
	*/
	public boolean obtainControls(final HashSet allowed, final Hashtable controls)
	{
		//cannot use foreach, as need to remove elements
		final Iterator it = v.iterator();
		boolean all = true;
		while (it.hasNext()) 
		{
			Query q = (Query)it.next();
			//controls can only exist at the top level
			if(q instanceof FieldQuery)
			{
				//check child field object
				if (q.obtainControls(allowed, controls))
				{
					//it was a control, and is now, dead so should be removed
					it.remove();
				}
				else
				{
					//we still have alive child objects, so dont commit suicide
					all = false;
				}
			}
			else
			{	//we still have alive child objects, so dont commit suicide
				all = false;
			}
		}
		return all;
	}
	
	/** 
	 * Returns all the query terms, in subqueries that
	 * are instances of a given class
	 * @param c Class a class of queries.
	 * @param alist ArrayList the list of query terms.
	 * @param req boolean indicates whether the subqueries 
	 *        are required or not.
	 */
	public void getTermsOf(final Class c, final ArrayList alist, final boolean req) {
		if (!req)
			return;
		Iterator it = v.iterator();
        while (it.hasNext())
        {
            ((Query)it.next()).getTermsOf(c, alist, req);
		}
	}
	
	public void obtainAllOf(final Class c, final ArrayList a){
		if (c.isInstance(this))
			a.add(this);
		Iterator it = v.iterator();
        while (it.hasNext())
        {
            ((Query)it.next()).obtainAllOf(c, a);
		}
	}
	
	/** 
	 * A private array used only in the method
	 * getAllOf(Object o), in order to get the 
	 * required type of objects from the query.
	 */
	private static Query[] tmpQueryArray = new Query[1];

}
