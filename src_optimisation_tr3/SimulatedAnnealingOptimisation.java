import uk.ac.gla.terrier.optimisation.ManyVariableFunction;
import uk.ac.gla.terrier.optimisation.SimulatedAnnealing;
import uk.ac.gla.terrier.optimisation.TerrierFunction2;
import org.terrier.utility.ApplicationSetup;
import webcab.lib.math.optimization.GenericFunction;
import webcab.lib.math.optimization.NDimFunction;
import webcab.lib.math.optimization.Optimization;

/**
 * @author vassilis &amp; craigm
 * This optimises a 1 or N dimensional parameters space wrt a function (ie IR system0 and evaluation of the output (eg trec_eval).
 * This takes place in two stages
 * <ol>
 * <li>Simulated annealing
 * See <a href="http://www.webcabcomponents.com/ejb/optimization/JavaDocs/com/webcab/ejb/math/optimization/multidimensional/MultiDimensionalSolver.html#globalAnnealing(com.webcab.ejb.math.optimization.ExtremumTypes,%20double[],%20int,%20double,%20double,%20double,%20double,%20int)">Documentation</a>
 * <li>Local optimisation - see uk.ac.gla.terrier.optimisation.SimulatedAnnealing.local_optimise. Algorithm implemented is described <a href="http://www.denizyuret.com/pub/aitr1569/index.html">here</a></li>
 * </ol>
 * <p>
 * <b>Parameters:</b>
 * <ul>
   <LI><i>For the simulated annealing</I>
 * <li>Cooling formula is (*) (1 - k / annealingSteps)^alpha

 * <li>0..(nDims-1) Double (starting) parameters for each dimension of the function. ie x[]

 * <li>nDims Integer (K/the init - probably annealingSteps) : From the manual : "the number of steps for the Simulated Annealing which is performed before the algorithm exists. Note that the number of iterations used effects the level at which the surface is vibrated before the algorithm exits. A reasonable value to take for this parameter in most case is 2." <i>I normally use 5, vassilis used 25</i>

 * <li>nDims+2 Double (Tzero - initial temperature) : From the manual "The initial temperature which corresponds to the amount which the "surface" of shaken during the first iteration. After which the "surface" cools resulting in decreased shaking until the surface come to rest.". <i>I normally use 5, vasslis used 10.</i>

 * <li>nDims+3 Double (alpha) : From the manual : "A constant indicating the non-linearity of the annealing process. The more non-linear (i.e. the worst) the object function is in terms of trying to find a global extremum the lower the value of alpha which should be given. Since the lower alpha, the slower the "shaking of the surface" will decrease, as explicit describe within the formula (*) given above." <i>Vassilis used 2, i used 3</i>

 * <li>nDims+4 Double (radius) : From the manual : "This parameter indicates the density of the randomly-chosen points of the initial simplex. Ideally this parameter should be large with a typical value being 1000." <i>Vassilis used 10, I used 0.3</i>

 * <li>nDims+5 int (Step_iter) : From the manual : "The number of iterations used when the Nedler and Mead algorithm is used at each step of the annealing procedure. Naturally the higher the number of iterations used the greater the expected precision however the slower the algorithm will become. A reasonable value to take for this parameter is 300". <i> Vassilis used 10, i normally used 5  </i>


 * <li><i>For the local optimisation</i> as described in http://www.denizyuret.com/pub/aitr1569/index.html
 * <li>nDims+6 : (Double) INIT_SIZE for the local optimisation - Initial step size. The maximum amount of movement in any parameter dimension for local optimisation. Vassilis and I concur that 5 is a good value for this parameter.
 * <li>nDims+7 : THRESHOLD for the local optimisation - minimum step size. The minimum amount of movement in any parameter dimension for local optimisation. 1E-4 seems a good value for this parameters, as any change of a parameter of less than that will unlikely have no effect on an MAP ranking.
 * <li>
 * </ul>
 * <b>Essential properties:</b>
 * <ul>
 * <li><tt>opt.function</tt> - set the optimisation parameter to optimise. 
 * <br>Examples are: uk.ac.gla.terrier.optimisation.TerrierFunction2 (default, optimises the c parameter), uk.ac.gla.terrier.optimisation.Terrier1DPropertyOptimisation &amp; uk.ac.gla.terrier.optimisation.Terrier2DPropertyOptimisation
 */
