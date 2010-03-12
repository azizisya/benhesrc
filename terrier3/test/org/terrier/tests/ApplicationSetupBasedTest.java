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
 * The Original Code is ApplicationSetupBasedTest.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;


import static org.junit.Assert.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.terrier.applications.TRECSetup;
import org.terrier.utility.Files;

/** Base class for a test that requries ApplicationSetup to be correctly initialised.
 * Uses a JUnit-created temporary folder, and invokes TRECSetup on it, to ensure that
 * a default configuration is generated.
 * @author craigm
 */
public class ApplicationSetupBasedTest {

	@Rule
    public TemporaryFolder tmpfolder = new TemporaryFolder();

	protected String terrier_home;
	protected String terrier_etc;
	
	public ApplicationSetupBasedTest() {}
	
	@Before
	public void makeEnvironment() throws Exception
	{
		System.setProperty("terrier.home", terrier_home = System.getProperty("user.dir"));
		terrier_etc = tmpfolder.getRoot().toString();
		System.err.println("terrier.home="+ System.getProperty("user.dir"));
		System.err.println("terrier.etc="+ terrier_etc);
		File fs[] = new File(terrier_etc).listFiles();
		if (fs != null)
			for(File f : fs)
				f.delete();
		new File(terrier_etc).mkdirs();
		System.setProperty("terrier.etc", terrier_etc);
		System.setProperty("terrier.setup", terrier_etc + "/terrier.properties");
		TRECSetup.main(new String[]{System.getProperty("user.dir")});
		
		BufferedReader br = Files.openFileReader(terrier_etc + "/terrier.properties");
		PrintWriter w = new PrintWriter(Files.writeFileWriter(terrier_etc+ "/terrier.properties.test"));
		String line = null;
		while((line = br.readLine()) != null)
		{
			w.println(line);
		}
		w.println("terrier.index.path="+terrier_etc);
		w.println("trec.results="+terrier_etc);
		addGlobalTerrierProperties(w);
		w.close();
		Files.delete(terrier_etc + "/terrier.properties");
		Files.rename(terrier_etc+ "/terrier.properties.test", terrier_etc + "/terrier.properties");		
		
		org.terrier.utility.ApplicationSetup.bootstrapInitialisation();
		Logger.getRootLogger().setLevel(Level.ALL);
		assertEquals(terrier_home, org.terrier.utility.ApplicationSetup.TERRIER_HOME);
		assertEquals(terrier_etc, org.terrier.utility.ApplicationSetup.TERRIER_ETC);
		assertEquals(terrier_etc, org.terrier.utility.ApplicationSetup.TERRIER_INDEX_PATH);
	}
	

	protected void addGlobalTerrierProperties(PrintWriter p) throws Exception
	{}
	
	@After public void deleteTerrierEtc()
	{
		File fs[] = new File(terrier_etc).listFiles();
		if (fs != null)
			for(File f : fs)
				f.delete();
		org.terrier.utility.ApplicationSetup.clearAllProperties();
	}
}
