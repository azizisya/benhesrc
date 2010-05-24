package org.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;


import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;

public class GlobalDocumentFeatureServer extends DocumentFeatureServer{
	protected static Logger logger = Logger.getRootLogger();
	
	/** maps from feature name to feature values. */
	protected THashMap<String, DocumentFeatureRepository> server;
	
	/** Names of the features */
	protected String[] featureNames;
	
	/**
	 * Repository of values of a given feature for all documents and queries.
	 * @author ben
	 *
	 */
	public class DocumentFeatureRepository{
		/**
		 * Feature value of a document.
		 */
		private class DocumentFeatureEntry{
			public int label;
			public double value;
			public DocumentFeatureEntry(int label, double value){
				this.label = label;
				this.value = value;
			}
		}
		/** Feature name */
		public String name;
		/** Map from key to feature entry */
		protected THashMap<String, DocumentFeatureEntry> idEntryMap;
		/**
		 * Get feature value for given qid and docid
		 * @param qid
		 * @param docid
		 * @return
		 */
		public double getFeatureValue(String qid, int docid){
			return idEntryMap.get(qid+"."+docid).value;
		}
		/**
		 * Get label by query id and docid.
		 * @param qid
		 * @param docid
		 * @return
		 */
		public int getLabel(String qid, int docid){
			return idEntryMap.get(qid+"."+docid).label;
		}
		/**
		 * Get all docids associated to a given docid.
		 * @param qid
		 * @return
		 */
		public int[] getDocids(String qid){
			String[] ids = idEntryMap.keySet().toArray(new String[idEntryMap.size()]);
			TIntHashSet docidSet = new TIntHashSet();
			for (int i=0; i<ids.length; i++){
				String[] tokens = ids[i].split("\\."); 
				if (tokens[0].equals(qid))
					docidSet.add(Integer.parseInt(tokens[1]));
			}
			return docidSet.toArray();
		}
		/**
		 * Constructor.
		 * @param featureName
		 * @param reposFilename
		 */
		public DocumentFeatureRepository(String featureName, String reposFilename){
			idEntryMap = new THashMap<String, DocumentFeatureEntry>();
			try{
				BufferedReader br = Files.openFileReader(reposFilename);
				String line = null;
				while ((line=br.readLine())!=null){
					String[] tokens = line.split(" ");
					idEntryMap.put(tokens[1]+"."+tokens[0], new DocumentFeatureEntry(Integer.parseInt(tokens[3]), Double.parseDouble(tokens[2])));
				}
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
	}
	/**
	 * Constructor of the feature server.
	 * @param featureFolder
	 * @param featureNames
	 */
	public GlobalDocumentFeatureServer(){
		String featureFolder = ApplicationSetup.getProperty("doc.feature.path", "must be given");
		this.featureNames = ApplicationSetup.getProperty("doc.feature.names", "").split(",");
		Arrays.sort(this.featureNames);
		server = new THashMap<String, DocumentFeatureRepository>();
		for (int i=0; i<featureNames.length; i++)
			server.put(featureNames[i], 
					new DocumentFeatureRepository(featureNames[i], featureFolder+ApplicationSetup.FILE_SEPARATOR+featureNames[i]));
	}
	
	public double[] getOddFeatures(String qid, int docid){
		double[] values = this.getAllFeatures(qid, docid);
		int size = (values.length%2==0)?(values.length/2):(values.length/2+1);
		double[] selValues = new double[size];
		for (int i=0; i<size; i++){
			selValues[i] = values[i*2];
		}
		return selValues;
	}
	
	public double[] getEvenFeatures(String qid, int docid){
		double[] values = this.getAllFeatures(qid, docid);
		int size = values.length/2;
		double[] selValues = new double[size];
		for (int i=0; i<size; i++){
			selValues[i] = values[i*2];
		}
		return selValues;
	}
	
	public int[] getDocids(String qid){
		TIntHashSet docidSet = new TIntHashSet();
		for (int i=0; i<featureNames.length; i++)
			docidSet.addAll(server.get(featureNames[i]).getDocids(qid));
		return docidSet.toArray();
	}
	
	public double[] getAllFeatures(String qid, int docid){
		double[] values = new double[featureNames.length];
		for (int i=0; i<featureNames.length; i++){
			values[i] = server.get(featureNames[i]).getFeatureValue(qid, docid);
		}
		return values;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
