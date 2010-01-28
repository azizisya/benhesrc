package uk.ac.gla.terrier.models.smoothing;

public abstract class SmoothingMethod {
	protected long numberOfTokens;
	
	protected int freqInColl;
	
	protected double hyperPara;
	
	public SmoothingMethod(long numberOfTokens){
		this.numberOfTokens = numberOfTokens;
	}
	
	public SmoothingMethod(long numberOfTokens, double paraValue){
		this.numberOfTokens = numberOfTokens;
		this.hyperPara = paraValue;
	}
	
	public abstract double smooth(int tf, int docLength);
	
	public double smooth(int tf, int docLength, int freqInColl){
		this.setFreqInColl(freqInColl);
		return smooth(tf, docLength);
	}

	public int getFreqInColl() {
		return freqInColl;
	}

	public void setFreqInColl(int freqInColl) {
		this.freqInColl = freqInColl;
	}

	public double getHyperPara() {
		return hyperPara;
	}

	public void setHyperPara(double hyperPara) {
		this.hyperPara = hyperPara;
	}
}
