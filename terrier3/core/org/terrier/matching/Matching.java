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
 * The Original Code is Matching.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package org.terrier.matching;

import java.io.IOException;

import org.terrier.structures.CollectionStatistics;

/** Interface for all Matching implementations.
 * @since 3.0
 * @author Vassilis Plachouras, Craig Macdonald
 */
public interface Matching
{
	
	/** Return a human readable description of this Matching class */
	public String getInfo();
	/** Get a ResultSet for the given query terms.
	 * @param queryNumber - some ID of the query
	 * @param queryTerms - query terms to match
	 * @return ResultSet - the matched results
	 * @throws IOException if a problem occurs during matching
	 */
	public ResultSet match(String queryNumber, MatchingQueryTerms queryTerms) throws IOException;
	
	/** Update the collection statistics being used by this matching instance 
	 * @param cs CollectionStatistics to use during matching 
	 */
	public void setCollectionStatistics(CollectionStatistics cs);
}
