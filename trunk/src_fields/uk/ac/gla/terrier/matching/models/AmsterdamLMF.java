/*
 * Created on 11 Jun 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.models;

public class AmsterdamLMF extends FieldWeightingModel {

	public AmsterdamLMF(int NumberOfFields, String... bla)
	{
		super(NumberOfFields);
	}
	
	@Override
	public double score(double tf, double[] tf_f, double docLength,
			double[] fieldLengths) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double score(double tf, double docLength) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double score(double tf, double docLength, double n_t, double F_t,
			double keyFrequency) {
		// TODO Auto-generated method stub
		return 0;
	}

}
