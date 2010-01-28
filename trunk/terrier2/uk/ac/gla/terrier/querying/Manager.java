 /*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
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
 * The Original Code is Manager.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.querying;
import gnu.trove.TIntArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import uk.ac.gla.terrier.querying.parser.QueryParser;
import uk.ac.gla.terrier.querying.parser.QueryParserException;


import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.matching.Matching;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.QueryResultSet;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.dsms.BooleanScoreModifier;
import uk.ac.gla.terrier.querying.parser.FieldQuery;
import uk.ac.gla.terrier.querying.parser.Query;
import uk.ac.gla.terrier.querying.parser.RequirementQuery;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.terms.TermPipeline;
import uk.ac.gla.terrier.terms.TermPipelineAccessor;
import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
  * This class is responsible for handling/co-ordinating the main high-level
  * operations of a query. These are:
  * <li>Pre Processing (Term Pipeline, Control finding, term aggregration)</li>
  * <li>Matching</li>
  * <li>Post-processing @see uk.ac.gla.terrier.querying.PostProcess </li>
  * <li>Post-filtering @see uk.ac.gla.terrier.querying.PostFilter </li>
  * </ul>
  * Example usage:
  * <pre>
  * Manager m = new Manager(index);
  * SearchRequest srq = m.newSearchRequest("Q1", "term1 title:term2");
  * m.runPreProcessing(srq);
  * m.runMatching(srq);
  * m.runPostProcess(srq);
  * m.runPostFilters(srq);
  * </pre>
  * <p>
  * <b>Properties</b><ul>
  * <li><tt>querying.normalise.weights</tt> - normalise the query term weights. Defaults to true</li>
  * <li><tt>querying.default.controls</tt> - sets the default controls for each query</li>
  * <li><tt>querying.allowed.controls</tt> - sets the controls which a users is allowed to set in a query</li>
  * <li><tt>querying.postprocesses.order</tt> - the order post processes should be run in</li>
  * <li><tt>querying.postprocesses.controls</tt> - mappings between controls and the post processes they should cause</li>
  * <li><tt>querying.postfilters.order</tt> - the order post filters should be run in </li>
  * <li><tt>querying.postfilters.controls</tt> - mappings between controls and the post filters they should cause</li>
  * </ul>
  * <p><b>Controls</b><ul>
  * <li><tt>start</tt> : The result number to start at - defaults to 0 (1st result)</li>
  * <li><tt>end</tt> : the result number to end at - defaults to 0 (display all results)</li>
  * <li><tt>c</tt> : the c parameter for the DFR models, or more generally, the parameters for weighting model scheme</li>
  * </ul>
  */
public class Manager implements TermPipeline, TermPipelineAccessor
{
	protected static Logger logger = Logger.getRootLogger();

	/* ------------Module default namespaces -----------*/
	/** The default namespace for PostProcesses to be loaded from */
	public static final String NAMESPACE_POSTPROCESS
		= "uk.ac.gla.terrier.querying.";
	/** The default namespace for PostFilters to be loaded from */
	public static final String NAMESPACE_POSTFILTER
		= "uk.ac.gla.terrier.querying.";
	/** The default namespace for Matching models to be loaded from */
	public static final String NAMESPACE_MATCHING
		= "uk.ac.gla.terrier.matching.";
	/** The default namespace for Weighting models to be loaded from */
	public static final String NAMESPACE_WEIGHTING
		= "uk.ac.gla.terrier.matching.models.";
	/** The default namespace for TermPipeline modules to be loaded from */
	public final static String NAMESPACE_PIPELINE = "uk.ac.gla.terrier.terms.";
	/* ------------------------------------------------*/
	/* ------------Instantiation caches --------------*/
	/** Cache loaded Matching models in this hashtable */
	protected Hashtable Cache_Matching = new Hashtable();
	/** Cache loaded PostProcess models in this hashtable */
	protected Hashtable Cache_PostProcess = new Hashtable();
	/** Cache loaded PostFitler models in this hashtable */
	protected Hashtable Cache_PostFilter = new Hashtable();
	/* ------------------------------------------------*/
	/** The term pipeline to pass all terms through */
	protected TermPipeline pipeline_first;
	/** The index this querying comes from */
	protected Index index;
	/** This contains a list of controls that may be set in the querying API */
	protected HashSet Allowed_Controls;
	/** This contains the mapping of controls and their values that should be 
	  * set identially for each query created by this Manager */
	protected Hashtable Default_Controls;
	/** How many default controls exist.
	  * Directly corresponds to Default_Controls.size() */
	protected int Defaults_Size = 0;

