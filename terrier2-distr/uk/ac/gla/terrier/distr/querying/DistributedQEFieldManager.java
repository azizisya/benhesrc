/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is DistributedQEFieldManager.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 */
package uk.ac.gla.terrier.distr.querying;

import gnu.trove.TDoubleHashSet;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.distr.matching.DistributedMatchingServer;
import uk.ac.gla.terrier.distr.structures.DistributedMatchingQueryTerms;
import uk.ac.gla.terrier.distr.structures.DistributedThread;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.dsms.BooleanScoreModifier;
import uk.ac.gla.terrier.querying.PostProcess;
import uk.ac.gla.terrier.querying.Request;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.querying.parser.FieldQuery;
import uk.ac.gla.terrier.querying.parser.Query;
import uk.ac.gla.terrier.querying.parser.RequirementQuery;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * This class manages query expansion on the three fields. The class bounds 
 * one or several servers, which can be external resource, for query expansion.
 * A member of this class, queryingManager, bounds the servers that mount the 
 * local collection. If the selective query expansion is not applied, we
 * does both first-pass and post retrievals on the local collection. Otherwise, 
 * we make a decision that on which collection we expand the query or disabling
 * query expansion.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.1 $
 */
public class DistributedQEFieldManager extends DistributedFieldManager{
	/**
	 * The DistributedFieldManager that corresponds to the local collection.
	 */
	DistributedFieldManager queryingManager;
	/**
	 * An integer indicating if the query expansion is local, external
	 * and disabled. The value can be either LOCAL_EXPANSION, EXTERNAL_EXPANSION
	 * or DISABLE_EXPANSION.
	 */
	protected int expansionStatus = 0;
	
	public long time_on_qe = 0L;
	/**
	 * An integer indicating that the query expansion is local.
	 */
	protected final int LOCAL_EXPANSION = 0;
	/**
	 * The number of queries that are expanded locally.
	 */
	protected int localExpansionCounter=0;
	
	//protected THashMap queryResultMap = new THashMap();
	
	/**
	 * An integer indicating that the query expansion is external.
	 */
	protected final int EXTERNAL_EXPANSION = 1;
	/**
	 * The number of queries that are expanded externally.
	 */
	protected int externalExpansionCounter=0;
	/**
	 * An integer indicating that the query expansion is disabled.
	 */
	protected final int DISABLE_EXPANSION = 2;
	/**
	 * The number of queries for which query expansion is disabled.
	 */
	protected int noExpansionCounter=0;
		
	/**
	 * A threshold for the selective query expansion mechanism.
	 */
	double threshold = Double.parseDouble(
		ApplicationSetup.getProperty("selective.expansion.threshold", "13.5d"));
	TDoubleHashSet avICTF = new TDoubleHashSet();
	
	
	
	/**
	 * A boolean variable that indicates if to apply the selective query
	 * expansion.
	 */
	boolean SELECTIVE_EXPANSION = (new Boolean(
			ApplicationSetup.getProperty("selective.expansion", "false"))).booleanValue();
	/**
	 * The default name space of the post process module.
	 */
	public static final String NAMESPACE_POSTPROCESS = "uk.ac.gla.terrier.distr.querying.";
	/**
	 * The default constructor. We override it because we don't want to
	 * load the indices at this stage.
	 */
	public DistributedQEFieldManager (){
		this.loadQELexicon();
		this.loadQEServers();
	}
	
	protected void loadLexicon(){
		
	}
	
	/**
	 * Load the global lexicon of both local and external collections.
	 * Only one instance is created if the two global lexicons refer to
	 * the identical file.
	 */
	protected void loadQELexicon(){
		
		queryingManager = new DistributedFieldManager();
		
		globalLexiconAddress = ApplicationSetup.getProperty("global.qelexicon.filename",
 					ApplicationSetup.getProperty("global.lexicon.filename", "Must be given"));
 		File f = new File(globalLexiconAddress);
 		if (!f.exists()){
 			System.err.println("Warning: Global lexicon does not exist.");
 		}
 		if (globalLexiconAddress.equals(
 				ApplicationSetup.getProperty("global.lexicon.filename",
 				"Must be given")))
 			globalLexicon = queryingManager.globalLexicon;
 		else
 			globalLexicon = new Lexicon(globalLexiconAddress);
 		System.err.println(globalLexiconAddress + " is chosen as the global lexicon for query expansion.");
 	}
	
	
	protected void loadServers(){
		
	}
	
