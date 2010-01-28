import uk.ac.gla.terrier.optimisation.ManyVariableFunction;
import java.util.Random;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TDoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
public class MetroplisHastingsMCMC
{
	/** the pseudo-random number generator of this object */
	protected final Random random = new Random();
	/** the function being MCMCd. Usually a Terrier instance */
	protected ManyVariableFunction function;
	/** cache of results */
	protected TObjectDoubleHashMap resultsCache = new TObjectDoubleHashMap();

	/** Instantiate a new MH MCMC using this function. Wont run until run() 
	 *  is called */	
	public MetroplisHastingsMCMC(ManyVariableFunction function)
	{
		this.function = function;
	}

	/** Runs the Metroplis Hastings Markov Chain Monte Carlo algorithm.
	 *  Definition from An Introduction to MCMC for Machine Learning,
	 *  C. Adrieu, N. De Freitas, A. Doucet, M. I. Jordan. 
	 *  J. of ML 50 5-43, 2003. 
	 *  @param x0 The initial settings of the paramters to be MCMCd.
	 *  @param MaxIterations The nunber of MH iterations to run. N from the article.
	 */
	public void run(final double[] x0, final int MaxIterations)
	{
		final ArrayList x_is = new ArrayList(MaxIterations);
		final TDoubleArrayList r_is = new TDoubleArrayList(MaxIterations);

		int AcceptanceCount = 0;
		int AttemptCount = 0;

		double current_x[] = x0; double current_xQ; double x_starQ =0;
		x_is.add(current_x);
		for(int iteration=0;iteration<MaxIterations-1;iteration++)
		{
			System.err.print("ITERATION: "+iteration);
			System.err.print(" X: "+stringify(current_x));
			System.err.println(" R: "+(current_xQ = evaluate(current_x)));
			x_is.add(current_x);
			r_is.add(current_xQ);

			//Sample u from the invariant distribution
			final double u = random.nextDouble(); //0.0<=u<=1.0
			//Sample x_star from the proposal distribution, based on current_x
			double x_star[] = proposal(current_x);
			
			//calculate acceptance probability A
			double Acceptance;
			final double static_xstar = static_prob(x_star);
			if (static_xstar == 0.0d)
			{
				//if the probability of the proposal is 0, (ie invalid proposal)
				//then we can shortcut with this branch, to prevent unnecessary
				//evaluation
				Acceptance = 0;
			}
			else
			{
				//static_prob(current_x) > 0.0d for all current_x
				Acceptance = Math.min(1, 
					(static_xstar * (x_starQ =evaluate(x_star)))
					/
					(static_prob(current_x) * current_xQ)
					//current_x has static_prob >0 by defn
				);
				//evaluate caches, so no worry 
				//about overheads here
			}

			if (u < Acceptance)
			{
				current_x = x_star;
				current_xQ = x_starQ;
				AcceptanceCount++;
			}
			AttemptCount++;
			//else current_x = current_x;
			
			//x_is.add(current_x);
			//r_is.add(evaluate(current_x));
		}
		System.err.println("Iterations="+MaxIterations+" AcceptanceCount="+AcceptanceCount+" AttemptCount="+AttemptCount);
		System.err.println("Acceptance Ratio="+((double)AcceptanceCount/(double)AttemptCount));
	}

	/** Evaluate function f with the following parameters.
	 *  Returns the evaluated result wrt to x. Caches results
	 *  for previous arrays of x */
	protected double evaluate(final double x[])
	{
		//check the cache for this x previously
		if (resultsCache.containsKey(x))
		{
			//done this x before, use the cached result
			return resultsCache.get(x);
		}

		//not seen this x before, calculate the result for it.
		System.err.println("INFO: Evaluate "+stringify(x));
		final double result = function.evaluate(x);
		System.err.println(" result="+result);
		if (result == 0.0d)
		{
			//ASSERT:
			System.err.print("WARNING: Evaluation of function with params ");
			System.err.println(stringify(x) + " gave output of 0");
		}
		//cache result
		resultsCache.put(x, result);
		return result;
	}

	public double stddev =1;
	/** Generate a proposal, based on the current x value passed in.
	 *  This is in the form of sampling from a gaussian centred on each
	 *  current value of x. The width of the gaussian can be controlled
	 *  by the stddev variable 
	 *  @param x the current x
	 *  @return the new proposed x
	 */
	protected double[] proposal(double x[])
	{	
		final int l = x.length;
		double[] new_proposal = new double[l];
		//sample from a guassian distribution around x
		for(int i=0;i<l;i++)
		{
			new_proposal[i] = nextGaussian(x[i], stddev);
		}
		return new_proposal;
	}

	protected double static_prob(double x[])
	{
		final double mins[] = new double[x.length];
		final double maxs[] = new double[x.length];
		Arrays.fill(mins, 0.0d);
		Arrays.fill(maxs, 1000.0d);
		return static_prob(x, mins, maxs);
	}

	protected double static_prob(double x[], double mins[], double maxs[])
	{
		final int l = x.length;
		for(int i=0;i<l;i++)
		{
			if (x[i] <= mins[i] || x[i] > maxs[i])
				return 0;
		}
		return 1;
	}

	/** Returns the next pseudorandom, Gaussian distributed double value 
	 * 	with mean mean and standard deviation sd from the random number 
	 * 	generator's sequence.
	 * 	See http://www.pitt.edu/~wpilib/statfaq/gaussfaq.html and
	 * 	http://forum.java.sun.com/thread.jspa?threadID=609074&amp;messageID=3336220
	 * 	@param mean Mean of the Gaussian
	 * 	@param sd StndDev of the Gaussin
	 */
	protected double nextGaussian(double mean, double sd){
		return (mean +  random.nextGaussian() * Math.sqrt(sd));
	}

	public int getndim()
	{
		return function.getndim();
	}

	/** Close everything */
	public void close()
	{
		resultsCache.clear();
		function.close();
	}

	public static String stringify(double x[])
	{
		StringBuffer buf = new StringBuffer();
		for(int i=0;i<x.length;i++)
		{
			buf.append(x[i]);
			if (i != x.length-1)
				buf.append(',');
		}
		return buf.toString();
	}

	public static void main(String args[])
	{
		if (args.length == 0)
		{
			System.err.println("MetroplisHastingsMCMC ManyVariableFunction NumIterations stddev [initial_x]");
			System.exit(1);
		}
		try {
			ManyVariableFunction f = null; 
			Class functionClass = Class.forName(args[0]);
			f = (ManyVariableFunction) functionClass.newInstance();
			MetroplisHastingsMCMC mcmc = new MetroplisHastingsMCMC(f);
			int Iterations = Integer.parseInt(args[1]);
			mcmc.stddev = Double.parseDouble(args[2]);
			
			final int dim = mcmc.getndim();
			double x0[] = new double[dim];
			for(int i=0;i<dim;i++)
			{
				x0[i] = Double.parseDouble(args[3+i]);
			}
			mcmc.run(x0, Iterations);
		}
		catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
