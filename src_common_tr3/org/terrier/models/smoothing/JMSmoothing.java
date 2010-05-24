package org.terrier.models.smoothing;

public class JMSmoothing extends SmoothingMethod {

	public JMSmoothing(long numberOfTokens) {
		super(numberOfTokens);
		// TODO Auto-generated constructor stub
	}

	public JMSmoothing(long numberOfTokens, double paraValue) {
		super(numberOfTokens, paraValue);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double smooth(int tf, int docLength) {
		return hyperPara*tf/docLength+(1-hyperPara)*freqInColl/numberOfTokens;
	}

}
