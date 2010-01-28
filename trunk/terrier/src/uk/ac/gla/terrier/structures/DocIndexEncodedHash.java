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
 * The Original Code is DirectIndex.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Iraklis A. Klampanos <iraklis{a.}dcs.gla.ac.uk> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.io.IOException;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.DocumentIndexEncoded;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import gnu.trove.TObjectIntHashMap;

public class DocIndexEncodedHash extends DocumentIndexEncoded {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	private TObjectIntHashMap<String> map = null;
	
	private String path;
	
	private String prefix;
	
	public DocIndexEncodedHash(String path, String prefix) throws IOException
	{
		super(path+ ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.DOC_INDEX_SUFFIX);
		this.path = path;
		this.prefix = prefix;
		loadHashMap();
	}
	
	public DocIndexEncodedHash(String filename) throws IOException {
		super(filename);
		this.path = filename.substring(0, filename.lastIndexOf('/'));
		this.prefix = filename.substring(filename.lastIndexOf('/')+1, filename.lastIndexOf('.'));
		loadHashMap();
	}
	
	/**
	 * 
	 */
	private void loadHashMap() throws IOException {
		if(logger.isInfoEnabled()){
			logger.info(">> Loading the document index into the hashtable ....");
		}
		map = new TObjectIntHashMap<String>();
		DocumentIndexInputStream diis = new DocumentIndexInputStream(path, prefix);
		while (diis.readNextEntry() > -1) {
			String docno = diis.getDocumentNumber();
			int docid = diis.getDocumentId();
			map.put(docno,docid);
		}
		if(logger.isInfoEnabled()){
			logger.info(" [ DONE ]");
		}

	}
	
	public boolean seek(String docno){
		return map.containsKey(docno);
	}

	public DocIndexEncodedHash() throws IOException {
		super();
		loadHashMap();
	}
	
	public int getDocumentId(String docno) {
		return map.get(docno);
	}
	
	
	public static void main(String[] args) {
	}
}
