package uk.ac.gla.terrier.structures.trees.bplustree;

public interface BplusInnerNode extends BplusNode{

	public abstract InsertResult insert(BplusKey k, BplusValue v);

	public abstract void nonfullInsert(InsertResult ir);

	public abstract BplusNode getChild(BplusKey k);
	
	public abstract BplusNode[] getChildren();

	public abstract boolean isLeaf();

	public abstract String toString();
	
	public abstract int getCurrentNumberOfKeys();
	
	public abstract void setPenultimate(boolean p);

}