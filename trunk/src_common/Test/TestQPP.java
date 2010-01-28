/*
 * Created on 2005-5-4
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package Test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.matching.BufferedMatching;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.BasicQuery;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.OnelineTRECQuery;
import uk.ac.gla.terrier.structures.QueryFeatures;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Rounding;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestQPP {
	protected String EOL = ApplicationSetup.EOL;
	
	public void computeQueryFeatures(File fIn, File fOut){
		Manager manager = new Manager(Index.createIndex());
		BufferedMatching matching = new BufferedMatching(manager.getIndex());
		try{
			BufferedReader br = new BufferedReader(new FileReader(fIn));
			String str = null;
			StringBuffer buffer = new StringBuffer(); 
			int counter = 1;
			while ((str=br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				BasicQuery query = new BasicQuery(str, "PP"+counter, manager);
				query.dumpQuery();
				System.err.println("<<<<<<<<<<<query " + query.getQueryNumber());
				QueryFeatures qf = new QueryFeatures(query, matching);
				double[] doubleQf = qf.getFeatureVector();
				buffer.append(query.getQueryNumber() + ": " + str + ",");
				for (int i = 0; i < doubleQf.length; i++)
					buffer.append(" " + Rounding.toString(doubleQf[i], 6));
				buffer.append(EOL);
				counter++;
			}
			br.close();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void extractQfAgainstAP(int index, File fAP, File fQf, File fOut){
		Hashtable tableAP = new Hashtable();
		// read average precision
		try{
			BufferedReader br = new BufferedReader(new FileReader(fAP));
			String str = null;
			while ((str=br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				String AP = stk.nextToken();
				tableAP.put(queryid, AP);
			}
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		StringBuffer buffer = new StringBuffer();
		//	read query feature
		try{
			BufferedReader br = new BufferedReader(new FileReader(fQf));
			String str = null;
			while ((str=br.readLine()) != null){
				if (str.trim().length() == 0)
					continue;
				String queryid = str.substring(0, str.indexOf(':'));
				//System.out.println("queryid: " + queryid);
				String qfString = str.trim().substring(str.indexOf(',')+1, str.length());
				StringTokenizer stk = new StringTokenizer(qfString);
				String qf = new String();
				for (int i = 0; i <= index; i++)
					qf = stk.nextToken();
				String AP = (String)tableAP.get(queryid);
				if (AP != null)
					buffer.append(qf + " " + AP + EOL);
			}
			br.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		//	write output
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Data saved in file " + fOut.getPath() + "." + 
				EOL + "Finished.");
	}
	
	public void computeTRECQueryFeatures(File fOut){
		Manager manager = new Manager(Index.createIndex());
		BufferedMatching matching = new BufferedMatching(manager.getIndex());
		try{
			TRECQuery queries = new TRECQuery();
			if (queries.getNumberOfQueries() == 0){
				System.err.println("Queries are not in TREC format. Loading " +
						"as plain text one-line queries...");
				queries = new OnelineTRECQuery(); 
			}
			StringBuffer buffer = new StringBuffer(); 
			int counter = 1;
			while (queries.hasMoreQueries()){
				BasicQuery query = new BasicQuery(queries.nextQuery(), queries.getQueryId(), manager);
				System.err.println("<<<<<<<<<<<query " + query.getQueryNumber());
				query.dumpQuery();
				QueryFeatures qf = new QueryFeatures(query, matching);
				double[] doubleQf = qf.getFeatureVector();
				buffer.append(query.getQueryNumber() + ": " + query.getQueryTermString() + ",");
				for (int i = 0; i < doubleQf.length; i++)
					buffer.append(" " + Rounding.toString(doubleQf[i], 6));
				buffer.append(EOL);
				counter++;
			}
			
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buffer.toString());
			bw.close();
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void printOption(){
		System.out.println("-apqf <index> <fqf> <fap> <fout> Extract average precision against query feature. " + EOL + 
				"-trecqf <fout> Extract query features for queries in TREC format." + EOL +
				"-qf <fout> Extract query features for queries in plain text.");
	}

	public static void main(String[] args) {
		TestQPP app = new TestQPP();
		
		if (args.length == 0){
			app.printOption();
		}
		
		// -apqf <index> <fqf> <fap> <fout>
		else if (args[0].equalsIgnoreCase("-apqf")){
			int index = Integer.parseInt(args[1]);
			String dir = ApplicationSetup.TREC_RESULTS;
			File fQf = new File(dir, args[2]);
			File fAP = new File(dir, args[3]);
			File fOut = new File(dir, args[4]);
			app.extractQfAgainstAP(index, fAP, fQf, fOut);
		}
		
		// -trecqf <fout>
		else if (args[0].equalsIgnoreCase("-trecqf")){
			File fOut = new File(ApplicationSetup.TREC_RESULTS, args[1]);
			app.computeTRECQueryFeatures(fOut);
		}
		
		// -qf <fout>
		else if (args[0].equalsIgnoreCase("-qf")){
			File fIn = null;
			try{
				BufferedReader br = new BufferedReader(new FileReader(
						new File(ApplicationSetup.TREC_TOPICS_LIST)));
				String str = null;
				while ((str=br.readLine()) != null){
					if (str.trim().length() == 0)
						continue;
					else if (!str.startsWith("#"))
						break;
				}
				br.close();
				fIn = new File(str);
				File fOut = new File(ApplicationSetup.TREC_RESULTS, args[1]);
				app.computeQueryFeatures(fIn, fOut);
			}
			catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
		}
	}
}
