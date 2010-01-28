/*
 * Created on 2005-1-7
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.evaluation.TRECResultsInMemory;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.BubbleSort;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.SpearmanCorrelation;
import uk.ac.gla.terrier.utility.SystemUtility;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestResults {
	protected static String EOL = ApplicationSetup.EOL; 
	
	public void writeAveragePrecisions(String option, File fOut){
		File fRes = new File(ApplicationSetup.TREC_RESULTS);
		String[] filenames = fRes.list();
		Vector resultVector = new Vector();
		Vector data = new Vector();
		for (int i = 0; i < filenames.length; i++){
			if (filenames[i].endsWith(".eval")){
				String model = null;
				double c = -1;
				String filename = null;
				if (option.equalsIgnoreCase("simple")){
					//filename = filenames[i].substring(0, filenames[i].indexOf('_'));
					filename = filenames[i];
				}
				else{
					model = filenames[i].substring(0, filenames[i].indexOf('c'));
					c = Double.parseDouble(filenames[i].substring(filenames[i].indexOf('c')+1, 
							filenames[i].indexOf('_', filenames[i].indexOf('c')+1)));
				}

				String AP = null;
				if (option.equals("10")){
					AP = Rounding.toString(SystemUtility.loadPrecisionAt10(
							new File(fRes, filenames[i])), 4);
				}
				else{
					AP = Rounding.toString(SystemUtility.loadAveragePrecision(
							new File(fRes, filenames[i])), 4);
				}
				if (option.equals("c")){// sort by beta			
					resultVector.add(c + " " + model + " " + AP);
					data.addElement(new Double(c));
				}
				if (option.equals("ap")){// sort by beta			
					resultVector.add(AP + " " + c + " " + model);
					data.addElement(new Double(AP));
				}
				if (option.equals("10")){// sort by precision at 10
					resultVector.add(AP + " " + c + " " + model);
					data.addElement(new Double(c));
				}
				if (option.equalsIgnoreCase("simple")){
					resultVector.add(AP + " " + filename);
					data.addElement(new Double(AP));
				}
			}
		}
		double[] dataArray = new double[data.size()];
		for (int i = 0; i < dataArray.length; i++)
			dataArray[i] = ((Double)data.get(i)).doubleValue();
		int[] orderedids = BubbleSort.getOrder(dataArray);
		String[] result = (String[])resultVector.toArray(new String[resultVector.size()]);
		//Arrays.sort(result);
    		
		StringBuffer buffer = new StringBuffer();
    		
		for (int i = 0; i < result.length; i++)
			buffer.append(result[orderedids[i]] + EOL);
    			
		try{
			SystemUtility.writeString(fOut, buffer.toString());
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
    		
		System.out.println("Results saved in file " + fOut.getPath());
	}
	
	public void sortEntry(File f, int pos){
		Vector vecEntry = new Vector();
		try{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = null;
			while((str=br.readLine())!=null){
				if (str.trim().length() == 0)
					continue;
				vecEntry.addElement(str);
			}
			br.close();
			//StringTokenizer stk = new StringTokenizer((String)vecEntry.get(0));
			//int numberOfColumns = stk.countTokens();
			
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void concatAveragePrecision(File f1, File f2, File fOut){
		try{
			Vector vecAP = new Vector();
			BufferedReader br = new BufferedReader(new FileReader(f1));
			String str = null;
			while((str = br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				stk.nextToken();// skip query id
				vecAP.addElement(stk.nextToken());
			}			
			br.close();
			
			StringBuffer buffer = new StringBuffer();
			br = new BufferedReader(new FileReader(f2));
			int counter = 0;
			while((str = br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				stk.nextToken();// skip query id
				buffer.append((String)vecAP.get(counter++) + " " + stk.nextToken() + EOL);
			}
			br.close();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
			System.out.println("Data saved in file " + fOut.getPath());
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void sortMatlabFormat(File f, int sortBy){
		try{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = null;
			ArrayList list = new ArrayList();
			while ((str=br.readLine())!=null){
				if (str.trim().length() == 0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				double[] data = new double[stk.countTokens()];
				for (int i = 0; i < data.length; i++)
					data[i] = Double.parseDouble(stk.nextToken());
				list.add(data);
			}
			br.close();
			int[] order = BubbleSort.getOrder((double[])((double[])list.get(sortBy)).clone());
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < list.size(); i++){
				double[] data = (double[])list.get(i);
				for (int j = 0; j < data.length; j++)
					buf.append(data[order[j]] + " ");
				buf.append(EOL);
			}
			bw.write(buf.toString());
			bw.close();
		}
		catch(IOException ioe){
           	ioe.printStackTrace();
           	System.exit(1);
        }
	}
	
	public void getSpearmanCorrelation(File f1, int pos1, File f2, int pos2){
		Vector vec1 = new Vector();
		SystemUtility.readStringColumn(f1, pos1, vec1);
		Vector vec2 = new Vector();
		SystemUtility.readStringColumn(f2, pos2, vec2);
		double[] x = new double[vec1.size()];
		double[] y = new double[vec2.size()];
		for (int i = 0; i < x.length; i++){
			x[i] = Double.parseDouble((String)vec1.get(i));
			y[i] = Double.parseDouble((String)vec2.get(i));
		}
		SpearmanCorrelation spear = new SpearmanCorrelation(x, y);
		double corr = spear.getCorrelation();
		System.out.println("SpearmanCorrelation: " + corr);
	}
	public void getCorrelation(File f1, int pos1, File f2, int pos2){
		Vector vec1 = new Vector();
		SystemUtility.readStringColumn(f1, pos1, vec1);
		Vector vec2 = new Vector();
		SystemUtility.readStringColumn(f2, pos2, vec2);
		double[] x = new double[vec1.size()];
		double[] y = new double[vec2.size()];
		for (int i = 0; i < x.length; i++){
			x[i] = Double.parseDouble((String)vec1.get(i));
			y[i] = Double.parseDouble((String)vec2.get(i));
		}
		double corr = Statistics.correlation(x, y);
		System.out.println("Correlation: " + corr);
	}
	
	public void TRECifyResultFile(String fResultFilename, String tag){
		try{
			File fIn = new File(fResultFilename);
			StringBuffer buffer = new StringBuffer();
			String str = null;
			BufferedReader br = new BufferedReader(new FileReader(fIn));
			while ((str=br.readLine())!=null){
				str = str.trim();
				String newStr = str.substring(0, str.lastIndexOf(' ')+1);
				newStr += (tag+ApplicationSetup.EOL);
				buffer.append(newStr);
			}
			br.close();
			File fOut = new File(fResultFilename.substring(0, fResultFilename.lastIndexOf(
					ApplicationSetup.FILE_SEPARATOR)+1), tag);
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
			System.err.println("Data saved in file "+fOut.getPath());
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void writeMatlabFormat(File f){
		File fDat = null;
        try{
	        // loads the data in the given file
            BufferedReader br = new BufferedReader(new FileReader(f));
            
            String buf = br.readLine();
            StringTokenizer stk = new StringTokenizer(buf);
            int dimension = stk.countTokens();
            
            
            StringBuffer[] sb = new StringBuffer[dimension];
            for (int i=0; i<dimension; i++)
            	sb[i] = new StringBuffer();
            while(buf!=null){
                if (buf.length()!=0){
                    stk = new StringTokenizer(buf, " ");
                    for (int i=0; i<dimension; i++)
                    	sb[i].append(stk.nextToken()+" ");
                }
                buf = br.readLine();
            }
            br.close();

            // obtains the name for the output file
            String sFilename = f.getPath();
//            if (sFilename.lastIndexOf('.')!=-1)
//                sFilename=(sFilename.substring(0, sFilename.lastIndexOf('.'))+".mtl");
//            else
//                sFilename+=".mtl";
            if (sFilename.lastIndexOf('.')!=-1)
                sFilename=(sFilename.substring(0, sFilename.lastIndexOf('.')));
            
            fDat = new File(sFilename);

            // output data in Matlab compatible format
            BufferedWriter bwDat = new BufferedWriter(
                new FileWriter(fDat));
            for (int i=0; i<dimension; i++)
            	bwDat.write(sb[i].toString()+ApplicationSetup.EOL);    	
            /**
            bwDat.write(sParameter); bwDat.newLine();
            bwDat.write(sAP); bwDat.newLine();
            bwDat.write(sFQ)
            */
            bwDat.close();
            System.out.println("Data saved in file " + sFilename);
        }
        catch(IOException ioe){
           	ioe.printStackTrace();
           	System.exit(1);
        }
	}
	
	
	
	/*public static void randomPromote(String resultFilename, String qrelsFilename){
		TRECResultsInMemory results = new TRECResultsInMemory(resultFilename);
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		try{
			int counter = 0;
			while ((new File(resultFilename+"rand"+counter+".res")).exists()){
				counter++;
			}
			String outputFilename = resultFilename+"rand"+counter+".res";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String[] queryids = results.getQueryids();
			for (int i=0; i<queryids.length; i++){
				THashSet<String> retDocnoSet = results.getDocnoSet(queryids[i]);
				String[] relDocnos = qrels.getRelevantDocumentsToArray(queryids[i]);
				THashSet<String> relRetDocnoSet = new THashSet<String>();
				for (int j=0; j<relDocnos.length; j++)
					if (retDocnoSet.contains(relDocnos[j]))
						relRetDocnoSet.add(relDocnos[j]);
				// get retdocnos and scores
				retDocnoSet = null;
				String[] retDocnos = results.getRetrievedDocnos(queryids[i]);
				double[] scores = results.getScores(queryids[i]);
				int n = scores.length;
				int limit = relRetDocnoSet.size()/2;
				//String[] relRetDocnos = (String[])relRetDocnoSet.toArray(new String[relRetDocnoSet.size()]);
				// ramdonly replace relevant documents
				for (int j=0; j<limit; j++){
					int[] ids = {-1, -1};
					while (ids[0]<0){
						int id = (int)(Math.random()*(n-1));
						if (relRetDocnoSet.contains(retDocnos[id]))
							ids[0] = id;
					}
					while (ids[1]<0&&ids[0]!=ids[1]){
						int id = (int)(Math.random()*(n-1));
						if (relRetDocnoSet.contains(retDocnos[id]))
							ids[1] = id;
					}
					// swap
					String tempDocno = ""+retDocnos[ids[1]];
					//double tempScore = scores[ids[1]];
					retDocnos[ids[1]] = ""+retDocnos[ids[0]];
					//scores[ids[1]] = scores[ids[0]];
					retDocnos[ids[0]]=""+tempDocno;
					//scores[ids[0]]=tempScore;
					ids=null;
				}
				// output
				int[] ids = new int[scores.length];
				for (int j=0; j<ids.length; j++)
					ids[j] = j;
				short[] dummy = new short[scores.length];
				Arrays.fill(dummy, (short)1);
				uk.ac.gla.terrier.utility.HeapSort.descendingHeapSort(scores, ids, dummy);
				for (int j=0; j<scores.length; j++)
					bw.write(queryids[i]+ " Q0 "+retDocnos[ids[j]]+" "+j+" "+scores[j]+" rand"+EOL);
			}
			bw.close();
			System.out.println("New result file saved in "+outputFilename);
		}catch(IOException ioe){
           	ioe.printStackTrace();
           	System.exit(1);
        }
		
		results = null;
		qrels = null;
	}*/
	
	public static void removeNonRelevantDocs(String resultFilename, String qrelsFilename){
		String outputFilename = resultFilename+".rel";
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		try{
			BufferedReader br = Files.openFileReader(resultFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String str = null;
			while ((str=br.readLine())!=null){
				String[] tokens = str.split(" ");
				if (qrels.isRelevant(tokens[0], tokens[2]))
					bw.write(str+ApplicationSetup.EOL);
			}
			br.close();
			bw.close();
		}catch(IOException ioe){
           	ioe.printStackTrace();
           	System.exit(1);
        }
		System.out.println("Done. Results saved in file "+outputFilename);
	}
	

	public static void main(String[] args) {
		TestResults app = new TestResults();
		// -r -o -orgres <option 10 ap c> <output filename>
		if (args[2].equalsIgnoreCase("-orgres")){
			String option = args[3];
			File fOut = new File(ApplicationSetup.TREC_RESULTS, args[4]);
			app.writeAveragePrecisions(option, fOut);
		}
		// -r -o -concatap f1 f2 fout
		if (args[2].equalsIgnoreCase("-concatap")){
			String resultDir = ApplicationSetup.TREC_RESULTS;
			File f1 = new File(resultDir, args[3]);
			File f2 = new File(resultDir, args[4]);
			File fOut = new File(resultDir, args[5]);
			app.concatAveragePrecision(f1, f2, fOut);
		}
		
		if (args[2].equalsIgnoreCase("-prnmtl")){
			File fIn = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			app.writeMatlabFormat(fIn);
		}
		
		if (args[2].equalsIgnoreCase("-sortmtl")){
			File f = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			int sortBy = Integer.parseInt(args[4]);
			app.sortMatlabFormat(f, sortBy);
		}
		
		if (args[2].equalsIgnoreCase("-sort")){
			File f = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			int sortBy = Integer.parseInt(args[4]);
			app.writeMatlabFormat(f);
			app.sortMatlabFormat(f, sortBy);
			app.writeMatlabFormat(f);
		}
		// -r -o -sc f1 pos1 f2 pos2
		if (args[2].equalsIgnoreCase("-sc")){
			File f1 = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			int pos1 = Integer.parseInt(args[4]);
			File f2 = new File(ApplicationSetup.TREC_RESULTS, args[5]);
			int pos2 = Integer.parseInt(args[6]);
			app.getSpearmanCorrelation(f1, pos1, f2, pos2);
		}
		// -r -o -trecify <relative filename> <tag>
		if (args[2].equalsIgnoreCase("-trecify")){
			String filename = args[3];
			String tag = args[4];
			app.TRECifyResultFile(filename, tag);
		}
//		 -res -xv [filenames]... [baseline] [outputFile]
		if (args[1].equalsIgnoreCase("-xv")){
			Vector vecFiles = new Vector();
			for (int i = 2; i < args.length-2; i++)
				vecFiles.addElement(new File(ApplicationSetup.TREC_RESULTS, args[i]));
			File baseline = new File(ApplicationSetup.TREC_RESULTS, args[args.length-2]);
			File fout = new File(ApplicationSetup.TREC_RESULTS, args[args.length-1]);
			SystemUtility.crossValidateResults(vecFiles, baseline, fout);
		}else if (args[1].equals("--removenonrelevantdocs")){
			// -res --removenonrelevantdocs resultfilename qrelsfilename
			TestResults.removeNonRelevantDocs(args[2], args[3]);
		}/*else if (args[1].equals("--randompromote")){
			// -res --randompromote <resultFilename> <qresFilename>
			TestResults.randomPromote(args[2], args[3]);
		}*/
	}
}
