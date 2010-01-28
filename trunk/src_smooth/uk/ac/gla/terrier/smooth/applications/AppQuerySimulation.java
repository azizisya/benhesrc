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
 * The Original Code is AppQuerySimulation.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.applications;

import uk.ac.gla.terrier.smooth.simulation.QuerySimulation;
import uk.ac.gla.terrier.smooth.structures.BasicQuery;
import uk.ac.gla.terrier.structures.Index;

/**
 * The text-based application that handles query simulation on
 * an index created by Terrier.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.1 $
 */
public class AppQuerySimulation {
	/** The index used for query simulation. */
	protected Index index;
	/** The instance of query simulation that handles requests of
	 * simuations.
	 */
	protected QuerySimulation simulation;
	/**
	 * The default constructor.
	 * @param index The index used for query simulation.
	 */
	public AppQuerySimulation(Index index){
		this.index = index;
		this.simulation = new QuerySimulation(index);
	}
	/**
	 * Simulate n queries using the 2-step simulation algorithm.
	 * Each simulated query consists of ql terms.
	 * @param n The number of queries to simulate.
	 * @param ql The query length of each simulate query.
	 */
	public void simulateTwoStepQuery(int n, int ql){
		for (int i = 0; i < n; i++){
			BasicQuery query = simulation.twoStepSimulation(ql);
			System.err.println("Simulated query:");
			System.err.println("-----------------------------");
			query.dumpQuery();
			System.err.println("-----------------------------");
		}
	}
	/**
	 * Simulate n queries using the 2-step simulation algorithm.
	 * The length of each simulated query is a random integer between
	 * (including) minLength and maxLength.
	 * @param n The number of queries to simulate.
	 * @param minLength The minimum length of a simulated query.
	 * @param maxLength The maximum length of a simulated query.
	 */
	public void simulateTwoStepQuery(int n, int minLength, int maxLength){
		for (int i = 0; i < n; i++){
			BasicQuery query = simulation.twoStepSimulation(
				minLength, maxLength);
			System.err.println("Simulated query:");
			System.err.println("-----------------------------");
			query.dumpQuery();
			System.err.println("-----------------------------");
		}
	}
	/**
	 * The main method that starts the application.
	 * @param args The arguments.
	 */
	public static void main(String[] args) {
		AppQuerySimulation app = new AppQuerySimulation(Index.createIndex());
		// -sim -2q 200 2 5 or -sim -2q 200 4
		if (args[1].equalsIgnoreCase("-2q")){
			if (args.length == 4){
				int n = Integer.parseInt(args[2]);
				int ql = Integer.parseInt(args[3]);
				app.simulateTwoStepQuery(n, ql);
			}
			if (args.length == 5){
				int n = Integer.parseInt(args[2]);
				int minql = Integer.parseInt(args[3]);
				int maxql = Integer.parseInt(args[4]);
				app.simulateTwoStepQuery(n, minql, maxql);
			}
		}
	}
}