	/**
	 * Load the query expansion servers.
	 */
	protected void loadQEServers() {
		
		String property = ApplicationSetup.getProperty("qeserver.names", 
				ApplicationSetup.getProperty("server.names", null));
 		StringTokenizer stk = new StringTokenizer(property, ", ");
 		
 		this.numberOfServers = stk.countTokens() / 2;
 		distMatch = new DistributedMatchingServer[numberOfServers];
 		distResult = new QueryResultSet[numberOfServers];
 		distThread = new DistributedThread[numberOfServers];
 		serverNames = new String[numberOfServers];
 		ids = new String[numberOfServers];
 		int counter = 0;
 		while (stk.hasMoreTokens()){
 			serverNames[counter] = stk.nextToken().trim();
 			ids[counter++] = stk.nextToken().trim();
 		}
 		System.out.println("Binding query expansion servers...");
 		this.bindServers();
 		this.updateStatistics();
 		this.numberOfTopRankedDocs = new int[numberOfServers];
 		Arrays.fill(numberOfTopRankedDocs, 0);
 	}
	
	/** Returns the PostProcess named Name. Caches already
	  * instantiaed classes in Hashtable Cache_PostProcess.
	  * If the post process class name doesn't contain '.', 
	  * then NAMESPACE_POSTPROCESS is prefixed to the name. 
	  * @param Name The name of the post process to return. */
	protected PostProcess getPostProcessModule(String Name)
	{
		PostProcess rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = NAMESPACE_POSTPROCESS +Name;
		//check for already loaded models
		rtr = (PostProcess)Cache_PostProcess.get(Name);
		if (rtr == null)
		{
			try
			{
				rtr = (PostProcess) Class.forName(Name).newInstance();
			}
			catch(Exception e)
			{
				System.err.println("Problem with postprocess named: "+Name+" : "+e);
				e.printStackTrace();
				return null;
			}
			Cache_PostProcess.put(Name, rtr);
		}
		return rtr;
	}
	
