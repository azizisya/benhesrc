package uk.ac.gla.terrier.learning;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import weka.classifiers.Classifier;
import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

public class WekaClassifier extends LearningAlgorithm {
	protected static final Runtime r = Runtime.getRuntime();
	
	protected static Logger logger = Logger.getRootLogger();
	
	/**
	 * Mapping from class label to performance array.
	 * The array consists of five elements: TP Rate   FP Rate   Precision   Recall  F-Measure   ROC Area.
	 */
	TIntObjectHashMap<double[]> performance;
	
	protected String tmpArffFilename = ApplicationSetup.TREC_RESULTS+ApplicationSetup.FILE_SEPARATOR+"data.arff.gz"; 

	public WekaClassifier(String methodName) {
		super(methodName);
	}

	@Override
	public String learn(String[] ids, int[] labels,
			THashMap<String, double[]> dataMap, String args) {
		DataFormatConvertor.DataToArff(ids, labels, dataMap, tmpArffFilename);
		return learn(tmpArffFilename, args);
	}

	protected String getModelName(String arffDataFilename){
		return arffDataFilename+// xxxx/data.arff.gz
				classifierName.substring(classifierName.lastIndexOf('.'), classifierName.length())+//.NaiveBayes
				".model";
	}
	
	protected void parsePerformance(String output){
		StringTokenizer stk = new StringTokenizer(output.replaceAll(ApplicationSetup.EOL, " "));
		boolean stratified = false;
		performance = new TIntObjectHashMap<double[]>();
		while(stk.hasMoreTokens()){
			String token = stk.nextToken();
			if (token.equals("Stratified")){
				stratified = true;
			}else if (token.equals("ROC") && stratified){
				/**
				 * 					TP Rate   FP Rate   Precision   Recall  F-Measure   ROC Area  Class
                 					0.471     0.258      0.805     0.471     0.594      0.652    1
                 					0.742     0.529      0.383     0.742     0.506      0.652    -1
					Weighted Avg.    0.554     0.341      0.675     0.554     0.567      0.652
				 */
				stk.nextToken();// ignore Area
				stk.nextToken();// ignore Class
				double[] result1 = new double[6];
				for (int i=0; i<result1.length; i++)
					try{
						result1[i] = Double.parseDouble(stk.nextToken());
					}catch(NumberFormatException e){
						logger.error(output);
						e.printStackTrace();
						System.exit(1);
					}
				int label = Integer.parseInt(stk.nextToken());
				performance.put(label, result1);
				
				double[] result2 = new double[6];
				for (int i=0; i<result2.length; i++)
					result2[i] = Double.parseDouble(stk.nextToken());
				try{
					label = Integer.parseInt(stk.nextToken());
				}catch(Exception e){
					e.printStackTrace();
					logger.debug(output);
					for (int i=0; i<result2.length; i++)
						logger.debug("result2."+i+"="+result2[i]);
					System.exit(1);
				}
				performance.put(label, result2);
				// get weighted average
				stk.nextToken(); // ignore Weighted
				stk.nextToken(); // ignore Avg.
				
				double[] resultAvg = new double[6];
				for (int i=0; i<resultAvg.length; i++)
					resultAvg[i] = Double.parseDouble(stk.nextToken());
				performance.put(0, resultAvg);
				
				break;
			}
		}
		stk = null;
	}
	
	public double[] getPerformanceByClass(int label){
		return performance.get(label);
	}
	
