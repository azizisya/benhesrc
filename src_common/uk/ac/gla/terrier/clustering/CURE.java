/*
 * Created on 2004-4-16
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.clustering;
import java.util.*;

import uk.ac.gla.terrier.structures.CVector;
;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CURE extends ClusteringMethod{
	
	protected double[] normaliser;
	
	public String getInfo(){
		return "CURE" + this.threshold;
	}
	
	public void cluster(){
		// intialise the normalisers
		int numDimension = elements[0].getNumberOfDimensions();
		normaliser = new double[numDimension];
		Arrays.fill(normaliser, 0d);
		for (int i = 0; i < elements.length; i++){
			double[] vec = elements[i].getVector();
			for (int j = 0; j < numDimension; j++){
				if (normaliser[j] < vec[j])
					normaliser[j] = vec[j];
			}
		}
		for (int i = 0; i < elements.length; i++){
			double[] vec = elements[i].getVector();
			for (int j = 0; j < numDimension; j++)
				vec[j] /= normaliser[j];
			elements[i].setVector(vec);
		}
		
		// at the beginning, each vector is a cluster
		clusters = new Vector();
		for (int i = 0; i < elements.length; i++){
			Vector vec = new Vector();
			vec.addElement((String)elements[i].getId());
			clusters.addElement(vec);
		}
		
		boolean flag = true;
		while (flag){
			flag = false;
			for (int i=0; i<clusters.size()-1; i++){
				Vector vecTemp = (Vector)clusters.get(i);
				Vector vec1 = (Vector)vecTemp.clone();
				double minDis = 100;
				double maxSim = 0;
				int pos = -1;
				for (int j=i+1; j<clusters.size(); j++){
					vecTemp = (Vector)clusters.get(j);
					Vector vec2 = (Vector)vecTemp.clone();
                        
					double dis = leastDistance(vec1, vec2);
					if (minDis>dis){
						minDis=dis;
						pos=j;
					}
				}

				Vector vec2 = (Vector)clusters.get(pos);
                
				if(!clusters.removeElement(vec1)|!clusters.removeElement(vec2))
					System.out.println("WARNING: element not removed");
				for (int t=0; t<vec2.size(); t++)
					vec1.addElement((String)vec2.get(t));
                				
				clusters.addElement(vec1);
				flag = true;
				break;
			}
			if (clusters.size()<=threshold)
				break;
		}// end of while
		
		for (int i = 0; i < clusters.size(); i++){
			Vector vec = (Vector)clusters.get(i);
			for (int j = 0; j < vec.size(); j++){
				String id = (String)vec.get(j);
				this.elements[this.getElementIndex(id)].setClusterNo(i);
			}
		}
		
		this.numberOfClusters = clusters.size();
		
	}
	
	public int closestCluster(CVector element){
		int index = this.getElementIndex(element.getId());
		if (index != -1){
			return elements[index].getClusterNo();
		}
		
		double[] v1 = element.getVector();
		for (int i = 0; i < v1.length; i++)
			v1[i] /= normaliser[i];
		double minDis = 100;
		int pos = -1;
		for (int i = 0; i < this.numberOfClusters; i++){
			double dis = this.leastDistance(v1, (Vector)clusters.get(i));
			if (minDis > dis){
				minDis = dis;
				pos = i;
			}
		}
		return pos;
	}
	
}
