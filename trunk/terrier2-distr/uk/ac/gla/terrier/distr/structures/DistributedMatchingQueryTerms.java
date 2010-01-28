 /*
  * Terrier - Terabyte Retriever
  * Webpage: http://ir.dcs.gla.ac.uk/terrier
  * Contact: terrier{a.}dcs.gla.ac.uk
  * University of Glasgow - Department of Computing Science
  * Information Retrieval Group
  *
  * The contents of this file are subject to the Mozilla Public License
  * Version 1.1 (the "License"); you may not use this file except in
  * compliance with the License. You may obtain a copy of the License at
  * http://www.mozilla.org/MPL/
  *
  * Software distributed under the License is distributed on an "AS IS"
  * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
  * the License for the specific language governing rights and limitations
  * under the License.
  *
  * The Original Code is DistributedThreeManager.java.
  *
  * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
  * All Rights Reserved.
  *
  * Contributor(s):
  *   Ben He <ben{a.}dcs.gla.ac.uk> (original author)
  *   Craig McDonald <craigm{a.}dcs.gla.ac.uk>
  *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
  */
package uk.ac.gla.terrier.distr.structures;

import java.io.Serializable;
import java.util.Arrays;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;

/**
 * This class extends the MatchingQueryTerms to fit into a distributed
 * setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.6 $
 */
public class DistributedMatchingQueryTerms extends MatchingQueryTerms implements Serializable {
	/** Document frequency in different fields. 
	 * Nt[i][j] is the document frequency of the jth term in the ith field.
	 * */
	public double[][] Nt;
	/** In-collection term frequency in different fields. */
	public double[][] TF;
	/** The term frequency in the whole collection. It is the sum of the
	 *  term frequency in the three fields. 
	 */
	private static final long serialVersionUID = 200603101234L;
	public double[] totalTF;
	/** The document frequency in the whole collection. */
	public double[] totalNt;
	/** The id the query. */
	public String queryid;
	
	public double[] parameterValues;
	
	public boolean SIMPLE_NORMALISATION;
	
	public double[] fieldWeights;
	
	protected int numberOfFields;
	
	/**
	 * Insert the statistics of the query from a subcollection.
	 * @param entry There should not be overlap between the input entry
	 * and the instance itself.
	 */
	public void addEntry(LocalMatchingQueryTerms entry){
		int queryLength = length();
		for (int i = 0; i < queryLength; i++){
			for (int j=0; j<numberOfFields; j++){
				Nt[j][i] += entry.Nt[j][i];
				TF[j][i] += entry.TF[j][i];
			}
			this.totalNt[i] += entry.totalNt[i];
			this.totalTF[i] += entry.totalTF[i];
		}
	}
	
	public String getKey(){
		String key = queryid;
		String[] termStrings = this.getTerms();
		for (int i=0; i<this.length(); i++){
			key += termStrings[i];
			key += this.getTermWeight(termStrings[i]);
		}
		for (int i=0; i<this.parameterValues.length;i++){
			key += parameterValues[i];
		}
		for (int i=0; i<this.fieldWeights.length; i++){
			key += this.fieldWeights[i];
		}
		return key;
	}
	
	public void setNormStrategy(boolean value){
		this.SIMPLE_NORMALISATION = value;
	}
	
	public void setParameterValues(double[] values){
		this.parameterValues = values;
	}
	
	public void setFieldWeights(double[] weights){
		this.fieldWeights = weights;
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
	
	public DistributedMatchingQueryTerms(String queryid, int numberOfFields){
		super();
		this.queryid = queryid;
		this.numberOfFields = numberOfFields;
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
