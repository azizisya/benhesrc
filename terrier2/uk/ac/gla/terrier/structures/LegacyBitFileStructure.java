
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
 * The Original Code is LegacyBitFileStructure.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;

import java.io.IOException;

/** This is marker interface which is used by Index to detect if an index structure
 *  has been written by Terrier 1.xx version of Terrier, and that hence it should
 *  be reopened using the OldBitFile classes. Implementing classes provide a
 *  reOpenLegacyBitFile() method, which reopens their internal file using the
 *  older OldBitFile class.
 *  @author Craig Macdonald
 *  @version $Revision: 1.1 $
 *  @since 2.0
 */
interface LegacyBitFileStructure {
	/** forces the data structure to reopen the underlying bitfile
	 *  using the legacy implementation of BitFile (OldBitFile)
	 * @throws IOException
	 */
	public void reOpenLegacyBitFile() throws IOException;
	
}
