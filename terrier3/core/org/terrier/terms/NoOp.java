/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is NoOp.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package org.terrier.terms;

/** A do-nothing term pipeline object. 
 *  Simply passes the term onto the next component of the pipeline. 
 *  @author Craig Macdonald
 *  @version $Revision: 1.3 $
 */
public class NoOp implements TermPipeline
{

	/** The implementation of a term pipeline.*/
    protected final TermPipeline next;

    /**
     * Constructs an instance of the class, given the next
     * component in the pipeline.
     * @param next TermPipeline the next component in
     *      the term pipeline.
     */
    public NoOp(TermPipeline next)
    {
        this.next = next;
    }

	/** Pass the term onto the next term pipeline object,
	 *  without making any changes to it.
	 * @param t The term
	 */
	public final void processTerm(final String t)
    {
        if (t == null)
            return;
        next.processTerm(t);
    }
}
