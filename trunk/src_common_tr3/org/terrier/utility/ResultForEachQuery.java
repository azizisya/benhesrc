/*
 * Created on 2005-1-3
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.terrier.utility.ApplicationSetup;

import uk.ac.gla.terrier.statistics.Statistics;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ResultForEachQuery {
	protected String[] queryids;
	protected double[] averagePrecision;
	public String model;
	public double c;
	public String qemodel;
	public boolean freeqe;
	public int index = -1;
	public int numberOfQueries;
	public double meanAveragePrecision;
	
	public ResultForEachQuery(String filename){
		String str = null;
		if (filename.lastIndexOf('/') > 0)
			str = filename.substring(filename.lastIndexOf('/')+1, filename.length());
		else
			str = filename;
		// should obtain information for query expansion
		
		// obtain information for single-pass retrieval
		model = str.substring(0, str.indexOf('c'));
		c = Double.parseDouble(str.substring(str.indexOf('c')+1, str.indexOf('_', str.indexOf('c')+1)));
		
		this.loadResultFile(filename);
		this.numberOfQueries = this.queryids.length;
		this.meanAveragePrecision = Statistics.mean(averagePrecision);
	}
	
	public ResultForEachQuery(File f){
		String filename = f.getPath();
		String str = null;
		if (filename.lastIndexOf('/') > 0)
			str = filename.substring(filename.lastIndexOf('/')+1, filename.length());
		else
			str = filename;
		System.out.println("filename: " + str);
		// should obtain information for query expansion
		
		// obtain information for single-pass retrieval
		model = str.substring(0, str.indexOf('c'));
		c = Double.parseDouble(str.substring(str.indexOf('c')+1, str.indexOf('_', str.indexOf('c')+1)));
		
		this.loadResultFile(f);
		this.numberOfQueries = this.queryids.length;
		this.meanAveragePrecision = Statistics.mean(averagePrecision);
	}
	
	protected void loadResultFile(File f){
		try{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = null;
			Vector vecQueryids = new Vector();
			Vector vecAPs = new Vector();
			while((str=br.readLine())!=null){
				if (str.trim().length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				vecQueryids.addElement(stk.nextToken());
				vecAPs.addElement(stk.nextToken());
			}
			br.close();
			this.queryids = new String[vecQueryids.size()];
			this.averagePrecision = new double[vecAPs.size()];
			for (int i = 0; i < queryids.length; i++){
				queryids[i] = (String)vecQueryids.get(i);
				averagePrecision[i] = Double.parseDouble((String)vecAPs.get(i));
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	// the result file should be an .apq file.
	protected void loadResultFile(String filename){
		if (filename.lastIndexOf('/') < 0)
			filename = ApplicationSetup.TREC_RESULTS.concat("/"+filename);
		File f = new File(filename);
		this.loadResultFile(f);
	}
	
	public String nextQueryid(){
		return queryids[++index];
	}
	
	public double getAveragePrecision(){
		return averagePrecision[index];
	}
	
	public double getAveragePrecision(String queryid){
		for (int i = 0; i < queryids.length; i++)
			if (queryids[i].equals(queryid))
				return averagePrecision[i];
		return -1d;
	}
	
	public void reset(){
		index = -1;
	}
	
	public boolean hasMoreQueries(){
		if (index >= this.numberOfQueries-1 || index < -1)
			return false;
		return true;
	}
	
	public String[] getQueryids(){
		return (String[])queryids.clone();
	}
	
	public double[] getAveragePrecisions(){
		return (double[])averagePrecision.clone();
	}
}