	/** An ordered list of post process names. The controls at the same index in the PostProcesses_Controls
	  * list turn on the post process in this list. */
	protected String[] PostProcesses_Order;
	
	/** A 2d array, contains (on 2nd level) the list of controls that turn on the PostProcesses
	  * at the same 1st level place on PostProcesses_Order */
	protected String[][] PostProcesses_Controls;

	/** An ordered list of post filters names. The controls at the same index in the  PostFilters_Controls
	  * list turn on the post process in this list. */
	protected String[] PostFilters_Order;

	/** A 2d array, contains (on 2nd level) the list of controls that turn on the PostFilters
	  * at the same 1st level place on PostFilters_Order */
	protected String[][] PostFilters_Controls;
	
	/** This class is used as a TermPipelineAccessor, and this variable stores
	  * the result of the TermPipeline run for that term. */
	protected String pipelineOutput = null;
	
	/**
	 * A boolean variable indicates whether to cache post filters. Defaults to false.
	 */
	protected boolean CACHING_FILTERS = Boolean.parseBoolean(ApplicationSetup.getProperty("manager.caching.filters","false"));
	/**
	 * A boolean variable indicates whether performs matching for empty queries. Defaults to false.
	 */
	protected final boolean MATCH_EMPTY_QUERY = Boolean.parseBoolean(ApplicationSetup.getProperty("match.empty.query","false"));
	/**
	 * A boolean variable indicates whether to normalise query term weights. Defaults to true.
	 * Set it to false if matching is performed over already created expanded queries.
	 */
	protected final boolean NORMALISE_QUERY_TERM_WEIGHTS = Boolean.parseBoolean(ApplicationSetup.getProperty("querying.normalise.weights", "true"));

	
	/** Constructor for making a querying copy of Terrier. All client
	  * code should instantiate one of these with the index they wish to
	  * use.
	  * @param diskIndexes The Index that should be used for this instance of Terrier
	  */

	/** Default constructor. Use the default index
	  * @since 2.0 */
	public Manager()
	{
		this(Index.createIndex());
	}

	/** Construct a Manager using the specified Index
	  * @param index The index to use in this manage
	  */
	public Manager(Index index)
	{
		this.useThisIndex(index);
		this.load_pipeline();
		this.load_controls_allowed();
		this.load_controls_default();
		this.load_postprocess_controls();
		this.load_postfilters_controls();
	}
	/* ----------------------- Initialisation methods --------------------------*/

	/** use the index specified for the Manager */
	protected void useThisIndex(final Index i)
	{
		index = i;
	}

	/** load in the controls that user is allowed to set */
	protected void load_controls_allowed()
	{
		/* load in the controls that user is allowed to set */
		String allowed = ApplicationSetup.getProperty("querying.allowed.controls", "c,start,end").trim().toLowerCase();
		String[] controls = allowed.split("\\s*,\\s*");
		Allowed_Controls = new HashSet();
		for(int i=0;i<controls.length;i++)
		{
			Allowed_Controls.add(controls[i]);
		}
	}
	/** load in the control defaults */
	protected void load_controls_default()
	{
		String defaults = ApplicationSetup.getProperty("querying.default.controls", "").trim();
		String[] controls = defaults.split("\\s*,\\s*");
		Default_Controls = new Hashtable();
		for(int i=0;i<controls.length;i++)
		{
			String control[] = controls[i].split(":");
			/* control[0] contains the control name, control[1] contains the value */
			if (control.length < 2)
			{
				continue;
			}
			Default_Controls.put(control[0].toLowerCase(), control[1]);
		}
		String def_c = null;
		Defaults_Size = Default_Controls.size();
	}
	
	protected static final String[] tinySingleStringArray = new String[0];
	protected static final String[][] tinyDoubleStringArray = new String[0][0];
	
