package org.terrier.matching.models.proximity;

import uk.ac.gla.terrier.statistics.kernels.KernelFunction;
import org.terrier.utility.ApplicationSetup;

public class Quadratic extends DistModel {
protected KernelFunction kernel;
	
	public Quadratic(){
		super();
		kernel = KernelFunction.getKernelFunction("QuadraticKernel");
		double wSize = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.length", "5"));
		if (wSize == 0)
			kernel.setParameter(1000d);
		else
			kernel.setParameter(wSize-1);
	}
	
	protected double getProbability(int minDist){
		return kernel.getKernelValue(minDist);
	}
}