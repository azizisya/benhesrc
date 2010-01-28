/**
 * 
 */
package uk.ac.gla.terrier.structures.incrementalindex.memoryindex;


import java.util.*;

import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.trees.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.compression.*;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.decorator.*;

import uk.ac.gla.terrier.sorting.*;

/**
 * @author kanej
 *
 */
public class MemoryDirectIndex extends DirectIndex {

	protected TIntObjectHashMap termOccurences = null;
		
	class termList{
		TIntArrayList[] rec;
		//term ids      0
		// frequencies  1
		// field scores 2
		public termList()
		{
			rec = new TIntArrayList[3];
			rec[0] = new TIntArrayList();
			rec[1] = new TIntArrayList();
			rec[2] = new TIntArrayList();
		}
		

		public void addTerm(int termid, int freq, int fieldScore)
		{
			rec[0].add(termid);
			rec[1].add(freq);
			rec[2].add(fieldScore);
		}
		
		public boolean removeTerm(int termid)
		{
			int pos;
			if((pos = rec[0].binarySearch(termid)) > 0)
			{
				rec[0].remove(pos);
				rec[1].remove(pos);
				rec[2].remove(pos);
				return true;
			}
			return false;
		}
		
		public boolean isEmpty()
		{
			return rec[0].isEmpty();
		}
		
		public int size()
		{
			return rec[0].size();
		}
		
		public void flush(BitOutputStream bos)
		{
			//TODO Instead of toNativeArray, retreive a set
			//and then its iterator for rec[0], then manipulate
			//rec[1] appropriately.
			int[] termids = rec[0].toNativeArray();
			int[] termfreq = rec[1].toNativeArray();
			SortAscendingPairedVectors.sort(termids,termfreq);

			//System.out.println("New Doc:");
			
			try{
			final int TermsCount = rec[0].size();
			
			//TreeNode treeNode1 = terms[0];
			/* write the first entry to the DirectIndex */
			int termCode = termids[0];
			bos.writeGamma(termCode + 1);
			bos.writeUnary(termfreq[0]);
			int prevTermCode = termCode;
			
			if (TermsCount > 1) {
				for (int termNo = 1; termNo < TermsCount; termNo++) {
					//treeNode1 = terms[termNo];
					termCode = termids[termNo];
					bos.writeGamma(termCode - prevTermCode);
					bos.writeUnary(termfreq[termNo]);
					prevTermCode = termCode;
					//System.out.print(termids[termNo]);
				}
			}	
			
			//bos.flush();
			//bos.writeFlush();
			//bos.writeReset();
			
			//System.out.println(endByte+" "+endBit);
			}catch(Exception ioe){
				ioe.printStackTrace();
			}
		}
		
		public String toString()
		{
			String output = "Num of enries: "+rec[0].size()+"\n";
			for(int i = 0; i < rec[0].size(); i++)
				output+="("+rec[0].get(i)+", "+rec[1].get(i)+/*", "+rec[2]+*/") ";
			
			return output;
		}
	}
	
	public MemoryDirectIndex()
	{
		termOccurences = new TIntObjectHashMap();
	}
	
	/**
	 * Returns a two dimensional array containing the 
	 * term ids and the term frequencies for 
	 * the given document. 
	 * @return int[][] the two dimensional [n][3] array 
	 * 		   containing the term ids, frequencies and field scores. If
	 *         the given document identifier is not found in the document
	 *         index, then the method returns null. If fields are not used, 
	 *         then the dimension of the returned array are [n][2].
	 * @param docid the document identifier of the document which terms 
	 * 		  we retrieve.
	 */
	public int[][] getTerms(int docid) {
		TIntArrayList[] occurrences = (TIntArrayList[])termOccurences.get(docid);
		
		//term id doesnot exist
		if (occurrences == null)
			return null;
		
		int[][] rtrPostings = new int[3][];
		rtrPostings[0] = occurrences[0].toNativeArray();
		rtrPostings[1] = occurrences[1].toNativeArray();
		rtrPostings[3] = occurrences[3].toNativeArray();
		return rtrPostings;
	}
	
	
	private class addTermsToList implements FieldDocumentTree.FDTnodeProcedure{
		
		termList list;
		
		public addTermsToList(termList l)
		{
			list = l;
		}
		
		public void execute(FieldDocumentTreeNode node)
		{
			list.addTerm(node.termCode,node.frequency,node.getFieldScore());
		}
	}
	
	
	
	
	
	public void addDoc(int docid, FieldDocumentTree fdt)
	{
		termList list = new termList();
		termOccurences.put(docid,list);
		fdt.forEachNode(new addTermsToList(list));
	}
	
	public void deleteDoc(int docid)
	{
		termOccurences.remove(docid);
	}


	/**
	 * Prints the Direct Index to standard output in the
	 * same format as if it were being written to file
	 */
	public void print() {
		//System.out.println("Number of Docs:"+IntDocidRecords.size());
		termOccurences.forEachEntry(new printProcedure());

	}
	
	
	class printProcedure implements TIntObjectProcedure{
		
		public boolean execute(int a, Object b)
		{
			System.out.println(b);
			
			return true;
		}
		
	}
	
	

	/**
	 * Writes the Direct index to disk as a file then
	 * deallocates the Direct Index. Clears it so that
	 * it as if a new instance.
	 */
	public void close() {
		termOccurences = null;
	}
	
	/** Write this MemoryDirectIndex out to a normal DirectIndex on the 
	 *	specified BitOutputStream
	 */
	public FilePosition[] flush(BitOutputStream bos)
	{
		TIntObjectHashMapDecorator d = new TIntObjectHashMapDecorator(termOccurences);
		TreeMap tm = new TreeMap(d);
		FilePosition[] offsets = new FilePosition[termOccurences.size()];
		
		Iterator iter = tm.values().iterator();
		termList temp;
		int count = 0;
		long endByte;
		byte endBit;
		
		try{
			while(iter.hasNext())
			{
				
				temp = (termList)iter.next();
				//System.out.println(temp.size());
				temp.flush(bos);
				
				endByte = bos.getByteOffset();
				endBit = bos.getBitOffset();
				endBit--;
				
				if (endBit < 0 && endByte > 0) {
					endBit = 7;
					endByte--;
				}
				
				offsets[count] = new FilePosition(endByte,endBit);
				count++;
				//Bos.addEntry(temp.stringDocId,temp.docLength,(long)0,(byte)0);
			}
						
			termOccurences = new TIntObjectHashMap();
		}
		catch(Exception ioe){
			System.err.println("Indexing failed to write a Direct index to disk : "+ioe);
			System.exit(1);
		}
		return offsets;
	}
	

}
