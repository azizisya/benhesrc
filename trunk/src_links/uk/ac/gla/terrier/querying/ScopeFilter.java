/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
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
 * The Original Code is ScopeFilter.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
  *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.querying;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.utility.ApplicationSetup;
public class ScopeFilter implements PostFilter
{
	/** Set <tt>scopefilter.docno.prefix</tt> property to E for EuroGOV collection */
	protected final String prefix = ApplicationSetup.getProperty("scopefilter.docno.prefix", "");
	protected String domain = "";
	public void new_query(Manager m, SearchRequest srq, ResultSet rs)
	{
		System.err.println("Got scope "+srq.getControl("scope").toLowerCase());
		domain = srq.getControl("scope").toLowerCase();
		if (domain.length() >0)
			domain = prefix+domain;
	}
	/**
	  * Called for each result in the resultset, used to filter out unwanted results.
	  * @param m The manager controlling this query
	  * @param srq The search request being processed
	  * @param DocAtNumber which array index in the resultset have we reached
	  * @param DocNo The document number of the currently being procesed result.
	  */
	public byte filter(Manager m, SearchRequest srq, ResultSet rs, int DocAtNumber, int DocNo)
	{
		String docid = rs.getMetaItem("docid", DocAtNumber);
		if (docid.length() > 0 && ! docid.startsWith(domain))
			return FILTER_REMOVE;
		return FILTER_OK;
	}
}
