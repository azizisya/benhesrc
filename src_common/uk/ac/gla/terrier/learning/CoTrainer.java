	/**
 * 
 */
package uk.ac.gla.terrier.learning;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import gnu.trove.THashMap;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;
import uk.ac.gla.terrier.documentfeature.DocumentFeatureServer;
import uk.ac.gla.terrier.documentfeature.GlobalDocumentFeatureServer;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.learning.structures.WekaPredictResults;
import uk.ac.gla.terrier.learning.structures.WekaPredictResultsWithRanking;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.StringUtility;

/**
 * @author ben
 *
 */
public class CoTrainer extends RFTrainer{
	protected static Logger logger = Logger.getRootLogger();
	/** number of iterations. */
	protected int K = Integer.parseInt(ApplicationSetup.getProperty("cot.K", "2"));
	/** number of added positive example per iteration */
	protected int cotp = Integer.parseInt(ApplicationSetup.getProperty("cot.p", "1"));
	/** number of added negative example per iteration */
	protected int cotn = Integer.parseInt(ApplicationSetup.getProperty("cot.n", "2"));
	/** Only takes the x most confident positive document into account. */
	protected int cotxPos = Integer.parseInt(ApplicationSetup.getProperty("cot.x.pos", "20"));
	
	private double ROC_threshold = Double.parseDouble(ApplicationSetup.getProperty("cot.roc", "0.80d"));
	
	/** The classifiers. */
	protected LearningAlgorithm classifier1;
	
	protected LearningAlgorithm classifier2;
	
	/** A pointer to the classifier being used. */
	protected LearningAlgorithm classifier;
	
	/** arguments when running the classifier.  */
	protected String args;
	
	protected DocumentFeatureServer fServer;
	
	protected TRECQrelsInMemory qrels;

