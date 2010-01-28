/*
 * Created on 14-Jan-2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.querying;

import java.util.ArrayList;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.Model;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.ThreeMatching;
import uk.ac.gla.terrier.matching.Matching;
import uk.ac.gla.terrier.matching.dsms.BooleanScoreModifier;
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
public class ThreeManager extends uk.ac.gla.terrier.querying.Manager {

	Index diskIndexes2 = null;
	
	Index diskIndexes3 = null;
	
	protected double ANCHOR_WEIGHT=Double.parseDouble(ApplicationSetup.getProperty("anchor.weight", "1.0"));
	protected double TITLE_WEIGHT=Double.parseDouble(ApplicationSetup.getProperty("title.weight", "1.0"));
	protected double BODY_WEIGHT=Double.parseDouble(ApplicationSetup.getProperty("body.weight", "1.0"));
		
	protected double cValueBody = Double.parseDouble(ApplicationSetup.getProperty("c.body","1.0D"));
	protected double cValueAnchor= Double.parseDouble(ApplicationSetup.getProperty("c.anchor","1.0D"));
	protected double cValueTitle = Double.parseDouble(ApplicationSetup.getProperty("c.title","1.0D"));
	
	protected String bodyNormalisationName = ApplicationSetup.getProperty("normalisation.body", "2");
	protected String atextNormalisationName = ApplicationSetup.getProperty("normalisation.atext", "2");
	protected String titleNormalisationName = ApplicationSetup.getProperty("normalisation.title", "2");

	public ThreeManager(Index diskIndexes, Index diskIndexes2, Index diskIndexes3) {
		super(diskIndexes);
		this.diskIndexes2 = diskIndexes2;
		this.diskIndexes3 = diskIndexes3;
		System.err.println("diskIndexes: " + diskIndexes);
		System.err.println("diskIndexes2: " + diskIndexes2);
		System.err.println("diskIndexes3: " + diskIndexes3);
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
			wmodel.setParameter(Double.parseDouble(rq.getControl("c")));
			ThreeMatching matching = (ThreeMatching)getMatchingModel(rq.getMatchingModel());
			matching.setModel(wmodel);
			String[] normalisationNames = {
					this.bodyNormalisationName,
					this.atextNormalisationName,
					this.titleNormalisationName};
			double[] parameters = {cValueBody, cValueAnchor, cValueTitle};
			double[] weights = {this.BODY_WEIGHT, this.ANCHOR_WEIGHT, this.TITLE_WEIGHT};
			matching.setNormalisation(normalisationNames, parameters, weights);
			System.err.println("weighting model: " + wmodel.getInfo());
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
		ThreeMatching rtr = null;

		//add the namespace if the modelname is not fully qualified
		if (ModelName.indexOf(".") < 0 )
			ModelName = NAMESPACE_MATCHING +ModelName;

		//check for already instantiated class
		rtr = (ThreeMatching)Cache_Matching.get(ModelName);
		if (rtr == null)
		{
			try
			{
				//load the class
				Class formatter = Class.forName(ModelName, false, this.getClass().getClassLoader());
				//get the correct constructor - an Index class in this case
				Class[] params = {Index.class, Index.class, Index.class};
				Object[] params2 = {index, diskIndexes2, diskIndexes3};
				//and instantaite
				rtr = (ThreeMatching) (formatter.getConstructor(params).newInstance(params2));
			}
			catch(Exception e)
			{
				System.err.println("Problem with matching model named: "+ModelName+" : "+e);
				e.printStackTrace();
				return null;
			}
			Cache_Matching.put(ModelName, rtr);
		}
		return rtr;
	}

}
