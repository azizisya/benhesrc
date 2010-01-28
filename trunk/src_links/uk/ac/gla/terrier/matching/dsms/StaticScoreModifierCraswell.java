package uk.ac.gla.terrier.matching.dsms;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.matching.MatchingQueryTerms;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.HeapSort;

/** See Relevance Weighting for Query Independant Evidence
  * Craswell et al, SIGIR 2005. Pages 416--423.
  * <p>
  * <li><tt>ssa.input.file</tt> - input file
  * <li><tt>ssa.modified.length</tt> - how much of the top-ranked documents to alter
  * <li><tt>ssaid</tt> - what function to apply to integrate the static score with the query-dependant score
  * <li><tt>ssa.w</tt>, <tt>ssa.k</tt> and <tt>ssa.a</tt> - paramters to the various functions.
  * <p>
  * <li>functionid=1 : Eq(1) - score(d,Q) + w.log(S_d)
  * <li>functionid=2 : Eq(2) - score(d,Q) + w.S_d / k+S_d
  * <li>functionid=3 : Eq(3) - score(d,Q) + w.S_d^a / k^a+S_d^a
  * <li>functionid=4 : Eq(4) - score(d,Q) + w.k^a / k^a+S_d^a (URLScore, ClickDistance etc)
  */
public class StaticScoreModifierCraswell implements DocumentScoreModifier {
	
	/** The number of top-ranked documents for which the scores will be modified. */
	protected int modifiedLength;
	
	protected int functionId;
	protected double w;
	protected double k;
	protected double a;
		
	/** The array that contains the statically computed scores.*/
	protected static double[] staticScores;


	/**
	 * Loads into memory an input file which contains a
	 * serialised double array with the static scores for
	 * each document.
	 */
	static {
		String inputFile = ApplicationSetup.getProperty("ssa.input.file","");
		try {
			java.io.ObjectInputStream ois = new java.io.ObjectInputStream(Files.openFileStream(inputFile));
			Object o = ois.readObject();
			
			try{
				staticScores = (double[]) o;
			} catch (ClassCastException cce) {
				staticScores = castToDoubleArr((float[]) o);
			}
			printStats(staticScores);
			ois.close();
		} catch (Exception e) {
			System.err.println("Problem opening file: \""+inputFile+"\" : "+e);
			e.printStackTrace();
		}
		final boolean normaliseToMean1 = Boolean.parseBoolean(ApplicationSetup.getProperty("ssa.normalise.mean1", "false"));
		if (normaliseToMean1)
			makeAverage1(staticScores);
	}

	protected static void makeAverage1(double ar[])
	{
		final int count = ar.length;
		double sum = 0;
		for(double v : ar)
			sum += v;
		final double average = sum / (double)count;
		for(int i=0;i<count;i++)
			ar[i] = ar[i] / average;
	}

	protected static void printStats(double ar[])
	{
		double sum = 0;
		final int l = ar.length;
		for(int i=0;i<l;i++)
			sum += ar[i];
		System.err.println("Sum of array is "+ sum+ " average "+ (sum/(double)l));
	}
	
	protected static double[] castToDoubleArr(float[] f)
	{
		final int l = f.length;
		final double rtr[] = new double[l];
		for(int i=0;i<l;i++)
			rtr[i] = (double)f[i];
		return rtr;
	}
	
	void initialise_parameters()
	{
		modifiedLength = Integer.parseInt(ApplicationSetup.getProperty("ssa.modified.length","1000"));
		functionId = Integer.parseInt(ApplicationSetup.getProperty("ssa.id","1"));
		w = Double.parseDouble(ApplicationSetup.getProperty("ssa.w","1"));
		k = Double.parseDouble(ApplicationSetup.getProperty("ssa.k","1"));
		a = Double.parseDouble(ApplicationSetup.getProperty("ssa.a","1"));
	}
	
	public StaticScoreModifierCraswell()
	{
		//initialise_parameters();
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#modifyScores(uk.ac.gla.terrier.structures.Index, uk.ac.gla.terrier.matching.MatchingQueryTerms, uk.ac.gla.terrier.matching.ResultSet)
	 */
	public boolean modifyScores(Index index, MatchingQueryTerms queryTerms, ResultSet set) {
		
		initialise_parameters();
		int minimum = modifiedLength;
		//onlyRerank = Boolean.getBoolean(ApplicationSetup.getProperty("ssa.only.rerank","false"));
		//onlyRerank = true;
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length
		
		if (minimum > set.getResultSize() || minimum == 0)
			minimum = set.getResultSize();


		int[] docids = set.getDocids();
		double[] scores = set.getScores();
		int start = 0;
		int end = minimum;
		System.out.println("Craswell: fnid="+functionId+ " w="+w);
		
		switch (functionId) {
			case 1: 
				for(int i=start;i<end;i++)
					scores[i] += w * Math.log(staticScores[docids[i]]);
				break;
			case 2:
				double S;
				for(int i=start;i<end;i++)
				{
					S = staticScores[docids[i]];
					scores[i] += (w*S)/(k+S);
				}
				break;
			case 3:
				double Sa;
				for(int i=start;i<end;i++)
				{
					Sa = Math.pow(staticScores[docids[i]], a);
					scores[i] += (w*Sa)/(Math.pow(k,a)+Sa);
				}
				break;
			case 4:
				double ka;
				for(int i=start;i<end;i++)
				{
					ka = Math.pow(k,a);
					scores[i] += (w*ka)/(ka + Math.pow(staticScores[docids[i]], a));
				}
				break;
		}
		HeapSort.descendingHeapSort(scores, docids, set.getOccurrences(), set.getResultSize());
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.gla.terrier.matching.dsms.DocumentScoreModifier#getName()
	 */
	public String getName() {
		return "StaticScoreModifier_Craswell";
	}

	public Object clone()	{
		return this;
	}
}