	/** load in the allowed postprocceses controls, and the order to run post processes in */
	protected void load_postprocess_controls()
	{
		/* what we have is a mapping of controls to post processes, and an order post processes should
		   be run in.
		   what we need is the order to check the controls in, and which pp to run for each
		*/

		String[] order_pp, control_pp;
		String tmp = ApplicationSetup.getProperty("querying.postprocesses.order", "").trim();
		if (tmp.length() > 0)
			order_pp = tmp.split("\\s*,\\s*");
		else
			order_pp = new String[0];
		
		tmp = ApplicationSetup.getProperty("querying.postprocesses.controls", "").trim();
		if (tmp.length() > 0)
			control_pp = tmp.split("\\s*,\\s*");
		else
			control_pp = new String[0];
		
		//control_and_pp holds an array of pairs - control, pp, control, pp, control, pp
		String[] control_and_pp = new String[control_pp.length*2]; int count = 0;
		
		//iterate through controls and pp names putting in 1d array
		for(int i=0; i<control_pp.length; i++)
		{
			if (control_pp[i].indexOf(":") > 0)
			{
				String[] control_and_postprocess = control_pp[i].split(":");
				control_and_pp[count] = control_and_postprocess[0];//control
				control_and_pp[count+1] = control_and_postprocess[1];//postfilter
				count+=2;
			}
		}

		/* basically, we now invert, so we have an array of pp names, and in a separate array, a list
		of controls that can turn that pf on */
		ArrayList pp_order = new ArrayList();
		ArrayList pp_controls = new ArrayList();
		
		for(int i=0; i<order_pp.length; i++)
		{
			ArrayList controls_for_this_pp = new ArrayList();
			String tmpPP = order_pp[i];
			for(int j=0;j<count;j+=2)
			{
				if (tmpPP.equals(control_and_pp[j+1]))
				{
					controls_for_this_pp.add(control_and_pp[j]);
				}
			}
			//ok, there are controls that can turn this pf on, so lets enable it
			if (controls_for_this_pp.size() > 0)
			{
				pp_controls.add((String[])controls_for_this_pp.toArray(tinySingleStringArray));
				pp_order.add(tmpPP);
			}
		}
		//cast back to arrays
		PostProcesses_Order= (String[])pp_order.toArray(tinySingleStringArray);
		PostProcesses_Controls = (String[][])pp_controls.toArray(tinyDoubleStringArray);
	}


	/** load in the allowed post filter controls, and the order to run post processes in */
	protected void load_postfilters_controls()
	{
		/* what we have is a mapping of controls to post filters, and an order post processes should
		   be run in.
		   what we need is the order to check the controls in, and which pp to run for each
		*/

		String[] order_pf, control_pf;
		String tmp = ApplicationSetup.getProperty("querying.postfilters.order", "").trim();
		if (tmp.length() > 0)
			order_pf = tmp.split("\\s*,\\s*");
		else
			order_pf = new String[0];
		
		tmp = ApplicationSetup.getProperty("querying.postfilters.controls", "").trim();
		if (tmp.length() > 0)
			control_pf = tmp.split("\\s*,\\s*");
		else
			control_pf = new String[0];
		
		String[] control_and_pf = new String[control_pf.length*2]; int count = 0;
		//iterate through controls and pf names putting in 1d array
		for(int i=0; i<control_pf.length; i++)
		{
			if (control_pf[i].indexOf(":") > 0)
			{
				String[] control_and_postfilter = control_pf[i].split(":");
				control_and_pf[count] = control_and_postfilter[0];//control
				control_and_pf[count+1] = control_and_postfilter[1];//postfilter
				count+=2;
			}
		}

		/* basically, we now invert, so we have an array of pf names, in a separate array, a list
		of controls that can turn that pf on */
		ArrayList pf_order = new ArrayList();
		ArrayList pf_controls = new ArrayList();
		for(int i=0; i<order_pf.length; i++)
		{
			ArrayList controls_for_this_pf = new ArrayList();
			String tmpPF = order_pf[i];
			for(int j=0;j<count;j+=2)
			{
				if (tmpPF.equals(control_and_pf[j+1]))
				{
					controls_for_this_pf.add(control_and_pf[j]);
				}
			}
			//ok, there are controls that can turn this pf on, so lets enable it
			if (controls_for_this_pf.size() > 0)
			{
				pf_controls.add((String[])controls_for_this_pf.toArray(tinySingleStringArray));
				pf_order.add(tmpPF);
			}			
		}
		//cast back to arrays
		PostFilters_Order = (String[])pf_order.toArray(tinySingleStringArray);
		PostFilters_Controls = (String[][])pf_controls.toArray(tinyDoubleStringArray);
	}

