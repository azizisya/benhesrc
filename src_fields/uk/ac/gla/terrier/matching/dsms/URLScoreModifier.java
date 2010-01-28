package uk.ac.gla.terrier.matching.dsms;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.HeapSort;
import uk.ac.gla.terrier.utility.PartialStringMatching;

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
 * @version $Revision: 1.3 $
 */
public class URLScoreModifier implements DocumentScoreModifier {

	private int functionId;
	
	private URLServer urlServer = null;
	
	private static float[] url_lengths = null;
	private int modifiedLength;
	private boolean addModified;
	private boolean onlyRerank;
	private double alpha=1.0d;
	private double beta = 10000.0d;
	private double gamma = 0.4d;
	private double omega = 0.5d;
	private double kappa = 1.0d;

	private double usm4_omega=10.0d;
	private double usm4_kappa=10.0d;
	
	private double usm5_omega=10.0d;
	private double usm5_kappa=10.0d;
	
	private double usm6_omega=10.0d;
	private double usm6_kappa=10.0d;

	private double usm7_omega=10.0;
	private double usm7_kappa=10.0;
	
	private double usm8_omega=10.0;
	private double usm8_kappa=10.0;
	
	static {
		
		String inputFile = ApplicationSetup.makeAbsolute(ApplicationSetup.getProperty("usm.input.file",""),ApplicationSetup.TERRIER_INDEX_PATH);
		try {
			System.out.println("input file: " + inputFile);
			java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new GZIPInputStream(new FileInputStream(inputFile)));
			url_lengths = (float[]) ois.readObject();
			ois.close();
			System.out.println("array has length: " + url_lengths.length);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void initialise() {
		functionId = Integer.parseInt(ApplicationSetup.getProperty("usm.id","1"));
		modifiedLength = Integer.parseInt(ApplicationSetup.getProperty("usm.modified.length","0"));
		addModified = (new Boolean(ApplicationSetup.getProperty("usm.add.modified","true"))).booleanValue();
		onlyRerank = (new Boolean(ApplicationSetup.getProperty("usm.only.rerank","true"))).booleanValue();
		alpha = Double.parseDouble(ApplicationSetup.getProperty("usm.2.alpha","1.0d"));
		beta = Double.parseDouble(ApplicationSetup.getProperty("usm.2.beta","10000.0d"));
		gamma = Double.parseDouble(ApplicationSetup.getProperty("usm.2.gamma","0.4d"));
		
		omega = Double.parseDouble(ApplicationSetup.getProperty("usm.3.omega", "0.5d"));
		kappa = Double.parseDouble(ApplicationSetup.getProperty("usm.3.kappa", "1.0d"));
		
		usm4_omega = Double.parseDouble(ApplicationSetup.getProperty("usm.4.omega", "10.0d"));
		usm4_kappa = Double.parseDouble(ApplicationSetup.getProperty("usm.4.kappa", "10.0d"));
		
		usm5_omega = Double.parseDouble(ApplicationSetup.getProperty("usm.5.omega", "10.0d"));
		usm5_kappa = Double.parseDouble(ApplicationSetup.getProperty("usm.5.kappa", "10.0d"));

		usm6_omega = Double.parseDouble(ApplicationSetup.getProperty("usm.6.omega", "10.0d"));
		usm6_kappa = Double.parseDouble(ApplicationSetup.getProperty("usm.6.kappa", "10.0d"));

		usm7_omega = Double.parseDouble(ApplicationSetup.getProperty("usm.7.omega", "10.0d"));
		usm7_kappa = Double.parseDouble(ApplicationSetup.getProperty("usm.7.kappa", "10.0d"));

		usm8_omega = Double.parseDouble(ApplicationSetup.getProperty("usm.8.omega", "10.0d"));
		usm8_kappa = Double.parseDouble(ApplicationSetup.getProperty("usm.8.kappa", "10.0d"));
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#modifyScores(uk.ac.gla.terrier.structures.Index, uk.ac.gla.terrier.matching.MatchingQueryTerms, uk.ac.gla.terrier.matching.ResultSet)
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet set) {
		
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
		System.out.println("add modified: " + addModified);
		System.out.println("only rerank: " + onlyRerank);		
		
		double addModifiedValue = 0.0d;
		if (addModified) {
			addModifiedValue = 1.0d;
		} else {
			addModifiedValue = 0.0d;
		}

		switch(functionId) {
			case 1 : // original
				for (int i = start; i < end; i++) {
                    scores[i] = scores[i] * (addModifiedValue + 
											(Math.log(2.0D) / Math.log(1.0D+url_lengths[docids[i]])));
				}
				break;
			case 2 : //logistic
				for (int i = start; i < end; i++) {
					scores[i] = scores[i] + (1.0d + beta*(alpha+1)*Math.exp(-gamma*url_lengths[docids[i]])/
											 1.0d + beta*Math.exp(-gamma*url_lengths[docids[i]]));
				}
				break;
			case 3: // k / (k+pathlength)
				System.out.println("omega: " + omega + ", kappa: " + kappa);
				for (int i = start; i < end; i++) {
					double s = scores[i];
					scores[i] = scores[i] + 
								omega * kappa / (kappa + url_lengths[docids[i]]);
				}
				break;
			case 4: // k / (k + log(pathlength))
				System.out.println("omega: " + usm4_omega + ", kappa: " + usm4_kappa);
				for (int i = start; i < end; i++) {
					double s = scores[i];
					scores[i] = scores[i] + 
						usm4_omega * usm4_kappa / (usm4_kappa + Math.log(url_lengths[docids[i]]));
				}
				break;
			
			case 5: //  + omega * exp(-kappa * path length)
				System.out.println("omega: " + usm5_omega + ", kappa: " + usm5_kappa);
				for (int i = start; i < end; i++) {
					double s = scores[i];
					scores[i] = scores[i] + 
						usm5_omega * Math.exp(-usm5_kappa*url_lengths[docids[i]]);
				}
				break;
			case 6: //  * omega * exp(-kappa * path length)
				System.out.println("omega: " + usm6_omega + ", kappa: " + usm6_kappa);
				for (int i = start; i < end; i++) {
					double s = scores[i];
					scores[i] = scores[i] *  
						(addModifiedValue + usm6_omega * Math.exp(-usm6_kappa*url_lengths[docids[i]]));
				}
				break;
			case 7: // like 3 but with the weighting of the url text
				System.out.println("omega: " + usm7_omega + ", kappa: " + usm7_kappa);
				double urlTextWeight = 0.0d;
				try {
				URLServer urlServer = new URLServer();
				for (int i = start; i < end; i++) {
					urlTextWeight = PartialStringMatching.partialMatch(queryTerms.getTerms(), urlServer.getURL(docids[i]));
					scores[i] = scores[i] + (usm7_omega + urlTextWeight) * usm7_kappa / (usm7_kappa + url_lengths[docids[i]]);
				}
				urlServer.close();
				} catch(IOException ioe ) {
				}
				break;
			case 8: // like 3 but with the weighting of the url text
				System.out.println("omega: " + usm8_omega + ", kappa: " + usm8_kappa);
				urlTextWeight = 0.0d;
				try {
				URLServer urlServer = new URLServer();
				for (int i = start; i < end; i++) {
					urlTextWeight = PartialStringMatching.partialMatch(queryTerms.getTerms(), urlServer.getURL(docids[i]), index.getLexicon());
					scores[i] = scores[i] + (usm8_omega + urlTextWeight) * usm8_kappa / (usm8_kappa + url_lengths[docids[i]]);
				}
				urlServer.close();
				} catch(IOException ioe ) {
				}
				break;

		}


		if (onlyRerank) {
			System.out.println("only reranking.");

			for (int i=start; i<end; i++)  
				scores[i] = scores[i] + scores[end];					
		}	
		//set.setResultSize(minimum);
		HeapSort.descendingHeapSort(scores, docids, set.getOccurrences(), set.getResultSize());
	

		
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#getName()
	 */
	public String getName() {
		return "URLScoreModifier";
	}

	public Object clone() { try{return super.clone();}catch (Exception e ){return null;} }

}

