/*
 * Smooth - Smoothing term frequency normalisation
 * Webpage: http://ir.dcs.gla.ac.uk/smooth
 * Contact: ben{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is TFRanking.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Ben He <ben{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.smooth.simulation;
import java.io.*;

import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.sorting.*;
import uk.ac.gla.terrier.utility.*;

/**
 * An index of terms that are ranked by their frequencies in the collection.
 * @author Ben He <ben{a.}dcs.gla.ac.uk>
 * @version $Revision: 1.2 $
 */
public class TFRanking {
	/** The term ids ranked by the frequencies in the collection. */
	protected int[] rankedTermId;
	/** The rank of each term according to the frequency in the collection. */
	protected int[] rank;
	protected CollectionStatistics collSta;
	/** The lexicon. */
	protected Lexicon lex;
	/** The file that stores the index of terms ranked by frequency in the collection. */
	protected File fRankLog;
	/** The parameter u of the skewed model. */
	protected double u = 0.01d;
	/** The parameter s of the skewed model. */
	protected double s = 0.00007d;
//	protected double u = 0.0008d;
//	protected double s = 0.00007d;
	/** The minimal of the a valid rank. A valid term 
	 * must have a rank that is within the range from minValidRank
	 * to maxValidRank.
	 */
	protected long minValidRank;
	/** The maximal of the a valid rank. A valid term 
	 * must have a rank that is within the range from minValidRank
	 * to maxValidRank.
	 */
	protected long maxValidRank;
	/**
	 * The default constructor.
	 * @param lexicon The lexicon.
	 */
	public TFRanking(Lexicon lexicon, CollectionStatistics _collSta){
		collSta = _collSta;
		this.lex = lexicon;
		this.rank = new int[(int)collSta.getNumberOfUniqueTerms()];
		this.rankedTermId = new int[(int)collSta.getNumberOfUniqueTerms()];
		this.getLogFile();
		if (!this.fRankLog.exists()){
			System.err.println("In the first run, it is necessary to create an index of " +
					"terms ranked by their term frequencies in the whole collection.");
			System.err.println("Creating the term ranking index...");
			this.createRankLog();
		}
		this.loadRankedTermId();	
		this.minValidRank = (long)(s * rank.length);
		this.maxValidRank = (long)(u * rank.length);
	}

	/**
	 * Get the term id that corresponds to the given rank.
	 * @param rank The rank of the term.
	 * @return The term id.
	 */
	public int getTermIdByRank(int rank){
		return this.rankedTermId[rank];
	}
	/**
	 * Get the total number of valid terms. 
	 * @return The number of valide terms.
	 */
	public long getNumberOfValidTerms(){
		return this.maxValidRank - this.minValidRank + 1;
	}
	/**
	 * Get the rank of a term by the given term id.
	 * @param termId The given term id.
	 * @return The rank of the term.
	 */
	public int getRankByTermId(int termId){
		return this.rank[termId];
	}
	/**
	 * Create the index of terms ranked by their frequencies in
	 * the collection.
	 *
	 */
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
	/**
	 * Load the index of terms.
	 *
	 */
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
	/**
	 * Dump the index.
	 * @param topX The number of top ranked terms to be displayed
	 * on screen.
	 */
	public void dumpTFLog(int topX){
		for (int i = 0; i < topX; i++){
			lex.findTerm(this.getTermIdByRank(i));
			System.out.println((i+1)+": " + lex.getTerm());
		}
	}
	/**
	 * See if a term is within the valid range.
	 * @param termId The id of the given term.
	 * @return True for valid and false for invalid.
	 */
	public boolean isValidTerm(int termId){
		//int T = this.rank.length;
		int t = this.rank[termId];
		//if (t >= s*T && t <= (u-s)*T)
		if (t >= this.minValidRank && t <= this.maxValidRank)
			return true;
		else
			return false;
	}
	/**
	 * Sort the term by their frequencies in the collection.
	 *
	 */
	private void sortTermsByTF(){
		System.out.print("Loading the lexicon...");
		int[] TF = new int[(int)collSta.getNumberOfUniqueTerms()];
		for (int i = 0; i < TF.length; i++){
			try{
				this.rankedTermId[i] = i;
				this.lex.findTerm(i);
				TF[i] = lex.getTF();
			}
			catch(Exception e){
				e.printStackTrace();
				System.err.println("i: " + i);
				System.exit(1);
			}
		}
		SortDescendingPairedVectors.sort(TF, this.rankedTermId);
	}
	/**
	 * Save the index of terms on disk.
	 *
	 */
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
	 * Get the id of a randomly selected valid term.
	 * @return The term id.
	 */
	public int getValidRandomTermId(){
		int validRank = 
			(int)((double)minValidRank-1+Math.random()*(maxValidRank-minValidRank+2));
		return this.getTermIdByRank(validRank);
	}
	/**
	 * Print the filename of the index of terms on disk.
	 *
	 */
	private void getLogFile(){
		String invertedFilename = ApplicationSetup.INVERTED_FILENAME;
		File invertedFile = new File(invertedFilename);
		String filePrefix = invertedFilename.substring(0, invertedFilename.lastIndexOf('.'));
		this.fRankLog = new File(filePrefix.concat(".rnk"));
	}
}

