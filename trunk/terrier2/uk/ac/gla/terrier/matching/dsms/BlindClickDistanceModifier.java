package uk.ac.gla.terrier.matching.dsms;

import uk.ac.gla.terrier.links.LinkIndex;
import uk.ac.gla.terrier.links.LinkServer;
import uk.ac.gla.terrier.links.BreadthFirstShortestPath;

import java.util.Arrays;
import java.util.HashMap;

import uk.ac.gla.terrier.matching.*;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.*;
import java.io.BufferedReader;

/** Takes an input file, specified by property <tt>ClickDistanceModifier.inputfile</tt>, which has the format:
<pre>
queryid docno docno docno
</pre>
  * @author Ben He, Craig Macdonald
  */
public class BlindClickDistanceModifier implements DocumentScoreModifier
{
	protected double w;
	protected double k;
	protected double a;
	
	protected LinkIndex linkServer;
	protected Idf idf = new Idf();
	protected int maxDistance = Integer.parseInt(ApplicationSetup.getProperty("ClickDistanceModifier.maxdist", "10"));

	public Object clone() { return this; }
	public String getName() { return "ClickDistanceModifier";}

	public BlindClickDistanceModifier()
	{
		try{	
			linkServer = new LinkServer();
		} catch (Exception e) {
			System.err.println("Couldn't create linkindex : "+ e);
		}
	}
	
	private void normaliseScores(double[] scores){
		int length = scores.length;
		double[] scoresTmp = scores.clone();
		Arrays.sort(scoresTmp);
		double minScore = scoresTmp[0];
		double maxScore = scoresTmp[length-1];
		System.err.println("maxscore: "+maxScore+", minscore: "+minScore);
		for (int i=0; i<length; i++){
			scores[i] /= (maxScore+minScore+0.0005d);
		}
	}
	
	protected double inverseSigmoid(int clickDist){
		return w*(Math.pow(clickDist+0.5, a)+Math.pow(k, a))/Math.pow(clickDist+0.5, a);
	}

	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		w = Double.parseDouble(ApplicationSetup.getProperty("click.distance.w", "1.8d"));
		k = Double.parseDouble(ApplicationSetup.getProperty("click.distance.k", "1d"));
		a = Double.parseDouble(ApplicationSetup.getProperty("click.distance.a", "0.6d"));
		int numberOfFeedbackDocuments = Integer.parseInt(
				ApplicationSetup.getProperty("click.distance.documents", "5"));
		System.err.println("k="+k+", a="+a+", w="+w);
		final int[] docids = resultSet.getDocids();
		if (docids.length<numberOfFeedbackDocuments){
			System.err.println("Too few returned documents.");
			return false;
		}
		double[] scores = resultSet.getScores();
		//this.normaliseScores(scores);
		final int docCount = docids.length;
		final String queryId = queryTerms.getQueryId();
		final int[] clickDocs = new int[numberOfFeedbackDocuments];
		for (int i=0; i<numberOfFeedbackDocuments; i++)
			clickDocs[i] = docids[i];
		int[] minClickDists = new int[docCount];
		Arrays.fill(minClickDists, maxDistance+1);
		if (clickDocs == null)
		{
			System.err.println("No clicked document found for query id "+queryId+" - resultset is unchanged");
			return false;
		}
		final int clickDocCount = clickDocs.length;
		
		try{
			for(int j=0;j<clickDocCount;j++)
			{
				final int[] clickDists = BreadthFirstShortestPath.ShortestPath(linkServer, clickDocs[j], docids, maxDistance);
				//a clickDists of -1 means not found within maxDistance clicks
				for(int i=0;i<docCount;i++)
				{
					//final double originalScore = scores[i];
					if (clickDists[i] != -1 ){
						//System.err.println("Docid "+ docids[i] + " should be higher ranked");
						//System.err.println("Docid "+ docids[i] + ", score: "+scores[i]+", clickDistance: "+clickDists[i]);
//						scores[i] = scores[i] - clickDists[i]  //TODO; 
						//scores[i] = idf.log(1d/scores[i])/clickDists[i];
						minClickDists[i] = Math.min(clickDists[i], minClickDists[i]);
					}
					//else if (scores[i]!=0){
						//scores[i] = idf.log(1d/scores[i])/maxDistance;
						//System.err.println("Docid "+ docids[i] + ", score: "+scores[i]+", clickDistance: "+clickDists[i]);
					//}
				}
			}
			for (int i=0;i<docCount;i++){
				final double originalScore = scores[i];
				if (scores[i]>0){
					minClickDists[i] = (minClickDists[i]==0)?(minClickDists[i]+1):(minClickDists[i]);
					scores[i] += inverseSigmoid(minClickDists[i]);
				}
				if (Double.isInfinite(scores[i]))
					System.err.println("scores["+i+"]="+originalScore+
							", mindist: "+minClickDists[i]);
			}
		}
		catch (Exception e) {
			System.err.println("Problem updating resultset using BreadthFirstShortestPath.ShortestPath : "+ e);
			e.printStackTrace();
		}
		return true;
	}
}
