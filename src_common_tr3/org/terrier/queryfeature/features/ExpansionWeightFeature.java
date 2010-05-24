package org.terrier.queryfeature.features;

import java.io.IOException;
import java.util.Arrays;

import org.terrier.documentfeature.DFEQPropFeature;
import org.terrier.documentfeature.DocumentFeature;
import org.terrier.matching.models.WeightingModel;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;
import org.terrier.querying.QueryExpansion;
import org.terrier.querying.termselector.TermSelector;
import org.terrier.structures.ExpansionTerm;
import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;

import gnu.trove.TIntDoubleHashMap;

public class ExpansionWeightFeature extends QueryFeature {
	protected int minTermRank = Integer.parseInt(ApplicationSetup.getProperty("extractor.min.term.rank", "0"));
	
	protected int maxTermRank = Integer.parseInt(ApplicationSetup.getProperty("extractor.max.term.rank", "19"));
	
	protected QueryExpansionModel model = QueryExpansionModel.getQueryExpansionModel(ApplicationSetup.getProperty("trec.model", "KL"));
	
	public ExpansionWeightFeature(Index index){
		super(index);
	}
	
	public String getInfo(){
		return "TermWeightFeature";
	}

	@Override
	public void extractQueryFeature(int[] docids, String queryid, int[] queryTermids, TIntDoubleHashMap featureMap) {
		int totalLength = 0;
		for (int i=0; i<docids.length; i++)
			try{
				totalLength+=docIndex.getDocumentLength(docids[i]);
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
		TermSelector selector = TermSelector.getTermSelector("DFRTermSelector", index);
		QueryExpansion qe = new QueryExpansion();
		ExpansionTerm[] expterms = qe.expandFromDocuments(docids, null, totalLength, index, model, selector);
		Arrays.sort(expterms);
		int start = minTermRank;
		int end = Math.min(maxTermRank, expterms.length-1);
		for (int i=start; i<=end; i++)
			// svm light doesn't accept feature ids smaller than 1 
			if (expterms[i].getTermID()>=1)
				featureMap.put(expterms[i].getTermID(), expterms[i].getWeightExpansion());
	}
	
	public static void main(String[] args){
		// --preprocess indexpath indexprefix qrelsname queryid outputname
		if (args[0].equals("--preprocess")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String queryid = args[4];
			String outputFilename = args[5];
			ExpansionWeightFeature app = new ExpansionWeightFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcess(qrelsFilename, queryid, outputFilename);
		}else if (args[0].equals("--preprocessall")){
			// --preprocessall indexpath indexprefix qrelsname outputname
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsFilename = args[3];
			String outputFilename = args[4];
			ExpansionWeightFeature app = new ExpansionWeightFeature(Index.createIndex(indexpath, indexprefix));
			app.preProcessAll(qrelsFilename, outputFilename);
		}
	}

}
