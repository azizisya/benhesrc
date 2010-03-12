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
 * The Original Code is Main.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> (original contributor)
  *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  *   Erik Graf <graf{a.}dcs.gla.ac.uk>
 */
package org.terrier.querying.parser;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.StringReader;

import org.apache.log4j.Logger;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.dsms.DocumentScoreModifier;
import org.terrier.matching.models.WeightingModel;
/**
 * TODO: JAVADOC
 * @author graf
 *
 */
class Main {
	
	protected static final Logger logger = Logger.getRootLogger();
	
	public static void main(String[] args) {
		try {
			if (args.length==0) {
				TerrierLexer lexer = new TerrierLexer(new DataInputStream(System.in));
				TerrierQueryParser parser = new TerrierQueryParser(lexer);
				Query q = parser.query();
				if(logger.isDebugEnabled()){
					logger.debug(q.toString());
				}
			} else {
				for (int i=0; i<args.length; i++) {
					BufferedReader br = new BufferedReader(new FileReader(args[i]));
					String line = null;
					int j=0; int failed = 0;
					while ((line = br.readLine())!=null) {
						j++;
						String[] entries = line.split("\\t+");
						TerrierLexer lexer = new TerrierLexer(new StringReader(entries[0]));
						TerrierQueryParser parser = new TerrierQueryParser(lexer);
						Query q = parser.query();
						if(q.toString().equals(entries[1]))
						{
							if(logger.isDebugEnabled()){
								logger.debug("OK: ");
								logger.debug("("+j+"): " + line + " : "+ q.toString());
							}
							MatchingQueryTerms terms = new MatchingQueryTerms();
							q.obtainQueryTerms(terms);
							String[] queryTerms = terms.getTerms();
							for (int k=0; k<queryTerms.length; k++) {
								if(logger.isDebugEnabled()){
									logger.debug(queryTerms[k] + " : " + terms.getTermWeight(queryTerms[k]));
								}
								WeightingModel[] tsms = terms.getTermWeightingModels(queryTerms[k]);
								if (tsms!=null) {
									for (int l=0; l<tsms.length; l++)
										if (tsms[l]!=null)
											logger.debug(" " + tsms[l].getInfo());
										
								}
							}
							DocumentScoreModifier[] dsms = terms.getDocumentScoreModifiers();
							if (dsms!=null) {
								for (int l=0; l<dsms.length; l++)
									if (dsms[l]!=null)
										logger.debug(dsms[l].getName());
							}
						
							logger.debug("--");	
						}
						else
						{
							failed++;
							logger.debug("NOT OK: ");
							logger.debug("(#"+j+"): " + line + " : "+ q.toString());
						}
						
					}
					logger.debug("Failed "+ failed +" tests");
					br.close();
				}
			}
		}
		catch(Exception e) {
			logger.error("An exception occured: ",e);
			
		}
	}
}
