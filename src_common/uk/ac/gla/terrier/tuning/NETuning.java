/*
 * Created on 2005-2-22
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.tuning;

import java.io.IOException;
import java.util.Vector;

import uk.ac.gla.terrier.matching.BufferedMatching;
import uk.ac.gla.terrier.simulation.QuerySimulation;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.BasicQuery;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.PorterStopPipeline;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.SystemUtility;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class NETuning extends ParameterTuning{
	
	protected int NUMBER_OF_BINS;
	
	protected int DEFINITION_NE;
	
	protected PorterStopPipeline pipe;
	
	protected BufferedMatching matching;
	
	protected CollectionStatistics collSta;
	
	protected final boolean debugging = new Boolean(
			ApplicationSetup.getProperty("debugging.mode", "false")).booleanValue();
	
	public NETuning(String methodName, Index index){
		super(methodName, index);
		collSta = index.getCollectionStatistics();
		this.NUMBER_OF_BINS = 
			Integer.parseInt(ApplicationSetup.getProperty("number.bins.tuning", "1000"));
		this.DEFINITION_NE = 
			Integer.parseInt(ApplicationSetup.getProperty("definition.ne", "2"));
		pipe = new PorterStopPipeline();
		matching = new BufferedMatching(index);
	}
	
	public double tuneRealTRECQuery(){
		TRECQuery queries = new TRECQuery();
		Vector vecDocLength = new Vector();
		while (queries.hasMoreQueries()){
			BasicQuery query = new BasicQuery(queries.nextQuery(), 
					queries.getQueryId(), pipe);
			System.out.println("processing query " + query.getQueryNumber());
			matching.matchWithoutScoring(query.getQueryNumber(), 
					query.getQueryTermStrings());
			double[] docLength = (double[])matching.getRetrievedDocLength().clone();
			if (docLength.length > this.NUMBER_OF_BINS)
				vecDocLength.addElement(Statistics.splitIntoBin(
					matching.getRetrievedDocLength(), this.NUMBER_OF_BINS));
			else{
				System.out.println("too few retrieved documents. ignore query " +
						"from the tuning process.");
			}
		}
		System.out.println("tuning...");
		return this.tune(vecDocLength);
	}
	
	public double tuneSampling(int numberOfSamples){
		QuerySimulation simulation = new QuerySimulation();
		Vector vecDocLength = new Vector();
		int minLength = 2;
		int maxLength = 5;
		int queryType = SystemUtility.queryType();
		if (queryType == 1){
			minLength = 2;
			maxLength = 5;
		}
		if (queryType == 2){
			minLength = 8;
			maxLength = 16;
		}
		if (queryType == 7){
			minLength = 16;
			maxLength = 25;
		}
		for (int i = 0; i < numberOfSamples; i++){
			System.out.println(">>>>>>>>>>>>Sample " + (i+1));
			double[] docLength = new double[this.NUMBER_OF_BINS-1];
			BasicQuery query = null;
			while (docLength.length < this.NUMBER_OF_BINS){
				query = new BasicQuery(SystemUtility.stripWeights(simulation.oneStepSimulation(minLength, maxLength)), "id");			
				matching.matchWithoutScoring(query.getQueryNumber(), 
						query.getQueryTermStrings());
				docLength = matching.getRetrievedDocLength();
			}
			if (docLength.length >= this.NUMBER_OF_BINS)
				vecDocLength.addElement(
						Statistics.splitIntoBin(docLength, this.NUMBER_OF_BINS));
			query.dumpQuery();
		}
		System.out.println("tuning...");
		return this.tune(vecDocLength);
	}
	
	public double tune(Vector vecDocLength){
		double[][] docLength = new double[vecDocLength.size()][this.NUMBER_OF_BINS];
		for (int i = 0; i < vecDocLength.size(); i++){
			docLength[i] = (double[])vecDocLength.get(i);
		}
		return tune(docLength);
	}
	
	public double tune(double[][] docLength){
		
//		System.out.println("size of the document length vector: " + 
//				vecDocLength.size());
//		double[][] docLength = new double[vecDocLength.size()][this.NUMBER_OF_BINS];
		// split document length into bins
//		for (int i = 0; i < vecDocLength.size(); i++)
//			docLength[i] = Statistics.splitIntoBin((double[])vecDocLength.get(i),
//					this.NUMBER_OF_BINS);
		
		System.out.println("size of the document length array: " +
				docLength.length + " X " + docLength[0].length);
		
		// tune
		double beta = 0;
    	double targetNED = 0;
    	double left = 0;
    	double right = 0;
    	double interval = 0.01;
    	
    	double[] max = this.getMaxNEDBatchMode(docLength, this.DEFINITION_NE);
    	System.out.println("maxNED = " + Rounding.toString(max[0], 4) +
    			" maxBeta = " + Rounding.toString(max[1], 2));
    	
    	int queryType = SystemUtility.queryType();
    	
    	//System.out.println("definition = " + definition + 
    			//" queryType = " + queryType +
    			//" method: " + normMethod.getInfo());
    			
		if (this.method.getInfo().endsWith("B")){
			if (this.DEFINITION_NE == 2){
				if (queryType == 1){ // title
					targetNED = max[0] * 0.8571;
					right = max[1];
					left = 0.01;
					beta = left;
				}
    			
				if (queryType == 2){ // desc
					targetNED = max[0] * 0.9878;
					left = max[1];
					right = 1;
					beta = left;
//					targetNED = max[0] * 0.9934;
//					left = max[1];
//					right = 1;
//					beta = left;
				}
				
				if (queryType == 7){ // title + desc + narr
					targetNED = max[0] * 0.9307;
					left = max[1];
					right = 1;
					beta = left;
				}
			}
		}
    	
    	if (this.method.getInfo().endsWith("2")){
    		if (this.DEFINITION_NE == 1){
    			if (queryType == 1){ // title
    				targetNED = max[0] 
					//				* 0.9424;
    				*0.9676;
    				left = max[1];
    				right = 48d;
    				beta = left;
    			}
    			
				if (queryType == 2){ // desc
					targetNED = max[0] * 0.9766;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				/**
				if (queryType == 3){ // title + desc
					targetNED = max[0] * 0.9989;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				*/
				/**
				if (queryType == 5){ // title + narr
					targetNED = max[0] * 0.9786;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				*/
				
				if (queryType == 7){ // title + desc + narr
					targetNED = max[0] * 0.9826;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
    		}
			if (this.DEFINITION_NE == 2){
				if (queryType == 1){ // title
					targetNED = max[0] *
					//0.9302;
					0.9595;
					left = max[1];
					right = 48d;
					beta = left;
				}
    			
				if (queryType == 2){ // desc
					targetNED = max[0] * 0.9792;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				/**
				if (queryType == 3){ // title + narr
					targetNED = max[0] * 0.9982;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				
				if (queryType == 5){ // title + narr
					targetNED = max[0] * 0.9806;
					right = max[1];
					left = 0.50d;
					beta = left;
				}
				*/
				
				if (queryType == 7){ // title + desc + narr
					targetNED = max[0] * 0.9874;
					left = max[1];
					right = 48d;
					beta = left;
				}
			}
    	}
    	
    	int counter = 0;
    	
    	boolean increasing = false;
    	
    	if (getNEDBatchMode(docLength, left, this.DEFINITION_NE) < 
    		getNEDBatchMode(docLength, right, this.DEFINITION_NE) )
    		increasing = true;
    	
    	while (beta <= right){
    		double NED = this.getNEDBatchMode(docLength, beta, this.DEFINITION_NE);
    		if (increasing && NED >= targetNED)
    			break;
    		if (!increasing && NED <= targetNED)
    			break;
    			
    		beta += interval;
    		counter++;
//    		if (counter % 100 == 0)
//				System.out.println("targetNED = " + Rounding.toString(targetNED, 4) + 
//							" NED = " + Rounding.toString(NED, 4) +
//							" beta = " + Rounding.toString(beta, 2));
    	}
    	
    	return beta;
	}
	
	public double[] getMaxNEDBatchMode(double[][] docLength, int definition){
        double[] max = new double[2];
        
        double left = 0;
        double right = 0;
        double beta = 0;
        double NED = 0;
        double interval = 0.01;
        
        if (this.method.getInfo().endsWith("2")){
            left = 0.3;
            right = 8;
            beta = left;  
            //System.out.println("normlisation 2");
        }
        
        if (this.method.getInfo().toLowerCase().endsWith("b")){
            left = interval;
            right = 1;
            beta = left;  
        }
        
        //int queryType = queryType();
        
        boolean flag = true;
        
        while (flag){
           if (debugging)
	           	System.out.println("left = " + Rounding.toString(left, 2) + 
	                ", beta = " + Rounding.toString(beta, 2) +
	                ", right = " + Rounding.toString(right, 2) +
	                ", NED = " + Rounding.toString(NED, 4)); 
	        
            double NEDbuffer = this.getNEDBatchMode(docLength, beta, definition);
            
            if (NEDbuffer < NED){
                beta -= interval;
                break;
            }
            
            NED = NEDbuffer;
            
            if (beta == right || beta > right)
                break;
            
            if (beta < interval && beta + interval > interval)
                beta = right;
            else
                beta += interval;
            
        }
        
        max[0] = NED;  
        max[1] = beta;
        
        return max;
    }    
    
	/** double[0] = maxNED, double[1] = maxBeta */
	public double[] getMaxNED(double[] docLength, int definition){
		double[] max = new double[2];
        
		double left = 0;
		double right = 0;
		double beta = 0;
		double NED = 0;
		double interval = 0.01;
        
		if (this.method.getInfo().endsWith("2")){
			left = 0.3;
			right = 8;
			beta = left;  
		}
        
		if (this.method.getInfo().toLowerCase().endsWith("b")){
			left = interval;
			right = 1;
			beta = left;  
		}
        
		//int queryType = queryType();
        
		boolean flag = true;
        
		while (flag){
            
            /**
				System.out.println("left = " + Rounding.toString(left, 2) + 
					", beta = " + Rounding.toString(beta, 2) +
					", right = " + Rounding.toString(right, 2) +
					", NED = " + Rounding.toString(NED, 4)); 
					*/
        
			double NEDbuffer = this.getNED(docLength, beta, definition);
            
			if (NEDbuffer < NED){
				beta -= interval;
				break;
			}
            
			NED = NEDbuffer;
            
			if (beta == right || beta > right)
				break;
            
			if (beta < interval && beta + interval > interval)
				beta = right;
			else
				beta += interval;
            
		}
        
		max[0] = NED;  
		max[1] = beta;
        
		return max;
	}
    
	
	public double getNED(double[] docLength, double beta, int definition){
        
        return this.method.getNED(docLength, 
            collSta.getAverageDocumentLength(), 
            beta, 
            definition);
         
    }
	
	public double[] getMaxNED(double[][] docLength){
		double[] max = new double[2];
        
		double left = 0;
		double right = 0;
		double beta = 0;
		double NED = 0;
		double interval = 0.01;
        
		if (this.method.getInfo().endsWith("2")){
			left = 0.3;
			right = 8;
			beta = left;  
		}
        
		if (this.method.getInfo().toLowerCase().endsWith("b")){
			left = interval;
			right = 1;
			beta = left;  
		}
        
		//int queryType = queryType();
        
		boolean flag = true;
        
		while (flag){
            
            /**
				System.out.println("left = " + Rounding.toString(left, 2) + 
					", beta = " + Rounding.toString(beta, 2) +
					", right = " + Rounding.toString(right, 2) +
					", NED = " + Rounding.toString(NED, 4)); 
					*/
        
			double NEDbuffer = this.getNEDBatchMode(docLength, beta, this.DEFINITION_NE);
            
			if (NEDbuffer < NED){
				beta -= interval;
				break;
			}
            
			NED = NEDbuffer;
            
			if (beta == right || beta > right)
				break;
            
			if (beta < interval && beta + interval > interval)
				beta = right;
			else
				beta += interval;
            
		}
        
		max[0] = NED;  
		max[1] = beta;
        
		return max;
	}
	
	public double getNEDBatchMode(double[][] docLength, double beta,
	        int definition){
	        double[] NED = new double[docLength.length];
	        for (int i = 0; i < NED.length; i++)
	            NED[i] = this.method.getNED(docLength[i], 
	                    collSta.getAverageDocumentLength(), 
	                    beta, 
	                    definition);
	        return Statistics.mean(NED);
	    }
}
