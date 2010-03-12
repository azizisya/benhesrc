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
 * The Original Code is AccumulatorResultSet.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Nicola Tonellotto (original author)
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *   
 */
package org.terrier.matching;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntShortHashMap;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import org.terrier.matching.QueryResultSet;
import org.terrier.matching.ResultSet;


/** A result set instance that uses maps internally until initialise() is called
 * @since 3.0
 * @author Nicola Tonelotto
 */
@SuppressWarnings("serial")
public class AccumulatorResultSet implements ResultSet, Serializable
{	
	private static Logger logger = Logger.getRootLogger();
	
	public int[] docids;
	public double[] scores;
	public short[] occurrences;
	protected boolean arraysInitialised = false;
	
	public TIntDoubleHashMap scoresMap;
	public TIntShortHashMap  occurrencesMap;
	protected boolean mapsInitialised = false;

	protected int resultSize;
	protected int exactResultSize;
	
	protected Lock lock;	
	public Lock getLock() { return lock; }
	
	protected int statusCode = 0;
	public int getStatusCode() { return statusCode; }
	public void setStatusCode(int _statusCode) { statusCode = _statusCode; }


	public AccumulatorResultSet(int numberOfDocuments) 
	{
		lock = new ReentrantLock();
		
		scoresMap = new TIntDoubleHashMap();
		occurrencesMap = new TIntShortHashMap();

		resultSize = numberOfDocuments;
		exactResultSize = numberOfDocuments;
	}
	
	/** This method initialises the arrays to be sorted, after the matching phase has been completed */
	public void initialise() 
	{
		this.docids = scoresMap.keys();
		this.scores = scoresMap.getValues();
		this.occurrences = occurrencesMap.getValues();		
		resultSize = this.docids.length;
		exactResultSize = this.docids.length;

		scoresMap.clear();
		occurrencesMap.clear();
		this.arraysInitialised = true;
	}
	
	/** Unsupported */
	public void initialise(double[] scs) 
	{
		throw new UnsupportedOperationException("This method is not available for class " + AccumulatorResultSet.class);
	}

	/** {@inheritDoc} */
	public int[] getDocids() 
	{
		if (arraysInitialised)
			return docids;
		else
			throw new UnsupportedOperationException("");
	}

	/** {@inheritDoc} */
	public int getResultSize() 
	{
		return resultSize;
	}
	
	/** {@inheritDoc} */
	public short[] getOccurrences() 
	{
		if (arraysInitialised)
			return occurrences;
		else
			throw new UnsupportedOperationException("");
	}

	/** {@inheritDoc} */
	public int getExactResultSize() 
	{
		return exactResultSize;
	}

	/** {@inheritDoc} */
	public double[] getScores() 
	{
		if (arraysInitialised)
			return scores;
		else
			throw new UnsupportedOperationException("");
	}
	
	/** {@inheritDoc} */
	public void setResultSize(int newResultSize) 
	{
		resultSize = newResultSize;
	}

	/** {@inheritDoc} */
	public void setExactResultSize(int newExactResultSize) 
	{
		exactResultSize = newExactResultSize;
	}
	
	/** Unsupported */
	public void addMetaItem(String name, int docid, String value) {}
	/** Unsupported */
	public void addMetaItems(String name, String[] values) {}	
	/** Unsupported */
	public String getMetaItem(String name, int docid) {	return null; }
	/** Unsupported */
	public String[] getMetaItems(String name) {	return null; }
	/** Unsupported */
	public boolean hasMetaItems(String name) { return false; }
	/** Unsupported */
	public String[] getMetaKeys() { return new String[0]; }
	
	/** {@inheritDoc} */
	public ResultSet getResultSet(int start, int length) 
	{
		if (arraysInitialised) {
			length = length < docids.length ? length : docids.length;
			QueryResultSet resultSet = new QueryResultSet(length);
			System.arraycopy(docids, start, resultSet.getDocids(), 0, length);
			System.arraycopy(scores, start, resultSet.getScores(), 0, length);
			System.arraycopy(occurrences, start, resultSet.getOccurrences(), 0, length);
			return resultSet;
		} else
			throw new UnsupportedOperationException("");
	}
	
	/** {@inheritDoc} */
	public ResultSet getResultSet(int[] positions) 
	{
		if (arraysInitialised) {
			int NewSize = positions.length;
			if (logger.isDebugEnabled())
				logger.debug("New results size is "+NewSize);
			QueryResultSet resultSet = new QueryResultSet(NewSize);
			int newDocids[] = resultSet.getDocids();
			double newScores[] = resultSet.getScores();
			short newOccurs[] = resultSet.getOccurrences();
			int thisPosition;
			for(int i=0;i<NewSize;i++)
			{
				thisPosition = positions[i];
				if (logger.isDebugEnabled())
					logger.debug("adding result at "+i);
				newDocids[i] = docids[thisPosition];
				newScores[i] = scores[thisPosition];
				newOccurs[i] = occurrences[thisPosition];
			}
			return resultSet;
		} else
			throw new UnsupportedOperationException("");		
	}
}
