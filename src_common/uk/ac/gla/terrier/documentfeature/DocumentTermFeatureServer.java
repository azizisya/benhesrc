package uk.ac.gla.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.structures.SingleLineTRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

public class DocumentTermFeatureServer extends DocumentFeatureServer{
	protected static Logger logger = Logger.getRootLogger();
	/**  TIntDoubleHashMap: termid to term weight.        
	 *   TIntObjectHashMap: docid to TIntObjectHashMap
	 *   THashMap: qid to TIntObjectHashMap
	 * */
	protected THashMap<String, TIntObjectHashMap<TIntDoubleHashMap>> repos;
	/** Map from query id to feature space size. */
	protected TObjectIntHashMap<String> qidFeatureSpaceSizeMap;
	
	
	
	public DocumentTermFeatureServer() {
		super();
		String reposFilename = ApplicationSetup.getProperty("doc.feature.expterm.filename", "Full path must be given");
		this.loadFeatureValues(reposFilename);
	}

	public TIntDoubleHashMap getTerms(String qid, int docid){
		if (!repos.containsKey(qid))
			return null;
		return repos.get(qid).get(docid);
	}
	
	public double[] getAllFeatures(String qid, int docid){
		TIntDoubleHashMap termMap = this.getTerms(qid, docid);
		if (termMap==null)
			return null;
		int size = qidFeatureSpaceSizeMap.get(qid);
		double[] values = new double[size];
		for (int i=1; i<=size; i++)
			values[i-1] = termMap.get(i);
		return values;
	}
	
	public double[] getOddFeatures(String qid, int docid){
		double[] values = this.getAllFeatures(qid, docid);
		if (values==null)
			return null;
		int size = (values.length%2==0)?(values.length/2):(values.length/2+1);
		double[] selValues = new double[size];
		for (int i=0; i<size; i++){
			selValues[i] = values[i*2];
		}
		return selValues;
	}
	
	public double[] getEvenFeatures(String qid, int docid){
		double[] values = this.getAllFeatures(qid, docid);
		if (values==null)
			return null;
		int size = values.length/2;
		double[] selValues = new double[size];
		for (int i=0; i<size; i++){
			selValues[i] = values[i*2];
		}
		return selValues;
	}
	
	public TIntObjectHashMap<TIntDoubleHashMap> getDocuments(String qid){
		return repos.get(qid);
	}
	
	public int[] getDocids(String qid){
		return repos.get(qid).keys();
	}
	
	/**
	 * 
	 * @param qfs
	 * @return
	 */
	protected void termFeatureMapping(String qid, TIntObjectHashMap<TIntDoubleHashMap> qfs){
		TIntHashSet uniqueTermidSet = new TIntHashSet();
		for (int docid : qfs.keys()){
			uniqueTermidSet.addAll(qfs.get(docid).keys());
		}
		int[] uniqueTermids = uniqueTermidSet.toArray();
		TIntIntHashMap termFeatureIdMap = new TIntIntHashMap();
		for (int i=0; i<uniqueTermids.length; i++)
			termFeatureIdMap.put(uniqueTermids[i], i+1);
		Arrays.sort(uniqueTermids);
		for (int docid : qfs.keys()){
			TIntDoubleHashMap termidValueMap = qfs.get(docid);
			TIntDoubleHashMap bufMap = new TIntDoubleHashMap();
			for (int termid : termidValueMap.keys())
				bufMap.put(termFeatureIdMap.get(termid), termidValueMap.get(termid));
			termidValueMap.clear(); termidValueMap = null;
			qfs.put(docid, bufMap);
		}
		qidFeatureSpaceSizeMap.put(qid, uniqueTermids.length);
		uniqueTermidSet.clear(); uniqueTermidSet = null;
		uniqueTermids = null;
		termFeatureIdMap.clear(); termFeatureIdMap = null;
	}
	
	/**
	 * Each line is formatted as follows: qid docid termid^value
	 * @param reposFilename
	 */
	protected void loadFeatureValues(String reposFilename){
		repos = new THashMap<String, TIntObjectHashMap<TIntDoubleHashMap>>();
		qidFeatureSpaceSizeMap = new TObjectIntHashMap<String>();
		try{
			BufferedReader br = Files.openFileReader(reposFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] segments = line.split(":");
				if (segments.length<2)
					continue;
				String[] tokens = segments[0].trim().split(" ");
				if (tokens.length>2)
					continue;
				String qid = tokens[0]; int docid = Integer.parseInt(tokens[1]);
				if (!repos.containsKey(qid))
					repos.put(qid, new TIntObjectHashMap<TIntDoubleHashMap>());
				TIntObjectHashMap<TIntDoubleHashMap> qFeatures = repos.get(qid);
				if (!qFeatures.containsKey(docid))
					qFeatures.put(docid, new TIntDoubleHashMap());
				TIntDoubleHashMap map = qFeatures.get(docid);
				TObjectDoubleHashMap<String> termWeightMap = null;
				try{
					termWeightMap = SingleLineTRECQuery.parseQueryStringWithWeights(segments[1]);
				}catch(ArrayIndexOutOfBoundsException e){
					logger.debug(line);
					e.printStackTrace();
					System.exit(1);
				}
				String[] terms = termWeightMap.keys(new String[termWeightMap.size()]);
				for (String term : terms){
					map.put(Integer.parseInt(term), termWeightMap.get(term));
				}
				terms = null; termWeightMap.clear(); termWeightMap=null;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		String[] qids = (String[])repos.keySet().toArray(new String[repos.keySet().size()]);
		for (String qid : qids){
			this.termFeatureMapping(qid, repos.get(qid));
		}
	}
	
}
