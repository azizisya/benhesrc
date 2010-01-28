/*
 * Created on 9 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class ExtractGenoLegalSpans {

	
	static public void extractLegalSpans(String collectionSpecFilename,
			String legalSpanFilename,
			String outputFilename){
		// map from docnos to filenames
		ArrayList<String> basenameList = new ArrayList<String>();
		THashMap<String, String> nameMap = new THashMap<String, String>(); // mapping from basenames to full filenames
		try{
			System.out.print("Loading collection spec...");
			BufferedReader br = Files.openFileReader(collectionSpecFilename); 
			String str = null;
			while ((str=br.readLine())!=null){
				String basename = str.substring(str.lastIndexOf('/')+1, str.length());
				String pmid = basename.substring(0, basename.indexOf('.'));
				basenameList.add(pmid);
				nameMap.put(pmid, str);
				//System.out.println(pmid+" "+str);
			}
			br.close();
			System.out.println("Done. "+basenameList.size()+" entries loaded.");
			//String[] basenames = (String[])basenameList.toArray(new String[basenameList.size()]);
			//basenameList.clear(); basenameList = null;
			THashMap<String, String> entryMap = new THashMap<String, String>();// mapping from pmids to entries
			br = Files.openFileReader(legalSpanFilename);			
			String currentpmid = "";
			String filename = "";
			StringBuilder sb = new StringBuilder();
			System.out.print("Loading legal spans...");
			while ((str=br.readLine())!=null){
				// for each entry in the legal span file
				StringTokenizer stk = new StringTokenizer(str);
				String pmid = stk.nextToken().trim();
				String offset = stk.nextToken().trim();
				String length = stk.nextToken().trim();
				// if seen pmid, read and save in memory
				if (pmid.equals(currentpmid)){
					sb.append(" "+offset+" "+length);
				}
				// or the first pmid
				else if(currentpmid.length()==0){
					filename = nameMap.get(pmid);
					currentpmid = ""+pmid;
					sb.append(filename);
					sb.append(" "+offset+" "+length);
				}
				// or a new pmid, save cache in the hashmap
				// reinitialise cache
				else{
					entryMap.put(currentpmid, sb.toString());
					//System.out.println(currentpmid+" "+sb.toString());
					sb = null;
					sb = new StringBuilder();
					filename = nameMap.get(pmid);
					currentpmid = ""+pmid;
					sb.append(filename);
					sb.append(" "+offset+" "+length);
				}
			}
			entryMap.put(currentpmid, sb.toString());
			System.out.println(currentpmid+" "+sb.toString());
			System.out.println("Done. "+entryMap.size()+" entries loaded.");
			br.close();
			System.out.print("Writting meta data...");
			String[] pmids = (String[])basenameList.toArray(new String[basenameList.size()]);
			Arrays.sort(pmids);
			// release memory
			basenameList.clear(); basenameList = null; nameMap.clear(); nameMap = null;
			System.gc();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			
			for (int i=0; i<pmids.length; i++){
				bw.write(pmids[i]+" "+entryMap.get(pmids[i])+ApplicationSetup.EOL);
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Saved in file "+outputFilename);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--extract")){
			ExtractGenoLegalSpans.extractLegalSpans(args[1], args[2], args[3]);
		}else if(args[0].equals("--help")){
			System.out.println("Usage:");
			System.out.println("--extract <collectionSpecFilename> <legalSpanFilename> <outputFilename>");
		}
	}

}
