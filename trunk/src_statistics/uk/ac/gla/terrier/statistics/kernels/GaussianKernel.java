package uk.ac.gla.terrier.statistics.kernels;

public class GaussianKernel extends KernelFunction {

	@Override
	public double getKernelValue(double mu) {
		return Math.pow(Math.E, -mu*mu/(2*sigma*sigma));
	}

}
