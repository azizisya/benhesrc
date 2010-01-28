package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexInputStream;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.StringUtility;

public class DocLengthRelevance {
	
	protected Index index;
	
	String indexpath;
	String indexprefix;
	
	DocumentIndex docindex;
	
	protected int binSize;
	
	protected int Nbin;
	
	protected int[] borders;
	
	protected int poolBinSize;
	
	protected int[] poolBinBorders;
	
	public DocLengthRelevance(String indexpath, String indexprefix, int Nbin){
		index = Index.createIndex(indexpath, indexprefix);
		this.docindex = index.getDocumentIndex();
		this.indexpath = indexpath;
		this.indexprefix = indexprefix;
		this.Nbin = Nbin;
		this.borders = this.getDocLengthBoundary();
	}
	
	/**
	 * 
	 * @param qrelsFilename qrels file should contain docids, not docnos
	 */
	public void printProbOfRelInCompRev(String qrelsFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		double[][] prob = new double[Nbin][qrels.getNumberOfQueries()];
		String[] queryids = qrels.getQueryids();
		Arrays.sort(queryids);
		// for each query
		for (int i=0; i<qrels.getNumberOfQueries(); i++){
			// allocate rel docs to bins
			int[] counts = new int[Nbin];
			Arrays.fill(counts, 0);
			String[] docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
			int judged = qrels.getNonRelevantDocumentsToArray(queryids[i]).length+qrels.getRelevantDocumentsToArray(queryids[i]).length;
			int[] docids = StringUtility.stringsToInts(docnos);
			int[] doclength = new int[docids.length];
			for (int j=0; j<docids.length; j++){
				doclength[j] = (docids[j]>=0)?(docindex.getDocumentLength(docids[j])):(0);
			}
			Arrays.sort(doclength);
			int bin = 1;
			for (int j=0; j<docids.length; j++){
				if (doclength[j] == 0)
					continue;
				while (!(doclength[j]>=borders[bin-1]&&doclength[j]<borders[bin])&&bin<=Nbin)
					bin++;
				if (bin<=Nbin)
					counts[bin-1]++;
			}
			// compute probs, what to do with empty bins?
			System.out.println("query "+queryids[i]);
			for (int j=0; j<Nbin; j++){
				prob[j][i] = (double)counts[j]/binSize;
				System.out.println("bin "+(j+1)+": "+Rounding.toString(prob[j][i], 8));
			}
		}
		// print for the mean
		System.out.println(">>>>>>>>>>>>>>IN AVERAGE");
		for (int j=0; j<Nbin; j++){
			System.out.println("bin "+(j+1)+": "+Rounding.toString(Statistics.mean(prob[j]), 8));
		}
	}
	
	/**
	 * 
	 * @param qrelsFilename qrels file should contain docids, not docnos
	 */
	public void printProbOfRel(String qrelsFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		double[][] prob = new double[Nbin][qrels.getNumberOfQueries()];
		String[] queryids = qrels.getQueryids();
		Arrays.sort(queryids);
		// for each query
		for (int i=0; i<qrels.getNumberOfQueries(); i++){
			// allocate rel docs to bins
			int[] counts = new int[Nbin];
			Arrays.fill(counts, 0);
			String[] docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
			int[] docids = StringUtility.stringsToInts(docnos);
			int[] doclength = new int[docids.length];
			for (int j=0; j<docids.length; j++){
				doclength[j] = (docids[j]>=0)?(docindex.getDocumentLength(docids[j])):(0);
			}
			Arrays.sort(doclength);
			int bin = 1;
			for (int j=0; j<docids.length; j++){
				if (doclength[j] == 0)
					continue;
				while (!(doclength[j]>=borders[bin-1]&&doclength[j]<borders[bin])&&bin<=Nbin)
					bin++;
				if (bin<=Nbin)
					counts[bin-1]++;
			}
			// compute probs, what to do with empty bins?
			System.out.println("query "+queryids[i]);
			for (int j=0; j<Nbin; j++){
				prob[j][i] = (double)counts[j]/binSize;
				System.out.println("bin "+(j+1)+": "+Rounding.toString(prob[j][i], 8));
			}
		}
		// print for the mean
		System.out.println(">>>>>>>>>>>>>>IN AVERAGE");
		for (int j=0; j<Nbin; j++){
			System.out.println("bin "+(j+1)+": "+Rounding.toString(Statistics.mean(prob[j]), 8));
		}
	}
	
