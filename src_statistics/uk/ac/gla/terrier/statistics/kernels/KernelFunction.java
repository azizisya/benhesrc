package uk.ac.gla.terrier.statistics.kernels;

public abstract class KernelFunction {
	public String kernelName;
	
	protected double sigma;
	
	public double getParameter() {
		return sigma;
	}

	public void setParameter(double sigma) {
		this.sigma = sigma;
	}

	public String getKernelName(){
		return kernelName;
	}
	
	public abstract double getKernelValue(double mu);
	
	public static KernelFunction getKernelFunction(String name){
		KernelFunction kernel = null;
		String prefix = "uk.ac.gla.terrier.statistics.kernels.";
		if (name.lastIndexOf('.')<0)
			name = prefix.concat(name);
		try{
			kernel = (KernelFunction)Class.forName(name).newInstance();
		}catch(ClassNotFoundException e){
			e.printStackTrace();
			System.exit(1);
		}catch(IllegalAccessException e){
			e.printStackTrace();
			System.exit(1);
		}catch(InstantiationException e){
			e.printStackTrace();
			System.exit(1);
		}
		return kernel;
	}
}
