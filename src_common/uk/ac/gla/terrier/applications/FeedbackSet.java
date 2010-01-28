package uk.ac.gla.terrier.applications;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import gnu.trove.TIntDoubleHashMap;
import uk.ac.gla.terrier.documentfeature.ArffFeatureExtractor;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.StringUtility;

abstract public class FeedbackSet {
	protected Index index;
	
	protected DirectIndex directIndex;
	
	protected DocumentIndex docIndex;
	
	protected CollectionStatistics collStats;
	
	protected Lexicon lexicon;
	
	public FeedbackSet(Index index){
		this.index = index;
		this.directIndex = index.getDirectIndex();
		this.lexicon = index.getLexicon();
		this.docIndex = index.getDocumentIndex();
		this.collStats = index.getCollectionStatistics();
	}
	
	private String formatPreProcess(int docid, String qid, int label, TIntDoubleHashMap map, Lexicon lexicon){
		StringBuilder sb = new StringBuilder();
		sb.append(docid+" "+qid+" "+label+" ");
		int[] termids = map.keys();
		Arrays.sort(termids);
		for (int i=0; i<termids.length; i++){
			sb.append(termids[i]+","+lexicon.getLexiconEntry(termids[i]).term+","+Rounding.toString(map.get(termids[i]), 6)+" ");
		}
		return sb.toString();
	}
	
	public void preProcess(String qrelsFilename, String queryid, String outputFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		String[] posDocnos = qrels.getRelevantDocumentsToArray(queryid);
		String[] negDocnos = qrels.getNonRelevantDocumentsToArray(queryid);
		int[] posDocids = StringUtility.stringsToInts(posDocnos);
		int[] negDocids = StringUtility.stringsToInts(negDocnos);
		int[] queryTermids = null;
		try{
			queryTermids = ArffFeatureExtractor.processQuery(((TRECQuery)Class.forName("uk.ac.gla.terrier.structures."+
					ApplicationSetup.getProperty("trec.topics.parser", 
					"TRECQuery")).newInstance()).getQuery(queryid), index).keys();
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}		
		StringBuffer buf = new StringBuffer();
		int counter = 0; int totalDocs = posDocids.length+negDocids.length;
		for (int i=0; i<posDocids.length; i++){
			if (posDocids[i]==-1)
				continue;
			System.out.print("processing positive document "+docIndex.getDocumentNumber(posDocids[i])+"...");
			TIntDoubleHashMap map = new TIntDoubleHashMap();
			//extractQueryFeature(posDocids[i], queryid, queryTermids, map);			
			System.out.println("Done. "+(++counter)+" out of "+totalDocs+" documents processed.");
			buf.append(this.formatPreProcess(posDocids[i], queryid, 1, map, lexicon)+ApplicationSetup.EOL);
		}
		for (int i=0; i<negDocids.length; i++){
			if (negDocids[i]==-1)
				continue;
			System.out.print("processing positive document "+docIndex.getDocumentNumber(posDocids[i])+"...");
			TIntDoubleHashMap map = new TIntDoubleHashMap();
			//extractQueryFeature(posDocids[i], queryid, queryTermids, map);			
			System.out.println("Done. "+(++counter)+" out of "+totalDocs+" documents processed.");
			buf.append(this.formatPreProcess(posDocids[i], queryid, 1, map, lexicon)+ApplicationSetup.EOL);
		}
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
