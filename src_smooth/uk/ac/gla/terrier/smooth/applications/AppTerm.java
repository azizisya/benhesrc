/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
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
 * The Original Code is AppTerms.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.applications;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.smooth.matching.BufferedMatching;
import uk.ac.gla.terrier.smooth.tuning.CorrelationTuning;
import uk.ac.gla.terrier.structures.Index;

/**
 * The text-based application that accessing information 
 * of single terms, for TREC-like test collections.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class AppTerm {
	/** The index used for accessing information of a given term. */
	protected Index index;
	/** The term pipeline. */
	protected Manager pipe;
	/**
	 * The default constructor.
	 * @param index The index used for accessing information of a given term.
	 */
	public AppTerm(Index index){
		this.index = index;
		pipe = new Manager(index);
	}
	/**
	 * Print the correlation of term frequency with document length for a given term.
	 * @param methodName The name of the normalisation method.
	 * @param term The given term.
	 * @param parameter The parameter setting of the normalisation method.
	 */
	public void printCorrelationTFLength(String methodName, String term, double parameter){
		System.out.println("Computing corr(tf, l) for term " + term + 
				" using normalisation " + methodName + " with parameter=" +
				parameter);
		CorrelationTuning corr = new CorrelationTuning(methodName, index);
		BufferedMatching matching = new BufferedMatching(index);
		double correlation = corr.getCorrelationTFLength(matching, parameter, term);
		System.out.println("corr(tf, l): " + correlation);
	}
	/**
	 * Print correlation of term frequency with document length for the
	 * real TREC queries.
	 * @param methodName The name of the normalisation method.
	 * @param parameter The parameter setting of the normalisation method.
	 */
	public void printCorrelationTFLengthRealTRECQuery(String methodName, double parameter){
		System.out.println("Computing the mean corr(tf, l) for real TREC query terms " + 
				"using normalisation " + methodName + " with parameter=" +
				parameter);
		CorrelationTuning corr = new CorrelationTuning(methodName, index);
		BufferedMatching matching = new BufferedMatching(index);
		double correlation = corr.getCorrelationTFLengthRealTRECQuery(methodName, matching, parameter);
		System.out.println("mean corr(tf, l): " + correlation);
	}
	/**
	 * The main method that starts the application.
	 * @param args The arguments.
	 */
	public static void main(String[] args) {
		AppTerm app = new AppTerm(Index.createIndex());
		// -term -corr <methodName> <term> <value>
		if (args[1].equalsIgnoreCase("-corr")){
			String methodName = args[2];
			String term = args[3];
			double parameter = Double.parseDouble(args[4]);
			app.printCorrelationTFLength(methodName, term, parameter);
		}
		// -term -rcorr <methodName> <value>
		if (args[1].equalsIgnoreCase("-rcorr")){
			String methodName = args[2];
			double parameter = Double.parseDouble(args[3]);
			app.printCorrelationTFLengthRealTRECQuery(methodName, parameter);
		}
	}
}