	private static Class[] constructor_array_termpipeline = new Class[]{TermPipeline.class};

	/** load in the term pipeline */
	protected void load_pipeline()
	{
		String[] pipes = ApplicationSetup.getProperty(
				"termpipelines", "Stopwords,PorterStemmer").trim()
				.split("\\s*,\\s*");
		TermPipeline next = this;
		TermPipeline tmp;
		for(int i=pipes.length-1; i>=0; i--)
		{
			try{
				String className = pipes[i];
				if (className.length() == 0)
					continue;
				if (className.indexOf(".") < 0 )
					className = NAMESPACE_PIPELINE + className;
				Class pipeClass = Class.forName(className, false, this.getClass().getClassLoader());
				tmp = (TermPipeline) (pipeClass.getConstructor(
						constructor_array_termpipeline)
						.newInstance(new Object[] {next}));
				next = tmp;
			}catch (Exception e){
				logger.error("TermPipeline object "+NAMESPACE_PIPELINE+pipes[i]+" not found",e);
			}
		}
		pipeline_first = next;
	}

	/* -------------------term pipeline implementation --------------------*/
	/**
	 * Make this object a term pipeline implementor.
	 * @see uk.ac.gla.terrier.terms.TermPipeline
	 */
	public void processTerm(String t)
	{
		pipelineOutput = t;
	}
	/*-------------- term pipeline accessor implementation ----------------*/
	/** A term pipeline accessor */
	public String pipelineTerm(String t)
	{
		pipelineOutput = null;
		pipeline_first.processTerm(t);
		return pipelineOutput;
	}
	/* -------------- factory methods for SearchRequest objects ---------*/
	/** Ask for new SearchRequest object to be made. This is internally a
	  * Request object */
	public SearchRequest newSearchRequest()
	{
		Request q = new Request();
		if (Defaults_Size >0)
			setDefaults(q);
		return (SearchRequest)q;
	}
	/** Ask for new SearchRequest object to be made. This is internally a
	  * Request object
	  * @param QueryID The request should be identified by QueryID
	  */
	public SearchRequest newSearchRequest(String QueryID)
	{
		Request q = new Request();
		if (Defaults_Size >0)
			setDefaults(q);
		q.setQueryID(QueryID);
		return (SearchRequest)q;
	}

	/** Ask for new SearchRequest object to be made, instantiated using the 
 	  * specified query id, and that the specified query should be parsed.
 	  * @since 2.0
 	  * @param QueryID The request should be identified by QueryID
 	  * @param query The actual user query
 	  * @return The fully init'd search request for use in the manager */
	public SearchRequest newSearchRequest(String QueryID, String query)
	{
		Request q = new Request();
		if (Defaults_Size >0)
			setDefaults(q);
		q.setQueryID(QueryID);
		try{
			QueryParser.parseQuery(query, q);	
		} catch (QueryParserException qpe) {
			logger.error(qpe);
		}
		return q;
	}
	

	/** Set the default values for the controls of this new search request
	 *  @param srq The search request to have the default set to. This is
	 *  done using the Default_Controls table, which is loaded by the load_controls_default
	 *  method. The default are set in the properties file, by the <tt>querying.default.controls</tt> */
	protected void setDefaults(Request srq)
	{
		//NB/TODO: Hashtable.clone() is relatively expensive according j2sdk docs for Hashtable
		srq.setControls((Hashtable)Default_Controls.clone());
	}
	/**
	 * Returns the index used by the manager. It is used for matching
	 * and possibly post-processing.
	 * @return Index the index used by the manager.
	 */
	public Index getIndex() {
		return index;
	}
	
	/** Provide a common interface for changing property values.
	  * @param key Key of property to set
	  * @param value Value of property to set */
	public void setProperty(String key, String value)
	{
		ApplicationSetup.setProperty(key, value);
	}

