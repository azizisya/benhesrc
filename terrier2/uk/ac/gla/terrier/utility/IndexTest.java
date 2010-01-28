package uk.ac.gla.terrier.utility;
import java.io.IOException;

import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.TerrierTimer;
public class IndexTest
{
	
	Index index;
	CollectionStatistics cs;
	Lexicon lex;
	InvertedIndex ii;
	
	public IndexTest(Index I)
	{
		this.index = I;
		if (I== null)
		{
			System.err.println("Initialiased with null index");
			return;
		}
		this.cs = I.getCollectionStatistics();
		this.lex = I.getLexicon();
		this.ii = I.getInvertedIndex();
	}

	public boolean test()
	{
		return testInvertedIndex();
	}
	
	public void testIndexIntegrity(){
		// for each entry in inverted index, check if there exists a corresponding entry in direct index
		LexiconInputStream lexInStream = new LexiconInputStream(index.getPath(), index.getPrefix());
		InvertedIndex invIndex = index.getInvertedIndex();
		DirectIndex directIndex = index.getDirectIndex();
		try{
			while (lexInStream.readNextEntry()!=-1){
				int termid = lexInStream.getTermId();
				int[][] pointers = invIndex.getDocuments(termid);
				for (int i=0; i<pointers[0].length; i++){
					if (!entryExistsInDirectIndex(termid, pointers[0][i], pointers[1][i], directIndex))
						System.err.println("ERROR: ("+termid+", "+pointers[0][i]+", "+
								pointers[1][i]+") does not exist in direct index.");
				}
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		lexInStream.close();
	}
	
	protected boolean entryExistsInDirectIndex(int termid, int docid, int tf, DirectIndex directIndex){
		boolean exists = false;
		int[][] terms = directIndex.getTerms(docid);
		if (terms==null) return true;
		int left = 0;
		int right = terms[0].length-1;
		int index = (left+right)/2;
		int prev = index;
		while (!exists){
			if (termid>terms[0][index]){
				left = index;
				index = (left+right)/2;
				if (index==prev)
					return false;
				prev=index;
			}else if (termid<terms[0][index]){
				right = index;
				index = (left+right)/2;
				if (index==prev)
					return false;
				prev=index;
			}else{
				if (tf==terms[1][index])
					return true;
				else
					return false;
			}
		}
			
		return exists;
	}
	

	public boolean testInvertedIndex()
	{
		if (this.index == null)
		{
			System.err.println("Initialiased with null index : index probably failed to load");
			return false;
		}
		System.err.println("Checking inverted index ");
		final int numTerms = cs.getNumberOfUniqueTerms();
		final int numDocs = cs.getNumberOfDocuments();
		final long numPointers = cs.getNumberOfPointers();
		final long numTokens = cs.getNumberOfTokens();
		TerrierTimer t = new TerrierTimer(); t.start();
		t.setTotalNumber(numTerms);
		long countNumPointers = 0;
		long countNumTokens = 0;
		int errCount =0;
		int maxErrors = 100;
		loopterm: for(int i=0;i<numTerms;i++)
		{
			if (! lex.findTerm(i))
			{
				continue;
			}
			if (i % 100 == 0)
			{
				t.setRemainingTime(i);
				System.err.println(t.toStringMinutesSeconds());
			}
			final int TF = lex.getTF();
			final int Nt = lex.getNt();
			if (lex.getStartOffset() > lex.getEndOffset() )
			{
				System.err.println("Offsets for term "+ lex.getTerm()+ " are nonsense");
				errCount++;
				continue loopterm;
			}
			if (lex.getStartOffset() < 0 || lex.getEndOffset() < 0)
			{
				System.err.println("Negative offsets for term "+ lex.getTerm()+ " are nonsense");
				errCount++;
                continue loopterm;
			}
			int[][] docs;
			try{
				docs = ii.getDocuments(i);
			} catch (Throwable e) {
				System.err.println("Error reading postings for term " + lex.getTerm() + " : " + e);
				e.printStackTrace();
				errCount++;
                continue loopterm;
			}
			if (docs == null)
			{
				System.err.println("Assertion failed: no terms obtained from ii for term " + lex.getTerm());
				errCount++;
                continue loopterm;
			}
			if (docs[0].length != Nt)
			{
				System.err.println("Assertion failed: postings list is not "+Nt+" in length for term " + lex.getTerm()+". Actual length was "+ docs[0].length);
				errCount++;
                continue loopterm;
			}
			int TfCheck = 0;
			for(int j=0;j<docs[0].length;j++)
			{
				if (docs[0][j] < 0)
				{
					System.err.println("Assertion failed: negative document id in postings for term "+ lex.getTerm());
					errCount++;
	                continue loopterm;
				}
				if (docs[0][j] >= numDocs)
				{
					System.err.println("Assertion failed: document id too large in postings for term "+ lex.getTerm());
					errCount++;
	                continue loopterm;
					
				} 
				if (docs[1][j] < 0)
				{
					System.err.println("Assertion failed: Negative tf in postings for term "+ lex.getTerm());
					errCount++;
    	            continue loopterm;
				}
				if (docs[1][j] > TF)
				{
					System.err.println("Assertion failed: tf too large in postings for term "+ lex.getTerm());
					errCount++;
	                continue loopterm;
				}
				TfCheck += docs[1][j];
				countNumTokens += docs[1][j];
			}
			countNumPointers += docs[0].length;
			if (TfCheck != TF)
			{
				System.err.println("Assertion failed: TF not accurate with postings ("+TfCheck+"!="+TF+") term "+ lex.getTerm());
				errCount++;
                continue loopterm;
			}
			if (errCount >= maxErrors)
			{
				System.err.println("Too many inverted index problems ("+errCount+" so far). Giving up");
				return false;
			}
		}

		if (countNumPointers != numPointers)
		{
			System.err.println("Assertion failed: pointers doesnt match ("+numPointers+" wasnt found, found "+ countNumPointers+")");
			return false;
		}
		if (countNumTokens != numTokens)
        {
            System.err.println("Assertion failed: pointers doesnt match ("+numTokens+" wasnt found, found "+ countNumTokens+")");
            return false;
        }
		return errCount ==0;
	}

	public static void main(String args[])
	{
		if (args[0].equals("--checkindex")){
			new IndexTest(Index.createIndex()).testIndexIntegrity();
		}
		//System.out.println("OK?: "+ new IndexTest(Index.createIndex()).test());		
	}

}
