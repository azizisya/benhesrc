package uk.ac.gla.terrier.optimisation;
import ireval.RetrievalEvaluator.Document;
import ireval.RetrievalEvaluator.Judgment;
import ireval.Main;
import ireval.SetRetrievalEvaluator;
import gnu.trove.TDoubleDoubleHashMap;
import org.terrier.utility.Rounding;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.StringTokenizer;


import org.terrier.applications.TRECQuerying;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.TerrierTimer;
import webcab.lib.math.optimization.NDimFunction;

/**
 * @author Vassilis Plachouras &amp; Craig Macdonald
 * @version $Revision: 1.11 $
 */
public class TerrierFunction2 implements OneVariableFunction, ManyVariableFunction, NDimFunction  {
	protected static final Runtime r = Runtime.getRuntime();

	/** The TREC querying application class to run the querying. Set by property <tt>opt.querying</tt>. 
	  * Defaults to <tt>uk.ac.gla.terrier.applications.TRECQuerying</tt>. Package name <tt>uk.ac.gla.terrier.applications.</tt> 
	  * will be prepended to any value without a period. For multi-core machines, consider 
	  * <tt>uk.ac.gla.terrier.applications.ParallelTRECQuerying</tt>.*/
	protected TRECQuerying trecQuerying = null;

	/** The filename of the file that contains the qrels for these experiments. Set by property <tt>opt.qrels</tt> */
	protected String qrelsFile = null;
	
	/**  
	 * The number of effective digit after the dot of the property to be optimised.
	 * effDigit is -1 by default, which implies no limit on the effective digit. This
	 * can be configured by the property <tt>parameter.effective.digit<tt> 
	 */
	protected int effDigit;

	/** The evaluation measure from trec_eval that is to be used to tune these experiments. Set by
	  * the property <tt>opt.evalmeasure</tt> */
	protected final String evalMeasure;

	/** The name of the the evaluation program to use to evaluate the results. This can be configured by
	  * the property <tt>opt.evalprogram</tt>, and defaults to <tt>trec_eval</tt>. The two command-line arguments
	  * are qrels and results filenames IN THAT ORDER. (trec_eval is suitable, TREC KI scripts are not). */
	protected final String evalProgram;

	/** Minimum that property is allowed. Set by property <tt>opt.property.min</tt>, defaults to 0.0d. Value not allowed is &lt;= the property. */
	protected final double min;
	/** Maximum that property is allowed. Set by property <tt>opt.property.max</tt>, defaults to 1000.0d. No value greater than this property allowed */
	protected final double max;
	/** Out of bounds value: used when the parameter being optimised is out of allowed range. Defaults to 0.0d, change this if minimising. 
	  * Set by property <tt>opt.property.oob</tt>. */
	protected final double oob;

	/** Should the evaluation function being optimised for be maximised? True means yes it should be maximised,
	  * False means it should be minimised. Controlled by property <tt>opt.maximise</tt>, default to true*/
	protected final boolean evalMaximise;

	/** The number of evaluations of this function */
	protected int functionEvaluations = 0;

	public int getNumberOfSuccessfulEvaluations()
	{
		return functionEvaluations;
	}
	/** Number of attempted evluations of this function that results in OOB */
	protected int oobEvaluations = 0;
	public int getNumberOfOOBEvaluations()
	{
		return oobEvaluations;
	}

	public void reset()
	{
		functionEvaluations = oobEvaluations = 0;
		pastResults.clear();
	}


	/** For one dimensional evaluations, this hashmap can be used to save the results of an evaluation
	  * for a given single parameter value. This is useful as the optimiser may return to a parameter value
	  * and attempt to re-evaluate it, thus wasting precious time. This could be caused, for instance by
	  * a change of optimisation algorithm */
	protected TDoubleDoubleHashMap pastResults = new TDoubleDoubleHashMap();

