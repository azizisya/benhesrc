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
 * The Original Code is Tuning.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.applications;

import java.io.IOException;

import uk.ac.gla.terrier.smooth.tuning.CorrelationTuning;
import uk.ac.gla.terrier.smooth.tuning.InternalCorrelationTuning;
import uk.ac.gla.terrier.smooth.tuning.NETuning;
import uk.ac.gla.terrier.smooth.tuning.ParameterTuning;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * The text-based application that handles requests of parameter tuning.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.2 $
 */
public class Tuning {
	/**
	 * The main method that starts the application.
	 * @param args The arguments.
	 */
	public static void main(String[] args) {
		CollectionStatistics collSta = Index.createIndex().getCollectionStatistics();
		String methodName = args[1];
		String tuningType = args[2];
		String samplingType = args[3];
		String taskName = args[4];
		int numberOfSamples = 200;
		if (args.length > 5)
			numberOfSamples = Integer.parseInt(args[5]);
		ParameterTuning tuning = null;
		String packagePrefix = "uk.ac.gla.terrier.smooth.tuning.";
		boolean recognised = false;
		if (tuningType.equalsIgnoreCase("-NE")){
			tuning = new NETuning(methodName, Index.createIndex());
			recognised = true;
		}
		if (tuningType.equalsIgnoreCase("-corr")){
			recognised = true;
			if (args[0].equalsIgnoreCase("-it"))
				tuning = new InternalCorrelationTuning(methodName, Index.createIndex());
			else{
				tuning = new CorrelationTuning(methodName, Index.createIndex());
				tuning.setAvl(collSta.getAverageDocumentLength());
				tuning.setNumberOfTokens(collSta.getNumberOfTokens());
			}
		}
		if(!recognised){
			System.err.println("Unrecognised tuning type. Exit...");
			System.exit(1);
		}
		
		if (samplingType.equalsIgnoreCase("-real")){
			double parameter = tuning.tuneRealTRECQuery(taskName);
			System.out.println("-------------------------------------");
			System.out.println("Normalisation method: " + methodName);
			System.out.println("Estimated setting: " + Rounding.toString(parameter, 4));
		}
		if (samplingType.equalsIgnoreCase("-sim")){
			double parameter = tuning.tuneSampling(taskName, numberOfSamples);
			System.out.println("-------------------------------------");
			System.out.println("Normalisation method: " + methodName);
			System.out.println("Estimated setting: " + Rounding.toString(parameter, 4));
		}
	}
}
