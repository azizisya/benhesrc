package uk.ac.gla.terrier.matching.dsms;

import uk.ac.gla.terrier.matching.*;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.*;

import java.io.BufferedReader;
import java.util.HashMap;

/** Takes an input file, specified by property <tt>DistanceModifier.inputfile</tt>, which has the format:
<pre>
queryid docno docno docno
</pre>
  * @author Craig Macdonald
  */
public class DistanceModifier implements DocumentScoreModifier
{
	protected HashMap<String, int[]> clickDocuments;
	
	protected WeightingModel wmodel; 
	
	protected Lexicon lexicon;

	public Object clone() { return this; }
	public String getName() { return "DistanceModifier";}

	public DistanceModifier()
	{
		clickDocuments = new HashMap<String, int[]>();
		try{
			final DocumentIndex doi = new DocumentIndex();
			final BufferedReader br = Files.openFileReader(
				ApplicationSetup.getProperty("DistanceModifier.inputfile", null));
			String line = null;
			while((line = br.readLine()) != null)
			{
				final String parts[] = line.trim().split("\\s+");
				final int l = parts.length;
				int[] docids = new int[l-1];
				for (int i=1;i<l;i++)
				{
					docids[i-1] = doi.getDocumentId( parts[i] );
				}
				clickDocuments.put(parts[0], docids);
			}

		} catch (Exception e) {
			System.err.println("Could not load file because "+ e);
            e.printStackTrace();
		}
	}

