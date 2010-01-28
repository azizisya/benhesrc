package uk.ac.gla.terrier.matching.dsms;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** 
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class LinkScoreModifier implements DocumentScoreModifier {

	private int functionId;
		
	private static double[] linkScores = null;
	private int modifiedLength;

	private double omega = 0.5d;
	private double kappa = 1.0d;
	
	static {
		final long startTime = System.currentTimeMillis();
		String inputFile = ApplicationSetup.makeAbsolute(ApplicationSetup.getProperty("lsm.input.file",""),ApplicationSetup.TERRIER_INDEX_PATH);
		boolean normalise = new Boolean(ApplicationSetup.getProperty("lsm.normalise","true")).booleanValue();
		try {
			System.out.println("input file: " + inputFile);
			java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
			linkScores = (double[]) ois.readObject();
			ois.close();
			System.out.println("array has length: " + linkScores.length);
			if (normalise)
			{
				System.out.println("normalising the values so that the mean is 1");
				final int length = linkScores.length;
				double average = 0.0d;
				for (int i=0; i<length; i++) {
					average += linkScores[i];
				}
				average = average / (double)length;
				for (int i=0; i<length; i++) {
					linkScores[i] = linkScores[i] / average;
				}
			}
			System.out.println("LinkScoreModifier initialisation time: "+ (System.currentTimeMillis() - startTime));
		} catch (Exception e) {
			System.err.println("Exception "+e);
			e.printStackTrace();
		}
	}
	
	private void initialise() {
		functionId = Integer.parseInt(System.getProperty("lsm.id","1"));
		modifiedLength = Integer.parseInt(ApplicationSetup.getProperty("lsm.modified.length","0"));
		
		omega = Double.parseDouble(ApplicationSetup.getProperty("lsm.omega", "0.5d"));
		kappa = Double.parseDouble(ApplicationSetup.getProperty("lsm.kappa", "1.0d"));
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#modifyScores(uk.ac.gla.terrier.structures.Index, uk.ac.gla.terrier.matching.MatchingQueryTerms, uk.ac.gla.terrier.matching.ResultSet)
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet set) {
		final long startTime = System.currentTimeMillis();	
		initialise();
		
		//urlServer = new URLServer();
		
		int minimum = modifiedLength;
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length
		System.out.println("result set size: " + set.getResultSize());
		System.out.println("minumum: " + minimum);
		if (minimum > set.getResultSize() || minimum == 0)
			minimum = set.getResultSize();
		System.out.println("minumum: " + minimum);

		int[] docids = set.getDocids();
		double[] scores = set.getScores();
		
		int start = 0;
		int end = minimum;
		System.out.println("start: " + start);
		System.out.println("end : " + end);
		Idf idf = new Idf();
				
		switch(functionId) {
			case 1 : // original
				for (int i = start; i < end; i++) {
					//use this formula for URL scoring, where higher score is less desirable
                    scores[i] = scores[i] + omega * kappa / (kappa + linkScores[docids[i]]);
				}
				break;
			case 2 : //logistic
				for (int i = start; i < end; i++) {
					//for link analysis, where bigger score is better
					scores[i] = scores[i] + omega * linkScores[docids[i]] / (kappa + linkScores[docids[i]]);
				}
				break;
			case 3: // -log - smaller score better
				for(int i=start;i<end;i++) {
					scores[i] = scores[i] - idf.log(linkScores[docids[i]]);
				}
			case 4: // + log - bigger score better
				for(int i=start;i<end;i++) {
					scores[i] = scores[i] + idf.log(linkScores[docids[i]]);
				}
		}
		//don't sort - when we return true, matching will do this for us
		//HeapSort.descendingHeapSort(scores, docids, set.getOccurrences(), set.getResultSize());
		System.err.println("Time to apply linkscoremodifier: "+( System.currentTimeMillis() - startTime ));
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#getName()
	 */
	public String getName() {
		return "LinkScoreModifier";
	}

	public Object clone() {
		return new LinkScoreModifier();
	}

}

