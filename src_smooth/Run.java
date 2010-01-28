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
 * The Original Code is CorrelationTuning.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */

import uk.ac.gla.terrier.smooth.applications.AppPrediction;
import uk.ac.gla.terrier.smooth.applications.AppQuerySimulation;
import uk.ac.gla.terrier.smooth.applications.AppTerm;
import uk.ac.gla.terrier.smooth.applications.TRECBasicQuerying;
import uk.ac.gla.terrier.smooth.applications.Tuning;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;
/**
 * The text-based application that handles querying 
 * with Smooth, for TREC-like test collections.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class Run {
	/**
	 * A boolean variable indicating if the input command is found.
	 */
	 boolean flag = false;
	 /** A boolean variable indicating if the program is run in debugging mode (with more output). */
	 protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	 /**
	  * The default constructor.
	  * @param args The arguments.
	  */
	 public Run(String[] args){
	 	if (args.length == 0){
	 		this.printOptions();
	 		flag = true;
	 	}
	 	else if (args[0].equalsIgnoreCase("-r")){
 			flag = true;
 			TRECBasicQuerying.main(args);
 		}
	 	else if (args[0].equalsIgnoreCase("-s")){
	 		flag = true;
	 		AppQuerySimulation.main(args);
	 	}
	 	else if (args[0].equalsIgnoreCase("-t")||args[0].equalsIgnoreCase("-it")){
	 		flag = true;
	 		Tuning.main(args);
	 	}
	 	else if(args[0].equalsIgnoreCase("-term")){
	 		flag = true;
	 		AppTerm.main(args);
	 	}
	 	else if (args[0].equalsIgnoreCase("-p")){
	 		flag = true;
	 		AppPrediction.main(args);
	 	}
	 	
	 	else this.printOptions();
	 }
	 /**
	  * Print the usage of Smooth.
	  *
	  */
	 public void printOptions(){
	 	System.out.println("Usage:");
	 	System.out.println("-r If invoked with \'-r\', there are the following options.");
	 	System.out.println("  -r <parameter> Do retrieval using query re-weighting.");
 		System.out.println("-s If invoked with \'-s\', there are the following options.");
 		System.out.println("  -s -2q <n> <ql> Create n simulated queries using the " +
 				"two step simulation. Each query has the length of ql.");
 		System.out.println("  -s -2q <n> <min-ql> <max-ql> Create n simulated queries " +
 				"using the two step simulation. The length of a query is a random " +
 				"number between (including) min-ql and max-ql."); 
 		System.out.println("-t If invoked with \'-t\', there are the following options.");
 		System.out.println("  -t <methodName> <-corr, -NE> <-real, -sim> <adhoc> " +
 				"<N> Tuning parameter for the specified normalisation method.");
 		System.out.println("-p If involed with \'-p\', there are the following options.");
 		System.out.println("    -p <filename> Save a list of query performance predictors " +
 				"in the specified file.");
 		//System.out.println("  -t -f <methodName> -real <taskName> <fweight>");
 		//System.out.println("  -t -f <methodName> -sim <taskName> <N> <fweight>");
 		//System.out.println("-term If invoked with \'-term\', there are the following options.");
 		//System.out.println("  -term -corr <methodName> <term> <value> Compute correlation" +
 				//" of normalised term frequency with document length for the given term using " +
 				//"the given normalisation " +
 				//"method with the given parameter value.");
 		//System.out.println("  -term -rcorr <methodName> <value> Print the mean " +
 				//"correlation for all the query terms in the topic file(s) specified " +
 				//"in trec.topics.list.");
 		
	 }
	 /**
	  * The main method of Run.
	  * @param args The arguments.
	  */
	 public static void main(String[] args) {
	 	System.out.println("Start...");
	 	TerrierTimer timer = new TerrierTimer();
		Run run = new Run(args);
		timer.setBreakPoint();
		if (args.length != 0)
			System.out.println("Finished. Time elapsed: " + timer.toStringMinutesSeconds());
	}
}