	/**
	 * 
	 * @param Nbin number of bins 
	 * @return
	 */
	private int[] getDocLengthBoundary(){
		int[] boundaries = new int[Nbin+1];
		// sort doclengths
		DocumentIndexInputStream dis = new DocumentIndexInputStream(indexpath, indexprefix);
		int N = index.getCollectionStatistics().getNumberOfDocuments();
		int[] doclength = new int[N];
		int counter = 0;
		
		try{
			while (dis.readNextEntry()!=-1){
				doclength[counter++] = dis.getDocumentLength();
			}
			System.err.println("counter: "+counter+", N: "+N);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		Arrays.sort(doclength);
		binSize = N/Nbin;
		// assign boundaries
		boundaries[0] = 1; boundaries[Nbin] = doclength[N-1];
		for (int i=1;i<Nbin; i++)
			boundaries[i] = doclength[i*binSize];
		for (int i=0; i<Nbin; i++)
			System.err.println("bin "+i+": "+boundaries[i]+", "+boundaries[i+1]);
		return boundaries;
	}
	
	/**
	 * 
	 * @param Nbin number of bins 
	 * @return
	 */
	private int[] getDocLengthBoundary(int[] docids){
		int[] boundaries = new int[Nbin+1];
		// sort doclengths
		int N = docids.length;
		int[] doclength = new int[N];
		int counter = 0;
		Arrays.sort(doclength);
		int size = N/Nbin;
		// assign boundaries
		boundaries[0] = 1; boundaries[Nbin] = doclength[N-1];
		for (int i=1;i<Nbin; i++)
			boundaries[i] = doclength[i*size];
		return boundaries;
	}
	
	private int[] cleanDocids(int[] docids){
		// remove dup and negative docids
		TIntHashSet docidSet = new TIntHashSet();
		for (int i=0; i<docids.length; i++){
			if (docids[i] >=0)
				docidSet.add(docids[i]);
		}
		return docidSet.toArray();
	}
	/**
	 * 
	 * @param borders
	 * @param doclength pre-sorted
	 * @param counts
	 */
	protected void allocateDocuments(int[] borders, int[] doclength, int[] counts){
		Arrays.fill(counts, 0);
		int bin = 1;
		for (int j=0; j<doclength.length; j++){
			if (doclength[j] == 0)
				continue;
			while (!(doclength[j]>=borders[bin-1]&&doclength[j]<borders[bin])&&bin<=Nbin)
				bin++;
			if (bin<=Nbin)
				counts[bin-1]++;
		}
	}
	/**
	 * docids should be cleaned 
	 * @param reldocids
	 * @param nonreldocids
	 * @return
	 */
	public double[] getProbs(int[] reldocids, int[] nonreldocids){
		//reldocids = cleanDocids(reldocids);
		// nonreldocids = cleanDocids(nonreldocids);
		 
		// sort by doclength
		int[] doclength = new int[reldocids.length+nonreldocids.length];
		int N = doclength.length;
		int counter = 0;
		int[] reldoclength = new int[reldocids.length];
		int[] nonreldoclength = new int[nonreldocids.length];
		for (int i=0; i<reldocids.length; i++){
			doclength[counter++] = docindex.getDocumentLength(reldocids[i]);
			reldoclength[i] = doclength[counter-1];
		}
		
		for (int i=0; i<nonreldocids.length; i++){
			doclength[counter++] = docindex.getDocumentLength(nonreldocids[i]);
			nonreldoclength[i] = doclength[counter-1];
		}
		Arrays.sort(doclength);
		Arrays.sort(reldoclength);
		Arrays.sort(nonreldoclength);
		// decide on borders
		this.poolBinSize = N / Nbin;
		this.poolBinBorders = new int[Nbin+1];
		poolBinBorders[0] = 0; // should consider empty docs in future
		poolBinBorders[Nbin] = doclength[N-1];
		for (int i=1; i<Nbin; i++)
			poolBinBorders[i] = poolBinBorders[i-1]+this.poolBinSize;
		// allocate docs
		int[] counts = new int[Nbin];
		int[] relcounts = new int[Nbin];
		int[] nonrelcounts = new int[Nbin];
		this.allocateDocuments(poolBinBorders, reldoclength, relcounts);
		// compute probs
		return computeProb(relcounts, poolBinSize);
	}
	
	protected double[] computeProb(int[] counts, int size){
		double[] probs = new double[Nbin];
		for (int i=0; i<Nbin; i++)
			probs[i] = (double)counts[i]/size;
		return probs;
	}
	
	public void print(){
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--printrawprob")){
			// --printrawprob indexpath indexprefix numBins qrelsfilename
			DocLengthRelevance app = new DocLengthRelevance(args[1], args[2], Integer.parseInt(args[3]));
			app.printProbOfRel(args[4]);
		}

	}

}
