/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
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
 * The Original Code is CollectionStatistics.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.structures;
/**
 * This class provides basic statistics for the indexed
 * collection of documents, such as the average length of documents,
 * or the total number of documents in the collection. <br />
 * After indexing, statistics are saved in the PREFIX.log file, along
 * with the classes that should be used for the Lexicon, the DocumentIndex,
 * the DirectIndex and the InvertedIndex. This means that an index knows
 * how it was build and how it should be opened again.
 *
 * @author Gianni Amati, Vassilis Plachouras, Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class CollectionStatistics {
	
	/** The total number of documents in the collection.*/
	protected int numberOfDocuments;
	
	/** The total number of tokens in the collection.*/
	protected long numberOfTokens;
	/** 
	 * The total number of pointers in the inverted file.
	 * This corresponds to the sum of the document frequencies for
	 * the terms in the lexicon.
	 */
	protected long numberOfPointers;
	/**
	 * The total number of unique terms in the collection.
	 * This corresponds to the number of entries in the lexicon.
	 */
	protected int numberOfUniqueTerms;
	/**
	 * The average length of a document in the collection.
	 */
	protected double averageDocumentLength;

	public CollectionStatistics(int numDocs, int numTerms, long numTokens, long numPointers)
	{
		numberOfDocuments = numDocs;
		numberOfUniqueTerms = numTerms;
		numberOfTokens = numTokens;
		numberOfPointers = numPointers;
		if (numberOfDocuments != 0)
			averageDocumentLength =
				(1.0D * numberOfTokens) / (1.0D * numberOfDocuments);
		else
			averageDocumentLength = 0.0D;
	}
	
	/**
	 * Returns the documents' average length.
	 * @return the average length of the documents in the collection.
	 */
	public double getAverageDocumentLength() {
		return averageDocumentLength;
	}
	/**
	 * Returns the total number of documents in the collection.
	 * @return the total number of documents in the collection
	 */
	public int getNumberOfDocuments() {
		return numberOfDocuments;
	}
	/**
	 * Returns the total number of pointers in the collection.
	 * @return the total number of pointers in the collection
	 */
	public long getNumberOfPointers() {
		return numberOfPointers;
	}
	/**
	 * Returns the total number of tokens in the collection.
	 * @return the total number of tokens in the collection
	 */
	public long getNumberOfTokens() {
		return numberOfTokens;
	}
	/**
	 * Returns the total number of unique terms in the lexicon.
	 * @return the total number of unique terms in the lexicon
	 */
	public int getNumberOfUniqueTerms() {
		return numberOfUniqueTerms;
	}

}
