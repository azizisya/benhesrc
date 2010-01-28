/*
 * Created on 2005-7-18
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.distr.structures;

import gnu.trove.THashSet;
import gnu.trove.TObjectIntHashMap;

import java.io.Serializable;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DistributedTerms implements Serializable{
	public THashSet terms;
	
	public TObjectIntHashMap mapTerms;
	
	int index = -1;
	
	public DistributedTerms(){
		terms = new THashSet();
		mapTerms = new TObjectIntHashMap();
	}
	
	public void setTerms(THashSet terms, TObjectIntHashMap mapTerms){
		this.terms = terms;
		this.mapTerms = mapTerms;
		index = -1;
	}
	
	public boolean hasMoreTerms(){
		boolean flag = false;
		if (index < terms.size()-1)
			flag = true;
		return flag;
	}
	
	public int size(){
		return terms.size();
	}
	
	public boolean consistencyCheck(){
		boolean flag = false;
		if (terms.size() == mapTerms.size())
			flag = true;
		return flag;
	}
	
	public void reset(){
		index = -1;
	}
	
	public String nextTerm(){
		return ((String[])terms.toArray(new String[terms.size()]))[++index];
	}
	
	public int getFrequency(){
		return mapTerms.get((terms.toArray())[index]);
	}
	
	public void insertTerm(String term, int tf){
		if (terms.contains(term)){
			int newtf = tf+mapTerms.get(term);
			mapTerms.remove(term);
			mapTerms.put(term, newtf);
		}
		else{
			terms.add(term);
			mapTerms.put(term, tf);
		}
	}
}