	/** Controls whether the cache is used. Set using property <tt>opt.cache</tt>. */
	protected boolean cacheResults = Boolean.parseBoolean(ApplicationSetup.getProperty("opt.cache","true"));

	public TerrierFunction2()
	{
		//load up the correct application class. This must be a descendant of uk.ac.gla.terrier.applications.TRECQuerying
		String trecQueryingClassName = ApplicationSetup.getProperty("opt.querying","TRECQuerying");
		if (trecQueryingClassName.indexOf('.') == -1)		
			trecQueryingClassName = "uk.ac.gla.terrier.applications." + trecQueryingClassName;

		try{
			trecQuerying = (TRECQuerying) Class.forName(trecQueryingClassName).newInstance();
		}catch (Exception e) {
			System.err.println("Exception: "+e);
			e.printStackTrace();
			System.exit(1);
		}
		effDigit = Integer.parseInt(ApplicationSetup.getProperty("parameter.effective.digit", "-1"));

		qrelsFile = ApplicationSetup.getProperty("opt.qrels","");
		evalMeasure = ApplicationSetup.getProperty("opt.evalmeasure","");
		evalProgram = ApplicationSetup.getProperty("opt.evalprogram", "trec_eval");
		evalMaximise = ApplicationSetup.getProperty("opt.maximise","true").equals("true");

		min = Double.parseDouble(ApplicationSetup.getProperty("opt.property.min","0.0d"));
		max = Double.parseDouble(ApplicationSetup.getProperty("opt.property.max","1000.0d"));
		oob = Double.parseDouble(ApplicationSetup.getProperty("opt.property.oob", "0.0d"));

		//assume that multiple optimisations can occurr concurrently, so play
		//safe with the filenames of results files	
		System.setProperty("trec.querycounter.type","random");
	}
	
	public double evaluate(double c) {
		double value = 0.0d;
		if (c <= min || c>max) 
		{
			System.err.println("OOB: c="+c);
			oobEvaluations++;
			return oob;
		}
		if (effDigit >=0&&cacheResults){
			double roundedParam = Double.parseDouble(Rounding.toString(c, effDigit));
			if (pastResults.contains(roundedParam)){
				System.err.println(functionEvaluations++ + 
						" : evaluation of " +evalMeasure + " for c=" + 
						c +" is " + -pastResults.get(c=roundedParam)+" cached");
				return pastResults.get(roundedParam);
			}
		}
		else {
			if (cacheResults&&pastResults.contains(c)){
				System.err.println(functionEvaluations++ + 
						" : evaluation of " +evalMeasure + " for c=" + 
						c +" is " + -pastResults.get(c)+" cached");
				return pastResults.get(c);
			}
		}
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		value = evaluate( trecQuerying.processQueries(c, true) );
		timer.setBreakPoint();
		System.err.println("Time to evaluate: "+timer.toStringMinutesSeconds());
		System.err.println(functionEvaluations++ + " : evaluation of " +evalMeasure + " for c=" + c +" is " + -value);
		if (effDigit >=0&&cacheResults){
			double roundedParam = Double.parseDouble(Rounding.toString(c,
			effDigit));
			pastResults.put(roundedParam, value);
		}
		else if (cacheResults)
			pastResults.put(c, value);
		return value;
	}

	public void close()
	{
		trecQuerying.close();
	}

	/** evaluates a result file for the measures specified by the properties, using the evaluation program and qrels specified by properties */	
	public double evaluate(String filename)
	{
		if (evalProgram.equals("ireval"))
			return _evaluate_ireval(filename);
		else if (evalProgram.endsWith("geno"))
			return _evaluate_geno(filename);
		else if (evalProgram.endsWith("statAP_MQ_eval_v4.pl") || evalMeasure.equals("statMAP_on_valid_topics"))
			return this._evaluate_statMAP(filename);
		else if (evalProgram.endsWith("eval.sh"))
			return this._evaluate_statMAP(filename);
		else return _evaluate_external(filename);
	}
	
