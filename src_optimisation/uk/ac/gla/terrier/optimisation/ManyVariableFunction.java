package uk.ac.gla.terrier.optimisation;

/**
 * @author Vassilis Plachouras
 * 
 */
public interface ManyVariableFunction {
	
	public double evaluate(double[] parameters);

	public OneVariableFunction getOneVariableFunction(
			int ncom, 
			double[] pcom, 
			double[] xicom);
	
	public int getndim();
	
	public void close();
}