	/**
	 * 
	 * @param fServer
	 * @param methodName
	 * @param args
	 * @param qrels
	 */
	public CoTrainer(String methodName, String args, TRECQrelsInMemory qrels) {
		super(methodName, args, qrels);
		String fServerName = ApplicationSetup.getProperty("doc.feature.server.name", "");
		try{
			this.fServer = (DocumentFeatureServer)Class.forName("uk.ac.gla.terrier.documentfeature."+fServerName).newInstance();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		
		this.fServer = fServer;
		classifier1 = new WekaClassifier(methodName);
		classifier2 = new WekaClassifier(ApplicationSetup.getProperty("weka.classifier2.name", 
				ApplicationSetup.getProperty("weka.classifier.name", "weka.classifiers.functions.Logistic")));
		this.args = args;
		this.qrels = qrels;
	}
	
	/**
	 * Run the training process to get increment the positive/negative training documents.
	 * The method returns an ArrayList containing two int arrays. The first array contains docids of positive documents,
	 * and the second array contains those of the negative ones. 
	 * @param qid
	 * @param posDocids Initial positive examples
	 * @param negDocids Initial negative examples
	 * @param retDocids Retrieved documents
	 * @param outputFolder The folder to write temporiary files.
	 * @return
	 */
	public ArrayList<int[]> run(String qid, int[] posDocids, int[] negDocids, int[] retDocids, TIntHashSet posCandSet, String outputFolder){
		
		for (int iteCounter=0; iteCounter<K*2; iteCounter++){
			// naming pattern: qid_train/test.arff.x.gz
			String trainArffFilename = outputFolder+ApplicationSetup.FILE_SEPARATOR+qid+"_train."+iteCounter+".arff.gz"; 
			String testArffFilename = outputFolder+ApplicationSetup.FILE_SEPARATOR+qid+"_test."+iteCounter+".arff.gz";
			String testIdMapFilename = outputFolder+ApplicationSetup.FILE_SEPARATOR+qid+"_test."+iteCounter+".arff.map.gz";
			logger.debug("Iteration "+(iteCounter+2)/2+"."+(iteCounter%2)+", args: "+args);
			classifier = (iteCounter%2==0)?(classifier1):(classifier2);
			if (createTrainTestArffFiles(qid, posDocids, negDocids, retDocids, trainArffFilename, 
					testArffFilename, iteCounter%2)==0){
				ArrayList<int[]> list = new ArrayList<int[]>();
				list.add(posDocids); list.add(negDocids);
				this.clean(trainArffFilename, testArffFilename, null, null);
				return list;
			}
			String[] files = this.iteration(trainArffFilename, testArffFilename);
			if (files == null){
				ArrayList<int[]> list = new ArrayList<int[]>();
				list.add(posDocids); list.add(negDocids);
				this.clean(trainArffFilename, testArffFilename, null, null);
				return list;
			}
			WekaPredictResultsWithRanking pred = new WekaPredictResultsWithRanking(
					files[1],
					testIdMapFilename,
					qid,
					retDocids
					);
			// get the most confidently predicted positive / negative instances
			String[] keys = pred.getPredictedEntriesDescending(1);
			TIntHashSet docidSet = new TIntHashSet(posDocids);
			if (keys.length==0){
				logger.debug("Classifier found no positive document for feedback.");
				break;
			}
			int limit = Math.min(cotp, keys.length);
			int counter = 0;
			for (int i=0; i<cotxPos; i++){
				int docid = Integer.parseInt(keys[i].split("\\.")[1]);
				if (posCandSet.contains(docid)){
					docidSet.add(docid);
					if (docidSet.size() == limit)
						break;
					logger.debug("Positive: Document "+docid+" added to the positive set.");
					counter++;
					if (counter == limit)
						break;
				}//else
					//logger.debug("Ignore: Document "+docid+" not in the candidate set.");
				if (i==keys.length-1)
					break;
			}
			posDocids = docidSet.toArray();
			
			keys = pred.getPredictedEntriesDescending(-1);
			docidSet = new TIntHashSet(negDocids);
			if (keys.length == 0){
				logger.debug("Classifier found no negative document for training.");
			}else{
				limit = Math.min(cotn, keys.length);
				for (int i=0; i<limit; i++){
					int docid = Integer.parseInt(keys[i].split("\\.")[1]);
					docidSet.add(docid);
					logger.debug("Negative: Document "+docid+" added to the negative set.");
				}
				negDocids = docidSet.toArray();
			}
			
			TIntHashSet retDocidSet = new TIntHashSet(retDocids);
			retDocidSet.removeAll(posDocids); retDocidSet.removeAll(negDocids);
			
			if (retDocidSet.size() == 0){
				logger.debug("No more candidate documents.");
				break;
			}
			
			keys = null;
			docidSet.clear(); docidSet = null;
			retDocidSet.clear(); retDocidSet = null;
			this.clean(trainArffFilename, testArffFilename, files[0], files[1]);
		}
		ArrayList<int[]> list = new ArrayList<int[]>();
		list.add(posDocids); list.add(negDocids);
		return list;
	}
	
	protected void clean(String trainArffFilename, String testArffFilename, String modelFilename, String predictFilename){
		// remove arff files
		(new File(trainArffFilename)).delete();
		(new File(testArffFilename)).delete();
		// remove map files
		String mapFilename = trainArffFilename.substring(0, trainArffFilename.lastIndexOf('.'))+".map.gz";
		(new File(mapFilename)).delete();
		mapFilename = testArffFilename.substring(0, testArffFilename.lastIndexOf('.'))+".map.gz";
		(new File(mapFilename)).delete();
		// remove model file
		if (modelFilename!=null)
			(new File(modelFilename)).delete();
		// remove predict file
		if (predictFilename!=null)
			(new File(predictFilename)).delete();
	}
	
	protected int createTrainTestArffFiles(
			String qid, 
			int[] posDocids, 
			int[] negDocids,
			int[] retDocids,
			String trainArffFilename, 
			String testArffFilename,
			int classifierId){
		// create data structures for train/test instances respectively. Data should be organized in the following format:
		// String[] ids, int[] labels, THashMap<String, double[]> dataMap, String outputFilename
		
		// convert training data
		int nInst = posDocids.length+negDocids.length;
		ArrayList<String>  idList = new ArrayList<String>();
		TIntArrayList labelList = new TIntArrayList();
		THashMap<String, double[]> dataMap = new THashMap<String, double[]>();
		int counter = 0;
		for (int i=0; i<posDocids.length; i++){
			String key = qid+"."+posDocids[i];
			double[] values = null;
			if (classifierId == 0)
				values = fServer.getOddFeatures(qid, posDocids[i]);
			else if (classifierId == 1)
				values = fServer.getEvenFeatures(qid, posDocids[i]);
			if (values!=null){
				dataMap.put(key, values);
				idList.add(key);
				labelList.add(1);
			}
		}
		
		for (int i=0; i<negDocids.length; i++){
			String key = qid+"."+negDocids[i];
			double[] values = null;
			if (classifierId == 0)
				values = fServer.getOddFeatures(qid, negDocids[i]);
			else if (classifierId == 1)
				values = fServer.getEvenFeatures(qid, negDocids[i]);
			if (values!=null){
				dataMap.put(key, values);
				idList.add(key);
				labelList.add(-1);
			}
		}
		
		String[] ids = idList.toArray(new String[idList.size()]);
		int[] labels = labelList.toNativeArray();
		
		if (ids.length == 0)
			return 0;
		
		DataFormatConvertor.DataToArff(ids, labels, dataMap, trainArffFilename);
		
		// find remaining test data using results and feature server
		TIntHashSet labelledDocidSet = new TIntHashSet(posDocids);
		labelledDocidSet.addAll(negDocids);
		// TIntHashSet docidSet = new TIntHashSet(StringUtility.stringsToInts(results.getDocnoSet(qid)));
		
		TIntHashSet docidSet = new TIntHashSet(retDocids);
		
		docidSet.removeAll(labelledDocidSet.toArray());
			// consider docs for which features do not exist in the fserver
		docidSet.retainAll(fServer.getDocids(qid));
		labelledDocidSet.clear(); labelledDocidSet = null;
		int[] docids = docidSet.toArray();
		ids = new String[docids.length];
		labels = new int[docids.length];
		dataMap.clear(); dataMap = new THashMap<String, double[]>();
		for (int i=0; i<docids.length; i++){
			ids[i] = qid+"."+docids[i];
			labels[i] = (qrels.isRelevant(qid, ""+docids[i]))?(1):(-1);
			double[] values = null;
			if (classifierId == 0)
				values = fServer.getOddFeatures(qid, docids[i]);
			else if (classifierId == 1)
				values = fServer.getEvenFeatures(qid, docids[i]);
			if (values!=null)
				dataMap.put(ids[i], values);
		}
		// convert test data arff files
		if (ids.length == 0)
			return 0;
		DataFormatConvertor.DataToArff(ids, labels, dataMap, testArffFilename);
		ids=null; labels=null; idList.clear(); labelList.clear(); idList=null; labelList=null;
		return 1;
	}
	
	protected String[] iteration(String trainArffDataFilename, String testArffDataFilename){
		System.out.println("Training...");
		String modelFilename = classifier.learn(trainArffDataFilename, args);
		double roc = classifier.getPerformanceByClass(0)[5];
		logger.debug("Weighted Avg. ROC Area: "+roc);
		if (roc<this.ROC_threshold){
			logger.debug("Roc Area lower than threshold. Iteration ends.");
			return null;
		}
		
		TObjectIntHashMap<String> idLabelMap = new TObjectIntHashMap<String>();
		TObjectDoubleHashMap<String> idConfMap = new TObjectDoubleHashMap<String>();
		System.out.println("Testing...");
		String predictFilename = classifier.predict(testArffDataFilename, modelFilename, idLabelMap, idConfMap);
		String[] files = {modelFilename, predictFilename};
		return files;
	}

	/*protected void iteration(String[] trainIds, int[] labels, THashMap<String, double[]> trainDataMap,
			String[] testIds, THashMap<String, double[]> testDataMap){
		String modelFilename = classifier.learn(trainIds, labels, trainDataMap, args);
	}*/
	
	
	// takes 
	public static void main(String[] args){
		if (args[0].equals("--test")){
			// --test classifierName trainArffFilename testArffFilename args
			String arguments = (args.length<=4)?(""):(args[4]);
			// EMTraining app = new EMTraining(args[1], arguments);
			// app.start(args[2], args[3]);
		}
	}

}