public class SimulatedAnnealingOptimisation {
	private static Optimization optimization = null;

	public static void main(String[] args) {

		try {

			ManyVariableFunction f = null; //new TerrierFunction();
			Class functionClass = Class.forName(ApplicationSetup.getProperty("opt.function","uk.ac.gla.terrier.optimisation.TerrierFunction2"));
			f = (ManyVariableFunction) functionClass.newInstance();
			
			// Here we create an instance of the Optimization Component
			// and pass the function implemented within TheFunctionm3.
			//
			optimization = createOptimizationInstance((NDimFunction)f);

			double[] minx; //the result
			
			//need to get the number of dimensions 
			int numberOfDimensions = f.getndim();
			
			double[] x = new double[numberOfDimensions];
			
			for (int i=0; i<numberOfDimensions; i++) {
				x[i] = Double.parseDouble(args[i]);
			}
			
			double fractional_tolerance = 1E-4; //the tollerance
			int K = Integer.parseInt(args[numberOfDimensions]); //25 //the init
			double T0 = Double.parseDouble(args[numberOfDimensions+1]);//10;
			double alpha = Double.parseDouble(args[numberOfDimensions+2]); //2;
			double radius = Double.parseDouble(args[numberOfDimensions+3]); //10;
			int step_iter = Integer.parseInt(args[numberOfDimensions+4]); //10;


			int GAattempts = 0;	
			final int maxGAattempts = 5;
			do {//repeat the optimsation until some sanity checks on number of evluations
				//check out OK

				//actual optimisation
				minx = optimization.multiGlobalAnnealing(x,K,T0,alpha,radius,fractional_tolerance,step_iter);
				GAattempts++;
				
				//display results
				for (int i=0; i<numberOfDimensions; i++) 
					System.out.println("minx["+i+"] = " + minx[i]);
				System.out.println("f(minx) = " + ((NDimFunction) f).getValueAtVector(minx));
				//check out fn that was evaluated properly
			} while (! checkFunctionEvaluation(f) && ++GAattempts <= maxGAattempts); 
			//give up if too many attempts
			if (GAattempts > maxGAattempts)
			{
				System.err.printf("Global SA failed %d times (threshold %d)\n", GAattempts, maxGAattempts);
				return;
			}

			removeOptimizationInstance();

			//2. local optimisation stage (ignore the class name) 
			SimulatedAnnealing sa = new SimulatedAnnealing();
			sa.setndim(numberOfDimensions);
			SimulatedAnnealing.INIT_SIZE = Double.parseDouble(args[numberOfDimensions+5]);//0.3;
			SimulatedAnnealing.THRESHOLD = Double.parseDouble(args[numberOfDimensions+6]);//1E-4;
			x = sa.local_optimise(f, minx);
			
			//Display the results of the optimisatino
			System.out.println("maximum " + f.evaluate(x) + " at ");
			for (int i=0; i<numberOfDimensions; i++) 
				System.out.println("minx["+i+"] = " + x[i]);
			
			
			f.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit( -1);
		}
	}

	private static boolean checkFunctionEvaluation(ManyVariableFunction f)
	{
		if (f instanceof TerrierFunction2)
		{
			TerrierFunction2 fnOfTerrier = (TerrierFunction2)f;
			if (fnOfTerrier.getNumberOfSuccessfulEvaluations() <= 4
					&& fnOfTerrier.getNumberOfOOBEvaluations() > 0)
			{
				System.err.printf("ERROR: Successful evaluations: %d OOB: %d outwith threshold. Redoing opt\n",
					fnOfTerrier.getNumberOfSuccessfulEvaluations(),
					fnOfTerrier.getNumberOfOOBEvaluations());
				fnOfTerrier.reset();
				return false;
			}
			return true;
		}
		//else
		return true;
	}

	private static Optimization createOptimizationInstance (GenericFunction instanceOfGenericFunction) {
		try {
			Optimization instance = new Optimization (instanceOfGenericFunction);
	
			return instance;
		}
		catch (Exception e) {
			e.printStackTrace ();
		}
		
		return null;
	}

	private static void removeOptimizationInstance () {
		optimization = null;
	 }
}
