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
 * The Original Code is MultiFileSplit.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   
 */
package org.terrier.structures.indexing.singlepass.hadoop;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;

/** An instance of org.apache.hadoop.mapred.MultiFileSplit that provides a default constructor.
 * @author Richard McCreadie and Craig Macdonald
 * @since 3.0
 */
public class MultiFileSplit extends org.apache.hadoop.mapred.MultiFileSplit {

	public MultiFileSplit() {
		super(null, null, new long[0]);
	}
	
	public MultiFileSplit(JobConf arg0, Path[] arg1, long[] arg2) {
		super(arg0, arg1, arg2);
	}

}
