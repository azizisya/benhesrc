package org.terrier.queryfeature.features;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import org.terrier.documentfeature.ArffFeatureExtractor;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.MetaIndex;
import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.Rounding;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import org.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.utility.Shuffling;
import uk.ac.gla.terrier.utility.StringUtility;

abstract public class QueryFeature {
	protected Index index;
	
	protected DirectIndex directIndex;
	
	protected DocumentIndex docIndex;
	
	protected CollectionStatistics collStats;
	
	protected MetaIndex metaIndex;
	
	protected String key = "docno";
	
	protected Lexicon lexicon;
	
	public QueryFeature(Index index){
		this.index = index;
		this.directIndex = index.getDirectIndex();
		this.lexicon = index.getLexicon();
		this.docIndex = index.getDocumentIndex();
		this.collStats = index.getCollectionStatistics();
		this.metaIndex = index.getMetaIndex();
	}
	
	abstract public String getInfo();
	
	private String formatPreProcess(int docid, String qid, int label, TIntDoubleHashMap map, Lexicon lexicon){
		StringBuilder sb = new StringBuilder();
		sb.append(docid+" "+qid+" "+label+" ");
		int[] termids = map.keys();
		Arrays.sort(termids);
		for (int i=0; i<termids.length; i++){
			sb.append(termids[i]+","+(String)lexicon.getLexiconEntry(termids[i]).getKey()+","+Rounding.toString(map.get(termids[i]), 6)+" ");
		}
		return sb.toString();
	}
	
	public void preProcess(String qrelsFilename, String queryid, String outputFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		String[] posDocnos = qrels.getRelevantDocumentsToArray(queryid);
		String[] negDocnos = qrels.getNonRelevantDocumentsToArray(queryid);
		int[] posDocids = StringUtility.stringsToInts(posDocnos);
		int[] negDocids = StringUtility.stringsToInts(negDocnos);
		int[] queryTermids = null;
		try{
			queryTermids = ArffFeatureExtractor.processQuery(((TRECQuery)Class.forName("uk.ac.gla.terrier.structures."+
					ApplicationSetup.getProperty("trec.topics.parser", 
					"TRECQuery")).newInstance()).getQuery(queryid), index).keys();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}		
		StringBuffer buf = new StringBuffer();
		int counter = 0; int totalDocs = posDocids.length+negDocids.length;
		for (int i=0; i<posDocids.length; i++){
			if (posDocids[i]==-1)
				continue;
			try{
				System.out.print("processing positive document "+metaIndex.getItem(key, posDocids[i])+"...");
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			TIntDoubleHashMap map = new TIntDoubleHashMap();
			int[] oneDocid = {posDocids[i]};
			extractQueryFeature(oneDocid, queryid, queryTermids, map);			
			System.out.println("Done. "+(++counter)+" out of "+totalDocs+" documents processed.");
			buf.append(this.formatPreProcess(posDocids[i], queryid, 1, map, lexicon)+ApplicationSetup.EOL);
		}
		for (int i=0; i<negDocids.length; i++){
			if (negDocids[i]==-1)
				continue;
			try{
				System.out.print("processing negative document "+metaIndex.getItem(key, negDocids[i])+"...");
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			TIntDoubleHashMap map = new TIntDoubleHashMap();
			int[] oneDocid = {negDocids[i]};
			extractQueryFeature(oneDocid, queryid, queryTermids, map);			
			System.out.println("Done. "+(++counter)+" out of "+totalDocs+" documents processed.");
			buf.append(this.formatPreProcess(negDocids[i], queryid, -1, map, lexicon)+ApplicationSetup.EOL);
		}
		System.out.print("processing all documents...");
		TIntDoubleHashMap map = new TIntDoubleHashMap();
		TIntHashSet docidSet = new TIntHashSet(posDocids);
		docidSet.addAll(negDocids);
		extractQueryFeature(docidSet.toArray(), queryid, queryTermids, map);
		System.out.println("Done. ");
		buf.append(this.formatPreProcess(-2, queryid, -2, map, lexicon)+ApplicationSetup.EOL);
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void preProcessAll(String qrelsFilename, String outputFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		String[] qids = qrels.getQueryids();
		StringBuffer buf = new StringBuffer();
		for (String queryid : qids){
			String[] posDocnos = qrels.getRelevantDocumentsToArray(queryid);
			String[] negDocnos = qrels.getNonRelevantDocumentsToArray(queryid);
			int[] posDocids = StringUtility.stringsToInts(posDocnos);
			int[] negDocids = StringUtility.stringsToInts(negDocnos);
			int[] queryTermids = null;
			try{
				queryTermids = ArffFeatureExtractor.processQuery(((TRECQuery)Class.forName("uk.ac.gla.terrier.structures."+
						ApplicationSetup.getProperty("trec.topics.parser", 
						"TRECQuery")).newInstance()).getQuery(queryid), index).keys();
			}catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}					
			int counter = 0; int totalDocs = posDocids.length+negDocids.length;
			for (int i=0; i<posDocids.length; i++){
				if (posDocids[i]==-1)
					continue;
				try{
					System.out.print("processing positive document "+metaIndex.getItem(key, posDocids[i])+" for query "+queryid+"...");
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
				TIntDoubleHashMap map = new TIntDoubleHashMap();
				int[] oneDocid = {posDocids[i]};
				extractQueryFeature(oneDocid, queryid, queryTermids, map);			
				System.out.println("Done. "+(++counter)+" out of "+totalDocs+" documents processed.");
				buf.append(this.formatPreProcess(posDocids[i], queryid, 1, map, lexicon)+ApplicationSetup.EOL);
			}
			for (int i=0; i<negDocids.length; i++){
				if (negDocids[i]==-1)
					continue;
				try{
					System.out.print("processing negative document "+metaIndex.getItem(key, negDocids[i])+" for query "+queryid+"...");
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
				TIntDoubleHashMap map = new TIntDoubleHashMap();
				int[] oneDocid = {negDocids[i]};
				extractQueryFeature(oneDocid, queryid, queryTermids, map);			
				System.out.println("Done. "+(++counter)+" out of "+totalDocs+" documents processed.");
				buf.append(this.formatPreProcess(negDocids[i], queryid, -1, map, lexicon)+ApplicationSetup.EOL);
			}
			System.out.print("processing all documents...");
			TIntDoubleHashMap map = new TIntDoubleHashMap();
			TIntHashSet docidSet = new TIntHashSet(posDocids);
			docidSet.addAll(negDocids);
			extractQueryFeature(docidSet.toArray(), queryid, queryTermids, map);
			System.out.println("Done. ");
			buf.append(this.formatPreProcess(-2, queryid, -2, map, lexicon)+ApplicationSetup.EOL);
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * 
	 * @param docid identifier of the document for which feature is extracted
	 * @param featureMap
	 * @param queryTermids Identifiers of the query terms.
	 */
	abstract public void extractQueryFeature(int[] docids, String queryid, int[] queryTermids, TIntDoubleHashMap featureMap);
}
