package tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TDoubleHashSet;
import gnu.trove.THashSet;
import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class TestQuery {
	
	public void geŧQrelsForOnelineQueries(String onelinetopicfilename,
			String qrelsFilename,
			String outputFilename
			){
		try{
			BufferedReader br = Files.openFileReader(onelinetopicfilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename); 
			String str = null;
			while ((str=br.readLine())!=null){
				String queryid = str.split(" ")[0];
				String[] nonRelDocnos = qrels.getNonRelevantDocumentsToArray(queryid);
				for (int j=0; j<nonRelDocnos.length; j++)
					bw.write(queryid+" 0 "+nonRelDocnos[j]+" 0"+ApplicationSetup.EOL);
				for (int t=1; t<=4; t++){
					String[] relDocnos = qrels.getRelevantDocumentsToArray(queryid, t);
					if (relDocnos!=null)
						for (int j=0; j<relDocnos.length; j++)
							bw.write(queryid+" 0 "+relDocnos[j]+" "+t+ApplicationSetup.EOL);
				}
			}
			
			bw.close();
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void checkTopicsOverlap(String dirname, String prefix){
		String[] filenames = (new File(dirname)).list();
		THashSet<String> filenameSet = new THashSet<String>();
		for (int i=0; i<filenames.length; i++)
			if (filenames[i].startsWith(prefix) && !filenames[i].endsWith(".rest"))
				filenameSet.add(filenames[i]);
		filenames = (String[])filenameSet.toArray(new String[filenameSet.size()]);
		Arrays.sort(filenames);
		for (int i=0; i<filenames.length-1; i++){
			for (int j=i+1; j<filenames.length; j++){
				System.out.println(filenames[i]+" "+filenames[j]+
						": "+getTopicsOverlap(new File(dirname, filenames[i]), new File(dirname, filenames[j])));
			}
		}
	}
	
	public THashSet<String> loadQueryids(File file1){
		THashSet<String> queryids = new THashSet<String>();
		try{
			BufferedReader br = Files.openFileReader(file1);
			String str = null;
			while ((str=br.readLine())!=null)
				queryids.add(str.split(" ")[0]);
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return queryids;
	}
	
	public void getOverallPerformance(String filename){
		TDoubleArrayList bestSet = new TDoubleArrayList();
		TDoubleArrayList medianSet = new TDoubleArrayList();
		TDoubleArrayList worstSet = new TDoubleArrayList();
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			int counter = 0;
			while ((str=br.readLine())!=null){
				 StringTokenizer stk = new StringTokenizer(str);
				 stk.nextToken(); stk.nextToken();
				 bestSet.add(Double.parseDouble(stk.nextToken()));
				 medianSet.add(Double.parseDouble(stk.nextToken()));
				 worstSet.add(Double.parseDouble(stk.nextToken()));
				 counter++;
			 }
			 br.close();
		 }catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		 }
		 double[] best = bestSet.toNativeArray();
		 double[] median = medianSet.toNativeArray();
		 double[] worst = worstSet.toNativeArray();
		 System.err.println("mean best: "+Statistics.mean(best));
		 System.err.println("mean median: "+Statistics.mean(median));
		 System.err.println("mean worst: "+Statistics.mean(worst));
	}
	
	public double getTopicsOverlap(File file1, File file2){
		THashSet<String> queryids1 = loadQueryids(file1);
		THashSet<String> queryids2 = loadQueryids(file2);
		String[] ids1 = (String[])queryids1.toArray(new String[queryids1.size()]);
		int counter = 0;
		for (int i=0; i<ids1.length; i++)
			if (queryids2.contains(ids1[i]))
				counter++;
		return (double)counter/queryids1.size();
	}
	// column: 0-based
	public static void printEntropy(String filename, int column){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			TDoubleHashSet valueSet = new TDoubleHashSet();
			while ((str=br.readLine())!=null){
				valueSet.add(Double.parseDouble(str.split(" ")[column]));
			}
			System.out.println("Entropy: "+Statistics.entropy(valueSet.toArray()));
			double[] values = valueSet.toArray();
			Arrays.sort(values);
			System.out.println("Spread: "+(values[values.length-1]-values[0]));
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	public void extractQueryTerms(String topicFilename){
		Manager manager = new Manager(null);
		TRECQuery queries = new TRECQuery(topicFilename);
		String[] queryids = queries.getQueryids();
		for (int i=0; i<queryids.length; i++){
			String queryid = queryids[i];
			String[] terms = queries.getQuery(queryid).split(" ");
			THashSet<String> termSet = new THashSet<String>();
			System.out.print(queryid);
			for (int j=0; j<terms.length; j++){
				terms[j] = manager.pipelineTerm(terms[j]);
				if (terms[j]!=null){
					if (!termSet.contains(terms[j])){
						System.out.print(" "+terms[j]);
						termSet.add(terms[j]);
					}
				}
			}
			System.out.println();
		}
	}
	
	public void mapDocnoDocidInQrels(String indexPath, String indexPrefix, String qrelsFilename){
		try{
			Index index = Index.createIndex(indexPath, indexPrefix);
			DocumentIndex docIndex = index.getDocumentIndex();
			String outputFilename = qrelsFilename+".map.gz";
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			String[] queryids = qrels.getQueryids();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			for (int i=0; i<queryids.length; i++){
				String[] docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
				for (int j=0; j<docnos.length; j++){
					int docid = docIndex.getDocumentId(docnos[j]);
					bw.write(docnos[i]+" "+docid+ApplicationSetup.EOL);
				}
			}
			bw.close();
			index.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void printNumberOfDocsInQrels(String qrelsFilename){
		try{
			BufferedReader br = Files.openFileReader(qrelsFilename);
			String str = null;
			THashSet<String> docnoSet = new THashSet<String>();
			while ((str=br.readLine())!=null){
				String[] tmp = str.split(" ");
				docnoSet.add(tmp[2]);
			}
			br.close();
			System.out.println("Number of unique docs: "+docnoSet.size());
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void getDocuments(String topicFilename){
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[1].equals("--extractqueryterms")){
			// -q --extractqueryterms topicfilename
			(new TestQuery()).extractQueryTerms(args[2]);
		}else if (args[1].equals("--getqrelsforonelinequeries")){
			// -q --getqrelsforonelinequeries <topicsFilename> <qrelsFilename> <outputFilename>
			(new TestQuery()).geŧQrelsForOnelineQueries(args[2], args[3], args[4]);
		}else if (args[1].equals("--checktopicsoverlap")){
			// -q --checktopicsoverlap <dirname> <prefix>
			(new TestQuery()).checkTopicsOverlap(args[2], args[3]);
		}else if (args[1].equals("--printoverallperformance")){
			// -q --printoverallperformance <filename>
			(new TestQuery()).getOverallPerformance(args[2]);
		}else if (args[1].equals("--createdocnoidmappingforqrels")){
			// -q --createdocnoidmappingforqrels <indexpath> <indexprefix> <qrelsfilename>
			(new TestQuery()).mapDocnoDocidInQrels(args[2], args[3], args[4]);
		}else if (args[1].equals("--printnumberofdocsinqrels")){
			// -q --printnumberofdocsinqrels
			TestQuery.printNumberOfDocsInQrels(args[2]);
		}else if (args[1].equals("--printentropy")){
			// -q --printentropy filename column(0-based)
			TestQuery.printEntropy(args[2], Integer.parseInt(args[3]));
		}
	}

}
