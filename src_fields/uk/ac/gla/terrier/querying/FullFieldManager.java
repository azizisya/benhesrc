/*
 * Created on 14-Jan-2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.querying;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.FullFieldMatching;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.FieldMatching;
import uk.ac.gla.terrier.matching.Matching;
import uk.ac.gla.terrier.matching.dsms.BooleanScoreModifier;
import uk.ac.gla.terrier.matching.models.FieldWeightingModel;
import uk.ac.gla.terrier.querying.parser.FieldQuery;
import uk.ac.gla.terrier.querying.parser.Query;
import uk.ac.gla.terrier.querying.parser.RequirementQuery;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author vassilis
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class FullFieldManager extends uk.ac.gla.terrier.querying.Manager {
	/** the logger for this class */
	private static Logger logger = Logger.getLogger("field");

	protected Index fieldIndices[] = null;
	protected int NumFields;
	
	protected String normalisationNames[];
	protected double cValues[];
	protected double weightValues[];
	protected double[] cPostValues;

	public FullFieldManager(Index[] diskIndexes) {
		super((diskIndexes==null)?null:diskIndexes[0]);
		NumFields = Integer.parseInt(ApplicationSetup.getProperty("fields.number", "1"));
		// NumFields = diskIndexes.length;

		//make all the arrays the correct sizes
		fieldIndices = new Index[NumFields];
		normalisationNames = new String[NumFields];
		cValues = new double[NumFields];
		weightValues = new double[NumFields];

		if (diskIndexes != null)
			for(int i=1;i<=NumFields;i++)
			{
				fieldIndices[i-1] = diskIndexes[i-1];
				System.err.println("FieldIndex"+i+": "+ fieldIndices[i-1]);
			}
		init();
	}

	public void init()
	{
		for(int i=1;i<=NumFields;i++)
		{
			normalisationNames[i-1] = ApplicationSetup.getProperty("normalisation."+i,"2");
			cValues[i-1] = Double.parseDouble(ApplicationSetup.getProperty("c."+i,"1.0D"));
			weightValues[i-1] = Double.parseDouble(ApplicationSetup.getProperty("weight."+i,"1.0D"));
		}
	}
	
	/** Runs the weighting and matching stage - this the main entry
	  * into the rest of the Terrier framework.
	  * @param srq the current SearchRequest object.
	  */
	public void runMatching(SearchRequest srq)
	{
		Request rq = (Request)srq;
		if (! rq.isEmpty())
		{
			//TODO some exception handling here for not found models
			Model wmodel = getWeightingModel(rq.getWeightingModel());
			boolean cset = rq.getControl("c").length() > 0;
			wmodel.setParameter(Double.parseDouble(cset ? rq.getControl("c") : "0"));
			FullFieldMatching matching = (FullFieldMatching)getMatchingModel(rq.getMatchingModel());
			
			matching.setModel(wmodel);
			init();
			matching.setNormalisation(normalisationNames, cValues, weightValues);
			if (logger.isDebugEnabled())
				logger.debug("weighting model: " + wmodel.getInfo());
			MatchingQueryTerms mqt = rq.getMatchingQueryTerms();
			Query q = rq.getQuery();
			
			/* now propagate fields into requirements, and apply boolean matching
			   for the decorated terms. */
			ArrayList requirement_list = new ArrayList();
			ArrayList field_list = new ArrayList();
			
			q.getTermsOf(RequirementQuery.class, requirement_list, true);
			q.getTermsOf(FieldQuery.class, field_list, true);
			for (int i=0; i<field_list.size(); i++) 
				if (!requirement_list.contains(field_list.get(i)))
					requirement_list.add(field_list.get(i));
				
			if (requirement_list.size()>0) {
				mqt.addDocumentScoreModifier(new BooleanScoreModifier(requirement_list));
			}

			mqt.setQuery(q);
			mqt.normaliseTermWeights();
			matching.match(rq.getQueryID(), mqt);
			//matching.match(rq.getQueryID(), rq.getMatchingQueryTerms());
			//now crop the collectionresultset down to a query result set.
			ResultSet outRs = matching.getResultSet();
			rq.setResultSet((ResultSet)(outRs.getResultSet(0, outRs.getResultSize())));
		}
		else
		{
			System.err.println("Returning empty result set as query "+rq.getQueryID()+" is empty");
			rq.setResultSet(new QueryResultSet(0));
		}
	}
	
	/** Returns the matching model named ModelName. Caches already 
	  * instantiaed matching models in Hashtable Cache_Matching.
	  * If the matching model name doesn't contain '.', then NAMESPACE_MATCHING
	  * is prefixed to the name. 
	  * @param ModelName The name of the class to instantiate and return*/
	protected Matching getMatchingModel(String ModelName)
	{
		FullFieldMatching rtr = null;

		//add the namespace if the modelname is not fully qualified
		if (ModelName.indexOf(".") < 0 )
			ModelName = NAMESPACE_MATCHING +ModelName;

		//check for already instantiated class
		rtr = (FullFieldMatching)Cache_Matching.get(ModelName);
		if (rtr == null)
		{
			try
			{
				//load the class
				Class formatter = Class.forName(ModelName, false, this.getClass().getClassLoader());
				//get the correct constructor - an Index class in this case
				Class[] params = {Index[].class};
				Object[] params2 = {fieldIndices};
				//and instantiate
				rtr = (FullFieldMatching) (formatter.getConstructor(params).newInstance(params2));
			}
			catch(java.lang.reflect.InvocationTargetException e)
			{
				System.err.println("Problem instantiating matching named: "+ModelName+": "+e.getCause());
				e.getCause().printStackTrace();
				System.err.println("at");
				e.printStackTrace();
			}
			catch(Exception e)
			{
				System.err.println("Problem with matching named: "+ModelName+" : "+e);
				e.printStackTrace();
				return null;
			}
			Cache_Matching.put(ModelName, rtr);
		}
		return rtr;
	}
	
	/** Returns the weighting model named ModelName. Caches already
	  * instantiaed matching models in Hashtable Cache_Weighting.
	  * If the weighting model name doesn't contain '.', then 
	  * NAMESPACE_WEIGHTING is prefixed to the name. 
	  * @param ModelName The name of the weighting model to instantiate */
	protected Model getWeightingModel(String ModelName)
	{
		Model rtr = null;
		
		if (ModelName.indexOf(".") < 0 )
			ModelName = NAMESPACE_WEIGHTING +ModelName;
		//check for already instantiated model
		rtr = (Model)Cache_Weighting.get(ModelName);
		if (rtr == null)
		{
			try
			{
				if (ModelName.indexOf("(") > 0){
					// if is DFRWeightingModel
					if (ModelName.trim().startsWith("DFRWeightingModel")){
						String params = ModelName.substring( 
							ModelName.indexOf("(")+1, ModelName.indexOf(")"));
						String[] parameters = params.split("\\s*,\\s*");
						
						rtr = (Model) Class.forName(
										ModelName.substring(0,ModelName.indexOf("(")))
								.getConstructor(
										new Class[]{String[].class})
								.newInstance(
										new Object[]{parameters});
					}
					// if is PFNWeightingModel
					else if (ModelName.trim().startsWith("PFNWeightingModel")){
						// get the member model name
						String wmodelname = ModelName.substring(
								ModelName.indexOf("(")+1,
								ModelName.lastIndexOf(",")
						);
						// get the name of the normalisation method
						String normalisationName = ModelName.substring(
								ModelName.lastIndexOf(","+1),
								ModelName.lastIndexOf(")")
						).trim();
						// instantiate the model
						String[] parameters = {""+NumFields, wmodelname, normalisationName};
						rtr = (Model) Class.forName(
								"PFNWeightingModel")
						.getConstructor(
								new Class[]{String[].class})
						.newInstance(
								new Object[]{parameters});
					}
					
				}else{
					// normal field weighting models
					//String[] parameters = {""+NumFields};
					rtr = (FieldWeightingModel) Class.forName(ModelName)
							.getConstructor(int.class)
							.newInstance(NumFields);
				}
			}
			catch(java.lang.reflect.InvocationTargetException ite)
			{
				logger.error("Recursive problem with weighting model named: "+ModelName,ite);
				return null;
			}
			catch(Exception e)
			{
				logger.error("Problem with weighting model named: "+ModelName,e);
				return null;
			}
			Cache_Weighting.put(ModelName, rtr);
		}

		return rtr;
	}
	
	protected void loadParameters(){
		weightValues = new double[NumFields];
		cValues = new double[NumFields];
		normalisationNames = new String[NumFields];
		for (int i=1;i<=NumFields;i++){
 			weightValues[i-1] = Double.parseDouble(ApplicationSetup.getProperty("weight."+i, "1d"));
 			cValues[i-1] = Double.parseDouble(ApplicationSetup.getProperty("c."+i,"1d"));
 			normalisationNames[i-1] = ApplicationSetup.getProperty("normalisation."+i, "2");
 			//System.err.println(weightValues[i-1]+", "+cValues[i-1]+", "+normalisationNames[i-1]);
 		}
	}
	
	protected void loadPostParameters(){
 		cPostValues = new double[this.NumFields];
 		weightValues = new double[NumFields];
 		normalisationNames = new String[NumFields];
 		for (int i=1; i<=NumFields; i++){
 			weightValues[i-1] = Double.parseDouble(ApplicationSetup.getProperty("weight."+i, "1d"));
 			normalisationNames[i-1] = ApplicationSetup.getProperty("normalisation."+i, "2");
 			cPostValues[i-1] = Double.parseDouble(
 					ApplicationSetup.getProperty("c."+i+".post", 
 							ApplicationSetup.getProperty("c."+i, "1d")
 							)
 							);
 		}
 	}

}
