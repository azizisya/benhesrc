package uk.ac.gla.terrier.matching.dsms;

import java.io.FileInputStream;
import java.util.zip.GZIPInputStream;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;

/**
 * Modifies the scores by multiplying with a statically computed score.
 * The static scores are read from a file whose name is specified by
 * property ssa.input.file in the default properties file. The number of 
 * top-ranked documents for which the score is modified is set with the 
 * property ssa.modified.length. The default value 0 modifies the scores of 
 * all the documents in the result set.
 * <br>
 * We use two more properties in order to specify whether the new scores will
 * be added to the old ones. This results in reranking only the top 
 * ssa.modified.length documents. The relevant property is ssa.add.modified.
 * The second property specifies whether we want to zero the scores of the
 * documents that do not belong to the top-ranked ones. The corresponding 
 * property is ssa.zero.rest 
 * 
 * @author Vassilis Plachouras
 * @version $Revision: 1.1 $
 */
public class StaticScoreModifier implements DocumentScoreModifier {
	/** Specifying whether to add the original score to the modified one, or not.*/
	private static boolean addModified;
	
	/** Specifying whether to rerank only the top N documents.*/
	private static boolean onlyRerank;

	private static boolean log; private double lambda;
	
	/** The number of top-ranked documents for which the scores will be modified. */
	private static int modifiedLength;

	/** The array that contains the statically computed scores.*/
	private static double[] staticScores;

	/**
	 * Loads into memory an input file which contains a
	 * serialised double array with the static scores for
	 * each document.
	 */
	static {
		String inputFile = ApplicationSetup.getProperty("ssa.input.file","");
		try {
			java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
			Object o = ois.readObject();
			try{
                staticScores = (double[]) o;
            } catch (ClassCastException cce) {
                float[] tmp = (float[]) o;
                staticScores = new double[tmp.length];
                int i=0;
                for(float f: tmp)
                    staticScores[i++] = (double)f;
            }
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		modifiedLength = Integer.parseInt(ApplicationSetup.getProperty("ssa.modified.length","0"));
		addModified = Boolean.parseBoolean(ApplicationSetup.getProperty("ssa.add.modified","true"));
		log = Boolean.parseBoolean(ApplicationSetup.getProperty("ssa.log", "false"));
		onlyRerank = Boolean.parseBoolean(ApplicationSetup.getProperty("ssa.only.rerank","true"));
			
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#modifyScores(uk.ac.gla.terrier.structures.Index, uk.ac.gla.terrier.matching.MatchingQueryTerms, uk.ac.gla.terrier.matching.ResultSet)
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms,
			ResultSet set) {
		
		int minimum = modifiedLength;
		//onlyRerank = Boolean.getBoolean(ApplicationSetup.getProperty("ssa.only.rerank","false"));
		//onlyRerank = true;
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length

		lambda = Double.parseDouble(ApplicationSetup.getProperty("ssa.lambda","1.0d"));
		
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
		System.out.println("add modified: " + addModified);
		System.out.println("only rerank: " + onlyRerank);
		
		if (System.getProperty("debug","false").equals("true")) 
			for (int i=start; i < end; i++) {
				System.out.println("scores["+i+"] = " + scores[i]);
			}
		final Idf idf = new Idf();
		if (log)
		{
			final double increment = Double.parseDouble(ApplicationSetup.getProperty("ssa.increment", "0.0d"));
			System.err.println("scores.length="+scores.length+ " docids.length="+ docids.length+ " staticScores.length="+ staticScores.length + " lambda="+lambda);
			//if (lambda != 0.0d)
			for (int i = start; i < end; i++) {
				scores[i] = scores[i] + lambda * idf.log(increment+staticScores[docids[i]]);
			}
		} 
		else if (addModified) {
			for (int i = start; i < end; i++) {
				scores[i] = scores[i] * (1.0 + staticScores[docids[i]]);
			}
		} else {
			for (int i = start; i < end; i++) {
				scores[i] = scores[i] * staticScores[docids[i]];
			}
		}
		
		if (System.getProperty("debug","false").equals("true")) 
			for (int i=start; i < end; i++) {
				System.out.println("scores["+i+"] = " + scores[i]);
			}
		
		if (onlyRerank) {
			System.out.println("only reranking.");
			//for (int i=end; i>=0; i--)
			//	scores[i] = 0;
			//sets the effective size of the result set.
			//set.setResultSize(minimum);
			for (int i=start; i<end; i++)  
				scores[i] = scores[i] + scores[end-1];
		}	
		//set.setResultSize(minimum);
		HeapSort.descendingHeapSort(scores, docids, set.getOccurrences(), set.getResultSize());
		if (System.getProperty("debug","false").equals("true")) 
			for (int i=start; i < end; i++) {
				System.out.println("scores["+(i)+"] = " + scores[i]);
			}

		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#getName()
	 */
	public String getName() {
		return "StaticScoreModifier";
	}

	public Object clone()	{
		return this;
	}

}

