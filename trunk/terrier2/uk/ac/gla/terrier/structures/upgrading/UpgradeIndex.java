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
 * The Original Code is UpgradeIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *  Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.structures.upgrading;

import uk.ac.gla.terrier.structures.Index;

/** Command line utility to upgrade a Terrier index. 
  * <p><b>Usage:</b><br>
  * <tt>bin/anyclass.sh uk.ac.gla.terrier.structures.upgrading.UpgradeIndex SourceIndexPath SourceIndexPrefix DestIndexPath DestIndexPrefix</tt>.
  * <p><b>Notes:</b><br>
  * <ol><li>Upgrading from Terrier 1.x indices: The upgrader can figure out most things, but you must specify the number of fields correctly 
  * in the property <tt>FieldTags.process</tt> - for example <tt>FieldTags.process=TITLE,H1</tt>. 
  * </ol>
  * @author Craig Macdonald
  * @since 2.0
  * @version $Revision: 1.1 $ */
public class UpgradeIndex
{
	public static void main (String[] args)
	{
		if (args.length != 4)
		{
			System.err.println("Usage: uk.ac.gla.terrier.structures.upgrading.UpgradeIndex"
				+ " sourceIndexPath sourceIndexPrefix destIndexPath destIndexPrefix");
			System.err.println("Upgrades a Terrier index to the latest version");
			return;
		}
		final Index sourceIndex = Index.createIndex(args[0], args[1]);
		final String sourceVersion = sourceIndex.getIndexProperty("index.terrier.version", "unknown");
		if (sourceVersion.startsWith("1.0"))
		{
			System.err.println("Upgrading Terrier 1.x.x index");
			try{
				new Terrier1xxIndexUpgrader(sourceIndex, Index.createNewIndex(args[2], args[3])).upgrade();
			} catch (Exception e) {
				System.err.println("Problem upgrading index:"+ e);
				e.printStackTrace();
			}
			System.err.println("Done");
		}
		else
		{
			System.err.println("Sorry, only Terrer version 1 indices can be upgraded. Your index format is + '"+sourceVersion+"'");	
		}
	}
}
