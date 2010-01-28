/*
 * Created on 11 Jun 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.models;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.matching.models.aftereffect.AfterEffect;
import uk.ac.gla.terrier.matching.models.basicmodel.BasicModel;
import uk.ac.gla.terrier.matching.models.normalisation.Normalisation;
import uk.ac.gla.terrier.utility.ApplicationSetup;

public class PFNWeightingModel extends FieldWeightingModel {
	
	protected static Logger logger = Logger.getRootLogger();
	protected WeightingModel wModel;
	
	protected Normalisation[] normalisations;
	/** The prefix of the package of the frequency normalisation methods. */
	protected final String NORMALISATION_PREFIX = "uk.ac.gla.terrier.matching.models.normalisation.Normalisation";
	/** The prefix of the package of the basic models for randomness. */
	protected final String WEIGHTINGMODEL_PREFIX = "uk.ac.gla.terrier.matching.models.";
	/**
	 * The default constructor. Takes an array of strings to define the 
	 * Basic Model, the After Effect component and the Normalisation component.
	 * If the array is less than 3 items in length, then empty strings will be passed
	 * instead of the After Effect and/or Normalisation components.
	 * @param components Corresponds to the names of the 3 DFR weighting models component
	 * names, as passed to initialise().
	 */
	public PFNWeightingModel (int numberOfFields, String... components){
		super(numberOfFields);
		this.initialise(
			components[0].trim(), 
			components.length > 1 ? components[1].trim() : "");
	}
	/**
	 * Initialise the components in the DFR model. For each component, if a package
	 * is not specified, then a prefix will be applied. These are BASICMODEL_PREFIX,
	 * AFTEREFFECT_PREFIX and NORMALISATION_PREFIX respectively. Note that NORMALISATION_PREFIX
	 * includes a partial class name.
	 * @param basicModelName The name of the applied basic model for randomness. This
	 * component must be specified and can NOT be an empty string.
	 * @param afterEffectName The name of the applied first normalisation by after
	 * effect. An empty string to disable this component.
	 * @param normalisationName The name of the applied frequency normalisation
	 * component. An empty string to disable this component.
	 */
	protected void initialise(String wModelName, 
			String normalisationName
			){

		try{

			this.wModel = (WeightingModel)Class.forName(wModelName.trim()).newInstance();


			// --------- NORMALISATION -----------------------------
			// check to see if we're using a frequency normalisation component
			if (normalisationName.length() == 0){
				//dont use the normalisation, but still load one in case it is used
				normalisationName = "0";
			}
			// initialise the frequency normalisation component
			if (normalisationName.indexOf('.') < 0)
				normalisationName = this.NORMALISATION_PREFIX.concat(normalisationName);
			this.normalisations = new Normalisation[this.NumberOfFields];
			for (int i=0; i<NumberOfFields; i++)
				normalisations[i] = (Normalisation)Class.forName(normalisationName.trim()).newInstance();
			// ------------------------------------------------------

		}
		catch(Exception e){
			logger.fatal("Error occured while initialising the DFR model.",e);
			logger.fatal("Exiting ...");
			System.exit(1);
		}
	}
	/**
	 * Sets the average length of documents in the collection.
	 * @param avgDocLength The documents' average length.
	 */
	public void setAverageDocumentLength(double avgDocLength) {
		wModel.setAverageDocumentLength(avgDocLength);
	}
	
	/**
	 * Sets the number of documents in the collection.
	 * @param numOfDocs the number of documents in the collection.
	 */
	public void setNumberOfDocuments(double numOfDocs) {
		wModel.setNumberOfDocuments(numOfDocs);
		for (int i=0; i<NumberOfFields; i++)
			normalisations[i].setNumberOfDocuments(numOfDocs);
	}
	/**
	 * Set the number of tokens in the collection.
	 * @param value The number of tokens in the collection.
	 */
	public void setNumberOfTokens(double value){
		wModel.setNumberOfTokens(value);
	}
	/**
	 * Set the number of unique terms in the collection.
	 * @param number double The number of unique terms in the collection.
	 */
	public void setNumberOfUniqueTerms(double number) {
		wModel.setNumberOfPointers(number);
    }
	
	/**
	 * Set the frequency normalisation parameter.
	 * @param value The given parameter value.
	 */
	public void setParameter(double value){
		wModel.setParameter(value);
	}

	/** Return the parameter set by setParameter()
	  * @return parameter double value */
	public double getParameter(){
		return wModel.getParameter();
	}
	
	
	/**
	 * Returns the name of the model.
	 * @return The name of the model.
	 */
	public final String getInfo() {
		return wModel.getInfo();
	}

	@Override
	public double score(double tf, double[] tf_f, double docLength,
			double[] fieldLengths) {
		double tfn = 0d;
		for (int i=0; i<NumberOfFields; i++)
			tfn += normalisations[i].normalise(tf_f[i], fieldLengths[i], TF_f[i]);
		return wModel.score(tfn, docLength);
	}

	@Override
	public double score(double tf, double docLength) {
		// TODO Auto-generated method stub
		return wModel.score(tf, docLength);
	}

	@Override
	public double score(double tf, double docLength, double n_t, double F_t,
			double keyFrequency) {
		// TODO Auto-generated method stub
		return wModel.score(tf, docLength, n_t, F_t, keyFrequency);
	}

}