	protected double _evaluate_ireval(String filename)
	{
		double value = -1d;
		try{
			TreeMap< String, ArrayList<Document> > ranking = Main.loadRanking( filename );
	        TreeMap< String, ArrayList<Judgment> > judgments = Main.loadJudgments( qrelsFile );
	        SetRetrievalEvaluator setEvaluator = Main.create( ranking, judgments );
	        //Main.singleEvaluation(setEvaluator);
	        if (evalMeasure.equals("recip_rank")||evalMeasure.equals("mrr"))
	        	value = setEvaluator.meanReciprocalRank();
	        else if (evalMeasure.equals("map"))
	        	value = setEvaluator.meanAveragePrecision();
	        else if (evalMeasure.equals("ndcg"))
	        	value = setEvaluator.meanNormalizedDiscountedCumulativeGain();
	        else if (evalMeasure.equals("bpref"))
	        	value = setEvaluator.meanBinaryPreference();
	        //etc for other measures
	        else if (evalMeasure.matches("^p[0-9]+$"))
	        {
	        	final int point = Integer.parseInt(evalMeasure);
	        	value = setEvaluator.meanPrecision(point); 
	        }else
	        	System.err.println("Unknown measure" + evalMeasure);
	        if (ApplicationSetup.getProperty("minimise","true").equals("true"))
				value = -value;
			// looks for the parameter value that gives the worst performance
			else value = -(1-value);
		} catch (Exception e) {
			System.err.println("Couldnt use ireval because exception :" + e);
			e.printStackTrace();
		}
		try{
			(new File(filename)).delete();
			(new File(filename+".settings")).delete();
		}catch(Exception e){
			e.printStackTrace();
		}
		return Double.parseDouble(Rounding.toString(value, 4));
	}
	
	protected double _evaluate_geno(String filename){
		double value = 0.0d;
		String script = ApplicationSetup.getProperty("geno.evaluate.script",
				"/users/grad/ben/tr.ben/uniworkspace/bin/trecgen2007_score.py");
		String[] measures = evalMeasure.split("-");
		try {
			System.out.println("filename: " + filename);
			System.out.println("qrelsFile: " + qrelsFile);
			System.out.println("evalProgram: "+ script);
			Process evaluation = r.exec("python "+script + " " + qrelsFile + " " +filename);
			System.out.println("evalMeasure: "+ measures[0]+", "+measures[1]);
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(evaluation.getInputStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
				StringTokenizer stk = new StringTokenizer(line);
				stk.nextToken();// skip tag
				String passageEval = stk.nextToken();
				String realMeasure = stk.nextToken();
				if (passageEval.equals(measures[0]) && realMeasure.equals(measures[1])){
					value = Double.parseDouble(stk.nextToken());
					if (ApplicationSetup.getProperty("minimise","true").equals("true"))
						value = -value;
					// looks for the parameter value that gives the worst performance
					else value = -(1-value);
				}
			}
			//(new File(filename)).delete();
			br.close();	br = null;
			try{
				evaluation.waitFor(); //wait for completion
			} catch (InterruptedException ie) {
			} finally { 
				evaluation.destroy();
			}
			int exitCode = evaluation.exitValue();
			System.err.println("exit code was " + exitCode);
			if (exitCode == 0)
			{
				try{
					(new File(filename)).delete();
					(new File(filename+".settings")).delete();
				} catch (Exception e) {
					System.err.println("WARNING: Failed to delete a results file: "+ e);
				}
			}
			evaluation = null;
			System.gc(); System.runFinalization();	
		} catch(IOException ioe) {
			System.err.println("ioe exception... : "+ioe);
			ioe.printStackTrace();
			displayMemoryUsage();	
			System.exit(1);
		}
		return value;
	}
	
