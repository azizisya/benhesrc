/*
 * Created on 02-Jul-2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.utility.ApplicationSetup;

public class ChopResultFile {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Choping result file "+args[1]);
		File f = new File(args[1]);
		int size = Integer.parseInt(args[2]);
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
					if (counter<size){
						counter++;
						buf.append(str+ApplicationSetup.EOL);
					}
				}
				else {
					counter=1;
					preid = queryid;
					buf.append(str+ApplicationSetup.EOL);
				}
			}
			br.close();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(fOut));
			bw.write(buf.toString());
			bw.close();
			System.out.println("Output saved in file "+fOut.getPath());
		}
		catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}

	}

}
