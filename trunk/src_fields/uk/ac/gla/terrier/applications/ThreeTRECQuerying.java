/*
 * Created on 14-Jan-2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import uk.ac.gla.terrier.utility.ApplicationSetup;

import uk.ac.gla.terrier.querying.ThreeManager;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.TerrierTimer;

/**
 */
public class ThreeTRECQuerying extends TRECQuerying {
	protected Index index2;
	
	protected Index index3;
	
	public ThreeTRECQuerying() {
		super();
		mModel = ApplicationSetup.getProperty("matching.model", "uk.ac.gla.terrier.matching.ThreeMatching");
	}


	
	protected void createManager(){
		queryingManager = new ThreeManager(index, index2, index3);
	}
	
	/**
	 * Loads index from disk.
	 *
	 */
	protected void loadIndex(){
		long startLoading = System.currentTimeMillis();
		index = Index.createIndex();
		
		index2 = Index.createIndex(
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("terrier.index.path2", ApplicationSetup.TERRIER_INDEX_PATH), 
				ApplicationSetup.TERRIER_VAR),
            ApplicationSetup.getProperty("terrier.index.prefix2", ApplicationSetup.TERRIER_INDEX_PREFIX));

		index3 = Index.createIndex(
			ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("terrier.index.path3", ApplicationSetup.TERRIER_INDEX_PATH), 
				ApplicationSetup.TERRIER_VAR),
			ApplicationSetup.getProperty("terrier.index.prefix3", ApplicationSetup.TERRIER_INDEX_PREFIX));
		long endLoading = System.currentTimeMillis();
		System.err.println("index: " + index);
		System.err.println("index2: " + index2);
		System.err.println("index3: " + index3);
		System.err.println("time to intialise  all 3 indexes : " + ((endLoading-startLoading)/1000.0D));
	}
	

	public static void main(String[] args) {
		ThreeTRECQuerying trecQuerying = new ThreeTRECQuerying();
		double[] anchorW = null;
		double[] titleW = null;
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		if (args.length == 2){
			double aw = Double.parseDouble(args[0]);
			double tw = Double.parseDouble(args[1]);
			ApplicationSetup.setProperty("anchor.weight", Double.toString(Rounding.round(aw,2)));
			ApplicationSetup.setProperty("title.weight", Double.toString(Rounding.round(tw,2)));
		}
		
		/*
		 * The arguments can be 
		 * a) double -aw double -tw double : the c value followed by the field weights
		 * b) cfield:range : a range of c values for a particular field
		 * c) double : just the c value
		 */
		
		if (args.length==5) {
			double c = (new Double(args[0])).doubleValue();
			if (args[1].equals("-aw")) {
				String anchorWeights = args[2];
				if (anchorWeights.indexOf(":")==-1) {
					anchorW = new double[1]; 
					anchorW[0] = (new Double(anchorWeights)).doubleValue();
				} else {
					StringTokenizer tokens = new StringTokenizer(anchorWeights, ":");
					anchorW = new double[3];
					anchorW[0] = (new Double(tokens.nextToken())).doubleValue();
					anchorW[1] = (new Double(tokens.nextToken())).doubleValue();
					anchorW[2] = (new Double(tokens.nextToken())).doubleValue();
				}
			}
			if (args[3].equals("-tw")) {
				String titleWeights = args[4];
				if (titleWeights.indexOf(":")==-1) {
					titleW = new double[1]; 
					titleW[0] = (new Double(titleWeights)).doubleValue();
				} else {
					StringTokenizer tokens = new StringTokenizer(titleWeights, ":");
					titleW = new double[3];
					titleW[0] = (new Double(tokens.nextToken())).doubleValue();
					titleW[1] = (new Double(tokens.nextToken())).doubleValue();
					titleW[2] = (new Double(tokens.nextToken())).doubleValue();
				}
			}
			
			if (anchorW.length == 1 && titleW.length == 1) {
				ApplicationSetup.setProperty("anchor.weight", Double.toString(Rounding.round(anchorW[0],2)));
				ApplicationSetup.setProperty("title.weight", Double.toString(Rounding.round(titleW[0],2)));
				trecQuerying.processQueries(Double.parseDouble(args[0]));
			} else if (anchorW.length > 1 && titleW.length == 1) {
				ApplicationSetup.setProperty("title.weight", Double.toString(Rounding.round(titleW[0],2)));
				for (double aw = anchorW[0]; aw <=anchorW[1]; aw+=anchorW[2]) {
					ApplicationSetup.setProperty("anchor.weight", Double.toString(Rounding.round(aw,2)));
					trecQuerying.processQueries(Double.parseDouble(args[0]));
				}
			} else if (anchorW.length == 1 && titleW.length > 1) {
				ApplicationSetup.setProperty("anchor.weight", Double.toString(Rounding.round(anchorW[0],2)));
				for (double tw = titleW[0]; tw <=titleW[1]; tw+=titleW[2]) {
					ApplicationSetup.setProperty("title.weight", Double.toString(Rounding.round(tw,2)));
					trecQuerying.processQueries(Double.parseDouble(args[0]));
				}
			} else if (anchorW.length > 1 && titleW.length > 1) {
				for (double aw = anchorW[0]; aw <=anchorW[1]; aw+=anchorW[2]) {
					ApplicationSetup.setProperty("anchor.weight", Double.toString(Rounding.round(aw,2)));
					for (double tw = titleW[0]; tw <=titleW[1]; tw+=titleW[2]) {
						ApplicationSetup.setProperty("title.weight", Double.toString(Rounding.round(tw,2)));
						trecQuerying.processQueries(Double.parseDouble(args[0]));
					}			
				}
			}
		} else if (args.length == 1) {
			int indexOfColon = args[0].indexOf(":");
			if (indexOfColon<0) {
				trecQuerying.processQueries(Double.parseDouble(args[0]));
			} else {
				StringTokenizer tokens = new StringTokenizer(args[0], ":");
				String field = tokens.nextToken();
				double from = (new Double(tokens.nextToken())).doubleValue();
				double to = (new Double(tokens.nextToken())).doubleValue();
				double step = (new Double(tokens.nextToken())).doubleValue();
				for (double c=from; c<=to; c+=step) {
					ApplicationSetup.setProperty(field, Double.toString(Rounding.round(c, 2)));
					trecQuerying.processQueries(c);
				}
			}
		} else if (args.length == 4) {
			double[] omega = null;
			double[] kappa = null;
			if (!ApplicationSetup.getProperty("usm.id","").equals("")) {
				if (args[0].equals("-omega")) {
					String omegaString = args[1];
					StringTokenizer tokens = new StringTokenizer(omegaString, ":");
					omega = new double[3];
					omega[0] = (new Double(tokens.nextToken())).doubleValue();
					omega[1] = (new Double(tokens.nextToken())).doubleValue();
					omega[2] = (new Double(tokens.nextToken())).doubleValue();
				}
				
				if (args[2].equals("-kappa")) {
					String kappaString = args[3];
					StringTokenizer tokens = new StringTokenizer(kappaString, ":");
					kappa = new double[3];
					kappa[0] = (new Double(tokens.nextToken())).doubleValue();
					kappa[1] = (new Double(tokens.nextToken())).doubleValue();
					kappa[2] = (new Double(tokens.nextToken())).doubleValue();
				}
				
				for (double om=omega[0]; om<=omega[1]; om+=omega[2]) {
					for (double ka=kappa[0]; ka<=kappa[1]; ka+=kappa[2]) {
						String usmId = ApplicationSetup.getProperty("usm.id","");
						ApplicationSetup.setProperty("usm."+usmId+".omega", Double.toString(Rounding.round(om,2)));
						ApplicationSetup.setProperty("usm."+usmId+".kappa", Double.toString(Rounding.round(ka,2)));	
						trecQuerying.processQueries();
					}
				}	
			} else if (!ApplicationSetup.getProperty("lsm.id","").equals("")) {
				if (args[0].equals("-omega")) {
					String omegaString = args[1];
					StringTokenizer tokens = new StringTokenizer(omegaString, ":");
					omega = new double[3];
					omega[0] = (new Double(tokens.nextToken())).doubleValue();
					omega[1] = (new Double(tokens.nextToken())).doubleValue();
					omega[2] = (new Double(tokens.nextToken())).doubleValue();
				}
				
				if (args[2].equals("-kappa")) {
					String kappaString = args[3];
					StringTokenizer tokens = new StringTokenizer(kappaString, ":");
					kappa = new double[3];
					kappa[0] = (new Double(tokens.nextToken())).doubleValue();
					kappa[1] = (new Double(tokens.nextToken())).doubleValue();
					kappa[2] = (new Double(tokens.nextToken())).doubleValue();
				}
				
				for (double om=omega[0]; om<=omega[1]; om+=omega[2]) {
					for (double ka=kappa[0]; ka<=kappa[1]; ka+=kappa[2]) {
						ApplicationSetup.setProperty("lsm.omega", Double.toString(Rounding.round(om,2)));
						ApplicationSetup.setProperty("lsm.kappa", Double.toString(Rounding.round(ka,2)));	
						trecQuerying.processQueries();
					}
				}	
			}

		}
		else
			 trecQuerying.processQueries();
		timer.setBreakPoint();
		System.err.println("Time elapsed: " + timer.toStringMinutesSeconds());
	}
	
 	/**
 	 * Returns a PrintWriter used to store the results.
 	 * @param predefinedName java.lang.String a non-standard prefix for the result file.
 	 * @return a handle used as a destination for storing results.
 	 */
	public PrintWriter getResultFile(String predefinedName) {
		final String PREDEFINED_RESULT_PREFIX = "prob";
		PrintWriter resultFile = null;
		File fx = new File(ApplicationSetup.TREC_RESULTS);
        if (!fx.exists())
            fx.mkdir();
		String querycounter = getNextQueryCounter(ApplicationSetup.TREC_RESULTS);
        try {
	        String prefix = null;
	        if (predefinedName==null || predefinedName.equals(""))
	        	prefix = PREDEFINED_RESULT_PREFIX;
	        else
	        	prefix = predefinedName;
	        	
	        //adding more info in the prefix
	        prefix += "_cb"+ApplicationSetup.getProperty("c.body","");
	        prefix += "_ca"+ApplicationSetup.getProperty("c.anchor","");
	        prefix += "_ct"+ApplicationSetup.getProperty("c.title","");
	        prefix += "_aw"+ApplicationSetup.getProperty("anchor.weight","");
	        prefix += "_tw"+ApplicationSetup.getProperty("title.weight","");
	        
	        if (!ApplicationSetup.getProperty("usm.id","").equals("")) {
	        	String usmId = ApplicationSetup.getProperty("usm.id","");
	        	prefix += "_usm"+usmId;
	        	prefix += "_omega"+ApplicationSetup.getProperty("usm."+usmId+".omega","");
	        	prefix += "_kappa"+ApplicationSetup.getProperty("usm."+usmId+".kappa","");
	        }
	        if (!ApplicationSetup.getProperty("lsm.id","").equals("")) {
	        	String lsmId = ApplicationSetup.getProperty("lsm.id","");
	        	prefix += "_lsm"+lsmId;
	        	prefix += "_omega"+ApplicationSetup.getProperty("lsm.omega","");
	        	prefix += "_kappa"+ApplicationSetup.getProperty("lsm.kappa","");
	        }
	       
			resultsFilename = ApplicationSetup.TREC_RESULTS + '/' + prefix + "_" + querycounter + ApplicationSetup.TREC_RESULTS_SUFFIX; 
            resultFile = new PrintWriter(new BufferedWriter(new FileWriter(resultsFilename)));
			System.err.println("Writing results to "+resultsFilename);
        } catch (IOException e) {
            System.err.println("Input/Output exception while creating the result file. Stack trace follows.");
            e.printStackTrace();
            System.exit(1);
        }
        return resultFile;
    }
}
