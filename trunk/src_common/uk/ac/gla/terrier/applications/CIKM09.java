package uk.ac.gla.terrier.applications;

import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.statistics.FittingFunction;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;

public class CIKM09 {
	/**
	 * Compute delta, improvement brought by QE.
	 * Each entry looks like the following: 701 22210368 0.1176 0.1255
	 * @param filename
	 */
	public static void computeDelta(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				if (tokens.length!=4)
					continue;
				try{
					double AP = Double.parseDouble(tokens[2]);
					double QEAP = Double.parseDouble(tokens[3]);
					double delta = QEAP-AP;
					buf.append(line + " "+Rounding.toString(delta, 4)+ApplicationSetup.EOL);
				}catch(ArrayIndexOutOfBoundsException e){
					System.err.println(line);
					e.printStackTrace();
					System.exit(1);
				}
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
	 * Get only query-document pairs which have positive improvement brought by QE.
	 * @param filename
	 * @param posOutputFilename
	 */
	public static void getPositivePairs(String filename, String posOutputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				if (!tokens[4].startsWith("-")){
					// non-negative improvement
					buf.append(line+ApplicationSetup.EOL);
				}
			}
			br.close();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(posOutputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Get only query-document pairs which have negaitive improvement brought by QE.
	 * @param filename
	 * @param negOutputFilename
	 */
	public static void getNegativePairs(String filename, String negOutputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				if (tokens[4].startsWith("-")){
					// non-negative improvement
					buf.append(line+ApplicationSetup.EOL);
				}
			}
			br.close();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(negOutputFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Label query-document pairs according to the quaudratic function (see ECIR'09) 
	 * @param filename
	 * @param outputFilename
	 * @param a
	 * @param b
	 * @param c
	 */
	public static void labelPairs3(String filename, String outputFilename,
			double a, double b, double c){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				double expectedDelta = FittingFunction.quadratic(a, b, c, AP);
				/**
				if (delta >= expectedDelta)
					// expectation met
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" -1"+ApplicationSetup.EOL);
					*/
				if (expectedDelta >= 0){
					if (delta>=expectedDelta)
						buf.append(line+" 1"+ApplicationSetup.EOL);
					else if (delta > 0)
						buf.append(line+" 0"+ApplicationSetup.EOL);
					else
						buf.append(line+" -1"+ApplicationSetup.EOL);
				}else{
					if (delta>0)
						buf.append(line+" 1"+ApplicationSetup.EOL);
					else if (delta>expectedDelta)
						buf.append(line+" 0"+ApplicationSetup.EOL);
					else
						buf.append(line+" -1"+ApplicationSetup.EOL);
				}
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
	
	public static void attachInstIds(String predFilename, String idMapFilename){
		try{
			// load inst id map
			BufferedReader br = Files.openFileReader(idMapFilename);
			String line = null;
			TIntObjectHashMap<String> idMap = new TIntObjectHashMap<String>();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				idMap.put(Integer.parseInt(tokens[0]), tokens[1]);
			}
			br.close();
		
			// load predict file and attach ids to buffer
			StringBuffer buf = new StringBuffer();
			br = Files.openFileReader(predFilename);
			while ((line=br.readLine())!=null){
				if (line.trim().length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(line);
				String id = idMap.get(Integer.parseInt(stk.nextToken()));
				buf.append(line+" "+id+ApplicationSetup.EOL);
			}
			br.close();
			// flush to the predict file
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(predFilename);
			bw.write(buf.toString());
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Label query-document pairs according to the quaudratic function (see ECIR'09) 
	 * @param filename
	 * @param outputFilename
	 * @param a
	 * @param b
	 * @param c
	 */
	public static void labelPairs(String filename, String outputFilename,
			double a, double b, double c){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				double expectedDelta = FittingFunction.quadratic(a, b, c, AP);
				
				if (delta >= expectedDelta)
					// expectation met
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" -1"+ApplicationSetup.EOL);
					
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
	 * Label query-document pairs according to the linear function 
	 * @param filename
	 * @param outputFilename
	 * @param a
	 * @param b
	 */
	public static void labelLinearPairs(String filename, String outputFilename,
			double a, double b){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				double expectedDelta = FittingFunction.linear(a, b, AP);
				
				if (delta >= expectedDelta)
					// expectation met
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" -1"+ApplicationSetup.EOL);
					
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
	 * Label query-document pairs according to the delta=QEAP-AP 
	 * @param filename
	 * @param outputFilename
	 */
	public static void directLabel3(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				if (Math.abs(delta)/AP < 0.05)
					buf.append(line+" 0"+ApplicationSetup.EOL);
				else if (delta > 0)
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" -1"+ApplicationSetup.EOL);
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
	 * Label query-document pairs according to the delta=QEAP-AP 
	 * @param filename
	 * @param outputFilename
	 */
	public static void directLabel(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				if (delta > 0)
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" -1"+ApplicationSetup.EOL);
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
	 * Label query-document pairs according to the delta=QEAP-AP 
	 * @param filename
	 * @param outputFilename
	 */
	public static void softLabel(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				if (delta >= 0 )
					// expectation met
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else if (Math.abs(delta)/AP <=0.05 )
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" -1"+ApplicationSetup.EOL);
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
	 * Label query-document pairs according to the delta=QEAP-AP 
	 * @param filename
	 * @param outputFilename
	 */
	public static void hardLabel(String filename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				if (delta <= 0 )
					// expectation met
					buf.append(line+" -1"+ApplicationSetup.EOL);
				else if (Math.abs(delta)/AP <=0.05 )
					buf.append(line+" -1"+ApplicationSetup.EOL);
				else
					// expectation not met
					buf.append(line+" 1"+ApplicationSetup.EOL);
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
	 * Label query-document pairs according to the delta=QEAP-AP 
	 * @param filename
	 * @param outputFilename
	 */
	public static void qrelsLabel(String filename, String qrelsFilename, String outputFilename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			StringBuffer  buf = new StringBuffer();
			TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String qid = tokens[0];
				String docid = tokens[1];
				double AP = Double.parseDouble(tokens[2]);
				double delta = Double.parseDouble(tokens[4]);
				if (qrels.isRelevant(qid, docid))
					buf.append(line+" 1"+ApplicationSetup.EOL);
				else
					buf.append(line+" -1"+ApplicationSetup.EOL);
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
	 * Each line in the labelled file has:
	 * qid docid AP QEAP delta label(1/-1)
	 * converted to:
	 * qid 0 docid 1/0
	 * @param labelFilename
	 * @param outputFilename
	 */
	public static void labelToFeedback(String labelFilename, String outputFilename){
		StringBuilder buf = new StringBuilder();
		try{
			BufferedReader br = Files.openFileReader(labelFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				
				if (tokens[5].equals("1"))
					buf.append(tokens[0]+" 0 "+tokens[1]+" 1"+ApplicationSetup.EOL);
				else
					buf.append(tokens[0]+" 0 "+tokens[1]+" 0"+ApplicationSetup.EOL);
				
				// buf.append(tokens[0]+" 0 "+tokens[1]+" "+tokens[5]+ApplicationSetup.EOL);
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
	 * remove only predicted bad feedback documents with confidence values higher than k.
	 * @param predictionFilename
	 * @param outputFilename
	 * @param k
	 */
	public static void predictionToFeedback(String predictionFilename, String outputFilename, String type, double k){
		try{
			BufferedReader br = Files.openFileReader(predictionFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line.replaceAll("\\+", " ").replaceAll(":", " ")
						// line.replaceAll("\\(", " ").replaceAll("\\)", " ").replaceAll(":", " ").replaceAll("\\+", " ")
						);
				stk.nextToken();// instNo
				stk.nextToken();// classId
				stk.nextToken();// class
				stk.nextToken();// classId
				int predicted = Integer.parseInt(stk.nextToken());
				double confValue = Double.parseDouble(stk.nextToken());
				try{
					String[] qidDocid = stk.nextToken().split("\\.");
					String qid = qidDocid[0]; String docid = qidDocid[1];
					StringBuilder buf = new StringBuilder();
					if (type.equals("neg")){
						// keep only predicted negative documents with confidence values smaller than the given k 
						if (predicted==1){
							buf.append(qid+" 0 "+docid+" 1"+ApplicationSetup.EOL);
						}else if (confValue<k)
							buf.append(qid+" 0 "+docid+" 1"+ApplicationSetup.EOL);
						else
							buf.append(qid+" 0 "+docid+" 0"+ApplicationSetup.EOL);
						bw.write(buf.toString());
						// System.err.println(line);
						// System.err.println(">>>>"+buf.toString());
					}else if (type.equals("pos")){
						// keep only predicted positive documents with confidence values larger than the absolute value of k
						if (predicted == -1)
							bw.write(qid+" 0 "+docid+" 0"+ApplicationSetup.EOL);
						else if (confValue<k)
							bw.write(qid+" 0 "+docid+" 0"+ApplicationSetup.EOL);
						else
							bw.write(qid+" 0 "+docid+" 1"+ApplicationSetup.EOL);
					}else if (type.equals("zero")){
						// k==0
						if (predicted==1){
							bw.write(qid+" 0 "+docid+" 1"+ApplicationSetup.EOL);
						}else
							bw.write(qid+" 0 "+docid+" 0"+ApplicationSetup.EOL);
					}
				}catch(java.lang.ArrayIndexOutOfBoundsException e){
					System.err.println("line: "+line);
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			
			br.close(); bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args){
		if (args[0].equals("--computedelta")){
			// --computedelta filename outputFilename
			CIKM09.computeDelta(args[1], args[2]);
		}else if (args[0].equals("--getpospairs")){
			// --getpospairs filename outputFilename
			CIKM09.getPositivePairs(args[1], args[2]);
		}else if (args[0].equals("--getnegpairs")){
			// --getnegpairs filename outputFilename
			CIKM09.getNegativePairs(args[1], args[2]);
		}
		else if (args[0].equals("--labelpairs"))
			// --labelpairs entryFilename outputFilename a b c
			CIKM09.labelPairs(args[1], args[2], 
					Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]));
		else if (args[0].equals("--label2feedback")){
			// --label2feedback labelFilename outputFilename
			CIKM09.labelToFeedback(args[1], args[2]);
		}else  if (args[0].equals("--directlabel")){
			// --directlabel entryFilename outputFilename
			CIKM09.directLabel(args[1], args[2]);
		}else if (args[0].equals("--predict2feedback")){
			// --predict2feedback predictFilename outputFilename type[neg/pos/qrels] k
			CIKM09.predictionToFeedback(args[1], args[2], args[3], Double.parseDouble(args[4]));
		}else  if (args[0].equals("--softlabel")){
			// --softlabel entryFilename outputFilename
			CIKM09.softLabel(args[1], args[2]);
		}else  if (args[0].equals("--hardlabel")){
			// --hardlabel entryFilename outputFilename
			CIKM09.hardLabel(args[1], args[2]);
		}else if (args[0].equals("--qrelslabel")){
			// --qrelslabel deltaFilename qrelsFilename  outputFilename
			CIKM09.qrelsLabel(args[1], args[2], args[3]);
		}else if (args[0].equals("--neglinearlabel")){
			// --neglinearlabel deltaFilename outputFilename a b
			CIKM09.labelLinearPairs(args[1], args[2], Double.parseDouble(args[3]), Double.parseDouble(args[4]));
		}else if (args[0].equals("--attachids")){
			// --attachids predictFilename idmapFilename
			CIKM09.attachInstIds(args[1], args[2]);
		}
	}
}