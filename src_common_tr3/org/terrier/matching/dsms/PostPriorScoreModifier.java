/*
 * Created on 23 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.matching.dsms;

import gnu.trove.TIntDoubleHashMap;

import java.io.BufferedReader;
import java.io.IOException;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import uk.ac.gla.terrier.statistics.ScoreNormaliser;

public class PostPriorScoreModifier implements DocumentScoreModifier {
	
	protected TIntDoubleHashMap postLMMap = new TIntDoubleHashMap();
	
	protected boolean scaleScores = Boolean.parseBoolean(
			ApplicationSetup.getProperty("VLDB.scale.scores", "false"));

	double max = 1;
	
	public PostPriorScoreModifier() {
		this.loadPostLM();
	}
	
	public Object clone() { return this; }
	
	public String getName() { return "PostLMScoreModifer";}
	
	protected void loadPostLM(){
		String filename = ApplicationSetup.getProperty("VLDB.postw.filename", "must be given");
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double score = Double.parseDouble(tokens[1]);
				if (Double.isNaN(score))
					score = 0;
				postLMMap.put(Integer.parseInt(tokens[0]), score);
				max = Math.max(Math.abs(score), max);
			}
			br.close();
			if (scaleScores)
				for (int key : postLMMap.keys())
					postLMMap.put(key, postLMMap.get(key)/max);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		int counter = 0;
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		if (this.scaleScores)
			ScoreNormaliser.normalizeScoresByMax(scores);
		final int docCount = docids.length;
		final double alpha = Double.parseDouble(ApplicationSetup.getProperty("VLDB.dsms.alpha", "1d"));
		for (int i=0; i<docCount; i++){
			// create data structure for prob
			double feedLM = postLMMap.get(docids[i]);
			scores[i] = alpha*scores[i]+feedLM+max;
			if (feedLM != 0)
				counter++;
		}
		System.out.println(counter+" document scores are modified.");
		return true;
	}

}
