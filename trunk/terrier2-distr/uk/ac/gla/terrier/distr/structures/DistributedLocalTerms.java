/*
 * Created on 2005-7-18
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.distr.structures;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

/**
 * This class has been obseleted by LocalMatchingQueryTerms.
 * @author ben
 *
 */
public class DistributedLocalTerms{
	public TIntHashSet termids;
	
	public TIntIntHashMap mapTerms;
	
	int index = -1;
	
	public DistributedLocalTerms(){
		termids = new TIntHashSet();
		mapTerms = new TIntIntHashMap();
	}
	
	public boolean hasMoreTerms(){
		boolean flag = false;
		if (index < termids.size()-1)
			flag = true;
		return flag;
	}
	
	public int size(){
		return termids.size();
	}
	
	public boolean consistencyCheck(){
		boolean flag = false;
		if (termids.size() == mapTerms.size())
			flag = true;
		return flag;
	}
	
	public void reset(){
		index = -1;
	}
	
	public int nextTermid(){
		return ((int[])termids.toArray())[++index];
	}
	
	public int getFrequency(){
		return mapTerms.get((termids.toArray())[index]);
	}
	
	public void insertTerm(int id, int tf){
		if (termids.contains(id)){
			int newtf = tf+mapTerms.get(id);
			mapTerms.remove(id);
			mapTerms.put(id, newtf);
		}
		else{
			termids.add(id);
			mapTerms.put(id, tf);
		}
	}
}
