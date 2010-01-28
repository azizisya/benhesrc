/*
 * Created on 18 Jan 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import uk.ac.gla.terrier.links.MetaServer4a;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntStack;
import gnu.trove.TObjectIntHashMap;

public class MetaData {

	static void allignMetaData(String docnoListFilename, 
			String metaFilename, String metaOutputFilename){
		THashMap<String, String> metaMap = new THashMap<String, String>();
		String EOL = ApplicationSetup.EOL;
		// load metadata
		try{
			BufferedReader br = Files.openFileReader(metaFilename);
			String str = null;
			System.out.print("Loading meta data file "+metaFilename+"...");
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				String[] tokens = str.split(" ");
				String docno = tokens[0];
				if (tokens.length==1){
					// do nothing
				}else{
					String meta = str.substring(str.indexOf(' '), str.length());
					metaMap.put(docno, meta);
				}
			}
			br.close();
			System.out.println("Done. "+metaMap.size()+" entries loaded.");
			System.out.print("Parsing docno list "+docnoListFilename+"...");
			br = null;
			br = Files.openFileReader(docnoListFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(metaOutputFilename);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				String meta = metaMap.get(str);
				if (meta!=null)
					bw.write(str+" "+meta+EOL);
				else
					bw.write(str+EOL);
			}
			br.close(); br = null;
			bw.close(); bw = null;
			System.out.println("Done. Alligned meta data saved in file "+metaOutputFilename);
			metaMap.clear(); metaMap = null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	
	
	static void allignGenoMetaData(String docnoListFilename, 
			String metaFilename, String metaOutputFilename){
		String EOL = ApplicationSetup.EOL;
		TIntIntHashMap metaMap = new TIntIntHashMap(); // map from position in index to pmid
		TIntStack pmidStack = new TIntStack();
		// load docno list
		try{
			BufferedReader br = Files.openFileReader(docnoListFilename);
			String str = null;
			System.out.print("Loading docno list from "+docnoListFilename+"...");
			int counter = 0;
			while ((str=br.readLine())!=null)
				metaMap.put(counter++, Integer.parseInt(str.trim()));
			br.close(); br = null; str = null;
			System.out.println("Done. "+metaMap.size()+" docnos loaded.");
			// dump pmids to stack
			for (int i=counter-1; i>=0; i--){
				pmidStack.push(metaMap.get(i));
			}
			metaMap.clear(); metaMap = null; System.gc();
			
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(metaOutputFilename);
			br = Files.openFileReader(metaFilename);
			TIntObjectHashMap<String> pmidMetaMap = new TIntObjectHashMap<String>();
			String meta = null;
			System.out.print("Parsing docno list "+docnoListFilename+"...");
			while (pmidStack.size()!=0){
				int pmid = pmidStack.pop();
				if ((meta=pmidMetaMap.get(pmid))!=null){
					if (meta.trim().split(" ").length>1)
						bw.write(meta+EOL);
					else
						bw.write(meta+" N/A"+EOL);
					pmidMetaMap.remove(pmid);
				}else{
					while ((meta=br.readLine())!=null){
						String[] tokens = meta.trim().split(" ");
						int current_pmid = Integer.parseInt(tokens[0]);
						if (current_pmid == pmid){
							if (tokens.length>1)
								bw.write(meta+EOL);
							else
								bw.write(meta+" N/A"+EOL);
							break;
						}else
							pmidMetaMap.put(current_pmid, str);
						tokens=null;
					}
				}
				if (pmidStack.size()%10000==0)
					System.gc();
			}
			br.close(); bw.close();
			System.out.println("Done. Alligned meta data saved in file "+metaOutputFilename);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
		
	}
	
	static void allignMetaDataOnMetaServer(String oldDocnolistFilename,
			String docnoListFilename, 
			String metaOutputFilename, String metakey){
		// create meta server
		try{
			MetaServer4a server = new MetaServer4a(ApplicationSetup.TERRIER_INDEX_PATH,
					ApplicationSetup.TERRIER_INDEX_PREFIX);
			// map old docnos to docids
			TObjectIntHashMap<String> oldDocMap = new TObjectIntHashMap<String>();
			BufferedReader br = Files.openFileReader(oldDocnolistFilename);
			String docno = null;
			int docid = 0;
			while((docno=br.readLine())!=null){
				oldDocMap.put(docno, docid++);
			}
			br.close();
			br = Files.openFileReader(docnoListFilename);
			docno = null;
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(metaOutputFilename);
			while((docno=br.readLine())!=null){
				docid = oldDocMap.get(docno);
				String metastring = server.getItem(metakey, docid);
				bw.write(docno+" "+metastring+ApplicationSetup.EOL);
			}
			br.close();
			server.close();
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		// read docno list
		// write meta data
		System.out.println("Done. New meta data file saved at "+metaOutputFilename);
	}
	
	static void allignMetaDataOnDisk(String docnoListFilename, 
			String metaFilename, String metaOutputFilename){
		TObjectIntHashMap<String> metaMap = new TObjectIntHashMap<String>();
		String EOL = ApplicationSetup.EOL;
		// load metadata
		try{
			BufferedReader br = Files.openFileReader(metaFilename);
			String str = null;
			System.out.print("Loading meta data file "+metaFilename+"...");
			int lineNumber = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				lineNumber++;
				if (str.length()==0)
					continue;
				String[] tokens = str.split(" ");
				String docno = tokens[0];
				if (tokens.length==1){
					// do nothing
				}else{
					//String meta = str.substring(str.indexOf(' '), str.length());
					metaMap.put(docno, lineNumber);
				}
			}
			br.close();
			System.out.println("Done. "+metaMap.size()+" entries loaded.");
			System.out.print("Parsing docno list "+docnoListFilename+"...");
			int fileLength = lineNumber;
			br = null;
			br = Files.openFileReader(docnoListFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(metaOutputFilename);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				int index = metaMap.get(str);
				String meta = "";
				if (index!=0){
					String[] cmd = {"/usr/local/bin/bash", "-c", ""};
					cmd[2] = (index < fileLength/2)?
							("head -"+index+" "+metaFilename+" | tail -1"):
							("tail -"+(lineNumber-index+1)+" "+metaFilename+" | head -1");
					try{
						 Process p = Runtime.getRuntime().exec(cmd);
						 String line = null;
						 BufferedReader input =
						       new BufferedReader
						         (new InputStreamReader(p.getInputStream()));
						 while ((line = input.readLine()) != null) {
						       meta = line;
						       //System.out.println("line: "+line);
						 }
						 input.close();
					 }catch(IOException e){
						 e.printStackTrace();
					 }
					bw.write(str+" "+meta+EOL);
				}
				else
					bw.write(str+EOL);
			}
			br.close(); br = null;
			bw.close(); bw = null;
			System.out.println("Done. Alligned meta data saved in file "+metaOutputFilename);
			metaMap.clear(); metaMap = null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	static void allignGenoMetaDataOnDisk(String docnoListFilename, 
			String metaFilename, String metaOutputFilename){
		TIntIntHashMap metaMap = new TIntIntHashMap();
		String EOL = ApplicationSetup.EOL;
		// load metadata
		try{
			BufferedReader br = Files.openFileReader(metaFilename);
			String str = null;
			System.out.print("Loading meta data file "+metaFilename+"...");
			int lineNumber = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				lineNumber++;
				if (str.length()==0)
					continue;
				String[] tokens = str.split(" ");
				int pmid = Integer.parseInt(tokens[0]);
				if (tokens.length==1){
					// do nothing
				}else{
					//String meta = str.substring(str.indexOf(' '), str.length());
					metaMap.put(pmid, lineNumber);
				}
			}
			br.close();
			System.out.println("Done. "+metaMap.size()+" entries loaded.");
			System.out.print("Parsing docno list "+docnoListFilename+"...");
			int fileLength = lineNumber;
			br = null;
			br = Files.openFileReader(docnoListFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(metaOutputFilename);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				int pmid = Integer.parseInt(str);
				int index = metaMap.get(pmid);
				String meta = "";
				if (index!=0){
					String[] cmd = {"/usr/local/bin/bash", "-c", ""};
					cmd[2] = (index < fileLength/2)?
							("head -"+index+" "+metaFilename+" | tail -1"):
							("tail -"+(lineNumber-index+1)+" "+metaFilename+" | head -1");
					try{
						 Process p = Runtime.getRuntime().exec(cmd);
						 String line = null;
						 BufferedReader input =
						       new BufferedReader
						         (new InputStreamReader(p.getInputStream()));
						 while ((line = input.readLine()) != null) {
						       meta = line;
						       //System.out.println("line: "+line);
						 }
						 input.close();
					 }catch(IOException e){
						 e.printStackTrace();
					 }
					bw.write(meta+EOL);
					// check if got correct entry
					String pmidStr = meta.substring(0, meta.indexOf(' '));
					if (!pmidStr.equals(str)){
						System.err.println("Error: "+str+", "+meta);
					}
				}
				else
					bw.write(str+EOL);
			}
			br.close(); br = null;
			bw.close(); bw = null;
			System.out.println("Done. Alligned meta data saved in file "+metaOutputFilename);
			metaMap.clear(); metaMap = null;
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args[0].equals("--allignmetadata")){
			MetaData.allignMetaData(args[1], args[2], args[3]);
		}else if (args[0].equals("--allignmetadataondisk")){
			MetaData.allignMetaDataOnDisk(args[1], args[2], args[3]);
		}else if (args[0].equals("--alligngenometadataondisk")){
			MetaData.allignGenoMetaDataOnDisk(args[1], args[2], args[3]);
		}else if (args[0].equals("--alligngenometadata")){
			MetaData.allignGenoMetaData(args[1], args[2], args[3]);
		}else if (args[0].equals("--allignmetadataonserver")){
			MetaData.allignMetaDataOnMetaServer(args[1], args[2], args[3], args[4]);
		}
		else if (args[0].equals("--help")){
			System.out.println("Usage: ");
			System.out.println("--allignmetadata docnolistfilename metadatafilename outputfilename");
			System.out.println("--alligngenometadata docnolistfilename metadatafilename outputfilename");
			System.out.println("--alligngenometadataondisk<deprecated> docnolistfilename metadatafilename outputfilename");
			System.out.println("--allignmetadataonserver olddocnolistfilename docnolistfilename metadataoutputfilename metakey");
		}else if (args[0].equals("--debug")){
			TIntStack stk = new TIntStack();
			stk.push(1); stk.push(2);
			while (stk.size()!=0)
				System.out.println(stk.pop());
			System.out.println(stk.pop());
		}
	}

}