	protected double _evaluate_external(String filename)
	{	
		double value = 0.0d;
		try {
			System.out.println("filename: " + filename);
			System.out.println("qrelsFile: " + qrelsFile);
			System.out.println("evalProgram: "+ evalProgram);
			//Process evaluation = r.exec(evalProgram + " " + ApplicationSetup.getProperty("eval.option", "").replaceAll(",", " ")
				//	+ " " + qrelsFile + " " +filename);
			System.out.println("evalMeasure: "+ evalMeasure);
			/**
			 * Ben: Added support for evaluation options
			 */
			System.out.println("eval options: "+ ApplicationSetup.getProperty("eval.option", ""));

			Process evaluation = null;
			int attempts = 5;
			Exception lastE = null;
			while(evaluation == null && attempts-- > 0)
			{
				lastE = null;
				try{
					//IOException occurs here, perhaps java-vm process to big to fork?
					evaluation = r.exec(evalProgram + " " + ApplicationSetup.getProperty("eval.option", "").replaceAll(",", " ")
							+ " " + qrelsFile + " " +filename);
				} catch (Exception e) {
					lastE = e;	
				}
			}
			if (evaluation == null && attempts-- == 0 && lastE != null)
			{
				throw lastE;
			}

			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(evaluation.getInputStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
				if (line.startsWith(evalMeasure+" ")) {
					value = (new Double(line.substring(line.lastIndexOf("\t")))).doubleValue();
					if (ApplicationSetup.getProperty("minimise","true").equals("true"))
						value = -value;
					// looks for the parameter value that gives the worst performance
					else value = -(1-value);
				}
			}
			//(new File(filename)).delete();
			br.close();	br = null;
			try{
				evaluation.waitFor(); //wait for completion
			} catch (InterruptedException ie) {
			} finally { 
				evaluation.destroy();
			}
			int exitCode = evaluation.exitValue();
			System.err.println("exit code was " + exitCode);
			if (exitCode == 0)
			{
				try{
					(new File(filename)).delete();
					(new File(filename+".settings")).delete();
				} catch (Exception e) {
					System.err.println("WARNING: Failed to delete a results file: "+ e);
				}
			}
			evaluation = null;
			System.gc(); System.runFinalization();	
		} catch(Exception ioe) {
			System.err.println("exception... : "+ioe);
			ioe.printStackTrace();
			displayMemoryUsage();	
			System.exit(1);
		}
		return value;
	}
	