	@Override
	public String learn(String arffDataFilename, String args) {
		String modelFilename = null;
		modelFilename = this.getModelName(arffDataFilename);
		try{
			String optionString = "-t "+arffDataFilename+" -d "+modelFilename+" -i "+args;
			String[] options = {"-t", arffDataFilename, "-d", modelFilename, "-i"};//weka.core.Utils.splitOptions(optionString);
			// Classifier classifier = Classifier.forName(classifierName, null);
			String output = Classifier.runClassifier(Classifier.forName(classifierName, null), weka.core.Utils.splitOptions(optionString));
			this.parsePerformance(output);
			output = null; options = null; optionString = null;
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		/**
		try{
			Process learn = null;
			int attempts = 5;
			Exception lastE = null;
			modelFilename = this.getModelName(arffDataFilename);
			while(learn == null && attempts-- > 0)
			{
				lastE = null;
				try{
					//IOException occurs here, perhaps java-vm process to big to fork?
					// learn = r.exec("wekaClassification " + classifierName +" "+arffDataFilename+" train "+args);
					String heapSpace = "2048M";
					System.err.println("modelFilename: "+modelFilename);
					learn = r.exec("java -Xmx"+heapSpace+" -classpath /home/ben/workspace/lib/weka.jar:/users/grad/ben/workspace/lib/libsvm.jar "+
			                classifierName+" -t "+arffDataFilename+" -d "+modelFilename+" -i "+args);
				} catch (Exception e) {
					lastE = e;	
				}
			}
			if (learn == null && attempts-- == 0 && lastE != null)
			{
				throw lastE;
			}
	
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(learn.getInputStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
			}
			//(new File(filename)).delete();
			br.close();	br = null;
			br = new BufferedReader(new InputStreamReader(learn.getErrorStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
			}
			//(new File(filename)).delete();
			br.close(); br=null;
			try{
				learn.waitFor(); //wait for completion
			} catch (InterruptedException ie) {
			} finally { 
				learn.destroy();
			}
			int exitCode = learn.exitValue();
			System.err.println("exit code was " + exitCode);
			if (exitCode == 0)
			{
				try{
					(new File(modelFilename)).delete();
				} catch (Exception e) {
					System.err.println("WARNING: Failed to delete a results file: "+ e);
				}
			}
			learn = null;
			System.gc(); System.runFinalization();	
		}catch(Exception ioe) {
			System.err.println("exception... : "+ioe);
			ioe.printStackTrace();
			System.exit(1);
		}*/
		return modelFilename;
	}

	@Override
	public String predict(String testArffFilename, String modelFilename, 
			TObjectIntHashMap<String> idLabelHashMap, TObjectDoubleHashMap<String> idConfHashMap) {
		String predictFilename = testArffFilename+
			classifierName.substring(classifierName.lastIndexOf('.'), classifierName.length())+".predict";
		System.out.println("predictFilename: "+predictFilename);
		try{
			String optionString = "-i -l "+modelFilename+" -T "+testArffFilename+" -y "+predictFilename+" -p 0";
			Classifier classifier = Classifier.forName(classifierName, null);
			String[] options = weka.core.Utils.splitOptions(optionString);
			for (int i=0; i<options.length; i++)
				System.out.println((i+1)+": "+options[i]);
			Classifier.runClassifier(classifier, options);			
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		return predictFilename;
		/**
		try{
			Process predict = null;
			int attempts = 5;
			Exception lastE = null;
			while(predict == null && attempts-- > 0)
			{
				lastE = null;
				try{
					predict = r.exec("wekaClassification " + classifierName +" "+testArffFilename+" test "+modelFilename);
				} catch (Exception e) {
					lastE = e;	
				}
			}
			if (predict == null && attempts-- == 0 && lastE != null)
			{
				throw lastE;
			}
	
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(predict.getInputStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
			}
			//(new File(filename)).delete();
			br.close();	br = null;
			br = new BufferedReader(new InputStreamReader(predict.getErrorStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
			}
			//(new File(filename)).delete();
			br.close(); br=null;
			try{
				predict.waitFor(); //wait for completion
			} catch (InterruptedException ie) {
			} finally { 
				predict.destroy();
			}
			int exitCode = predict.exitValue();
			System.err.println("exit code was " + exitCode);
			//if (exitCode != 0){
				
			//}
			predict = null;
			System.gc(); System.runFinalization();	
		}catch(Exception ioe) {
			System.err.println("exception... : "+ioe);
			ioe.printStackTrace();
			System.exit(1);
		}
		*/
	}

	public String predict(String[] ids, THashMap<String, double[]> dataMap, String modelFilename, 
			TObjectIntHashMap<String> idLabelHashMap, TObjectDoubleHashMap<String> idConfHashMap){
		return null;
	}

}
