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
 * The Original Code is PostFilter.java.
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
import uk.ac.gla.terrier.links.GroupingIndex;
import uk.ac.gla.terrier.links.GroupingServer;
/** PostFilters are designed to complement PostProcesses. While PostProcesses
  * operate on the entire resultset at once, with PostFilters, each PostFilter
  * is called for each result in the resultset. According to the return of <tt>filter()</tt>
  * the result can then be included, discarded, or (un)boosted in the resultset. Possible
  * return values for <tt>filter</tt> are FILTER_OK, FILTER_REMOVE, FILTER_ADJUSTED
  * Which PostFilters are run, and when is controlled by two properties, as mentioned below.<br/>
  * <B>Properties</B>
  * <ul>
  * <li><tt>querying.postfilters.controls</tt> : A comma separated list of control to PostFilter
  * class mappings. Mappings are separated by ":". eg <tt>querying.postfilters.controls=scope:Scope</tt></li>
  * <li><tt>querying.postfilters.order</tt> : The order postfilters should be run in</li></ul>
  * '''NB:''' Initialisation and running of post filters is carried out by the Manager.
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
public class GroupFilter implements PostFilter
{
	protected String group = "";
	protected GroupingIndex GroupsServer = 
		new GroupingServer(
			ApplicationSetup.makeAbsolute("groups_definitions", ApplicationSetup.TERRIER_INDEX_PATH),
			ApplicationSetup.makeAbsolute(ApplicationSetup.TERRIER_INDEX_PREFIX+".groups", ApplicationSetup.TERRIER_INDEX_PATH)
			);
			
	public void new_query(Manager m, SearchRequest srq, ResultSet rs)
	{
		group = srq.getControl("group").toLowerCase();

		if (group == null)
			return;

		if (group.length() ==0 || ! GroupsServer.isGroup(group))
		{
			System.err.println("WARNING: Unknown filter group \""+group+"\" - ignoring");
			group = null;
		}
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
		if (group != null)
		{
			if (! GroupsServer.isMember(group, DocNo))
				return FILTER_REMOVE;
		}
		return FILTER_OK;
	}
}
