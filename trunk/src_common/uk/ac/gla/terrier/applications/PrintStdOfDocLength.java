/*
 * Created on 2006-5-2
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.IOException;

import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;

public class PrintStdOfDocLength {
	
	public double getStdOfDocLength(){
		Index index = Index.createIndex();
		DocumentIndex docIndex = index.getDocumentIndex();
		CollectionStatistics collSta = index.getCollectionStatistics();
		double var = 0;
		for (int i =0; i<collSta.getNumberOfDocuments(); i++)
			var += Math.pow(docIndex.getDocumentLength(i), 2);
		return Math.sqrt(var/collSta.getNumberOfDocuments());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PrintStdOfDocLength app = new PrintStdOfDocLength();
		System.out.println("Std of document length: " + app.getStdOfDocLength());
	}

}
