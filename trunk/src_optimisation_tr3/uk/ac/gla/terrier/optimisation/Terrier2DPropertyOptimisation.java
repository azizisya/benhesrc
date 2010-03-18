package uk.ac.gla.terrier.optimisation;
import uk.ac.gla.terrier.optimisation.TerrierFunction2;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Rounding;
import org.terrier.utility.TerrierTimer;
import gnu.trove.TObjectDoubleHashMap;
public class Terrier2DPropertyOptimisation extends TerrierFunction2
{

	public final int ndims;
	protected final String[] propertyName;
	protected final double propertyMin;
	protected double propertyMax;
	protected TObjectDoubleHashMap pastResults = new TObjectDoubleHashMap();
	public Terrier2DPropertyOptimisation() { 
		this(
			ApplicationSetup.getProperty("opt.property","").split("\\s*,\\s*"),
			Double.parseDouble(ApplicationSetup.getProperty("opt.property.min","0.0d")),
			Double.parseDouble(ApplicationSetup.getProperty("opt.property.max","1000.0d"))
			);
	}

	public void reset()
	{
		super.reset();
		pastResults.clear();
	}

	public Terrier2DPropertyOptimisation(String[] props, double min, double max)
	{
		propertyName = props;
		ndims = propertyName.length;
		propertyMin = min;
		propertyMax = max;
	}
	
	public double evaluate(double[] params) {
		//check the parameters for range, and then set the corresponding properties
		for (int i=0;i<ndims;i++)
		{
			if (params[i] < propertyMin || params[i] > propertyMax) {
				System.err.println("OOB: x["+i+"]="+params[i] + ", returning "+Math.abs(params[i] - propertyMin));
				oobEvaluations++;
				return Math.pow(Math.abs(params[i] - propertyMin), 2);
			}
			trecQuerying.getManager().setProperty(propertyName[i], Double.toString(params[i]));
		}
		String key = "";
		if (cacheResults && effDigit>=0){
			for (int i=0;i<ndims;i++)
			{
				key+=Rounding.toString(params[i], effDigit)+" ";
			}
			key=key.trim();
			if (pastResults.contains(key)){
				double value = pastResults.get(key); 
				for(int i=0;i<ndims;i++)
				{
					System.err.print(propertyName[i]+"="+params[i]+" ");
				}
				System.err.println("value="+value+" cached");
				functionEvaluations++;
				return value;
			}
		}

		//run the query and the evaluation		
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		double value = evaluate( trecQuerying.processQueries() );
		timer.setBreakPoint();
		System.err.println("Time to evaluate: "+timer.toStringMinutesSeconds());
		functionEvaluations++;
		//display the properties and their values, and the evaluation result
		for(int i=0;i<ndims;i++)
		{
			System.err.print(propertyName[i]+"="+params[i] + " ");
		}
		System.err.println("value="+value);
		if (cacheResults)
			pastResults.put(key, value);
		return value;
	}
	
	public int getndim() { return ndims; }
}
