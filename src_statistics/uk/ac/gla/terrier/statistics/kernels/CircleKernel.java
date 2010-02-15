package uk.ac.gla.terrier.statistics.kernels;

public class CircleKernel extends KernelFunction {

	@Override
	public double getKernelValue(double mu) {
		double indicator = (mu<=sigma)?(1d):(0d);
		return indicator*Math.sqrt(1-(mu/sigma)*(mu/sigma));
	}

}
