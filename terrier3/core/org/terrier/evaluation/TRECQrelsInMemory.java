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
 * The Original Code is TRECQrelsInMemory.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 * Ben He <ben{a.}dcs.gla.ac.uk> 
 * Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.evaluation;
import org.terrier.utility.Files;
import uk.ac.gla.terrier.utility.StringUtility;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.terrier.utility.ApplicationSetup;
/**
 * Loads the relevance assessments in memory, for performing
 * evaluation of runs.
 * @author Ben He &amp; Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class TRECQrelsInMemory{
	protected static final Logger logger = Logger.getRootLogger();
	/** 
	 * Each element in the array contains the docids of the 
	 * relevant documents with respect to a query.
	 */  
	public QrelsHashSet[] qrelsPerQuery;
	
	protected boolean keepNumericChars = Boolean.parseBoolean(ApplicationSetup.getProperty("qrels.keep.numeric.chars", "false"));
	
	/**
	 * An array with the qrels files.
	 */
	protected File[] fqrels;
	
	/** The total number of relevant documents. */
	public int totalNumberOfRelevantDocs;
	
	/** 
	 * A constructor that creates an instance of the class
	 * and loads in memory the relevance assessments from the 
	 * given file.
	 * @param qrelsFilename String The full path of the qrels file to load.
	 */
	public TRECQrelsInMemory(String qrelsFilename){
		fqrels = new File[1];
		fqrels[0] = new File(qrelsFilename);
		this.loadQrelsFile();
	}
	/**
	 * Get ids of the queries that appear in the pool.
	 * @return The ids of queries in the pool.
	 */
	public String[] getQueryids(){
		String[] queryids = new String[getNumberOfQueries()];
		for (int i=0; i<getNumberOfQueries(); i++)
			queryids[i] = this.qrelsPerQuery[i].queryid;
		return queryids;
	}
	/**
	 * Get the identifiers of all relevant documents with the given relevance grades.
	 * @param grades The relevance grades.
	 * @return The identifiers of all relevant documents with the given relevance grades.
	 */
	public String[] getRelevantDocuments(int[] grades){
		THashSet<String> docnoSet = new THashSet<String>();
		for (int i=0; i<grades.length; i++){
			String[] docnos = this.getRelevantDocumentsToArray(grades[i]);
			int N = docnos.length;
			for (int j=0; j<N; j++)
				docnoSet.add(docnos[j]);
		}
		return (String[])docnoSet.toArray(new String[docnoSet.size()]);
	}
	
	/**
	 * Get the identifiers of all relevant documents in the pool.
	 * @return The identifiers of all relevant documents in the pool.
	 */
	public THashSet<String> getAllRelevantDocuments(){
		THashSet<String> docnos = new THashSet<String>();
		String[] queryids = this.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String[] docnosTmp = this.getRelevantDocumentsToArray(queryids[i]);
			if (docnosTmp!=null)
				for (int j=0; j<docnosTmp.length; j++)
					docnos.add(docnosTmp[j]);
		}
		return docnos;
	}
	
	/**
	 * Get the identifiers of all relevant documents in the pool with a given
	 * relevance grade.
	 * @return The identifiers of all relevant documents in the pool.
	 */
	public THashSet<String> getRelevantDocuments(int grade){
		THashSet<String> docnos = new THashSet<String>();
		String[] queryids = this.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String[] docnosTmp = this.getRelevantDocumentsToArray(queryids[i], grade);
			if (docnosTmp!=null)
				for (int j=0; j<docnosTmp.length; j++)
					docnos.add(docnosTmp[j]);
		}
		return docnos;
	}
	
	/**
	 * Get the identifiers of all relevant documents in the pool.
	 * @return The identifiers of all relevant documents in the pool.
	 */
	public String[] getAllRelevantDocumentsToArray(){
		THashSet<String> docnos = new THashSet<String>();
		String[] queryids = this.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String[] docnosTmp = this.getRelevantDocumentsToArray(queryids[i]);
			if (docnosTmp!=null)
				for (int j=0; j<docnosTmp.length; j++)
					docnos.add(docnosTmp[j]);
		}
		return (String[])docnos.toArray(new String[docnos.size()]);
	}
	
	/**
	 * Get the identifiers of all relevant documents in the pool with the given
	 * relevance grade.
	 * @return The identifiers of all relevant documents in the pool.
	 */
	public String[] getRelevantDocumentsToArray(int grade){
		THashSet<String> docnos = new THashSet<String>();
		String[] queryids = this.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String[] docnosTmp = this.getRelevantDocumentsToArray(queryids[i], grade);
			if (docnosTmp!=null)
				for (int j=0; j<docnosTmp.length; j++)
					docnos.add(docnosTmp[j]);
		}
		return (String[])docnos.toArray(new String[docnos.size()]);
	}
	
	/**
	 * A default constructor that creates an instance of the class
	 * and loads in memory the relevance assessments from the files
	 * that are specified in the file specified by the property
	 * TREC_QRELS.
	 */
	public TRECQrelsInMemory(){
		// initialise the qrels file
		try{
			BufferedReader br = Files.openFileReader(ApplicationSetup.TREC_QRELS);
			Vector<File> buf = new Vector<File>();
			String str = null;
			while ((str=br.readLine())!=null) {
				if (str.length() > 0 && !str.startsWith("#"))
					buf.addElement(new File(str));
			}
				
			fqrels = (File[])buf.toArray(new File[buf.size()]);
			br.close();
		}
		catch(IOException ioe){
			if(ApplicationSetup.TREC_QRELS==null)
			{
				logger.fatal("An error occured while initialising the qrels file path",ioe);
			} else 
			{
				logger.fatal("An error occured while initialising the qrels file at:" +ApplicationSetup.TREC_QRELS ,ioe);
			}
			return;
		}
		this.loadQrelsFile();
	}
	/**
	 * Get the pooled non-relevant documents for the given query.
	 * @param queryid The id of the given query.
	 * @return A hashset containing the docnos of the pooled non-relevant documents for the
	 * given query.
	 */
	public THashSet<String> getNonRelevantDocuments(String queryid){
		THashSet<String> relDocnos = null;
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				relDocnos = (THashSet<String>)qrelsPerQuery[i].nonRelDocnos.clone();
			}
		}
		return relDocnos;
	}
	/**
	 * Get all the pooled non-relevant documents.
	 * @return All the pooled non-relevant documents.
	 */
	public THashSet<String> getNonRelevantDocuments(){
		THashSet<String> docnoSet = new THashSet<String>();
		int numberOfQueries = getNumberOfQueries();
		for (int i=0; i<numberOfQueries; i++){
			THashSet<String> tmpSet = qrelsPerQuery[i].nonRelDocnos;
			String[] docnos = (String[])tmpSet.toArray(new String[tmpSet.size()]);
			int N = docnos.length;
			for (int j=0; j<N; j++)
				docnoSet.add(docnos[j]);
		}
		return docnoSet;
	}
	/**
	 * Get all the pooled non-relevant documents.
	 * @return All the pooled non-relevant documents.
	 */
	public String[] getNonRelevantDocumentsToArray(){
		THashSet<String> docnoSet = this.getNonRelevantDocuments();
		return (String[])docnoSet.toArray(new String[docnoSet.size()]);
	}
	/**
	 * Get the pooled relevant documents for the given query.
	 * @param queryid The id of the given query.
	 * @return A hashset containing the docnos of the pooled relevant documents for the
	 * given query.
	 */
	public THashSet<String> getRelevantDocuments(String queryid){
		THashSet<String> relDocnos = null;
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				relDocnos = qrelsPerQuery[i].getAllRelevantDocuments();
			}
		}
		return relDocnos;
	}
	/**
	 * Get the pooled relevant documents for the given query.
	 * @param queryid The id of the given query.
	 * @return A hashset containing the docnos of the pooled relevant documents for the
	 * given query.
	 */
	public THashSet<String> getRelevantDocuments(String queryid, int grade){
		THashSet<String> relDocnos = null;
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				relDocnos = qrelsPerQuery[i].getRelevantDocuments(grade);
			}
		}
		return relDocnos;
	}
	/**
	 * Get the pooled relevant documents for the given query.
	 * @param queryid The id of the given query.
	 * @return A hashset containing the docnos of the pooled relevant documents for the
	 * given query.
	 */
	public THashSet<String> getRelevantDocuments(String queryid, int grades[]){
		THashSet<String> docnoSet = new THashSet<String>();
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				for (int j=0; j<grades.length; j++){
					String[] docnos = qrelsPerQuery[i].getRelevantDocumentsToArray(grades[j]);
					if (docnos!=null){
						int N = docnos.length;
						for (int k=0; k<N; k++)
							docnoSet.add(docnos[k]);
					}
				}
			}
		}
		return docnoSet;
	}
	/**
	 * Get the pooled relevant documents for the given query.
	 * @param queryid The id of the given query.
	 * @return A hashset containing the docnos of the pooled relevant documents for the
	 * given query.
	 */
	public String[] getRelevantDocumentsToArray(String queryid, int grades[]){
		THashSet<String> docnoSet = getRelevantDocuments(queryid, grades);
		return (String[])docnoSet.toArray(new String[docnoSet.size()]);
	}
	/**
	 * Get the pooled non-relevant documents for a given query.
	 * @param queryid The id of the given query.
	 * @return An array of the docnos of the pooled non-relevant documents
	 * for the given query.
	 */
	public String[] getNonRelevantDocumentsToArray(String queryid){
		String[] nonRelDocnos = null;
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				nonRelDocnos = (String[])qrelsPerQuery[i].nonRelDocnos.toArray(new String[qrelsPerQuery[i].nonRelDocnos.size()]);
			}
		}
		return nonRelDocnos;
	}
	/**
	 * Get the pooled relevant documents for a given query.
	 * @param queryid The id of the given query.
	 * @return An array of the docnos of the pooled relevant documents
	 * for the given query.
	 */
	public String[] getRelevantDocumentsToArray(String queryid){
		String[] relDocnos = null;
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				relDocnos = qrelsPerQuery[i].getAllRelevantDocumentsToArray();
			}
		}
		return relDocnos;
	}
	/**
	 * Get the pooled relevant documents for a given query.
	 * @param queryid The id of the given query.
	 * @return An array of the docnos of the pooled relevant documents
	 * for the given query.
	 */
	public String[] getRelevantDocumentsToArray(String queryid, int grade){
		String[] relDocnos = null;
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				relDocnos = qrelsPerQuery[i].getRelevantDocumentsToArray(grade);
			}
		}
		return relDocnos;
	}
	
	/** 
	 * Returns the total number of queries contained in the
	 * loaded relevance assessments.
	 * @return int The number of unique queries in the loaded
	 *         relevance assessments.
	 */
	public int getNumberOfQueries(){
		return this.qrelsPerQuery.length;
	}
	
	/**
	 * Returns the numbe of relevant documents for a given query.
	 * @param queryid String The identifier of a query.
	 * @return int The number of relevant documents for the given query.
	 */
	public int getNumberOfRelevant(String queryid){
		for (int i = 0; i < this.getNumberOfQueries(); i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid)){
				return qrelsPerQuery[i].getAllRelevantDocuments().size();
			}
		}
		return 0;
	}
	
	/**
	 * Load in memory the relevance assessment files that are
	 * specified in the array fqrels.
	 */
	protected void loadQrelsFile(){
		Vector<QrelsHashSet> vector = new Vector<QrelsHashSet>();
		try{
			int qrelsCounter = 0;
			BufferedReader br = Files.openFileReader(fqrels[0].toString());
			String preQueryid = "1st";
			QrelsHashSet qrelsHashSet = null;
			String str = null;
			while ((str=br.readLine()) != null || qrelsCounter != fqrels.length-1){
				if (str == null){
					br.close();
					br = Files.openFileReader(fqrels[++qrelsCounter].toString());
					continue;
				}
				if (str.startsWith("#"))
					continue;
				if (str.trim().length() == 0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				// takes only the numeric chars at the end of an query id to
				// cope with ids like "WT04-065", which is interpretated as "65"
				/*StringBuilder queryNoTmp = new StringBuilder();
				boolean firstNumericChar = false;
				for (int i = queryid.length()-1; i >=0; i--){
					char ch = queryid.charAt(i);
					if (Character.isDigit(ch)){
						queryNoTmp.append(queryid.charAt(i));
						firstNumericChar = true;
					}
					else if (firstNumericChar)
						break;
				}
				queryid = ""+Integer.parseInt(queryNoTmp.reverse().toString());*/
				if (this.keepNumericChars)
					queryid = StringUtility.keepNumericChars(queryid);
				stk.nextToken();
				String docno = stk.nextToken();
				int relGrade = Integer.parseInt(stk.nextToken());
				boolean relevant = !(relGrade == 0);
				if (!queryid.equals(preQueryid)){
					if (preQueryid.equals("1st")){
						qrelsHashSet = new QrelsHashSet(queryid);
						if (relevant){
							qrelsHashSet.insertRelDocno(docno, relGrade);
						}
						else
                            qrelsHashSet.insertNonRelDocno(docno);
						preQueryid = queryid;
					}
					else{
						vector.addElement((QrelsHashSet)qrelsHashSet.clone());
						qrelsHashSet = new QrelsHashSet(queryid);
						if (relevant)
							qrelsHashSet.insertRelDocno(docno, relGrade);
						else
                            qrelsHashSet.insertNonRelDocno(docno);
						preQueryid = queryid;
					}
				}
				else{
					if (relevant)
						qrelsHashSet.insertRelDocno(docno, relGrade);
					else
                        qrelsHashSet.insertNonRelDocno(docno);
				}
			}
			vector.addElement((QrelsHashSet)qrelsHashSet.clone());
			br.close();
		}
		catch(Exception ioe){
			logger.fatal(ioe.getMessage(),ioe);
			return;
		}
		this.qrelsPerQuery = (QrelsHashSet[])vector.toArray(new QrelsHashSet[vector.size()]);
		this.totalNumberOfRelevantDocs = 0;
		for (int i = 0; i < qrelsPerQuery.length; i++)
			this.totalNumberOfRelevantDocs += qrelsPerQuery[i].getAllRelevantDocuments().size();
	}
	
	/**
	 * Checks whether there is a query with a given identifier
	 * in the relevance assessments. 
	 * @param queryid String the identifier of a query.
	 * @return boolean true if the given query exists in the qrels file, 
	 *         otherwise it returns false.
	 */
	public boolean queryExistInQrels(String queryid){
		for (int i = 0; i < this.qrelsPerQuery.length; i++){
			if (qrelsPerQuery[i].queryid.equalsIgnoreCase(queryid))
				return true;
		}
		return false;
	}
	
	/**
	 * Check if the given document is relevant for a given query. 
	 * @param queryid String a query identifier.
	 * @param docno String a document identifier.
	 * @return boolean true if the given document is relevant for the given
	 *         query, or otherwise false.
	 */
	public boolean isRelevant(String queryid, String docno){
		boolean relevant = false;
		for (int i = 0; i < qrelsPerQuery.length; i++)
			if (qrelsPerQuery[i].queryid.equals(queryid)){
				relevant = qrelsPerQuery[i].isRelevant(docno);
				break;
			}
		return relevant;
	}
	
	public boolean isJudgedNonRelevant(String queryid, String docno){
		boolean nonRelevant = false;
		for (int i = 0; i < qrelsPerQuery.length; i++)
			if (qrelsPerQuery[i].queryid.equals(queryid)){
				nonRelevant = qrelsPerQuery[i].isJudgedNonRelevant(docno); break;
			}
		return nonRelevant;
	}
	
	public int checkDocStatus(String queryid, String docno){
		int status = -1;
		for (int i = 0; i < qrelsPerQuery.length; i++){
			if (qrelsPerQuery[i].queryid.equals(queryid)){
				if (qrelsPerQuery[i].isRelevant(docno))
					status = 1;
				else if (qrelsPerQuery[i].isJudgedNonRelevant(docno))
					status = 0;
				else status = -1;
				break;
			}
		}
		return status;
	}
	
	/**
     * Models the set of relevant documents for one query.
     */
    static public class QrelsHashSet{
            /** The identifier of the query.*/
            public String queryid = "";
            /** All relevance grades indicated in the qrels. */
            public TIntHashSet relGrade;
            /** The ids of the pooled non-relevant documents. */
            public THashSet<String> nonRelDocnos;
            
            /** A hashmap from the relevance grade to a hashset containing ids of
             * documents with the given relevance grade. */
            public TIntObjectHashMap<THashSet<String>> relGradeDocnosMap;

            /**
             * Creates the an instance of the class with a given
             * query identifier.
             * @param queryid String the query identifier.
             */
            public QrelsHashSet(String queryid){
                    this.queryid = queryid;
                    nonRelDocnos = new THashSet<String>();
                    relGrade = new TIntHashSet();
                    relGradeDocnosMap = new TIntObjectHashMap<THashSet<String>>();
            }

            /**
             * Creates a clone of the current instance of the class.
             * @return Object the clone of the current object.
             */
            public Object clone() {
            		QrelsHashSet dup;
            		try{
            			dup = (QrelsHashSet)super.clone();
            		}catch(CloneNotSupportedException e){dup = new QrelsHashSet(queryid);}
            		dup.queryid = this.queryid;
                    dup.nonRelDocnos = (THashSet<String>)nonRelDocnos.clone();
                    dup.relGrade = (TIntHashSet)relGrade.clone();
                    dup.relGradeDocnosMap = (TIntObjectHashMap<THashSet<String>>)relGradeDocnosMap.clone();
                    return dup;
            }
            /**
             * Check if a given document is relevant to the associated query.
             * @param docno The identifier of the given document.
             * @return Returns true if the document is relevant, false otherwise.
             */
            public boolean isRelevant(String docno){
            	int[] grades = relGrade.toArray();
            	int numOfGrades = grades.length;
            	for (int i=0; i<numOfGrades; i++){
            		if (((THashSet<String>)relGradeDocnosMap.get(grades[i])).contains(docno)){
            			return true;
            		}
            	}
            	return false;
            }
            
            /**
             * Check if a document is judged non-relevant.
             * @param docno The given document.
             * @return True for judged non-relevant, false otherwise.
             */
            public boolean isJudgedNonRelevant(String docno){
            	return nonRelDocnos.contains(docno);
            }
            
            /**
             * Get all relevant documents regardless of their relevance grades.
             * @return The identifiers of all relevant documents.
             */
            public String[] getAllRelevantDocumentsToArray(){
            	THashSet<String> docnos = new THashSet<String>();
            	int[] grades = relGrade.toArray();
            	for (int i=0; i<relGrade.size(); i++){
            		THashSet<String> docnosTmp = (THashSet<String>)relGradeDocnosMap.get(grades[i]);
            		String[] docnosArray = (String[])docnosTmp.toArray(new String[docnosTmp.size()]);
            		for (int j=0; j<docnosArray.length; j++)
            			docnos.add(docnosArray[j]);
            	}
            	return (String[])docnos.toArray(new String[docnos.size()]);
            }
            /**
             * Get all relevant documents regardless of their relevance grades.
             * @return The identifiers of all relevant documents.
             */
            public THashSet<String> getAllRelevantDocuments(){
            	THashSet<String> docnos = new THashSet<String>();
            	int[] grades = relGrade.toArray();
            	for (int i=0; i<relGrade.size(); i++){
            		THashSet<String> docnosTmp = (THashSet<String>)relGradeDocnosMap.get(grades[i]);
            		String[] docnosArray = (String[])docnosTmp.toArray(new String[docnosTmp.size()]);
            		for (int j=0; j<docnosArray.length; j++)
            			docnos.add(docnosArray[j]);
            	}
            	return docnos;
            }
            
            /**
             * Get the relevant documents for a given relevance grade.
             * @param grade The given relevance grade.
             * @return The identifiers of the relevant documents with the given relevance
             * grade.
             */
            public String[] getRelevantDocumentsToArray(int grade){
            	if (!relGrade.contains(grade))
            		return null;
            	THashSet<String> docnos = (THashSet<String>)relGradeDocnosMap.get(grade);
            	return (String[])docnos.toArray(new String[docnos.size()]);
            }
            /**
             * Get the relevant documents for a given relevance grade.
             * @param grade The given relevance grade.
             * @return The identifiers of the relevant documents with the given relevance
             * grade.
             */
            public THashSet<String> getRelevantDocuments(int grade){
            	if (!relGrade.contains(grade))
            		return null;
            	return (THashSet<String>)relGradeDocnosMap.get(grade);
            }
  
            /**
             * Add an identifier of a relevant document with its relevance grade.
             * @param docno The identifier of the given relevant document.
             * @param grade The relevance grade of the given relevant document.
             */
            public void insertRelDocno(String docno, int grade){
            	if (!relGrade.contains(grade)){
            		relGrade.add(grade);
            		THashSet<String> gradeDocnos = new THashSet<String>();
            		gradeDocnos.add(docno);
            		relGradeDocnosMap.put(grade, gradeDocnos);
            	}else{
            		((THashSet<String>)relGradeDocnosMap.get(grade)).add(docno);
            	}
            }

            public void insertNonRelDocno(String docno){
                    this.nonRelDocnos.add(docno);
            }
    }
}
