/*
 * Created on 23 Aug 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.matching.dsms;

import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.IOException;

import org.terrier.matching.MatchingQueryTerms;
import org.terrier.matching.ResultSet;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import uk.ac.gla.terrier.statistics.ScoreNormaliser;

public class FeedPriorScoreModifier extends PostPriorScoreModifier {
	// Maps from blog post id to feed id
	protected TIntIntHashMap postFeedMap = new TIntIntHashMap();
	
	// protected TIntIntHashMap feedSizeMap = new TIntIntHashMap();
	
	protected TIntDoubleHashMap feedLMMap = new TIntDoubleHashMap();
	
	protected boolean scaleScores = Boolean.parseBoolean(
			ApplicationSetup.getProperty("VLDB.scale.scores", "false"));

	public FeedPriorScoreModifier() {
		super();
		this.loadPostFeedMap();
		this.loadFeedLM();
	}
	
	double max = 1;
	
	public Object clone() { return this; }
	
	public String getName() { return "FeedLMScoreModifer";}
	
	protected void loadFeedLM(){
		String filename = ApplicationSetup.getProperty("VLDB.feedw.filename", "must be given");
		System.out.print("Loading feed LM from "+filename+"...");
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			double feedMax = 1;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double score = Double.parseDouble(tokens[1]);
				if (Double.isNaN(score))
					score = 0;
				feedLMMap.put(Integer.parseInt(tokens[0]), score);
				feedMax = Math.max(Math.abs(score), feedMax);
			}
			br.close();
			max += feedMax;
			if (scaleScores)
				for (int key : feedLMMap.keys())
					feedLMMap.put(key, feedLMMap.get(key)/feedMax);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+feedLMMap.size()+" entries loaded.");
		if (this.scaleScores){
			for (int key : feedLMMap.keys()){
				feedLMMap.put(key, feedLMMap.get(key)/max);
			}
		}
	}
	
	protected void loadPostFeedMap(){
		String filename = "/home/ben/tr.ben/Backup/Resources/TREC/Blogs06/blog2doc/docid2feedid.txt.gz";
		System.out.print("Loading post to feed ids map from "+filename+"...");
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int docid = Integer.parseInt(tokens[0]);
				int feedid = Integer.parseInt(tokens[1]);
				postFeedMap.put(docid, feedid);
				// feedSizeMap.adjustOrPutValue(feedid, 1, 1);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. "+postFeedMap.size()+" entries loaded.");
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
		// final double beta = Double.parseDouble(ApplicationSetup.getProperty("VLDB.dsms.beta", "1d"));
		for (int i=0; i<docCount; i++){
			// create data structure for prob
			int feedid = postFeedMap.get(docids[i]);
			double feedLM = feedLMMap.get(feedid);
			double postLM = postLMMap.get(docids[i]);
			// scores[i] = alpha*scores[i]+feedLM/feedSizeMap.get(feedid)+max;
			scores[i] = alpha*scores[i]+feedLM+postLM+max;
			if (feedLM != 0 && postLM!=0)
				counter++;
		}
		System.out.println(counter+" document scores are modified.");
		return true;
	}

}
