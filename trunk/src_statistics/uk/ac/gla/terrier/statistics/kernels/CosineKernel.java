package uk.ac.gla.terrier.statistics.kernels;

public class CosineKernel extends KernelFunction {

	@Override
	public double getKernelValue(double mu) {
		double indicator = (mu<=sigma)?(1d):(0d);
		return indicator*0.5*(1+Math.cos(mu*Math.PI/sigma));
	}

}
