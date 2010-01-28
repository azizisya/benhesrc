/*
 * Created on 2005-2-22
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.tuning;

import java.util.Vector;

import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.PorterStopPipeline;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CorrelationTuning extends ParameterTuning{
	
	protected PorterStopPipeline pipe;
	
	//protected BufferedMatching matching;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	public CorrelationTuning(String methodName, Index index){
		super(methodName, index);
		pipe = new PorterStopPipeline();
		//matching = new BufferedMatching(index);
	}
	
	public double getCorrelationTFLength(double[] tf, double[] docLength, double parameter){
		double[] tfn = new double[tf.length];
		this.method.setParameter(parameter);
		for (int i = 0; i < tf.length; i++)
			tfn[i] = method.getNormalisedTermFrequency(tf[i], docLength[i]);
		return Statistics.correlation(tfn, docLength);
	}
	
	public double getParameter(double[] tf, double[] docLength, double target,
			boolean increasing){
		double left = 0.00001;
		double right = 1d;
		if (this.method.getInfo().endsWith("2")){
			if (increasing){
				left = this.getMinCorrelation(tf, docLength)[0];
				right = 128d;
			}
			else{
				right = this.getMinCorrelation(tf, docLength)[0];
			}
		}
		if (this.method.getInfo().endsWith("B")){
//			if (increasing){
//				right = this.getMaxCorrelation(tf, docLength)[0];
//			}
//			else{
//				left = this.getMaxCorrelation(tf, docLength)[0];
//			}
			increasing = false;
		}
		if (this.method.getInfo().endsWith("3")){
			if (increasing){
				left = this.getMinCorrelation(tf, docLength)[0];
				right = 10000d;
			}
			else{
				right = this.getMinCorrelation(tf, docLength)[0];
			}
		}
		if (debugging)
			System.out.println("left: " + left + ", right: " + right);
		if (left < -1 || right < -1)
			return -1;
		return this.getParameter(tf, docLength, left, right, target, increasing);
	}
	
	public double[] getMinCorrelation(double[] tf, double[] docLength){
		double left = 0d;
		double end = 1d;
		double interval = 0.01;
		double parameter = left+interval;
		//double limit = 0.001;
		double[] min = null;
		if (this.method.getInfo().endsWith("2")){
			end = 5d;
		}
		if (this.method.getInfo().endsWith("B")){
			
		}
		if (this.method.getInfo().endsWith("3")){
			left = 50d;
			interval = 50d;
			end = 2000d;
		}
		double previousCorr = this.getCorrelationTFLength(tf, docLength, parameter);
		while (true){
			double previousParameter = parameter;
			parameter += interval;
			double corr = this.getCorrelationTFLength(tf, docLength, parameter);
			if (debugging){
				System.out.println("previous parameter: " 
						+ Rounding.toString(previousParameter, 2)
						+ ", previous correlation: " + Rounding.toString(previousCorr, 4)
				);
				System.out.println("current parameter: " 
						+ Rounding.toString(parameter, 2)
						+ ", current correlation: " + Rounding.toString(corr, 4)
				);		
			}
			if (corr == Double.NaN){
				min = new double[2];
				min[0] = -2;
				min[1] = -2;
				return min;
			}
			if (corr > previousCorr){
				min = new double[2];
				min[0] = previousParameter;
				min[1] = previousCorr;
				return min;
			}
			else{
				previousCorr = corr;
				if (parameter > end){
					min = new double[2];
					min[0] = -2;
					min[1] = -2;
					return min;
				}
			}
		}
	}
	
	public double[] getMaxCorrelation(double[] tf, double[] docLength){
		double left = 0d;
		double end = 1d;
		double interval = 0.01;
		double parameter = left+interval;
		//double limit = 0.001;
		double[] max = null;
		if (this.method.getInfo().endsWith("B")){
			
		}
		double previousCorr = this.getCorrelationTFLength(tf, docLength, parameter);
		while (true){
			double previousParameter = parameter;
			parameter += interval;
			double corr = this.getCorrelationTFLength(tf, docLength, parameter);
			if (debugging){
				System.out.println("previous parameter: " 
						+ Rounding.toString(previousParameter, 2)
						+ ", previous correlation: " + Rounding.toString(previousCorr, 4)
				);
				System.out.println("current parameter: " 
						+ Rounding.toString(parameter, 2)
						+ ", current correlation: " + Rounding.toString(corr, 4)
				);		
			}
			if (corr == Double.NaN){
				max = new double[2];
				max[0] = -2;
				max[1] = -2;
				return max;
			}
			if (corr < previousCorr){
				max = new double[2];
				max[0] = previousParameter;
				max[1] = previousCorr;
				return max;
			}
			else{
				previousCorr = corr;
				if (parameter > end){
					max = new double[2];
					max[0] = -2;
					max[1] = -2;
					return max;
				}
			}
		}
	}
	
	public double getParameter(double[] tf, double[] docLength, 
			double left, double right, double target, boolean increasing){
		double leftCorr = this.getCorrelationTFLength(tf, docLength, left);
		double rightCorr = this.getCorrelationTFLength(tf, docLength, right);
		if (debugging){
			System.out.println("leftCorr: " + Rounding.toString(leftCorr, 4) +
					", rightCorr: " + Rounding.toString(rightCorr, 4) +
					", target: " + Rounding.toString(target, 4));
		}
		if ((leftCorr>target&&rightCorr>target)||
				(leftCorr<target&&rightCorr<target)){
			return -1;
		}
		if (left < 0 || right < 0){
			return -1;
		}
		double error = 0.005;
		double parameter = (left+right)/2;
		while (true){
			double corr = this.getCorrelationTFLength(tf, docLength, parameter);
			if (debugging){
				System.out.println("left: " + Rounding.toString(left, 4) +
						", parameter: " + Rounding.toString(parameter, 4) +
						", right: " + Rounding.toString(right, 4));
				System.out.println("leftCorr: " + Rounding.toString(leftCorr, 4) +
						", corr: " + Rounding.toString(corr, 4) +
						", rightCorr: " + Rounding.toString(rightCorr, 4));
			}
			if (Math.abs(corr-target) < error){
				return parameter;
			}
			if (right - left < 0.001){
				return -1;
			}
			if (increasing){
				if (corr > target){
					right = parameter;
					parameter = (left+right)/2;
				}
				else{
					left = parameter;
					parameter = (left+right)/2;
				}
			}
			else{
				if (corr < target){
					right = parameter;
					parameter = (left+right)/2;
				}
				else{
					left = parameter;
					parameter = (left+right)/2;
				}
			}
		}
	}
	
	public double tuneRealTRECQuery(){
//		TRECQuery queries = new TRECQuery();
//		Vector vecDocLength = new Vector();
//		while (queries.hasMoreQueries()){
//			BasicQuery query = new BasicQuery(queries.nextQuery(), 
//					queries.getQueryId(), pipe);
//			System.out.println("processing query " + query.getQueryNumber());
//			matching.matchWithoutScoring(query.getQueryNumber(), 
//					query.getQueryTermStrings());
//			vecDocLength.addElement(matching.getRetrievedDocLength());
//		}
//		System.out.println("tuning...");
		return 1;
	}
	
	public double tuneSampling(int numberOfSamples){
		System.out.println("Not implemented.");
		return -1;
	}
	
	public double tune(Vector vecDocLength){
//		double[][] docLength = new double[vecDocLength.size()][this.NUMBER_OF_BINS];
//		for (int i = 0; i < vecDocLength.size(); i++){
//			docLength[i] = (double[])vecDocLength.get(i);
//		}
		//return tune(docLength);
		return -1;
	}
	
	public double tune(double[][] docLength){
		return 1;
	}
	
	
	
}