	/** Set all these properties. Implemented using setProperty(String,String).
	  * @param p All properties to set */
	public void setProperties(Properties p) {
        Enumeration e = p.keys();
        while (e.hasMoreElements()) {
            String propertyName = (String)e.nextElement();
            String propertyValue = p.getProperty(propertyName);
            setProperty(propertyName, propertyValue);
        }
    }


	//run methods
	//These methods are called by the application in turn
	//(or could just have one RUN method, and these are privates,
	//but I would prefer the separate method)
	/** runPreProcessing */
	public void runPreProcessing(SearchRequest srq)
	{
		Request rq = (Request)srq;
		Query query = rq.getQuery();
		//System.out.println(query);
		//get the controls
		boolean rtr = ! query.obtainControls(Allowed_Controls, rq.getControlHashtable());
		//we check that there is stil something left in the query
		if (! rtr)
		{
			rq.setEmpty(true);
			return;
		}
		
		/*if(ApplicationSetup.getProperty("querying.manager.sendlang","").equals("true"))
		{
			String lang = rq.getControl("lang").toLowerCase();
			String marker = ApplicationSetup.getProperty("termpipelines.languageselector.markerlang", "||LANG:");
			if(lang.length() > 0)
			{
				if(logger.isDebugEnabled()){
					logger.debug("Sending marker through pipeline "+marker+lang);
				}
				pipelineTerm(marker+lang);
				
			}
		}*/
		synchronized(this) {
			rtr = query.applyTermPipeline(this);
		}

		if (! rtr)
		{
			rq.setEmpty(true);
			return;
		}

		if (ApplicationSetup.getProperty("querying.no.negative.requirement", "").equals("true"))
		{
			ArrayList terms = new ArrayList();
			query.getTermsOf(uk.ac.gla.terrier.querying.parser.SingleTermQuery.class, terms, true);
			final int size = terms.size();
			for(int x=0;x<size;x++)
			{
				uk.ac.gla.terrier.querying.parser.SingleTermQuery sqt = (uk.ac.gla.terrier.querying.parser.SingleTermQuery)terms.get(x);
				sqt.setRequired(0);
			}
		}

		MatchingQueryTerms queryTerms = new MatchingQueryTerms(rq.getQueryID());
		query.obtainQueryTerms(queryTerms);
		rq.setMatchingQueryTerms(queryTerms);
		
	}
	/** Runs the weighting and matching stage - this the main entry
	  * into the rest of the Terrier framework.
	  * @param srq the current SearchRequest object.
	  */
	public void runMatching(SearchRequest srq)
	{
		Request rq = (Request)srq;
		if ( (! rq.isEmpty()) || MATCH_EMPTY_QUERY )
		{
			//TODO some exception handling here for not found models
			WeightingModel wmodel = WeightingModel.getWeightingModel(rq.getWeightingModel());
			
			/* craigm 05/09/2006: only set the parameter of the weighting model
			 * if it was explicitly set if c_set control is set to true. Otherwise
			 * allow the weighting model to assume it's default value.
			 * This changes previous behaviour. TODO: some consideration for
			 * non TREC applications */
			if (rq.getControl("c_set").equals("true"))
			{
				wmodel.setParameter(Double.parseDouble(rq.getControl("c")));
			}

			Matching matching = getMatchingModel(rq.getMatchingModel());
			matching.setModel(wmodel);
			if (logger.isDebugEnabled()){
				logger.debug("weighting model: " + wmodel.getInfo());
			}
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

			/*if (logger.isDebugEnabled())
			{
				for (int i=0; i<requirement_list.size(); i++) {
					if(logger.isDebugEnabled()){
					logger.debug("requirement: " + ((RequiredTermModifier)requirement_list.get(i)).getName());
					}
				}
				for (int i=0; i<field_list.size(); i++) {
					if(logger.isDebugEnabled()){
					logger.debug("field: " + ((TermInFieldModifier)field_list.get(i)).getName()); 
					}
				}
			}*/		
		
			if (requirement_list.size()>0) {
				mqt.addDocumentScoreModifier(new BooleanScoreModifier(requirement_list));
			}

			mqt.setQuery(q);
			if (this.NORMALISE_QUERY_TERM_WEIGHTS)
				mqt.normaliseTermWeights();
			matching.match(rq.getQueryID(), mqt);
			//matching.match(rq.getQueryID(), rq.getMatchingQueryTerms());
			//now crop the collectionresultset down to a query result set.
			ResultSet outRs = matching.getResultSet();
			rq.setResultSet((ResultSet)(outRs.getResultSet(0, outRs.getResultSize())));
		}
		else
		{
			logger.warn("Returning empty result set as query "+rq.getQueryID()+" is empty");
			rq.setResultSet(new QueryResultSet(0));
		}
	}
	/** Runs the PostProcessing modules in order added. PostProcess modules
	  * alter the resultset. Examples might be query expansions which completelty replaces
	  * the resultset.
	  * @param srq the current SearchRequest object. */
	public void runPostProcessing(SearchRequest srq)
	{
		Request rq = (Request)srq;
		Hashtable controls = rq.getControlHashtable();
		
		for(int i=0; i<PostProcesses_Order.length; i++)
		{
			String PostProcesses_Name = PostProcesses_Order[i];
			for(int j=0; j<PostProcesses_Controls[i].length; j++)
			{
				String ControlName = PostProcesses_Controls[i][j];
				String value = (String)controls.get(ControlName);
				//System.err.println(ControlName+ "("+PostProcesses_Name+") => "+value);
				if (value == null)
					continue;
				value = value.toLowerCase();
				if(! (value.equals("off") || value.equals("false")))
				{
					if (logger.isDebugEnabled()){
						logger.debug("Processing: "+PostProcesses_Name);
					}
					getPostProcessModule(PostProcesses_Name).process(this, srq);
					//we've now run this post process module, no need to check the rest of the controls for it.
					break;
				}
			}
		}
		String lastPP = null;
		if ((lastPP = ApplicationSetup.getProperty("querying.lastpostprocess",null)) != null)
		{
			getPostProcessModule(lastPP).process(this, srq);
		}
	}
	
	
	/** Runs the PostFilter modules in order added. PostFilter modules
	  * filter the resultset. Examples might be removing results that don't have
	  * a hostname ending in the required postfix (site), or document ids that don't match
	  * a pattern (scope).
	  * @param srq the current SearchRequest object. */
	public void runPostFilters(SearchRequest srq)
	{
		Request rq = (Request)srq;
		PostFilter[] filters = getPostFilters(rq);
		final int filters_length = filters.length;
		
		//the results to filter
		ResultSet results = rq.getResultSet();

		//the size of the results - this could be more than what we need to display
		int ResultsSize = results.getResultSize();

		//load in the lower and upper bounds of the resultset
		String tmp = rq.getControl("start");/* 0 based */
		if (tmp.length() == 0)
			tmp = "0";
		int Start = Integer.parseInt(tmp);
		tmp = rq.getControl("end");
		if (tmp.length() == 0)
			tmp = "0";
		int End = Integer.parseInt(tmp);/* 0 based */
		if (End == 0)
		{
			End = ResultsSize -1;
		}
		int length = End-Start+1;
		if (length > ResultsSize)
			length = ResultsSize-Start;
		//we've got no filters set, so just give the results ASAP
		if (filters_length == 0)
		{
			rq.setResultSet( results.getResultSet(Start, length) );
			if (logger.isDebugEnabled()) { 
				logger.debug("No filters, just Crop: "+Start+", length"+length);
				logger.debug("Resultset is now "+results.getScores().length + " long");
			}
			return;
		}

		//tell all the postfilters that they are processing another query
		for(int i=0;i<filters_length; i++)
		{
			filters[i].new_query(this, srq, results);
		}
		
		int doccount = -1;//doccount is zero-based, so 0 means 1 document
		TIntArrayList docatnumbers = new TIntArrayList();//list of resultset index numbers to keep
		byte docstatus; int thisDocId;
		int[] docids = results.getDocids();
		//int elitesetsize = results.getExactResultSize();
		//the exact result size is the total number of
		//documents that would be retrieved if we
		//didn't do any cropping
		int elitesetsize = results.getResultSize();
		for(int thisdoc = 0; thisdoc < elitesetsize; thisdoc++)
		{
			//run this document through all the filters
			docstatus = PostFilter.FILTER_OK;
			thisDocId = docids[thisdoc];
			//run this doc through the filters
			for(int i=0;i<filters_length; i++)
			{
				if ( ( docstatus = filters[i].filter(this, srq, results, thisdoc, thisDocId) )
					== PostFilter.FILTER_REMOVE
				)
					break;
					//break if the document has to be removed
			}
			//if it's not being removed, then
			if (docstatus != PostFilter.FILTER_REMOVE) //TODO this should always be true
			{
				//success, another real document
				doccount++;
				//check if it's in our results "WINDOW"
				if (doccount >= Start)
				{
					if (doccount <= End)
					{	//add to the list of documents to keep
						docatnumbers.add(thisdoc);
						//System.out.println("Keeping @"+thisdoc);
					}
					else
					{
						//we've now got enough results, break
						break;
					}
				}
			}
			else
			{
				//System.out.println("Removed");
			}
		}
		//since doccount is zero-based, we add one so that it
		//corresponds to the real number of documents.
		doccount++; 
		rq.setNumberOfDocumentsAfterFiltering(doccount);
		if (docatnumbers.size() < docids.length)
		{
			//result set is definently shorter, replace with new one
			rq.setResultSet( results.getResultSet(docatnumbers.toNativeArray()));
			rq.getResultSet().setExactResultSize(results.getExactResultSize());
		}
	}
	/** parses the controls hashtable, looking for references to controls, and returns the appropriate
	  * postfilters to be run. */
	private PostFilter[] getPostFilters(Request rq)
	{
		Hashtable controls = rq.getControlHashtable();
		ArrayList postfilters = new ArrayList();
		for(int i=0; i<PostFilters_Order.length; i++)
		{
			String PostFilter_Name = PostFilters_Order[i];
			for(int j=0; j<PostFilters_Controls[i].length; j++)
			{
				String ControlName = PostFilters_Controls[i][j];
				String value = (String)controls.get(ControlName);
				if (logger.isDebugEnabled()){
					logger.debug(ControlName+ "("+PostFilter_Name+") => "+value);
				}
				if (value == null)
					continue;
				value = value.toLowerCase();
				if(! (value.equals("off") || value.equals("false")))
				{
					postfilters.add(getPostFilterModule(PostFilter_Name));
					//we've now run this post process module, no need to check the rest of the controls for it.
					break;
				}
			}
		}
		return (PostFilter[])postfilters.toArray(new PostFilter[0]);
	}
	
