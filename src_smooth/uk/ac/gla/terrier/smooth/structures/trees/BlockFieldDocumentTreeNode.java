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
 * The Original Code is BlockFieldDocumentTreeNode.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Johnson <johnsoda{a.}dcs.gla.ac.uk> (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.structures.trees;
import java.util.HashSet;
import gnu.trove.TIntHashSet;
import java.util.Arrays;

/**
 * Models the tree node used when building the direct index
 * with block and field information.
 * @author Douglas Johnson, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class BlockFieldDocumentTreeNode extends FieldDocumentTreeNode {
	/** The left child of the node.*/
	public BlockFieldDocumentTreeNode left = null;
	/** The right child of the node.*/
	public BlockFieldDocumentTreeNode right = null;
	/** Hashset of blockids */
	final public TIntHashSet BlockIds = new TIntHashSet();
	
	
	/**
	 * Constructs a tree node for the given term and block id.
	 * @param newTerm the term.
	 * @param blockId the block id.
	 */
	public BlockFieldDocumentTreeNode(String newTerm, int blockId) {
		super(newTerm);
		BlockIds.add(blockId);
		
	}
	/**
	 * Constructs a tree node for the given term, block id and the 
	 * field.
	 * @param newTerm the term.
	 * @param blockId the block id.
	 * @param field the field containing the term.
	 */
	public BlockFieldDocumentTreeNode(String newTerm, int blockId, String field) {
		super(newTerm, field);
		BlockIds.add(blockId);
	}
	
	/**
	 * Constructs a tree node for the given term, block id and the 
	 * html tag.
	 * @param newTerm the term.
	 * @param blockId the block id.
	 * @param fields the fields that contain the term.
	 */
	public BlockFieldDocumentTreeNode(String newTerm, int blockId, HashSet fields) {
		super(newTerm, fields);
		BlockIds.add(blockId);
	}

	/** 
	 * Adds a block to the hashset of blocks already saved for this node
	 * @param blockId The id of the new block
	 * @return True if the blockid hasn't been seen before, false if we already had something at that block
	 */
	public boolean insertBlock(int blockId)
	{
		return BlockIds.add(blockId);
	}

	/**
	 * Returns an array of the blocksids stored in this object. 
	 * Sorted using Arrays.sort()
	 * @return integer array of sorted ascending blockids
	 */
	public int[] getBlockIds()
	{
		final int[] blocks = BlockIds.toArray();
		Arrays.sort(blocks);
		return blocks;
	}

	/** 
	 * Returns how many separate blocks does this term occur in. This should be
	 * the same as the term frequency if the block size == 1.
	 * @return integer of how many different blocks this terms occurs in.
	 */
	public int getBlockCount()
	{
		return BlockIds.size();
	}
}
