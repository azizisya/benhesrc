package uk.ac.gla.terrier.statistics.kernels;

public class TriangleKernel extends KernelFunction {

	@Override
	public double getKernelValue(double mu) {
		double indicator = (mu<=sigma)?(1d):(0d);
		return 	indicator*(1-mu/sigma);
	}

}
