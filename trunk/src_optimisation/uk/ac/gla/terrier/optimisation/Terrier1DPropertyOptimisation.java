package uk.ac.gla.terrier.optimisation;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;
public class Terrier1DPropertyOptimisation extends TerrierFunction2 {

	public static final int ndims = 1;
	
	protected final String propertyName;
	protected final double propertyMin;
	protected double propertyMax;
	//protected double threshold = Double.parseDouble(ApplicationSetup.getProperty("opt.property.threshold", "0.01"));
	public Terrier1DPropertyOptimisation() { 
		this(
			ApplicationSetup.getProperty("opt.property",null),
			Double.parseDouble(ApplicationSetup.getProperty("opt.property.min","0.0d")),
			Double.parseDouble(ApplicationSetup.getProperty("opt.property.max","1000.0d"))
			);
	}

	public Terrier1DPropertyOptimisation(String p, double min, double max)
	{
		propertyName = p;
		propertyMin = min;
		propertyMax = max;
	}
	
	public double evaluate(double param) { 
		if (param < propertyMin || param > propertyMax) {
			System.err.println("OOB: "+propertyName+"="+param);
            oobEvaluations++;
			return 0.0d;
		}
		//if (effDigit >=0)
		//	param = Double.parseDouble(Rounding.toString(param, effDigit));
		//if (pastResults.contains(param))
		//	return pastResults.get(param);
		if (effDigit >=0){
			double roundedParam = Double.parseDouble(Rounding.toString(param, effDigit));
			if (pastResults.contains(roundedParam)){
				System.err.println(propertyName+"="+param +
				         " value="+pastResults.get(roundedParam)+" cached");
				functionEvaluations++;
				return pastResults.get(roundedParam);
			}
		}
		else {
			if (pastResults.contains(param)){
				System.err.println(propertyName+"="+param +
				" value="+pastResults.get(param)+" cached");
				functionEvaluations++;
				return pastResults.get(param);
			}
		}

		/*
		if (threshold > 0.0d && pastResults.size() > 0)
        {
            double n = nearest(param);
            System.err.print("Nearest to "+ param +" is "+ n);
            if (Math.abs( n - param ) < threshold)
            {
                System.err.println(" accepted");
                return pastResults.get(n);
            }
            System.err.println(" not accepted");
        }*/

		trecQuerying.getManager().setProperty(propertyName, Double.toString(param));
		double value = evaluate( trecQuerying.processQueries() );
		System.err.println(propertyName+"="+param + " value="+value);
		functionEvaluations++;
		if (effDigit >=0){
			double roundedParam = Double.parseDouble(Rounding.toString(param,
			effDigit));
			pastResults.put(roundedParam, value);
		}
		else
			pastResults.put(param, value);
		return value;
	}
	
	public int getndim() { return ndims; }

	/*
    protected double nearest(double in)
    {
        double rtr = -1;
        double a[] = pastResults.keys();
        Arrays.sort(a);
        if (a.length == 1)
            return a[0];
        int loc = Arrays.binarySearch(a, in);
        if (loc >= 0)
            return a[loc];
        int ip = -(loc +1);

        if (ip == a.length)
            return a[ip-1];
        if (ip+1 == a.length)
            return a[ip];

        //we know  a[ip] < in < a[ip+1]
        //what is the nearest? a[ip] or a[ip+1]
        if ( in - a[ip] < a[ip+1] - in)
            return a[ip];
        return a[ip+1];
    }*/

}
