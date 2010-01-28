/*
 * Created on 12 Jul 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.matching.models;

public class PL2F extends FieldWeightingModel {	
	
	public PL2F(int numberOfFields){
		super(numberOfFields);
	}
	
	/**
	 * This method gets the normalised term frequency.
	 * @param tf The frequency of the query term in the document.
	 * @param docLength The number of tokens in the document.
	 * @param termFrequency The frequency of the query term in the collection.
	 * @return The normalised term frequency.
	 */
	protected double normalise(
			double parameter, 
			double tf, 
			double length,
			double avl,
			double termFrequency,
			double weight){
		if (length == 0)
			return tf;
		double tfn = tf * i.log(1.0d + (parameter * avl) / length);
		return weight * tfn;
	}

	@Override
	public double score(double tf, double[] tf_f, double docLength,
			double[] fieldLengths) {
		double TF = 0;
		for (int k=0; k<this.NumberOfFields; k++){
			TF += normalise(c_f[k], 
					tf_f[k], 
					fieldLengths[k], 
					averageFieldLength[k],
					TF_f[k],
					w_f[k]);
		}
		double NORM = 1.0D / (TF + 1d);
		double f = (1.0D * termFrequency) / (1.0D * numberOfDocuments);
		return NORM
			* keyFrequency
			* (TF * i.log(1.0D / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * i.log(2 * Math.PI * TF)
				+ TF * (i.log(TF) - Idf.REC_LOG_2_OF_E));
	}

	@Override
	public String getInfo() {
		String info = "PL2F";
		for (int k=0; k<NumberOfFields; k++)
			info += "_c"+(k+1)+"_"+c_f[k]+"+w"+(k+1)+"_"+w_f[k];
		return info;
	}

	@Override
	public double score(double tf, double docLength) {
		double TF =
			tf * i.log(1.0d + (c * averageDocumentLength) / docLength);
		double NORM = 1.0D / (TF + 1d);
		double f = (1.0D * termFrequency) / (1.0D * numberOfDocuments);
		return NORM
			* keyFrequency
			* (TF * i.log(1.0D / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * i.log(2 * Math.PI * TF)
				+ TF * (i.log(TF) - Idf.REC_LOG_2_OF_E));
	}

	@Override
	public double score(double tf, double docLength, double n_t, double F_t,
			double keyFrequency) {
		double TF =
			tf * i.log(1.0d + (c * averageDocumentLength) / docLength);
		double NORM = 1.0D / (TF + 1d);
		double f = F_t / numberOfDocuments;
		return NORM
			* keyFrequency
			* (TF * i.log(1d / f)
				+ f * Idf.REC_LOG_2_OF_E
				+ 0.5d * i.log(2 * Math.PI * TF)
				+ TF * (i.log(TF) - Idf.REC_LOG_2_OF_E));
	}

}
