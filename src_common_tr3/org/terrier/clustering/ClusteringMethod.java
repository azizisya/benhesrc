/*
 * Created on 2004-4-16
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.clustering;

import java.util.*;

import org.terrier.structures.CVector;
import org.terrier.utility.ApplicationSetup;



/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class ClusteringMethod {

	/** A Vector consists of several Vectors that each Vector is a cluster, 
     *  in which contains the elements of the cluster.
     */
    protected Vector clusters;
    
    /** A Vector contains the elements to be clustered. */
    protected CVector[] elements;    
    
    /** The threshold of the clustering process. Generally, 
     *  it is the number of clusters to be achieved.
     */
    protected int threshold;
    
    protected int numberOfClusters;
    
    protected int normalise = 2;
    
    protected double[] min;
    protected double[] max;
    
    public void initialise(){
    	this.threshold = (new Integer(ApplicationSetup.getProperty("clustering.threshold", "2"))).intValue();
    }

	/**
	* A default constructor that initialises the idf i attribute
	*/
	public ClusteringMethod() {
		this.initialise();
	}

	/**
	* Returns the name of the model.
	* Creation date: (19/06/2003 12:09:55)
	* @return java.lang.String
	*/
	public abstract String getInfo();
	
	public abstract void cluster();
	
	public int getNumberOfClusters(){
		return this.numberOfClusters;
	}

	public void setElements(CVector[] elements){
		this.elements = elements;
		this.min = new double[elements[0].getNumberOfDimensions()];
		this.max = new double[elements[0].getNumberOfDimensions()];
		double[] vector = elements[0].getVector();
		for (int i = 0; i < min.length; i++){
			min[i] = vector[i];
			max[i] = vector[i];
		}
		for (int i = 0; i < elements.length; i++){
			vector = elements[i].getVector();
			for (int j = 0; j < vector.length; j++){
				if (vector[j] > max[j])
					max[j] = vector[j];
				if (vector[j] < min[j])
					min[j] = vector[j];
			}
		}
	}
	
	public double distance(double[] data1, double[] data2){
		if (data1.length!=data2.length){
			return -1;
		}
		double distance = 0;
		for (int i=0; i<data1.length; i++){
			if (this.normalise == 0)
				distance += (data1[i] - data2[i])*(data1[i] - data2[i]);
			else if (normalise == 1)
				distance += (data1[i]/max[i] - data2[i]/max[i])
					*(data1[i]/max[i] - data2[i]/max[i]);
			else if (normalise == 2){
				double normaliser = max[i]-min[i];
				distance += Math.pow((data1[i]-min[i])/normaliser - 
						(data2[i]-min[i])/normaliser, 2);
			}
		}
		return Math.sqrt(distance);
	}
	
	protected int getElementIndex(String id){
		int index = -1;
		for (int i = 0; i < elements.length; i++)
			if (elements[i].getId().equals(id)){
				index = i;
				break;
			}
		return index;	
	}
	
	public int getClusterNo(String id){
		int index = this.getElementIndex(id);
		if (index < 0 || index >= this.elements.length)
			return -1;
		return this.elements[index].getClusterNo();
	}
	
	public void setThreshold(int threshold){
		this.threshold = threshold;
	}
	
	public abstract int closestCluster(CVector element);
	
	protected double leastDistance(Vector vec1, Vector vec2){
		double minDis = 100;
		for (int i=0; i<vec1.size(); i++){  
			CVector elm1 = this.elements[this.getElementIndex((String)vec1.get(i))];      
			double[] v1 = elm1.getVector();
			for (int j=0; j<vec2.size(); j++){
				CVector elm2 = this.elements[this.getElementIndex((String)vec2.get(j))];
				double[] v2 = elm2.getVector();
				double dis = distance(v1, v2);
				if (dis<minDis)
					minDis = dis;
			}
		}
		return minDis;
	}
	
	protected double leastDistance(double[] v1, Vector vec2){
		double minDis = 100;
		for (int j=0; j<vec2.size(); j++){
			CVector elm2 = this.elements[this.getElementIndex((String)vec2.get(j))];
			double[] v2 = elm2.getVector();
			double dis = distance(v1, v2);
			if (dis<minDis)
				minDis = dis;	
		}
		return minDis;
	}
	
	/**
	 * 
	 * @param vec A Vector of elements
	 * @return
	 */
	protected double[] centroid(Vector vec){
		double[] cent = null;
		int N = vec.size();
		double[] element = null;
		if (N != 0){
			element = (double[])vec.get(0);
		}
		cent = new double[element.length];
		Arrays.fill(cent, 0d);
		for (int i = 0; i < N; i++){
			element = (double[])vec.get(i);
			for (int j = 0; j < element.length; j++)
				cent[j] += element[j];
		}
		
		for (int i = 0; i < cent.length; i++)
			cent[i] /= N;
		
		return cent;
	}
	
	public double[] getCentroid(int clusterId){
		Vector vec = new Vector();
		Vector cluster = (Vector)clusters.get(clusterId);
		
		for (int i = 0; i < cluster.size(); i++){
			vec.addElement(elements[this.getElementIndex((String)cluster.get(i))].getVector());
		}
		
		return centroid(vec);
	}
	
	public double lossFunction(int clusterId){
		double loss = 0;
		CVector[] vectors = this.getVectorsInCluster(clusterId);
		for (int i = 0; i < vectors.length-1; i++)
			for (int j = i+1; j < vectors.length; j++)
				loss += this.EuclideanDistance(vectors[i], vectors[j]);
		/*if (vectors.length > 2)
			loss/=MathTools.combination(vectors.length, 2);
		else
			loss /= vectors.length;*/
		
		int factor = 0;
		for (int i = 1; i < vectors.length; i++)
			factor += i;
		loss /= factor;
		return loss;
		//return loss;
		//return loss / Math.pow(vectors.length, 2);
	}
	
	public double lossFunction(){
		double sum = 0;
		for (int i = 0; i < this.threshold; i++){
			double loss = lossFunction(i);
			System.out.println(loss);
			sum += loss;
		}
		return sum;
	}
	
	/**
	 * 
	 * @param clusterId
	 * @return 
	 */
	public CVector[] getVectorsInCluster(int clusterId){
		String[] ids = this.getIdsInCluster(clusterId);
		CVector[] vec = new CVector[ids.length];
		for (int i = 0; i < ids.length; i++){
			vec[i] = elements[this.getElementIndex(ids[i])];
		}
		return vec;
	}
	
	protected String[] getIdsInCluster(int clusterId){
		if (clusterId < 0 || clusterId >= this.numberOfClusters)
			return null;
		Vector vec = (Vector)clusters.get(clusterId);
		return (String[])vec.toArray(new String[vec.size()]);
	}
	
	private double EuclideanDistance(CVector element1, CVector element2){
		double[] vector1 = element1.getVector();
		double[] vector2 = element2.getVector();
		double sum = 0;
		for (int i = 0; i < vector1.length; i++)
			sum += Math.pow(vector1[i] - vector2[i], 2);
		return sum;
	}
		
}
