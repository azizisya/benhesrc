/*
 * Created on 5 Nov 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.statistics;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;

public class CosineSimilarity {
	public static double cosine(TIntDoubleHashMap vMap1, TIntDoubleHashMap vMap2){
		// count the total number of keys
		TIntHashSet keySet = new TIntHashSet(vMap1.keys());
		keySet.addAll(vMap2.keys());
		int[] keys = keySet.toArray();
		int n = keys.length;
		// construct the two vectors in double arrays
		double[] v1 = new double[n];
		double[] v2 = new double[n];
		for (int i=0; i<n; i++){
			v1[i] = vMap1.get(keys[i]);
			v2[i] = vMap2.get(keys[i]);
		}
		// compute the cosine similarity
		return cosine(v1, v2);
	}
	
	/** See Finding Out About (FOA), pg 96 */
	public static double cosine(final double[] v1, final double[] v2)
    {
        final int length = v1.length;
        double total = 0; double t1=0; double t2=0;
        for(int i=0;i<length;i++)
        {
            if (v1[i] > 0)
                t1+= (v1[i] * v1[i]);
            if (v2[i] > 0)
            {
                t2+= (v2[i] * v2[i]);
                if (v1[i] > 0) /* and v2[i]>0 */
                    total += (v1[i] * v2[i]);// compute the inner product
            }
        }
        return total/(Math.sqrt(t1) * Math.sqrt(t2) );
    }
}
