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
 * The Original Code is QueryExpansionModel.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Gianni Amati <gba{a.}fub.it> (original author)
 *   Ben He <ben{a.}dcs.gla.ac.uk> 
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package org.terrier.matching.models.queryexpansion;
import org.terrier.matching.models.Idf;
import org.terrier.matching.models.WeightingModel;
import org.terrier.utility.ApplicationSetup;
/**
 * This class should be extended by the classes used
 * for weighting temrs and documents.
 * <p><b>Properties:</b><br><ul>
 * <li><tt>rocchio.beta</tt> - defaults to 0.4d</li>
 * <li><tt>parameter.free.expansion</tt> - defaults to true.</li>
 * </ul>
 * @author Gianni Amati, Ben He, Vassilis Plachouras
 * @version $Revision: 1.22 $
 */
public abstract class QueryExpansionModel extends WeightingModel{
	protected double EXPANSION_DOCUMENTS = 
		Integer.parseInt(ApplicationSetup.getProperty("expansion.documents", "3"));
	/** The number of the most weighted terms from the pseudo relevance set 
	 * to be added to the original query. There can be overlap between the 
	 * original query terms and the added terms from the pseudo relevance set.*/
	protected double EXPANSION_TERMS = 
		Integer.parseInt(ApplicationSetup.getProperty("expansion.terms", "10"));
	
	/** Rocchio's beta for query expansion. Its default value is 1.*/
	public double ROCCHIO_ALPHA;
	
	/** Rocchio's beta for query expansion. Its default value is 0.4.*/
	public double ROCCHIO_BETA;
	
	/** Boolean variable indicates whether to apply the parameter free query expansion. */
	public boolean PARAMETER_FREE;
	
	public boolean SUPPORT_PARAMETER_FREE_QE = false;
	
	/**
	 * Initialises the Rocchio's beta for query expansion.
	 */
	public void initialise() {
		/* Accept both rocchio.beta and rocchio_beta as property name. rocchio_beta will deprecated in due course. */
		ROCCHIO_BETA = Double.parseDouble(ApplicationSetup.getProperty("rocchio.beta", ApplicationSetup.getProperty("rocchio_beta", "0.4d")));
		ROCCHIO_ALPHA = Double.parseDouble(ApplicationSetup.getProperty("rocchio.alpha", ApplicationSetup.getProperty("rocchio_alpha", "1d")));
		PARAMETER_FREE = Boolean.parseBoolean(ApplicationSetup.getProperty("parameter.free.expansion", "false"));
	}
	/**
	 *  A default constructor for the class that initialises the idf attribute.
	 */
	public QueryExpansionModel() {
		super();
		i = new Idf();
		this.initialise();
	}
	
	/** Obtain the weighting model to use.
	 *  If Name does not contain ".", then <tt>
	 *  NAMESPACE_QEMODEL will be prefixed to it before loading.
	 *  @param Name the name of the weighting model to load.
	 */
	public static QueryExpansionModel getQueryExpansionModel(String Name)
	{
		QueryExpansionModel rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = "org.terrier.matching.models.queryexpansion." +Name;
		if (rtr == null)
		{
			try
			{
				if (Name.indexOf("(") > 0){
					String params = Name.substring( 
						Name.indexOf("(")+1, Name.indexOf(")"));
					String[] parameters = params.split("\\s*,\\s*");
					
					rtr = (QueryExpansionModel) Class.forName(
									Name.substring(0,Name.indexOf("(")))
							.getConstructor(
									new Class[]{String[].class})
							.newInstance(
									new Object[]{parameters});
				}else{						
					rtr = (QueryExpansionModel) Class.forName(Name).newInstance();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}
		return rtr;
	}
	
	/** Obtain the query expansion model to use.
	 *  If Name does not contain ".", then <tt>
	 *  NAMESPACE_QEMODEL will be prefixed to it before loading.
	 *  @param Name the name of the weighting model to load.
	 */
	public static QueryExpansionModel getModel(String Name)
	{
		QueryExpansionModel rtr = null;
		if (Name.indexOf(".") < 0 )
			Name = "org.terrier.matching.models.queryexpansion." +Name;
		if (rtr == null)
		{
			try
			{
				if (Name.indexOf("(") > 0){
					String params = Name.substring( 
						Name.indexOf("(")+1, Name.indexOf(")"));
					String[] parameters = params.split("\\s*,\\s*");
					
					rtr = (QueryExpansionModel) Class.forName(
									Name.substring(0,Name.indexOf("(")))
							.getConstructor(
									new Class[]{String[].class})
							.newInstance(
									new Object[]{parameters});
				}else{						
					rtr = (QueryExpansionModel) Class.forName(Name).newInstance();
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}
		return rtr;
	}
	
    /**
     * Returns the name of the model.
     * Creation date: (19/06/2003 12:09:55)
     * @return java.lang.String
     */
    public abstract String getInfo();
    
    /**
     * This method provides the contract for computing the normaliser of
     * parameter-free query expansion.
     * @param maxTermFrequency The maximum of the in-collection term frequency of the terms in the pseudo relevance set.
     * @param collectionLength The number of tokens in the collections.
     * @param totalDocumentLength The sum of the length of the top-ranked documents.
     * @return The normaliser.
     */
    public abstract double parameterFreeNormaliser(int maxTermFrequency, int documentLength);
}
