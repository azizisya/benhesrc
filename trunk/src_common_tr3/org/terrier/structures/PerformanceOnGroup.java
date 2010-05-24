/*
 * Created on 2005-1-7
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.structures;

import uk.ac.gla.terrier.statistics.Statistics;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PerformanceOnGroup {
	public String model;
	
	public double c;
	
	public String qemodel = null;
	
	public boolean parameter_free_qe = true;
	
	public String groupid;
	
	public String[] queryids;
	
	public double[] averagePrecisions;
	
	public double meanAveragePrecision;
	
	public PerformanceOnGroup(String model, double c, String[] queryids, 
			double[] averagePrecisions){
		this.model = model;
		this.c = c;
		this.queryids = (String[])queryids.clone();
		this.averagePrecisions = (double[])averagePrecisions.clone();
		this.meanAveragePrecision = Statistics.mean(this.averagePrecisions);
	}
}
