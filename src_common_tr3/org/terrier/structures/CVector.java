/*
 * Created on 2004-4-17
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.structures;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CVector {
	protected double[] vector;
	protected String id;
	protected int clusterNo;
	public CVector(double[] vector, String id){
		this.vector = vector;
		this.id = id;
		this.clusterNo = -1;
	}
	
	public double[] getVector(){
		return vector;
	}
	
	public String getId(){
		return this.id;
	}

	public int getNumberOfDimensions(){
		return this.vector.length;
	}
	
	public void setClusterNo (int clusterNo){
		this.clusterNo = clusterNo;
	}
	
	public int getClusterNo(){
		return this.clusterNo;
	}
	
	public boolean setVector(double[] vector){
		if (vector.length != this.vector.length){
			return false;
		}
		this.vector = vector;
		return true;
	}
}