	public boolean modifyScores(final Index index, final MatchingQueryTerms queryTerms, final ResultSet resultSet)
	{
		double w = Double.parseDouble(ApplicationSetup.getProperty("cosine.distance.w", "1d"));
		Idf idf = new Idf();
		boolean USE_TF = Boolean.parseBoolean(ApplicationSetup.getProperty("cosine.distance.usetf", "true"));
		
		// initialise index structures and weighting model
		lexicon = index.getLexicon();
		DocumentIndex docIndex = index.getDocumentIndex();
		if (!USE_TF){
			CollectionStatistics collSta = index.getCollectionStatistics();
			//	initialise weighting model
			String wmodelName = ApplicationSetup.getProperty("cosine.distance.modelname", "DLH13");
			String modelNamePrefix = "uk.ac.gla.terrier.matching.models.";
			if (wmodelName.lastIndexOf('.')==-1)
				wmodelName = modelNamePrefix.concat(wmodelName);
			try{
				wmodel = (WeightingModel)Class.forName(wmodelName).newInstance();
			}catch(Exception e) {
	            e.printStackTrace();
	            System.exit(1);
			}
			double c = Double.parseDouble(ApplicationSetup.getProperty("cosine.distance.c", "1d"));
			wmodel.setParameter(c);
			wmodel.setNumberOfTokens(collSta.getNumberOfTokens());
			wmodel.setNumberOfDocuments(collSta.getNumberOfDocuments());
			wmodel.setAverageDocumentLength(collSta.getAverageDocumentLength());
			System.err.println("cosine.distance.w="+w+", model: "+wmodel.getInfo()+
			                                ", use tf: "+USE_TF);
		}
		else
			System.err.println("cosine.distance.w="+w+", use tf: "+USE_TF);
		
		int[] docids = resultSet.getDocids();
		double[] scores = resultSet.getScores();
		final int docCount = docids.length;
		final String queryId = queryTerms.getQueryId();
		final int[] clickDocs = clickDocuments.get(queryId);
		if (clickDocs == null)
		{
			System.err.println("No clicked document found for query id "+queryId+" - resultset is unchanged");
			return false;
		}
		final int clickDocCount = clickDocs.length;
		
		int numberOfDocumentsToModifyScores = Integer.parseInt(
				ApplicationSetup.getProperty("cosine.distance.document.uplimit", "0d"));
		numberOfDocumentsToModifyScores = (numberOfDocumentsToModifyScores==0)?
				(docCount):
				(Math.min(numberOfDocumentsToModifyScores, docCount));

		final int termsInCollection = index.getCollectionStatistics().getNumberOfUniqueTerms();
		final DirectIndex di = index.getDirectIndex();

		double[][] clickLM = new double[clickDocCount][termsInCollection];
		TerrierTimer timer = new TerrierTimer();
		timer.start();
		System.err.print("Modifying scores for "+numberOfDocumentsToModifyScores+" documents...");
		for(int j=0;j<clickDocCount;j++)
	        {
			int[][] terms = di.getTerms(docids[j]);
			if (terms==null) continue;
			if (USE_TF)
				makeDocumentVector(terms, clickLM[j]);
			else
				makeDocumentVector(terms, clickLM[j], docIndex.getDocumentLength(docids[j]));
		}
		
		
		double[] maxCos = new double[docCount];
		
		int counter = 0;

		for(int i=0;i<numberOfDocumentsToModifyScores;i++)
		{
			if (scores[i]<=0d)
				continue;
			//scores[i] = //TODO;
			//System.err.println("Calculating distances for docid "+ docids[i]);
			double[] v2 = new double[termsInCollection];
			int[][] terms = di.getTerms(docids[i]);
			if (terms==null) continue;
			if (USE_TF)
				makeDocumentVector(terms, v2);
			else
				makeDocumentVector(terms, v2, docIndex.getDocumentLength(docids[i]));
			double[] cos = new double[clickDocCount];
			
			
			for(int j=0;j<clickDocCount;j++)
			{
				maxCos[i] = (j==0)?(cosine1(clickLM[j], v2)):(Math.max(maxCos[i], cosine1(clickLM[j], v2)));
				// cos[j] = cosine1(clickLM[j], v2);
			}
			//System.out.println("");
			//TODO: alter scores[i] in some way based on the contents of cos[]
			//NOTE not necessarily the average - eg could be a function of the max of cos[]
			if (maxCos[i] >0){
				if (maxCos[i]>=1d)// remove double value errors
					maxCos[i] = 1d-0.00000001d;
				scores[i] = scores[i] - w*idf.log(1-maxCos[i]);
				counter++;
				if (Double.isNaN(scores[i])){
					System.err.println("Found NaN. maxCos[i]="+maxCos[i]);
				}
			}
		}
		timer.setBreakPoint();
		System.err.println("Done in "+timer.toStringMinutesSeconds()+". ");
		System.err.println("Modified scores for "+counter+" documents");
		return true;
	}

    double[] makeDocumentVector(int[][] terms, double[] rtr)
    {
        //double[] rtr = new double[TermCount];
        final int l = terms[0].length;
        for(int i=0;i<l;i++)
            rtr[terms[0][i]] = terms[1][i];
        return rtr;
    }
    
    double[] makeDocumentVector(int[][] terms, double[] rtr, double docLength)
    {
        //double[] rtr = new double[TermCount];
        final int l = terms[0].length;
        for(int i=0;i<l;i++){
        	int tf = terms[1][i];
        	int termid = terms[0][i];
        	if (lexicon.findTerm(termid)){
        		rtr[termid] = wmodel.score(tf, docLength, lexicon.getNt(), lexicon.getTF(), 1d);
        	}
        }
        return rtr;
    }


	    /** See Finding Out About (FOA), pg 96 */
    public static double cosine1(final double[] v1, final double[] v2)
    {
        final int length = v1.length;
        double total = 0; double t1=0; double t2=0;
        for(int i=0;i<length;i++)
        {
            if (v1[i] > 0)
                t1+= (v1[i] * v1[i]);
            if (v2[i] > 0)
            {
                t2+= (v2[i] * v2[i]);
                if (v1[i] > 0) /* and v2[i]>0 */
                    total += (v1[i] * v2[i]);// compute the inner product
            }
        }
        return total/(Math.sqrt(t1) * Math.sqrt(t2) );
    }

}
