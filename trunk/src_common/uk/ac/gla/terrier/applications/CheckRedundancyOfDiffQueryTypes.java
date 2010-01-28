/*
 * Created on 2006-5-25
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.util.StringTokenizer;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.statistics.Statistics;

public class CheckRedundancyOfDiffQueryTypes {
	
	Manager pipe = new Manager(Index.createIndex());

	public double getRedundancy(String process1, 
			String ignore1,
			String process2,
			String ignore2
			){
		double rate = 0;
		THashSet queryids = new THashSet();
		ApplicationSetup.setProperty("TrecQueryTags.process", process1);
		ApplicationSetup.setProperty("TrecQueryTags.skip", ignore1);
		TRECQuery queries = new TRECQuery();
		THashMap idTermMap1 = new THashMap();
		while (queries.hasMoreQueries()){
			String query = queries.nextQuery();
			String queryid = queries.getQueryId(); 
			queryids.add(queryid);
			StringTokenizer stk = new StringTokenizer(query);
			THashSet terms = new THashSet();
			while (stk.hasMoreTokens()){
				String term = pipe.pipelineTerm(stk.nextToken());
				if (term!=null)
					terms.add(term);
			}
			
			idTermMap1.put(queryid, terms);
		}
		
		ApplicationSetup.setProperty("TrecQueryTags.process", process2);
		ApplicationSetup.setProperty("TrecQueryTags.skip", ignore2);
		queries = new TRECQuery();
		THashMap idTermMap2 = new THashMap();
		while (queries.hasMoreQueries()){
			String query = queries.nextQuery();
			String queryid = queries.getQueryId(); 
			queryids.add(queryid);
			StringTokenizer stk = new StringTokenizer(query);
			THashSet terms = new THashSet();
			while (stk.hasMoreTokens()){
				String term = pipe.pipelineTerm(stk.nextToken());
				if (term!=null)
					terms.add(term);
			}
			idTermMap2.put(queryid, terms);
		}
		double[] rates = new double[queryids.size()];
		String[] ids = (String[])queryids.toArray(new String[queryids.size()]);
		for (int i=0; i<rates.length; i++){
			THashSet terms1 = (THashSet)idTermMap1.get(ids[i]);
			THashSet terms2 = (THashSet)idTermMap2.get(ids[i]);
			rates[i] = this.getRedundancyRate(terms1, terms2);
		}
		
		return Statistics.mean(rates);
	}
	
	private double getRedundancyRate(THashSet terms1, THashSet terms2){
		String[] str = (String[])terms1.toArray(new String[terms1.size()]);
		int counter = 0;
		for (int i=0; i<str.length; i++)
			if (terms2.contains(str[i]))
				counter++;
		return (double)counter/terms1.size();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String process1 = args[1];
		String ignore1 = args[2];
		String process2 = args[3];
		String ignore2 = "";
		if (args[4]!=null)
			ignore2 = args[4];
		CheckRedundancyOfDiffQueryTypes app = new CheckRedundancyOfDiffQueryTypes();
		double redun = app.getRedundancy(process1, ignore1, process2, ignore2);
		System.out.println("Average redundancy rate: "+redun);
	}

}