	/*-------------------------------- helper methods -----------------------------------*/
	//helper methods. These get the appropriate modules named Name of the appropate type
	//from a hashtable cache, or instantiate them should they not exist. ref is then casted and returned.
	//this all makes the run*() code much much cleaner
	/** Returns the matching model named ModelName. Caches already 
	  * instantiaed matching models in Hashtable Cache_Matching.
	  * If the matching model name doesn't contain '.', then NAMESPACE_MATCHING
	  * is prefixed to the name. 
	  * @param ModelName The name of the class to instantiate and return*/
	protected Matching getMatchingModel(String ModelName)
	{
		Matching rtr = null;
		//add the namespace if the modelname is not fully qualified
		
		final String ModelNames[] = ModelName.split("\\s*,\\s*");
		final int modelCount = ModelNames.length;
		StringBuilder entireSequence = new StringBuilder();
		for(int i =0;i<modelCount;i++)
		{
			if (ModelNames[i].indexOf(".") < 0 )
				ModelNames[i]  = NAMESPACE_MATCHING + ModelNames[i];
			entireSequence.append(ModelNames[i]);
			entireSequence.append(",");
		}
		ModelName = entireSequence.substring(0,entireSequence.length() -1);
		//check for already instantiated class
		rtr = (Matching)Cache_Matching.get(ModelName);
		if (rtr == null)
		{
			boolean first = true;
			for(int i=modelCount-1;i>=0;i--)
			{
				try
				{
					//load the class
					Class formatter = Class.forName(ModelNames[i], false, this.getClass().getClassLoader());
					//get the correct constructor - an Index class in this case
					
					Class[] params = new Class[1];
					Object[] params2 = new Object[1];
					if (first)
					{
						params[0] = Index.class;
						params2[0] = index;
					}
					else
					{
						params[0] = Matching.class;
						params2[0] = rtr;
					}
					//and instantiate
					rtr = (Matching) (formatter.getConstructor(params).newInstance(params2));
					first = false;
				}
				catch(java.lang.reflect.InvocationTargetException ite)
				{
					logger.error("Recursive problem with matching model named: "+ModelNames[i],ite);
					return null;
				}
				catch(Exception e)
				{
					logger.error("Problem with matching model named: "+ModelNames[i],e);
					return null;
				}
			}
		}
		Cache_Matching.put(ModelName, rtr);
		return rtr;
	}
	/** Returns the weighting model named ModelName. Caches already
	  * instantiaed matching models in Hashtable Cache_Weighting.
	  * If the weighting model name doesn't contain '.', then 
	  * NAMESPACE_WEIGHTING is prefixed to the name. 
	  * @param ModelName The name of the weighting model to instantiate */
	/**
	 * 04/09/2008, Ben: This method is moved to WeightingModel as a static
	 * method. Use WeightingModel.getWeightingModel() instead.
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
					String params = ModelName.substring( 
						ModelName.indexOf("(")+1, ModelName.indexOf(")"));
					String[] parameters = params.split("\\s*,\\s*");
					
					rtr = (Model) Class.forName(
									ModelName.substring(0,ModelName.indexOf("(")))
							.getConstructor(
									new Class[]{String[].class})
							.newInstance(
									new Object[]{parameters});
				}else{						
					rtr = (Model) Class.forName(ModelName).newInstance();
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
	}*/
	
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
				logger.error("Problem with postprocess named: "+Name,e);
				return null;
			}
			Cache_PostProcess.put(Name, rtr);
		}
		return rtr;
	}
	/** Returns the post filter class named ModelName. Caches already
	  * instantiaed matching models in Hashtable Cache_PostFilter.
	  * If the matching model name doesn't contain '.',
	  * then NAMESPACE_POSTFILTER is prefixed to the name.
	  * @param Name The name of the post filter to return */
	protected PostFilter getPostFilterModule(String Name)
	{
		PostFilter rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = NAMESPACE_POSTFILTER +Name;
		//check for already loaded post filters
		if (CACHING_FILTERS)
			rtr = (PostFilter)Cache_PostFilter.get(Name);
		if (rtr == null)
		{
			try
			{
				rtr = (PostFilter) Class.forName(Name).newInstance();
			}
			catch(Exception e)
			{
				logger.error("Problem with postprocess named: "+Name,e);
				return null;
			}
			if (CACHING_FILTERS)
				Cache_PostFilter.put(Name, rtr);
		}
		return rtr;
	}
	
	/**
	 * Returns information about the weighting models and 
	 * the post processors used for the given search request.
	 * @param srq the search request for which we obtain 
	 *		the information.
	 * @return String information about the weighting models 
	 *		 and the post processors used.
	 */
	public String getInfo(SearchRequest srq) {
		Request rq = (Request)srq; 
		StringBuilder info = new StringBuilder();
		
		//obtaining the weighting model information
		WeightingModel wmodel = WeightingModel.getWeightingModel(rq.getWeightingModel());
		final String param = rq.getControl("c");
		if (rq.getControl("c_set").equals("true") && param.length() > 0)
			wmodel.setParameter(Double.parseDouble(param));
		info.append(wmodel.getInfo());
		
		//obtaining the post-processors information
		Hashtable controls = rq.getControlHashtable();
		
		for(int i=0; i<PostProcesses_Order.length; i++)
		{
			String PostProcesses_Name = PostProcesses_Order[i];
			for(int j=0; j<PostProcesses_Controls[i].length; j++)
			{
				String ControlName = PostProcesses_Controls[i][j];
				String value = (String)controls.get(ControlName);
				//System.err.println(ControlName+ "("+PostProcesses_Name+") => "+value);
				if (value == null)
					continue;
				value = value.toLowerCase();
				if(! (value.equals("off") || value.equals("false")))
				{
					info.append("_"+getPostProcessModule(PostProcesses_Name).getInfo());
					//we've now run this post process module, no need to check the rest of the controls for it.
					break;
				}
			}
		}		
		return info.toString();
	}
}
