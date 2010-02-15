package uk.ac.gla.terrier.matching.models.proximity;

import uk.ac.gla.terrier.statistics.kernels.KernelFunction;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class Cosine extends DistModel {
	protected KernelFunction kernel;
	
	public Cosine(){
		super();
		kernel = KernelFunction.getKernelFunction("CosineKernel");
		kernel.setParameter(Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.length", "5"))/2);
	}
	
	protected double getProbability(int minDist){
		return kernel.getKernelValue(minDist);
	}
}