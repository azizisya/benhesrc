package uk.ac.gla.terrier.statistics.kernels;

public class EpanechnikovKernel extends KernelFunction {

	@Override
	public double getKernelValue(double mu) {
		double indicator = (mu<=sigma)?(1d):(0d);
		return indicator*0.75*(1-Math.pow(mu/sigma, 2));
	}

}
