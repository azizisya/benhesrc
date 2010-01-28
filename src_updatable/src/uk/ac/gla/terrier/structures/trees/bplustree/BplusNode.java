package uk.ac.gla.terrier.structures.trees.bplustree;

import uk.ac.gla.terrier.structures.FilePosition;

/**
 * Interface describing the properties that all B+ nodes share.
 * They must provide methods to allow insertion and ways of verifying
 * their status as inner or leaf nodes.
 * @author John Kane
 */
public interface BplusNode {
	
	public abstract InsertResult insert(BplusKey k, BplusValue v);
	
	public abstract boolean isLeaf();
	
	public abstract FilePosition close();

}
