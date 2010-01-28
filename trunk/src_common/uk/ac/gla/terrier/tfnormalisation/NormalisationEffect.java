/*
 * NormalisationEffect.java
 *
 */

package uk.ac.gla.terrier.tfnormalisation;

import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.utility.*;

/**
 *
 * @author  ben
 */
public class NormalisationEffect {
    public TermFrequencyNormalisation normMethod;    
    
    /** Creates a new instance of NormalisationEffect */
    public NormalisationEffect(TermFrequencyNormalisation normMethod) {
        this.normMethod = normMethod;
    }

    
    public double getTFn(double termFrequency,
            double documentLength,
	        double averageDocumentLength,
	        double beta){
    	return this.normMethod.getNormalisedTermFrequency(termFrequency,
    			documentLength, averageDocumentLength, beta);
    }
    
    /** Creates a new instance of NormalisationEffect */
    public NormalisationEffect(String methodName) {
        if (methodName.lastIndexOf('.') < 0)
            methodName = ("tfnormalisation.Normalisation" + methodName).trim();
        try {
            this.normMethod = (TermFrequencyNormalisation) Class.forName(methodName).newInstance();				
        } catch(InstantiationException ie) {
            System.err.println("Exception while loading the normalisation method class:\n" + ie);
            System.err.println("Exiting...");
            System.exit(1);
        } catch(IllegalAccessException iae) {
            System.err.println("Exception while loading the normalisation method class:\n" + iae);
            System.err.println("Exiting...");
            System.exit(1);
        } catch(ClassNotFoundException cnfe) {
            System.err.println("Exception while loading the normalisation method class:\n" + cnfe);
            System.err.println("Exiting...");
            System.exit(1);
        }
    }
    /**
    public double getBetaBatchMode(double[][] docLength, double beta,
        int definition){       
    }
     */
    
    /** double[0] = maxNED, double[1] = maxBeta */
    public double[] getMaxNEDBatchMode(double[][] docLength, int definition){
        double[] max = new double[2];
        
        double left = 0;
        double right = 0;
        double beta = 0;
        double NED = 0;
        double interval = 0.01;
        
        if (this.normMethod.getInfo().endsWith("2")){
            left = 0.3;
            right = 8;
            beta = left;  
            //System.out.println("normlisation 2");
        }
        
        if (this.normMethod.getInfo().toLowerCase().endsWith("b")){
            left = interval;
            right = 1;
            beta = left;  
        }
        
        //int queryType = queryType();
        
        boolean flag = true;
        
        while (flag){
            
//           System.out.println("left = " + Rounding.toString(left, 2) + 
//                ", beta = " + Rounding.toString(beta, 2) +
//                ", right = " + Rounding.toString(right, 2) +
//                ", NED = " + Rounding.toString(NED, 4)); 
        
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
        
		if (this.normMethod.getInfo().endsWith("2")){
			left = 0.3;
			right = 8;
			beta = left;  
		}
        
		if (this.normMethod.getInfo().toLowerCase().endsWith("b")){
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
        
        return this.normMethod.getNED(docLength, 
            normMethod.collSta.getAverageDocumentLength(), 
            beta, 
            definition);
         
    }
    
    public String getMethodInfo(){
    	return this.normMethod.getInfo();
    }
    
    public double[] getNEnBatchMode(double[][] docLength, double[] beta,
    		int definition){
    	double[] NEn = new double[beta.length];
    	double[] max = this.getMaxNEDBatchMode(docLength, definition);
    	for (int i = 0; i < beta.length; i++){
    		NEn[i] = this.getNEDBatchMode(docLength, beta[i], definition) / max[0];
    	}
    	return NEn;
    }
    
    public double getBeta(double[] docLength, int definition){
    	double[][] docLength2D = new double[1][docLength.length];
    	docLength2D[0] = docLength;
    	return this.getBetaBatchMode(docLength2D, definition);
    }
    
    public double getBetaBatchMode(double[][] docLength, int definition){
    	double beta = 0;
    	double targetNED = 0;
    	double left = 0;
    	double right = 0;
    	double interval = 0.01;
    	
    	double[] max = this.getMaxNEDBatchMode(docLength, definition);
//    	System.out.println("maxNED = " + Rounding.toString(max[0], 4) +
//    			" maxBeta = " + Rounding.toString(max[1], 2));
    	
    	int queryType = SystemUtility.queryType();
    	
    	//System.out.println("definition = " + definition + 
    			//" queryType = " + queryType +
    			//" method: " + normMethod.getInfo());
    			
		if (this.normMethod.getInfo().endsWith("B")){
			if (definition == 2){
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
    	
    	if (this.normMethod.getInfo().endsWith("2")){
    		if (definition == 1){
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
			if (definition == 2){
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
    	
    	if (getNEDBatchMode(docLength, left, definition) < 
    		getNEDBatchMode(docLength, right, definition) )
    		increasing = true;
    	
    	while (beta <= right){
    		double NED = this.getNEDBatchMode(docLength, beta, definition);
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
    
	public double[] getNEn(double[] docLength, double[] beta,
				int definition){
			double[] NEn = new double[beta.length];
			double[] max = this.getMaxNED(docLength, definition);
			for (int i = 0; i < beta.length; i++){
				NEn[i] = this.getNED(docLength, beta[i], definition) / max[0];
			}
			return NEn;
		}
    
    public double getNEDBatchMode(double[][] docLength, double beta,
        int definition){
        double[] NED = new double[docLength.length];
        for (int i = 0; i < NED.length; i++)
            NED[i] = this.normMethod.getNED(docLength[i], 
                    normMethod.collSta.getAverageDocumentLength(), 
                    beta, 
                    definition);
        return Statistics.mean(NED);
    }
    
}