	protected double _evaluate_statMAP(String filename)
	{	
		double value = 0.0d;
		try {
			System.out.println("filename: " + filename);
			System.out.println("qrelsFile: " + qrelsFile);
			System.out.println("evalProgram: "+ evalProgram);
			//Process evaluation = r.exec(evalProgram + " " + ApplicationSetup.getProperty("eval.option", "").replaceAll(",", " ")
				//	+ " " + qrelsFile + " " +filename);
			System.out.println("evalMeasure: "+ evalMeasure);
			/**
			 * Ben: Added support for evaluation options
			 */
			System.out.println("eval options: "+ ApplicationSetup.getProperty("eval.option", ""));

			Process evaluation = null;
			int attempts = 5;
			Exception lastE = null;
			//System.gc();
			while(evaluation == null && attempts-- > 0)
			{
				lastE = null;
				try{
					if (evalMeasure.equals("statMAP_on_valid_topics"))
					//IOException occurs here, perhaps java-vm process to big to fork?
						evaluation = r.exec(evalProgram + " " + qrelsFile + " " +filename+" statMAP no "+
								ApplicationSetup.getProperty("eval.option", "").replaceAll(",", " "));
					else
						evaluation = r.exec(evalProgram + " " + qrelsFile + " " +filename+" "+evalMeasure+" no "+
								ApplicationSetup.getProperty("eval.option", "").replaceAll(",", " "));
				} catch (Exception e) {
					lastE = e;	
					e.printStackTrace();
				}
			}
			if (evaluation == null && attempts-- == 0 && lastE != null)
			{
				System.err.println("Warning: null evaluation process.");
				throw lastE;
			}

			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(evaluation.getInputStream()));
			while ((line = br.readLine())!=null) {
				System.out.println(line);
				line = line.trim();
				if (line.startsWith(evalMeasure)) {
					if (evalMeasure.equals("statMAP_on_valid_topics")||evalMeasure.equals("statMAP")){
						StringTokenizer stk = new StringTokenizer(line);
						value=Double.parseDouble(stk.nextToken().split("=")[1]);
						if (ApplicationSetup.getProperty("minimise","true").equals("true"))
							value = -value;
						// looks for the parameter value that gives the worst performance
						else value = -(1-value);
						stk = null;
					}else{
						StringTokenizer stk = new StringTokenizer(line);
						stk.nextToken(); // skip map
						stk.nextToken(); // skip all
						try{
							value = Double.parseDouble(stk.nextToken());
						}catch(NoSuchElementException e){
							System.err.println("line: "+line+", measure: "+evalMeasure);
							e.printStackTrace();
							System.exit(1);
						}
						if (ApplicationSetup.getProperty("minimise","true").equals("true"))
							value = -value;
						// looks for the parameter value that gives the worst performance
						else value = -(1-value);
						stk = null;
					}
				}
			}
			//(new File(filename)).delete();
			br.close();	br = null;
			try{
				evaluation.waitFor(); //wait for completion
			} catch (InterruptedException ie) {
			} finally { 
				evaluation.destroy();
			}
			int exitCode = evaluation.exitValue();
			System.err.println("exit code was " + exitCode);
			if (exitCode == 0)
			{
				try{
					(new File(filename)).delete();
					(new File(filename+".settings")).delete();
				} catch (Exception e) {
					System.err.println("WARNING: Failed to delete a results file: "+ e);
				}
			}
			evaluation = null;
			System.gc(); System.runFinalization();	
		} catch(Exception ioe) {
			System.err.println("exception... : "+ioe);
			ioe.printStackTrace();
			displayMemoryUsage();	
			System.exit(1);
		}
		return value;
	}
	
	public static void optimise(OneVariableFunction terrier, double lowC, double highC) {
		
		MinimumBracketing mbracket = new MinimumBracketing(lowC, highC, terrier);
		
		mbracket.mnbrak();

		System.out.println("xa = " + mbracket.getxa());
		System.out.println("xb = " + mbracket.getxb());
		System.out.println("xc = " + mbracket.getxc());
		
		System.out.println("fa = " + mbracket.getfa());
		System.out.println("fb = " + mbracket.getfb());
		System.out.println("fc = " + mbracket.getfc());
		
		OneDimensionOptimiser onedoptimiser = new BrentMethod(mbracket.getxa(), mbracket.getxb(), mbracket.getxc(),
																   terrier, 1E-4);
		double fmin = onedoptimiser.optimise();
		
		System.out.println("brent minimum is " + fmin + " for x = " + onedoptimiser.getxmin());

	}
	
	public double evaluate(double[] params) { 
		return evaluate(params[0]);
	}
	
	public OneVariableFunction getOneVariableFunction(int ncom, double[] pcom, double[] xicom) {
		return this;
	}

	/* (non-Javadoc)
	 * @see webcab.lib.math.optimization.NDimFunction#getValueAtVector(double[])
	 */
	public double getValueAtVector(double[] arg0) throws Exception {
		return evaluate(arg0);
	}
	
	public int getndim() { return 1;}

	public static void displayMemoryUsage()
	{
		System.err.println("free: "+ (r.freeMemory() /1024) + "kb; total: "+(r.totalMemory()/1024)
			+"kb; max: "+(r.maxMemory()/1024)+"kb; "+
			Rounding.toString((100*r.freeMemory() / r.totalMemory()),1)+"% free; "+
			Rounding.toString((100*r.totalMemory() / r.maxMemory()),1)+"% allocated; "
		);
	}

	
}
