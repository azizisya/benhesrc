/*
 * Created on 2006-5-2
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;

import java.util.StringTokenizer;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.TRECQuery;

public class PrintAverageQueryLength {
	
	public double getAverageQueryLength(){
		TRECQuery queries = new TRECQuery();
		Manager pipe = new Manager(Index.createIndex());
	 	double[] ql = new double[queries.getNumberOfQueries()];
	 	int counter = 0;
	 	while (queries.hasMoreQueries()){
	 		String query = queries.nextQuery();
	 		StringTokenizer stk = new StringTokenizer(query);
	 		THashSet terms = new THashSet();
	 		while (stk.hasMoreTokens()){
	 			String term = stk.nextToken();
	 			term = pipe.pipelineTerm(term);
	 			if (term != null){
	 				if (terms.size()>0){
	 					if (!terms.contains(term))
	 						terms.add(term);
	 				}
	 				else
	 					terms.add(term);
	 			}
	 		}
	 		int length = terms.size();
	 		System.out.println(queries.getQueryId() + ": " + length);
	 		ql[counter++] = length;
	 	}
	 	return Statistics.mean(ql);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PrintAverageQueryLength app = new PrintAverageQueryLength();
		System.out.println("Average query length: " + app.getAverageQueryLength());
	}

}
