package uk.ac.gla.terrier.structures.trees.bplustree;

import java.util.LinkedList;
import java.util.ListIterator;

public class BplusNodeCache {

	protected static int maxCacheSize = 100;
	
	protected LinkedList queue;
	
	public BplusNodeCache()
	{
		queue = new LinkedList();
	}
	
	public BplusNodeCache(int cacheSize)
	{
		this();
		maxCacheSize = cacheSize;
	}
	
	public void add(BplusDiskLeafNode n)
	{
		if(!queue.contains(n))
		{
			queue.addFirst(n);
			if(queue.size() > maxCacheSize)
			{
				BplusDiskLeafNode old = (BplusDiskLeafNode)queue.removeLast();
				old.deactivate();
			}
		}
		else
		{
			queue.remove(n);
			queue.addFirst(n);
		}
		
		
	}
	
	public boolean contains(BplusKey key)
	{
		ListIterator iter = queue.listIterator();
		int count = 0;
		while(iter.hasNext())
		{
			System.out.println(count+" "+queue.size());
			if(((BplusLeafNode)iter.next()).contains(key))
				return true;
			
			count++;
		}
		return false;
	}
	
	public BplusLeafNode get(BplusKey key)
	{
		ListIterator iter = queue.listIterator();
		BplusLeafNode temp;
		while(iter.hasNext())
		{
			temp = (BplusLeafNode)iter.next();
			if(temp.contains(key))
				return temp;
		}
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
