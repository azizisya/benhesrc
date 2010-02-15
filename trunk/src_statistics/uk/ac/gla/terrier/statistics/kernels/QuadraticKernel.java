package uk.ac.gla.terrier.statistics.kernels;

public class QuadraticKernel extends KernelFunction {

	@Override
	public double getKernelValue(double mu) {
		double indicator = (mu<=sigma)?(1d):(0d);
		return indicator*15d/16d*Math.pow(1-Math.pow(mu/sigma, 2), 2);
	}

}
