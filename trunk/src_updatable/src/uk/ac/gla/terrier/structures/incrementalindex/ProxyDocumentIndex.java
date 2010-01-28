package uk.ac.gla.terrier.structures.incrementalindex;

import java.io.IOException;

import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.structures.incrementalindex.globaldocumentindex.GlobalDocumentIndex;
import uk.ac.gla.terrier.structures.incrementalindex.memoryindex.MemoryIndex;

public class ProxyDocumentIndex extends DocumentIndex {
	
	protected GlobalDocumentIndex gDocInd;
	protected IncrementalIndex incIndex;
	
	public ProxyDocumentIndex(GlobalDocumentIndex gdi, IncrementalIndex ii)
	{
		super(2,2,2);
		gDocInd = gdi;
		incIndex = ii;
	}

	public void close() {
		// TODO Auto-generated method stub

	}

	public void print() {
		// TODO Auto-generated method stub

	}

	public int getDocumentId(String docno) {

		int Mdocno = incIndex.getMemIndex().getDocumentIndex().getDocumentId(docno);
		if(Mdocno == -1)
			return gDocInd.getDocumentId(docno);
		else
			return Mdocno;
	}

	public int getDocumentLength(int i) {

		int Mdocno = incIndex.getMemIndex().getDocumentIndex().getDocumentLength(i);
		System.out.println(Mdocno);
		if(Mdocno == -1)
			return gDocInd.getDocumentLength(i);
		else
			return Mdocno;
	}

	public int getDocumentLength(String docno) {

		int Mdocno = incIndex.getMemIndex().getDocumentIndex().getDocumentLength(docno);
		if(Mdocno == -1)
			return gDocInd.getDocumentLength(docno);
		else
			return Mdocno;
	}

	public String getDocumentNumber(int i) {

		String output = incIndex.getMemIndex().getDocumentIndex().getDocumentNumber(i);
		if(output == null)
			return gDocInd.getDocumentNumber(i);
		else
			return output;
	}

	public FilePosition getDirectIndexEndOffset() {

		return null;
	}

	public int getNumberOfDocuments() {

		return incIndex.getMemIndex().getDocumentIndex().getNumberOfDocuments()+
				gDocInd.getNumberOfDocuments();
	}

	public FilePosition getDirectIndexStartOffset() {

		return null;
	}

	public boolean seek(int i) throws IOException {

		// TODO Auto-generated method stub
		return false;
	}

	public boolean seek(String docno) throws IOException {

		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
