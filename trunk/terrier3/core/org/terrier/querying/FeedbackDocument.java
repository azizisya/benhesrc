/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
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
 * The Original Code is FeedbackDocument.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.querying;	
/** Class representing feedback documents, pseudo- or otherwise.
  * @since 3.0
  * @author Craig Macdonald
  */
public class FeedbackDocument
{
	public int docid;
	public int rank;
	public byte relevance;
	public double score;

	
	@Override
	public int hashCode() {
		return docid;
	}

	@Override
	public boolean equals(Object y)
	{
		if (!(y instanceof FeedbackDocument))
			return false;
		return ((FeedbackDocument)y).docid == docid;
	}
}
