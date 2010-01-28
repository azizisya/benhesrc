package uk.ac.gla.terrier.applications;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.models.FieldWeightingModel;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.querying.FullFieldManager;
import uk.ac.gla.terrier.structures.Index;

import uk.ac.gla.terrier.utility.TerrierTimer;

/**
 * @author Vassilis Plachouras &amp; Craig Macdonald
 *
 */
public class FullFieldTRECQuerying extends TRECQuerying {
	/** the logger for this class */
	protected static Logger logger = Logger.getLogger("field");
	protected Index[] indices;
	protected int NumFields;
	
	public FullFieldTRECQuerying() {
		//long startLoading = System.currentTimeMillis();
		//this.loadIndex();
		//this.createManager();
		//long endLoading = System.currentTimeMillis();
		mModel = ApplicationSetup.getProperty("matching.model", "uk.ac.gla.terrier.matching.FullFieldMatching");
		//if (logger.isInfoEnabled() && indices!=null)
			//logger.info("time to intialise all "+indices.length+" indexes : " + ((endLoading-startLoading)/1000.0D));
	}
	
	protected void createManager(){
		queryingManager = new FullFieldManager(indices);
	}
	
	/**
	 * Loads index from disk.
	 *
	 */
	protected void loadIndex(){
		NumFields = Integer.parseInt(ApplicationSetup.getProperty("fields.number", "1"));
		indices = new Index[NumFields];
		indices[0] = index = Index.createIndex();
		if (logger.isDebugEnabled())
			logger.debug("index:"+index);
		
		for(int i=1;i<NumFields;i++)
		{
			int externalNumber = i+1;
			indices[i] = Index.createIndex(
				ApplicationSetup.makeAbsolute(
					ApplicationSetup.getProperty("terrier.index.path"+externalNumber, 
						ApplicationSetup.TERRIER_INDEX_PATH), 
					ApplicationSetup.TERRIER_VAR),
            	ApplicationSetup.getProperty("terrier.index.prefix"+externalNumber, 
					ApplicationSetup.TERRIER_INDEX_PREFIX));
			if (logger.isDebugEnabled())
				logger.debug("index"+indices[i]);
		}
	}

	public static void main(String[] args) {
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		FullFieldTRECQuerying trecQuerying = new FullFieldTRECQuerying();
		trecQuerying.processQueries();
		timer.setBreakPoint();
		if (logger.isInfoEnabled())
			logger.info("Time elapsed: " + timer.toStringMinutesSeconds());
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
			//int NumFields = indices.length;
	     
			/*for(int i=1;i<=NumFields;i++)
			{
				String c = ApplicationSetup.getProperty("c."+i,"");
				prefix += "_c"+i+"_"+c;
				String weight = ApplicationSetup.getProperty("weight."+i,"");	
				prefix += "_w"+i+"_"+weight;
			}*/
			
	        /*
			prefix += "_cb"+ApplicationSetup.getProperty("c.body","");
	        prefix += "_ca"+ApplicationSetup.getProperty("c.anchor","");
	        prefix += "_ct"+ApplicationSetup.getProperty("c.title","");
	        prefix += "_aw"+ApplicationSetup.getProperty("anchor.weight","");
	        prefix += "_tw"+ApplicationSetup.getProperty("title.weight","");
	        */
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
            if (logger.isDebugEnabled())
				logger.debug("Writing results to "+resultsFilename);
        } catch (IOException e) {
            logger.fatal("Input/Output exception while creating the result file. Stack trace follows.");
            e.printStackTrace();
            System.exit(1);
        }
        return resultFile;
    }
}
