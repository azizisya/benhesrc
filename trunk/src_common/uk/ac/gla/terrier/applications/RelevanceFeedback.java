/*
 * Created on 28 Feb 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class RelevanceFeedback {

	public void writeFeedbackDocuments(int grade, int numberOfFeedbackDocumentsPerQuery){
		try{
			TRECQrelsInMemory qrels = new TRECQrelsInMemory();
			String outputFilename = ApplicationSetup.TERRIER_ETC+
					ApplicationSetup.FILE_SEPARATOR+"feedbackDocuments_"+grade+
					"_"+numberOfFeedbackDocumentsPerQuery;
			String qrelsOutputFilename = outputFilename+".qrels";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			BufferedWriter qrelsBw = (BufferedWriter)Files.writeFileWriter(qrelsOutputFilename);
			
			// get queryids
			String[] queryids = qrels.getQueryids();
			for (int i=0; i<queryids.length; i++){
				// for each query, get relevant documents with the given grade
				String[] docnos = null;
				if (grade == -1)
					docnos = qrels.getNonRelevantDocumentsToArray(queryids[i]);
				else if (grade == 0)
					docnos = qrels.getRelevantDocumentsToArray(queryids[i]);
				else
					docnos = qrels.getRelevantDocumentsToArray(queryids[i], grade);
				// write disk
				if (docnos == null || docnos.length == 0)
					continue;
				int numberOfDocumentsToWrite = numberOfFeedbackDocumentsPerQuery;
				if (numberOfFeedbackDocumentsPerQuery > docnos.length){
					if (docnos.length == 1)
						continue;
					else
						numberOfDocumentsToWrite = docnos.length/2;
				}
				THashSet<String> feedbackDocnos = new THashSet<String>();
				bw.write(queryids[i]+" ");
				for (int j=0; j<numberOfDocumentsToWrite; j++){
					int randomPosition = (int)(Math.random()*(docnos.length));
					if (feedbackDocnos.contains(randomPosition))
						continue;
					else{
						feedbackDocnos.add(docnos[randomPosition]);
						bw.write(docnos[randomPosition]+" ");
					}
				}
				bw.write(ApplicationSetup.EOL);
			}
			bw.close(); qrelsBw.close();
			System.out.println("Done. Feedback documents saved in "+outputFilename);
			System.out.println("Qrels saved in "+qrelsOutputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		 
	} 
	
	public void printOpitions(){
		System.out.println("--writefeedbackdocs <grade> <exp_docs>");
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RelevanceFeedback apps = new RelevanceFeedback();
		if (args.length == 0)
			apps.printOpitions();
		else if (args[0].equals("--writefeedbackdocs")){
			int grade = Integer.parseInt(args[1]);
			int exp_docs = Integer.parseInt(args[2]);
			apps.writeFeedbackDocuments(grade, exp_docs);
		}
	}

}
