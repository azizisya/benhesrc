package uk.ac.gla.terrier.structures.incrementalindex.globallexicon;

public class MemoryLexiconRecord {
	
	/**
	 * Used as storage object for all term information inside hashmap.
	 * @author kanej
	 *
	 */

		public String term;
		public int termid;
		public int Nt;
		public int TF;
		//public int fieldScore;
		//public LinkedList postingsList; //A pointer to the postings
							//list for the term also stored in the
							//inverted index.
		
		public MemoryLexiconRecord(){
	
		}
		
		public MemoryLexiconRecord(String t, int td, int collfreq, int docfreq)
		{
			term = t;
			termid = td;
			Nt = docfreq;
			TF = collfreq;
			//postingsList = pl;
		}
		
		public String toString()
		{
			return term+ " termid "+termid+" TF "+TF+" Nt "+Nt;	
			
		}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
