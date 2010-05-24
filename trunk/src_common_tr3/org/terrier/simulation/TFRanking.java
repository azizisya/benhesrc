/*
 * Created on 2004-5-28
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.simulation;
import java.io.*;

import org.terrier.structures.CollectionStatistics;
import org.terrier.structures.Index;
import org.terrier.structures.Lexicon;
import org.terrier.structures.LexiconEntry;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.TerrierTimer;

import org.terrier.sorting.*;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TFRanking {
	protected int[] rankedTermId;
	protected int[] rank;
	protected Index index;
	protected Lexicon lex;
	protected CollectionStatistics collSta;
	
	protected File fRankLog;
	//protected double u = 0.01d;
	//protected double s = 0.00007d;
	protected double u = Double.parseDouble(ApplicationSetup.getProperty("skewed.u", "0.01d"));
	protected double s = Double.parseDouble(ApplicationSetup.getProperty("skewed.s", "0.00007d"));
	
	protected long minValidRank;
	protected long maxValidRank;
	
	public TFRanking (String indexPath, String indexPrefix){
		this(Index.createIndex(indexPath, indexPrefix));
	}
	
	public TFRanking (Index index){
		this.index = index;
		collSta = index.getCollectionStatistics();
		this.lex = index.getLexicon();
		this.rank = new int[(int)collSta.getNumberOfUniqueTerms()];
		this.rankedTermId = new int[(int)collSta.getNumberOfUniqueTerms()];
		this.getLogFile();
		if (!this.fRankLog.exists())
			this.createRankLog();
		this.loadRankedTermId();	
		this.minValidRank = (long)(s * rank.length);
		this.maxValidRank = (long)(u * rank.length);
		System.out.println("valid range: " + minValidRank + " -- " + maxValidRank);
	}
	/**
	 * @deprecated
	 * @param lexicon
	 */
	public TFRanking(Lexicon lexicon){
		collSta = index.getCollectionStatistics();
		this.lex = lexicon;
		this.rank = new int[(int)collSta.getNumberOfUniqueTerms()];
		this.rankedTermId = new int[(int)collSta.getNumberOfUniqueTerms()];
		this.getLogFile();
		if (!this.fRankLog.exists())
			this.createRankLog();
		this.loadRankedTermId();	
		this.minValidRank = (long)(s * rank.length);
		this.maxValidRank = (long)(u * rank.length);
		System.out.println("valid range: " + minValidRank + " -- " + maxValidRank);
	}
	
//	public TFRanking(){
//		this.lex = Index.createIndex().getLexicon();
//		this.rank = new int[(int)CollectionStatistics.getNumberOfUniqueTerms()];
//		this.rankedTermId = new int[(int)CollectionStatistics.getNumberOfUniqueTerms()];
//		this.getLogFile();
//		if (!this.fRankLog.exists())
//			this.createRankLog();
//		this.loadRankedTermId();
//		this.minValidRank = (int)(s * rank.length);
//		this.maxValidRank = (int)(u * rank.length);
//		System.out.println("valid range: " + minValidRank + " -- " + maxValidRank);
//	}
	
	public int getTermIdByRank(int rank){
		return this.rankedTermId[rank];
	}
	
	public long getNumberOfValidTerms(){
		return this.maxValidRank - this.minValidRank + 1;
	}
	
	public int getRankByTermId(int termId){
		return this.rank[termId];
	}
	
	private void createRankLog(){
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		System.out.println("Initialising...");
		this.getLogFile();
		this.sortTermsByTF();
		this.writeRankedTermId();
		timer.setBreakPoint();
		System.out.println("Finished. Time elapsed: " + 
				timer.toStringMinutesSeconds());
	}
	
	private void loadRankedTermId(){		
		int length = rankedTermId.length;
		try{
			DataInputStream in = new DataInputStream(
				new BufferedInputStream(new FileInputStream(this.fRankLog)));
			for (int i = 0; i < length; i++){
				rankedTermId[i] = in.readInt();
				rank[rankedTermId[i]] = i;
			}
			in.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void dumpTFLog(int topX){
		for (int i = 0; i < topX; i++){
			System.out.println((i+1)+": " + (String)lex.getLexiconEntry(this.getTermIdByRank(i)).getKey());
		}
	}
	
	public boolean isValidTerm(int termId){
		//int T = this.rank.length;
		int t = this.rank[termId];
		//if (t >= s*T && t <= (u-s)*T)
		if (t >= this.minValidRank && t <= this.maxValidRank)
			return true;
		else
			return false;
	}
	
	private void sortTermsByTF(){
		System.out.print("Loading the lexicon...");
		int[] TF = new int[(int)collSta.getNumberOfUniqueTerms()];
		int n = collSta.getNumberOfUniqueTerms();
		for (int i=0; i<n; i++){
			LexiconEntry le = (LexiconEntry)lex.getLexiconEntry(i).getValue();
			rankedTermId[i] = i;
			TF[i] = le.getFrequency();
		}
		
		/*for (int i = 0; i < TF.length; i++){
			this.rankedTermId[i] = i;
			this.lex.findTerm(i);
			TF[i] = lex.getTF();
		}*/
		SortDescendingPairedVectors.sort(TF, this.rankedTermId);
	}

	private void writeRankedTermId(){
		try{
			DataOutputStream output = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(fRankLog)));
			for (int i = 0; i < this.rankedTermId.length; i++){
				output.writeInt(rankedTermId[i]);
			}
			output.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Get a random term id without any restriction.
	 * @return
	 */
	public int getValidRandomTermId(){
		int validRank = 
			(int)((double)minValidRank-1+Math.random()*(maxValidRank-minValidRank+2));
		return this.getTermIdByRank(validRank);
//		int id = this.getTermIdByRank(0);
//		while (!isValidTerm(id)){
//			id = (int)(Math.random() * CollectionStatistics.getNumberOfUniqueTerms());
			//lex.findTerm(id);
//			System.out.println("id: " + id + ", term: " + lex.getTerm() + 
//					", ranking: " + this.getRankByTermId(id));
		//}
		//return id;
	}
	
	private void getLogFile(){
		this.fRankLog = new File(ApplicationSetup.TERRIER_INDEX_PATH.concat(
				ApplicationSetup.FILE_SEPARATOR).concat(
						ApplicationSetup.TERRIER_INDEX_PREFIX).concat(
								".rnk"));
	}
}

