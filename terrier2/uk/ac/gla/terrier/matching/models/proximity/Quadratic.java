package uk.ac.gla.terrier.matching.models.proximity;

import uk.ac.gla.terrier.statistics.kernels.KernelFunction;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class Quadratic extends DistModel {
protected KernelFunction kernel;
	
	public Quadratic(){
		super();
		kernel = KernelFunction.getKernelFunction("QuadraticKernel");
		kernel.setParameter(Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.length", "5"))/2);
	}
	
	protected double getProbability(int minDist){
		return kernel.getKernelValue(minDist);
	}
}