/*
 * Created on 11 Jun 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.models;

import uk.ac.gla.terrier.matching.models.WeightingModel;

public abstract class FieldWeightingModel extends WeightingModel {
	
	protected final int NumberOfFields;
	
	protected double[] keyFrequency_f;
	//term statistics
	protected double[] TF_f;
	protected double[] Nt_f;
	
	//global statistics
	protected double[] averageFieldLength;
	protected double[] numberOfTokens_f;
	protected double[] numberOfUniqueTerms_f;
	
	protected double[] c_f;
	protected double[] w_f;
	
	protected boolean FIELD_RETRIEVAL = true;
	
	
	
	public abstract double score(double tf, double[] tf_f, double docLength, double[] fieldLengths);

	public FieldWeightingModel(int numberOfFields)
	{
		super();
		this.NumberOfFields = numberOfFields;
		this.keyFrequency_f = new double[numberOfFields];
		this.TF_f = new double[numberOfFields];
		this.Nt_f = new double[numberOfFields];
		this.averageFieldLength = new double[numberOfFields];
		this.numberOfTokens_f = new double[numberOfFields]; 
		this.numberOfUniqueTerms_f = new double[numberOfFields];
		this.c_f = new double[numberOfFields];
		this.w_f = new double[numberOfFields];
	}
	
	public void enableFieldRetrieval(){
		this.FIELD_RETRIEVAL = true;
	}
	
	public void disableFieldRetrieval(){
		this.FIELD_RETRIEVAL = false;
	}


	/**
	 * @param averageFieldLength the averageFieldLength to set
	 */
	public void setAverageFieldLength(double[] averageFieldLength) {
		this.averageFieldLength = averageFieldLength;
	}



	/**
	 * @param c_f the c_f to set
	 */
	public void setC_f(double[] c_f) {
		this.c_f = c_f;
	}



	/**
	 * @param keyFrequency_f the keyFrequency_f to set
	 */
	public void setKeyFrequency_f(double[] keyFrequency_f) {
		this.keyFrequency_f = keyFrequency_f;
	}



	/**
	 * @param nt_f the nt_f to set
	 */
	public void setNt_f(double[] nt_f) {
		Nt_f = nt_f;
	}





	/**
	 * @param numberOfTokens_f the numberOfTokens_f to set
	 */
	public void setNumberOfTokens_f(double[] numberOfTokens_f) {
		this.numberOfTokens_f = numberOfTokens_f;
	}



	/**
	 * @param numberOfUniqueTerms_f the numberOfUniqueTerms_f to set
	 */
	public void setNumberOfUniqueTerms_f(double[] numberOfUniqueTerms_f) {
		this.numberOfUniqueTerms_f = numberOfUniqueTerms_f;
	}



	/**
	 * @param tf_f the tF_f to set
	 */
	public void setTF_f(double[] tf_f) {
		TF_f = tf_f;
	}



	/**
	 * @param w_f the w_f to set
	 */
	public void setW_f(double[] w_f) {
		this.w_f = w_f;
	}
	
}
