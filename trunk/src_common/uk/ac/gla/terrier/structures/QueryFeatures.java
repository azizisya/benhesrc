package uk.ac.gla.terrier.structures;

import java.io.IOException;
import java.util.Vector;

import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.matching.BufferedMatching;
import uk.ac.gla.terrier.matching.models.Idf;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.trees.TermTreeNode;

/**
 *	This class implements a data structure for the query features vector.
 * @author  Ben HE
 */

public class QueryFeatures {

    /** The number of documents containing (at least one of) the query terms. */
    public int Nq;
    
    protected String queryId = new String("" + System.currentTimeMillis());
    
    protected CollectionStatistics collSta;
    
    protected double[] Nt;
    
    protected double[] TF;

    /** The query length. */
    public int queryLength;
    
    public double[] qtf;
     
    /** standard deviation of idf. */
    public double stdIdf; 
    
    /** Average inverse collection term frequency. */
    public double avICTF;
    
    /** Inverse collection term frequency./ */
    public double ICTF;
    
    public QueryFeatures(BasicQuery query, BufferedMatching matching){
    	this.initialise(query, matching);
    }
    
    protected void initialise(BasicQuery query, BufferedMatching matching){
    	collSta = matching.collSta;
    	this.qtf = new double[query.getQueryLength()];
    	TermTreeNode[] terms = query.getQueryTerms();
    	for (int i = 0; i < terms.length; i++)
    		qtf[i] = terms[i].normalisedFrequency / query.getQueryLength();
    	matching.matchWithoutScoring(query.getQueryNumber(), query.getQueryTermStrings());
    	this.Nq = matching.getNumberOfRetrievedDocuments();
    	double[] NtLocal = matching.getNt();
    	double[] TFLocal = matching.getTF();
    	if (NtLocal.length != TFLocal.length){
    		System.err.println("WARNING: Nt.length != TF.length");
    	}
    	
    	Vector vecNt = new Vector();
    	Vector vecTF = new Vector();
    	
    	for (int i = 0; i < TFLocal.length; i++)
    		if (TFLocal[i]!=0d){
    			vecNt.addElement(new Double(NtLocal[i]));
    			vecTF.addElement(new Double(TFLocal[i]));
    		}
    	Nt = new double[vecNt.size()];
    	TF = new double[vecTF.size()];
    	for (int i = 0; i < vecTF.size(); i++){
    		Nt[i] = ((Double)vecNt.get(i)).doubleValue();
    		TF[i] = ((Double)vecTF.get(i)).doubleValue();
    	}
    	
    	this.computeFeatureVector();
    	this.queryId = query.getQueryNumber();
    }
    
    public double[] getFeatureVector(){
    	double[] qf = new double[2];
    	qf[0] = this.stdIdf;
    	qf[1] = this.avICTF;
    	return qf;
    }
    
    protected void computeFeatureVector(){
    	
    	this.queryLength = this.Nt.length;

		// standard deviation of idf 
		double[] idfs = new double[this.Nt.length];
		Idf idf = new Idf();
		for (int i = 0; i < idfs.length; i++){
			idfs[i] = idf.idfNENQUIRY(this.Nt[i]);
		}
		this.stdIdf = Statistics.standardDeviation(idfs);
				
		/** AvICTF */
		ICTF = 0d;
		for (int i = 0; i < TF.length; i++){
			ICTF += Statistics.logx(2, collSta.getNumberOfTokens() / TF[i]);
		}
		this.avICTF = ICTF/queryLength;
		
    }
	
	public void setQueryId(String id){
		this.queryId = id;
	}
	
	public String getQueryId(){
		return this.queryId;
	}
    
}