	/** Runs the PostProcessing modules in order added. PostProcess modules
	  * alter the resultset. Examples might be query expansions which completelty replaces
	  * the resultset.
	  * @param srq the current SearchRequest object. 
	  * @return The search request with expanded query terms.
	  * */
	public void runPostProcessing(SearchRequest srq)
	{
		Request rq = (Request)srq;
		Hashtable controls = rq.getControlHashtable();
		
//		 do not cache for post-process
		for (int i = 0; i < this.numberOfServers; i++){
	 		try{
	 			distMatch[i].setProperty("cache.results", "false");
	 		}catch(RemoteException e){
	 			e.printStackTrace();
	 			System.err.println("RemoteException occurs while setting property cache.results.");
	 		}
	 	}
		
		/*if (this.cacheResults){
			DistributedMatchingQueryTerms mqt = (DistributedMatchingQueryTerms)rq.getMatchingQueryTerms();
			
			ANCHOR_WEIGHT=Double.parseDouble(ApplicationSetup.getProperty("anchor.weight", "1.0"));
	 		TITLE_WEIGHT=Double.parseDouble(ApplicationSetup.getProperty("title.weight", "1.0"));
	 		BODY_WEIGHT=Double.parseDouble(ApplicationSetup.getProperty("body.weight", "1.0"));
	 			
	 		cValueBody = Double.parseDouble(ApplicationSetup.getProperty("c.body.post","1.0D"));
	 		cValueAnchor = Double.parseDouble(ApplicationSetup.getProperty("c.anchor.post","1.0D"));
	 		cValueTitle = Double.parseDouble(ApplicationSetup.getProperty("c.title.post","1.0D"));
	 		
	 		boolean normalisationStrategy = (new Boolean(ApplicationSetup.getProperty(
					"simple.normalisation", "false"))).booleanValue();
	 		
	 		double[] parameterValues = {cValueBody, cValueAnchor, cValueTitle};
	 		double[] weights = {BODY_WEIGHT,ANCHOR_WEIGHT,TITLE_WEIGHT};
	 		
	 		mqt.setNormStrategy(normalisationStrategy);
	 		mqt.setParameterValues(parameterValues);
	 		mqt.setFieldWeights(weights);
	 		rq.setMatchingQueryTerms(mqt);
		}*/
		
		for(int i=0; i<PostProcesses_Order.length; i++)
		{
			String PostProcesses_Name = PostProcesses_Order[i];
			for(int j=0; j<PostProcesses_Controls[i].length; j++)
			{
				String ControlName = PostProcesses_Controls[i][j];
				String value = (String)controls.get(ControlName);
				if (value == null)
					continue;
				this.setSecondPassRetrievalNormalisation();
				value = value.toLowerCase();
				if(! (value.equals("off") || value.equals("false")))
				{
					System.err.println("Processing: "+PostProcesses_Name);
					// if the post process module is DistributedQueryExpansion, expand the query in a distributed setting, and then run then matching again.
					if (PostProcesses_Name.equals("DistributedQueryExpansion")){
						DistributedQueryExpansion expansion = 
							((DistributedQueryExpansion)getPostProcessModule(PostProcesses_Name));
						// if the selective QE is not enabled, change the QE status to local expansion.
						//if (!this.SELECTIVE_EXPANSION)
							//expansionStatus=LOCAL_EXPANSION;
						// if QE is disabled, skip the post process. 
						if (expansionStatus==DISABLE_EXPANSION){
							String qeModel = rq.getControl("qemodel");
							if (qeModel == null || qeModel.length() ==0)
							{
								qeModel = "Bo1";
							}
						    expansion.setQueryExpansionModel(expansion.getQueryExpansionModel(qeModel));
							System.err.println("No query expansion applied.");
						}
						// if the QE status is local, expand the query on the local collection.
						else if (expansionStatus==this.LOCAL_EXPANSION){
							long startingTime = System.currentTimeMillis();
							expansion.process(this.queryingManager, srq);
							this.time_on_qe += (System.currentTimeMillis() -startingTime); 							
							queryingManager.setSecondPassRetrievalNormalisation();
							this.queryingManager.runMatching(srq);
						}
						// if the QE status is external, expand the query on the external collection.
						else if (expansionStatus==this.EXTERNAL_EXPANSION){
							long startingTime = System.currentTimeMillis();
							expansion.process(this, srq);
							this.time_on_qe += (System.currentTimeMillis() -startingTime);
							this.setSecondPassRetrievalNormalisation();
							this.runMatching(srq);
						}
					}
					else{
						getPostProcessModule(PostProcesses_Name).process(this, srq);
					}
					//we've now run this post process module, no need to check the rest of the controls for it.
					break;
				}
			}
		}
		
		
	}
	
