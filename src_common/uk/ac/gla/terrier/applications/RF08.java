package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class RF08 {
	
	public static void countUnstemmedLex(String lexOutputFilename){
		THashSet<String> stemmed = new THashSet<String>();
		Manager manager = new Manager(null);
		char initial = ' ';
		int count = 0;
		try{
			BufferedReader br = Files.openFileReader(lexOutputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String term = line.replaceAll(",", "").split(" ")[1];
				term = manager.pipelineTerm(term);
				if (term!=null &&
						!Character.isDigit(term.charAt(0))&&
						term.length()<=20){
					stemmed.add(term);
					if (initial != term.charAt(0)){
						System.out.println("initial: "+initial+", count: "+count);
						initial = term.charAt(0);
						count+=stemmed.size();
						stemmed.clear();
						stemmed = new THashSet<String>();
					}
				}
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("# of unique stemmed stopped terms: "+(count+stemmed.size()));
	}
	/**
	 * Remove documents in the qrels from the results.
	 * @param resultFilename
	 * @param qrelsFilename
	 * @param outputFilename
	 */
	public static void FilterResultFile(String resultFilename, String qrelsFilename, String outputFilename){
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			String queryid = "";
			THashSet<String> relSet = null;
			THashSet<String> nonrelSet = null;
			String line = null;
			StringBuffer buf = new StringBuffer();
			int rank = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				if (!queryid.equals(tokens[0])){
					queryid = tokens[0];
					rank = 0;
					relSet = qrels.getRelevantDocuments(queryid);
					nonrelSet = qrels.getNonRelevantDocuments(queryid);
				}
				if (relSet == null)
					continue;
				else if (relSet.contains(tokens[2]))
					continue;
				else if (nonrelSet == null || !nonrelSet.contains(tokens[2]))
					buf.append(tokens[0]+" "+tokens[1]+" "+tokens[2]+" "+(rank++)+" "+tokens[4]+" "+tokens[5]+ApplicationSetup.EOL);
			}
			br.close();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--removefeedbackdocs")){
			RF08.FilterResultFile(args[1], args[2], args[3]);
		}else if (args[0].equals("--countunstemmedlex")){
			RF08.countUnstemmedLex(args[1]);
		}

	}

}
