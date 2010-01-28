/*
 * Created on 02-Jul-2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class CheckResultFile {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Checking result file "+args[1]);
		File f = new File(args[1]);
		THashSet queryids = new THashSet();
		File fOut = new File(ApplicationSetup.TREC_RESULTS, "out.res");
		StringBuffer buf = new StringBuffer();
		try{
			BufferedReader br = new BufferedReader(new FileReader(f));
			String str = null;
			String preid = "";
			int counter = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				if (queryid.equals(preid)){
					counter++;
					buf.append(str+ApplicationSetup.EOL);
				}
				queryids.add(queryid);
				stk.nextToken();
				String docno = stk.nextToken();
				String rank = stk.nextToken();
				double score = Double.parseDouble(stk.nextToken());
				if (score <= 0)
					System.out.println(str);
			}
			br.close();
			System.out.println("number of queries: "+queryids.size());
		}
		catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
	}

}
