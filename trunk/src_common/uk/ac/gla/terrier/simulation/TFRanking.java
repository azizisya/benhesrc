/*
 * Created on 2004-5-28
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.simulation;
import java.io.*;
import uk.ac.gla.terrier.sorting.*;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.TerrierTimer;

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
			lex.findTerm(this.getTermIdByRank(i));
			System.out.println((i+1)+": " + lex.getTerm());
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
		
		LexiconInputStream lexin = new LexiconInputStream(index.getPath(), index.getPrefix());
		int i=0;
		try{
			while (lexin.readNextEntry()!=-1){
				rankedTermId[i] = lexin.getTermId();
				TF[i] = lexin.getTF();
				i++;
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		/*for (int i = 0; i < TF.length; i++){
			this.rankedTermId[i] = i;
			this.lex.findTerm(i);
			TF[i] = lex.getTF();
		}*/
		lexin.close();
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
		String invertedFilename = ApplicationSetup.INVERTED_FILENAME;
		File invertedFile = new File(invertedFilename);
		String filePrefix = invertedFilename.substring(0, invertedFilename.lastIndexOf('.'));
		this.fRankLog = new File(filePrefix.concat(".rnk"));
	}
}

