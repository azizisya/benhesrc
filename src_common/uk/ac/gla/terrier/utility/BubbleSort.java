/*
 * Created on 2005-1-7
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.utility;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BubbleSort {
	public static int[] sort(double[] x){
		int[] rank = new int[x.length];
		int[] id = new int[x.length];
		for (int i = 0; i < x.length; i++)
			id[i] = i;
		for (int i = 0; i < x.length - 1; i ++){
			for (int j = i + 1; j < x.length; j++){
				if (x[i] > x[j]){
					double temp = x[i];
					x[i] = x[j];
					x[j] = temp;
					int tempId = id[i];
					id[i] = id[j];
					id[j] = tempId;
				}
			}
		}
		for (int i = 0; i < id.length; i++){
			rank[id[i]] = i;
		}
//		for (int i = 0; i < id.length; i++){
//			System.out.println(i + ":   " + id[i] + "   " + x[i]);
//		}
		return rank;
	}
	
	public static int[] getOrder(double[] x){
		//int[] order = new int[x.length];
		int[] id = new int[x.length];
		for (int i = 0; i < x.length; i++)
			id[i] = i;
		for (int i = 0; i < x.length - 1; i ++){
			for (int j = i + 1; j < x.length; j++){
				if (x[i] > x[j]){
					double temp = x[i];
					x[i] = x[j];
					x[j] = temp;
					int tempId = id[i];
					id[i] = id[j];
					id[j] = tempId;
				}
			}
		}
//		for (int i = 0; i < id.length; i++){
//			System.out.println(i + ":   " + id[i] + "   " + x[i]);
//		}
		return id;
	}
	
	public static int[] sort(int[] x){
		int[] rank = new int[x.length];
		int[] id = new int[x.length];
		for (int i = 0; i < x.length; i++)
			id[i] = i;
		for (int i = 0; i < x.length - 1; i ++){
			for (int j = i + 1; j < x.length; j++){
				if (x[i] > x[j]){
					int temp = x[i];
					x[i] = x[j];
					x[j] = temp;
					int tempId = id[i];
					id[i] = id[j];
					id[j] = tempId;
				}
			}
		}
		for (int i = 0; i < id.length; i++){
			rank[id[i]] = i;
		}
//		for (int i = 0; i < id.length; i++){
//			System.out.println(i + ":   " + id[i] + "   " + x[i]);
//		}
		return rank;
	}
}