	/** Runs the weighting and matching stage - this the main entry
	  * into the rest of the Terrier framework. In particular, it runs
	  * a selective query expansion mechanism if this functionality is
	  * enabled.
	  * @param srq the current SearchRequest object.
	  */
	public void runMatching(SearchRequest srq)
	{
	   //System.out.println("querying on qe manager.");
		Request rq = (Request)srq;
		System.err.println("weighting model: " + rq.getWeightingModel());
		DistributedMatchingQueryTerms mqt = (DistributedMatchingQueryTerms)rq.getMatchingQueryTerms();
		
		Query q = rq.getQuery();
		
		/* now propagate fields into requirements, and apply boolean matching
		   for the decorated terms. */
		ArrayList requirement_list = new ArrayList();
		ArrayList field_list = new ArrayList();
		
		q.getTermsOf(RequirementQuery.class, requirement_list, true);
		q.getTermsOf(FieldQuery.class, field_list, true);
		for (int j=0; j<field_list.size(); j++) 
			if (!requirement_list.contains(field_list.get(j)))
				requirement_list.add(field_list.get(j));
			
		if (requirement_list.size()>0) {
			mqt.addDocumentScoreModifier(new BooleanScoreModifier(requirement_list));
		}
		mqt.setQueryid(rq.getQueryID());
		mqt.setQuery(q);
		mqt.normaliseTermWeights();
		
		/*
		 * If the selective query expansion is not enabled, change the
		 * expansion status to be local and increase the counter for
		 * the queries that are external expanded. If the external resource
		 * it the local collection itself, it is equal to a local query expansion.
		 */
		if (!SELECTIVE_EXPANSION){
			this.expansionStatus = this.EXTERNAL_EXPANSION;
			externalExpansionCounter++;
		}
		/*
		 * If the selective query expansion is enabled, we compute the performance
		 * predictor values, and then make a decision of the selective query
		 * expansion. 
		 */
		else{
			/*
			 * Get the performance predictor's value of the local
			 * and external collections.
			 */
			double externalAvICTF = this.getAvICTF(mqt);
			double localAvICTF = this.queryingManager.getAvICTF(mqt);
			System.err.println("localAvICTF: " + Rounding.toString(localAvICTF, 4) +
					", externalAvICTF: " + Rounding.toString(externalAvICTF, 4));
			if (localAvICTF>0)
				avICTF.add(localAvICTF);
			if (externalAvICTF>0)
				avICTF.add(externalAvICTF);
			/*
			 * If the predictor values are lower than the threshold on both
			 * collections, we disable query expansion.
			 */
			if (localAvICTF<threshold && externalAvICTF<threshold){
				this.expansionStatus = this.DISABLE_EXPANSION;
				noExpansionCounter++;
			}
			/*
			 * Else, if the predictor value on the local collection is larger than the
			 * one on the external collection, we apply local query expansion.
			 */
			else if (localAvICTF>=externalAvICTF){
				this.expansionStatus = this.LOCAL_EXPANSION;
				localExpansionCounter++;
			}
			/*
			 * Else, apply external query expansion.
			 */
			else{
				this.expansionStatus = this.EXTERNAL_EXPANSION;
				externalExpansionCounter++;
			}
			System.err.println("Local QE for "+localExpansionCounter+" queries.");
			System.err.println("External QE for "+externalExpansionCounter+" queries.");
			System.err.println("Disable QE for "+noExpansionCounter+" queries.");
			double[] values = avICTF.toArray();
			Arrays.sort(values);
			System.err.println("min: " + values[0] + ", max: " + values[values.length-1]);
		}
		
		switch (expansionStatus){
		/*
		 * If disable query expansion, run matching on the local collection.
		 */
		case DISABLE_EXPANSION:
			queryingManager.runMatching(rq);
			break;
		/*
		 * Also run matching on the local collection if apply local query expansion.
		 */
		case LOCAL_EXPANSION:
			queryingManager.runMatching(rq);
			break;
		/*
		 * Run matching on the external collection if apply external query expansion.
		 */
		case EXTERNAL_EXPANSION:
		  System.out.println("querying on external collection.");
			mqt = updateQuery(mqt);
			if (! rq.isEmpty())
			{			
				QueryResultSet outRs = null;
				// retrieve for the query terms in a threaded setting
				this.retrieveThreaded(mqt);
				//now crop the collectionresultset down to a query result set.
				int numberOfRetrievedDocuments = 
						Integer.parseInt(ApplicationSetup.getProperty("matching.retrieved_set_size", "1000"));
				outRs = mergeResults(numberOfRetrievedDocuments);
				
				rq.setResultSet(outRs);
			}
			else
			{
				System.err.println("Returning empty result set as query "+rq.getQueryID()+" is empty");
				rq.setResultSet(new QueryResultSet(0));
			}
			break;
		}
	}
	
	
}
