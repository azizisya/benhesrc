package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.terrier.matching.models.WeightingModel;
import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.DirectIndex;
import org.terrier.structures.DocumentIndex;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.SingleLineTRECQuery;
import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.Rounding;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntObjectHashMap;
import org.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.utility.StringUtility;

abstract public class DocumentFeature {
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	protected Index index;
	
	protected DirectIndex directIndex;
	
	protected DocumentIndex docIndex;
	
	protected CollectionStatistics collStats;
	
	protected Lexicon lexicon;
	
	protected int featureId;
	
	protected boolean CACHED;
	
	protected String cacheFolder = ApplicationSetup.getProperty("document.feature.cache", "");
	
	protected THashMap metaMap = new THashMap();
	
	public DocumentFeature(Index index){
		this.index = (index==null)?(null):(index);
		if (index!=null){
			this.directIndex = index.getDirectIndex();
			this.lexicon = index.getLexicon();
			this.docIndex = index.getDocumentIndex();
			this.collStats = index.getCollectionStatistics();
		}
	}
	
	abstract public String getInfo();
	
	protected abstract void loadMetaData();
	
	public void preProcess(String qrelsFilename, String queryid, String outputFilename, int featureId){
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
			logger.info("processing positive document "+posDocids[i]+"...");
			TIntObjectHashMap map = new TIntObjectHashMap();
			extractDocumentFeature(posDocids[i], queryid, queryTermids, map);			
			String value = ""+map.get(featureId);
			logger.info("Done. Value="+value+". "+(++counter)+" out of "+totalDocs+" documents processed.");
			buf.append(posDocids[i]+" "+queryid+" "+value+" 1"+ApplicationSetup.EOL);
		}
		for (int i=0; i<negDocids.length; i++){
			if (negDocids[i]==-1)
				continue;
			logger.info("processing negative document "+negDocids[i]+"...");
			TIntObjectHashMap map = new TIntObjectHashMap();
			extractDocumentFeature(negDocids[i], queryid, queryTermids, map);
			String value = ""+map.get(featureId);
			logger.info("Done. Value="+value+". "+(++counter)+" out of "+totalDocs+" documents processed.");
			buf.append(negDocids[i]+" "+queryid+" "+value+" -1"+ApplicationSetup.EOL);
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
	
	protected abstract void loadCache(File fCache);
	
	public void preProcessAll(String qrelsFilename, String outputFilename, int featureId){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		TRECQuery queries = null;
		String topicFilename = ApplicationSetup.getProperty("trec.topics", null);
		logger.info("qrels: "+qrelsFilename);
		logger.info("topics: "+topicFilename);
		try{
			/**queries = (TRECQuery)Class.forName("uk.ac.gla.terrier.structures."+
				ApplicationSetup.getProperty("trec.topics.parser", 
				"TRECQuery")).newInstance();*/
		
			if (topicFilename == null)
				queries = new SingleLineTRECQuery();
			else
				queries = new SingleLineTRECQuery(topicFilename);
				
			/**
			String[] args = {topicFilename};
			
			queries = (TRECQuery)Class.forName(
					"uk.ac.gla.terrier.structures."+
					ApplicationSetup.getProperty("trec.topics.parser", 
					"TRECQuery"))
			.getConstructor(
					new Class[]{String[].class})
			.newInstance(
					new Object[]{args});
			*/
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		// String[] queryids = qrels.getQueryids();
		String[] queryids = queries.getQueryIds();
		Arrays.sort(queryids);
		StringBuffer buf = new StringBuffer();
		for (String queryid : queryids){
			logger.info(">>>>>Processing query "+queryid);
			String[] posDocnos = qrels.getRelevantDocumentsToArray(queryid);
			String[] negDocnos = qrels.getNonRelevantDocumentsToArray(queryid);
			int[] posDocids = StringUtility.stringsToInts(posDocnos);
			int[] negDocids = StringUtility.stringsToInts(negDocnos);
			int[] queryTermids = null;
			queryTermids = ArffFeatureExtractor.processQuery(queries.getQuery(queryid), index).keys();		
			int counter = 0; int totalDocs = posDocids.length+negDocids.length;
			for (int i=0; i<posDocids.length; i++){
				if (posDocids[i]==-1)
					continue;
				logger.info("processing positive document "+posDocids[i]+"...");
				TIntObjectHashMap map = new TIntObjectHashMap();
				extractDocumentFeature(posDocids[i], queryid, queryTermids, map);			
				String value = ""+map.get(featureId);
				logger.info("Done. Value="+value+". "+(++counter)+" out of "+totalDocs+" documents processed.");
				buf.append(posDocids[i]+" "+queryid+" "+value+" 1"+ApplicationSetup.EOL);
			}
			for (int i=0; i<negDocids.length; i++){
				if (negDocids[i]==-1)
					continue;
				logger.info("processing negative document "+negDocids[i]+"...");
				TIntObjectHashMap map = new TIntObjectHashMap();
				extractDocumentFeature(negDocids[i], queryid, queryTermids, map);
				String value = ""+map.get(featureId);
				logger.info("Done. Value="+value+". "+(++counter)+" out of "+totalDocs+" documents processed.");
				buf.append(negDocids[i]+" "+queryid+" "+value+" -1"+ApplicationSetup.EOL);
			}
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
	abstract public void extractDocumentFeature(int docid, String queryid, int[] queryTermids, TIntObjectHashMap featureMap);
}
