package uk.ac.gla.terrier.distr.structures;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class LocalMatchingQueryTerms implements Serializable {
	public double[][] Nt;
	public double[][] TF;
	public double[] totalTF;
	public double[] totalNt;
	
	public String[] queryTerms;
	
	protected int numberOfFields;
	
	private static final long serialVersionUID = 200603101235L;

	public String queryid;
	
	public LocalMatchingQueryTerms(String queryid, String[] queryTerms, int numberOfFields){
		this.queryid = queryid;
		this.queryTerms = queryTerms;
		this.numberOfFields = numberOfFields;
		this.initialise();
	}
	
	/**
	 * 
	 * @param entry There should not be overlap between the input entry
	 * and the instance itself.
	 */
	public void addEntry(LocalMatchingQueryTerms entry){
		int queryLength = Nt[0].length;
		int numberOfFields = Nt.length; 
		for (int i = 0; i < numberOfFields; i++){
			for (int j=0; j< queryLength; j++){
				Nt[i][j] += entry.Nt[i][j];
				TF[i][j] += entry.TF[i][j];
				totalNt[j] += entry.totalNt[j];
				totalTF[j] += entry.totalTF[j];
			}
		}
	}
	
	public int length(){
		return queryTerms.length;
	}
	
	/**
	 * 
	 * @param entry There should not be overlap between the input entry
	 * and the instance itself.
	 */
	public void addEntries(LocalMatchingQueryTerms[] entries){
		for (int t = 0; t < entries.length; t++)
			this.addEntry(entries[t]);
	}
	
	public LocalMatchingQueryTerms(String queryid){
		super();
		this.queryid = queryid;		
		this.initialise();
	}
	
	public void setQueryid(String queryid){
		this.queryid = queryid;		
		this.initialise();
	}
	
	public void initialise(){
		Nt = new double[numberOfFields][length()];
		TF = new double[numberOfFields][length()];
		totalTF = new double[length()];
		totalNt = new double[length()];
		for (int i=0; i<numberOfFields;i++){
			Arrays.fill(Nt[i], 0d);
			Arrays.fill(TF[i], 0d);
		}
		Arrays.fill(totalNt, 0d);
		Arrays.fill(totalTF, 0d);
	}
	
	public void setQueryGlobalStatistics(
			double[][]Nt,
			double[][]TF,
			double[] totalTF,
			double[] totalNt){
		this.Nt = Nt;
		this.totalNt = totalNt;
		this.TF = TF;
		this.totalTF = totalTF;
	}
}