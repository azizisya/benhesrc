/*
 * Created on 2005-1-12
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

import gnu.trove.TDoubleHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestSIGIR05 {
	
	protected String EOL = ApplicationSetup.EOL;
	
	public void mergeAveragePrecision(File f1, File f2, File fOut, int index){
		int positiveSign = 0;
		int negativeSign = 0;
		int tie = 0;
		try{
			Vector vecAP = new Vector();
			BufferedReader br = new BufferedReader(new FileReader(f1));
			String str = null;
			while((str = br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				int counter = 0;
				StringTokenizer stk = new StringTokenizer(str);
				while (counter!=index){
					stk.nextToken();
					counter++;
				}
				vecAP.addElement(stk.nextToken());
			}			
			br.close();
			
			StringBuffer buffer = new StringBuffer();
			int qcounter = 0;
			br = new BufferedReader(new FileReader(f2));
			TDoubleHashSet bestAP = new TDoubleHashSet();
			while((str = br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				int counter = 0;
				StringTokenizer stk = new StringTokenizer(str);
				while (counter!=index){
					stk.nextToken();
					counter++;
				}
				String strAP1 = (String)vecAP.get(qcounter);
				String strAP2 = stk.nextToken();
				buffer.append(strAP1 + " " + strAP2 + EOL);
				double AP1 = Double.parseDouble(strAP1);
				double AP2 = Double.parseDouble(strAP2);
				if (AP2 > AP1)
					positiveSign++;
				else if (AP2 < AP1)
					negativeSign++;
				else tie++;
				bestAP.add(Math.max(AP1, AP2));
				qcounter++;
			}
			br.close();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
			System.out.println("Data saved in file " + fOut.getPath());
			System.err.println("+ " + positiveSign + ", - " + negativeSign + ", = " +
					tie);
			double[] APs = bestAP.toArray();
			double bestMAP = Statistics.mean(APs);
			System.err.println("Best MAP: " + Rounding.toString(bestMAP, 4));
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	
	
	public void mergeMRR(File f1, File f2, File fOut){
	   try{
	       BufferedReader br1 = new BufferedReader(new FileReader(f1));
	       BufferedReader br2 = new BufferedReader(new FileReader(f2));
	       
	       StringBuffer buf = new StringBuffer();
	       StringTokenizer stk1= new StringTokenizer(br1.readLine());
	       StringTokenizer stk2= new StringTokenizer(br2.readLine());
	       int counter = 0;
	       boolean all = false;
	       int plusCounter = 0;
	       int minusCounter = 0;
	       int equalCounter = 0;
	       while (stk1.hasMoreTokens()||stk2.hasMoreTokens()){
	           String str1 = stk1.nextToken();
	           String str2 = stk2.nextToken();
	           counter++;
	           if (counter==1){
	               if (str1.equalsIgnoreCase("all"))
	                   all = true;
	                else
	                all = false;
	               continue;
               }
               if (counter==2){
                    continue;
               }
               if (counter==3){
                    counter=0;
                    if (!all){
                    	buf.append(str1+" "+str2+EOL);
                    	double MRR1 = Double.parseDouble(str1);
                    	double MRR2 = Double.parseDouble(str2);
                    	if (MRR1>MRR2)
                    		plusCounter++;
                    	else if (MRR1<MRR2)
                    		minusCounter++;
                    	else
                    		equalCounter++;
                    }
               }
           }
	       br1.close();
	       br2.close();
	       BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
	       bw.write(buf.toString());
	       bw.close();
	       System.out.println("Merged data writted in file "+fOut.getPath());
	       System.out.println("+ "+plusCounter+", - "+minusCounter +
	    		   ", = "+equalCounter);
       }catch(IOException ioe){
        ioe.printStackTrace();
        System.exit(1);
       }
    }

	public static void main(String[] args) {
		TestSIGIR05 app = new TestSIGIR05();
		if (args[1].equalsIgnoreCase("-m")){
//			if (args[2].indexOf('/')>0){
//				args[2]=args[2].substring(args[2].lastIndexOf('/')+1, args[2].length());
//			}
//			if (args[3].indexOf('/')>0){
//			                                args[3]=args[3].substring(args[3].lastIndexOf('/')+1, args[3].length());
//					
//							                        }
			//File f1 = new File(ApplicationSetup.TREC_RESULTS, args[2]);
			//File f2 = new File(ApplicationSetup.TREC_RESULTS, args[3]);
			File f1 = new File(args[2]); File f2 = new File(args[3]);
			
			int index = 0;
            if (!args[4].equalsIgnoreCase("-n")){ 
                index = Integer.parseInt(args[4]);
			    // File fOut = new File(ApplicationSetup.TREC_RESULTS, args[5]);
                File fOut = new File(args[5]);
			     app.mergeAveragePrecision(f1, f2, fOut, index);
			     //TestResults ts = new TestResults();
			    // ts.getSpearmanCorrelation(f1, index, f2, index);
			    // ts.getCorrelation(f1, index, f2, index);
			}
			else{
			     File fOut = new File(ApplicationSetup.TREC_RESULTS, args[5]);
			     app.mergeMRR(f1, f2, fOut);
            }
		}
	}
}
