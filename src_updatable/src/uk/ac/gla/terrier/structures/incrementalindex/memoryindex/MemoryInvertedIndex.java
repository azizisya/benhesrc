package uk.ac.gla.terrier.structures.incrementalindex.memoryindex;


import java.util.Iterator;
import java.util.TreeMap;

import uk.ac.gla.terrier.compression.*;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.merging.*;
import uk.ac.gla.terrier.sorting.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import gnu.trove.*;
import gnu.trove.decorator.TIntObjectHashMapDecorator;

public class MemoryInvertedIndex extends InvertedIndex {

	//operation on this index
		//insert new term with postings
	
	//operations on each posting list:
		//insert new document
			//find appropriate place in ArrayLists
			//insert Docid, TF at that place
		//delete document
			//find appropriate place in ArrayLists
			//delete that element from each array

	//Each term postings is:
		//2 element array
			//each of which contains a TIntArrayList
			//TIntArrayList of docids
			//TIntArrayList of tfs
	
	/**
	 * stores a matching between the termid and postings list,
	 *  a linked list of doc descriptors
	 */
	private TIntObjectHashMap postingsLists = null;
	
	public class postingList{
		TIntArrayList[] rec;
		//docids 0
		//tfs    1
		public postingList()
		{
			rec = new TIntArrayList[2];
			rec[0] = new TIntArrayList();
			rec[1] = new TIntArrayList();
		}
		
		/**
		 * Adds a doc to the postings list, if one already exists
		 * it overwrites it
		 * @param docid
		 * @param tfs
		 */
		public void addPosting(int docid, int tfs)
		{

			rec[0].add(docid+1);/*NOTE: the 1 is added to correct for ununderstood
					behaviour in the method that writes the list to file*/
			rec[1].add(tfs);
		}
		
		public boolean removePosting(int docid)
		{
			int pos;
			if((pos = rec[0].binarySearch(docid)) > 0)
			{
				rec[0].remove(pos);
				rec[1].remove(pos);
				return true;
			}
			return false;
		}
		
		public boolean isEmpty()
		{
			return rec[0].isEmpty();
		}
		
		public TIntArrayList[] toIntArrayListArray()
		{
			return rec;
		}
		
		public FilePosition flush(BitOutputStream bf)
		{
			int[] tempArray1, tempArray2;
			tempArray1 = rec[0].toNativeArray();
			tempArray2 = rec[1].toNativeArray();
			SortAscendingPairedVectors.sort(tempArray1,tempArray2);
			int[][] tempArray = new int[2][];
			tempArray[0] = tempArray1;
			tempArray[1] = tempArray2;
			
			try{
				StructureMerger.writeNoFieldPostings(tempArray,tempArray[0][0]+1,bf);
				
				long endByte = bf.getByteOffset();
				byte endBit = bf.getBitOffset();
				endBit--;
				
				if (endBit < 0 && endByte > 0) {
					endBit = 7;
					endByte--;
				}
				return (new FilePosition(endByte,endBit));
				}
			catch(Exception ioe){System.out.println("Failed while writing inverted index to disk from memory: ");ioe.printStackTrace();
			
			return null;
		}
			
		}
		
		public String toString()
		{
			String output = "";
			for(int i = 0; i < rec[0].size(); i++)
				output+="("+rec[0].get(i)+", "+rec[1].get(i)+") ";
			
			return output;
		}
	}
	
	public MemoryInvertedIndex()
	{
		postingsLists = new TIntObjectHashMap();
	}
	


	/**
	 * Returns a two dimensional array containing the document ids, term
	 * frequencies and field scores for the given documents. 	  
	 * @return int[][] the two dimensional [3][n] array containing the n 
	 *         document identifiers, frequencies and field scores.
	 * @param termid the identifier of the term whose documents we are looking for.
	 */
	public int[][] getDocuments(int termid) {
		
		postingList postings = (postingList)postingsLists.get(termid);
		
		//term id doesnot exist
		if (postings == null)
			return null;
		
		TIntArrayList[] tempPostings = postings.toIntArrayListArray();
		
		int[][] rtrPostings = new int[2][];
		rtrPostings[0] = tempPostings[0].toNativeArray();
		rtrPostings[1] = tempPostings[1].toNativeArray();
		
		//to balance for 1 that is added in the posting constructor
		for(int i = 0; i < rtrPostings[0].length; i++)
			rtrPostings[0][i]--;
		
		return rtrPostings;
	}

	public postingList getPostings(int termid)
	{
		return (postingList)postingsLists.get(termid);
	}
	/**
	 * Writes the inverted index to disk as a file
	 * then deallocates the inverted index.
	 */
	public void close() {
		postingsLists = null;
	}
	
	
	

	
	/**
	 * Updates the entry with fresh docs
	 * @param termid
	 * @param docid
	 */
	public void addDocForTerm(int termid, int docno, int tfs)
	{
		postingList pl;
		if((pl = (postingList)postingsLists.get(termid)) == null)
		{
			pl = new postingList();
			postingsLists.put(termid,pl);
		}
	
		pl.addPosting(docno,tfs);
	}
	
	/**
	 * Removes a document from the term's entry
	 * @param termid
	 * @param docid
	 */
	public void deleteDocForTerm(int termid, int docid)
	{
		postingList pl;
		if((pl = (postingList)postingsLists.get(termid)) != null)
		{
			pl.removePosting(docid);
			if (pl.isEmpty())
				deleteEntry(termid);
		}
	}
	
	/**
	 * Deletes the given term's entry from the inverted
	 * index.
	 * @param termid
	 */
	public void deleteEntry(int termid)
	{
		postingsLists.remove(termid);
	}
	
	/**
	 * Prints out the inverted index in conformance with
	 * the inverted index file format.
	 */
	
	class printProcedure implements TIntObjectProcedure{
		
		public boolean execute(int a, Object b)
		{
			System.out.println((postingList)b);
			
			return true;
		}
		
	}

	public void print()
	{

		System.out.println("Size of postingsLists:"+postingsLists.size());
		postingsLists.forEachEntry(new printProcedure());
		//TIntObjectIterator iter = new TIntObjectIterator(postingsLists);
		
		/*while(iter.hasNext())
		{
			iter.advance();
			postingList list = (postingList)iter.value();
			System.out.println(list);	
		}*/

	}
	
	/**
	 * Writes the Inverted Index to file. Then clears its internal
	 * datastructures so its effectively new.
	 *@param int the segment number of the segment that the file 
	 *should be written to.
	 */
	
	public FilePosition[] flush(final BitOutputStream bitfile) {
		TIntObjectHashMapDecorator d = new TIntObjectHashMapDecorator(postingsLists);
		TreeMap tm = new TreeMap(d);
		FilePosition[] offsets = new FilePosition[postingsLists.size()];		
		Iterator iter = tm.values().iterator();
		int count = 0;
		
		
		try{
		
			postingList temp;
			while(iter.hasNext())
			{
				temp = (postingList)iter.next();
				offsets[count] = temp.flush(bitfile);
			
				count++;
			}
			bitfile.close();
		
			postingsLists = new TIntObjectHashMap();
		}catch(Exception ioe){
			System.err.println("Indexing failed to write an Inverted index to disk : "+ioe);
			System.exit(1);}
		return offsets;
	}

}
