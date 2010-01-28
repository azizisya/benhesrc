package uk.ac.gla.terrier.applications;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedWriter;
import java.io.IOException;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.ExpansionTerms;
import uk.ac.gla.terrier.structures.GenoArticle;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.DataUtility;
import uk.ac.gla.terrier.utility.PDFUtility;
import uk.ac.gla.terrier.utility.TroveUtility;

public class AureusTask {
	
	public static void checkRepeatedPMIDs(String qrelsPmidFilename, String indexPmidFilename){
		int[] qrelsPmids = null;
		int[] indexPmids = null;
		try{
			System.out.print("loading pmids from "+qrelsPmidFilename+"...");
			qrelsPmids = DataUtility.loadInt(qrelsPmidFilename);
			System.out.println("Done. "+qrelsPmids.length+" pmids loaded.");
			System.out.print("loading pmids from "+indexPmidFilename+"...");
			indexPmids = DataUtility.loadInt(indexPmidFilename);
			System.out.println("Done. "+indexPmids.length+" pmids loaded.");
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		TIntHashSet qrelsPmidSet = new TIntHashSet();
		qrelsPmidSet.addAll(qrelsPmids);
		int counter = 0;
		for (int i=0; i<indexPmids.length; i++){
			if (qrelsPmidSet.contains(indexPmids[i]))
				counter++;
		}
		System.out.println("qrels: "+qrelsPmids.length+", index: "+indexPmids.length+
				", overlap: "+counter);
	}
	
	public static void createPmidList(String pmidFilelist, String outputFilename){
		try{
			Object[] pmidFiles = DataUtility.loadObject(pmidFilelist);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			for (int i=0; i<pmidFiles.length; i++){
				Object[] pmids = DataUtility.loadObject((String)pmidFiles[i]);
				StringBuffer buf = new StringBuffer();
				for (int j=0; j<pmids.length; j++){
					buf.append((String)pmids[j]+" "+(String)pmidFiles[i]+ApplicationSetup.EOL);
				}
				bw.write(buf.toString());
				buf = null;
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void expandFromPDF(String[] pdfFilenames, String indexpath, String indexprefix){
		Index index = Index.createIndex(indexpath, indexprefix);
		// parse pdf files
		GenoArticle[] articles = new GenoArticle[pdfFilenames.length];
		for (int i=0; i<articles.length; i++){
			articles[i] = PDFUtility.getGenoPDFArticle(pdfFilenames[i]);
			articles[i].dumpSpans(index.getLexicon());
		}
		// merge stats
		TIntIntHashMap[] maps = new TIntIntHashMap[articles.length];
		for (int i=0; i<articles.length; i++){
			maps[i] = articles[i].getTxtSpanStats();
		}
		TIntIntHashMap map = TroveUtility.mergeIntIntHashMaps(maps);
		// TroveUtility.dumpIntIntHashMap(map);
		// expand query
		System.err.println("expanding query...");
		ExpansionTerms expTerms = new ExpansionTerms(
				index.getCollectionStatistics(), TroveUtility.sumOfValues(map), index.getLexicon());
		System.out.println("maps.length="+maps.length);
		for (int i=0; i<maps.length; i++){
			System.out.println("maps["+i+"].size()="+maps[i].size());
			for (int termid:maps[i].keys())
				expTerms.insertTerm(termid, maps[i].get(termid));
		}
		
		TIntObjectHashMap<ExpansionTerm> terms = expTerms.getExpandedTermHashSet(
				Integer.parseInt(ApplicationSetup.getProperty("expansion.terms", "10")), 
				WeightingModel.getWeightingModel("KL"));
		System.out.println("terms.length="+terms.size());
		StringBuffer buffer = new StringBuffer();
		Lexicon lexicon = index.getLexicon();
		for (int termid : terms.keys())
			buffer.append(lexicon.getLexiconEntry(termid).term+"^"+terms.get(termid).getWeightExpansion()+" ");
		System.out.println(buffer.toString());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--checkrepeatedpmids")){
			// --checkrepeatedpmids qrelsPmidFilename indexPmidFilename
			AureusTask.checkRepeatedPMIDs(args[1], args[2]);
		}else if (args[0].equals("--createpmidlist")){
			// --createpmidlist <pmidFilelist> <outputFilename>
			AureusTask.createPmidList(args[1], args[2]);
		}else if (args[0].equals("--expandfrompdf")){
			// --expandfrompdf indexpath indexprefix pdffilenames...
			String[] pdfFilenames = new String[args.length-3];
			for (int i=0; i<pdfFilenames.length; i++)
				pdfFilenames[i] = args[i+3];
			AureusTask.expandFromPDF(pdfFilenames, args[1], args[2]);
		}

	}

}
