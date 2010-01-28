/*
 * Created on 14 Sep 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tests;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.terms.PorterStemmer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.TerrierTimer;

public class TestMedline {
	public static String docTag = "<MedlineCitation ";
	
	public static String docEndTag = "</MedlineCitation>";
	
	public static String fileTag = "<MedlineCitationSet>";
	
	public static String fileEndTag = "</MedlineCitationSet>";
	
	public static String EOL = ApplicationSetup.EOL;
	
	public static void romoveDTD(String filename){
		
	}
	
	public static void loadWordlist(String filename, THashSet<String> termSet){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()!=0)
					termSet.add(str);
			}
			br.close();	
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void repairGZipFile(String filename){
		if (!filename.toLowerCase().endsWith(".gz")){
			System.err.println("Filename must ends with .gz. Existing...");
			System.exit(0);
		}
		String bash = ApplicationSetup.getProperty("default.bash", "/usr/local/bin/bash");
		String[] cmd = {bash, "-c", "gzip -d "+filename};
		try{
			 Process p = Runtime.getRuntime().exec(cmd);
			 String line = null;
			 BufferedReader input =
			       new BufferedReader
			         (new InputStreamReader(p.getErrorStream()));
			 while ((line = input.readLine()) != null) {
			       //totalNumberOfDocuments = Integer.parseInt(line.trim());
			       System.err.println(line);
			 }
			 input.close();
		 }catch(IOException e){
			 e.printStackTrace();
		 }
		filename = filename.substring(0, filename.lastIndexOf('.'));
		
		String outputFilename = filename+".fix.gz";
		try{
			BufferedReader br = Files.openFileReader(filename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String str = null;
			String EOL = ApplicationSetup.EOL;
			while ((str=br.readLine())!=null)
				bw.write(str+EOL);
			br.close();
			bw.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static void checkWordlist(String filename){
		THashSet<String> termSet = new THashSet<String>();
		loadWordlist(filename, termSet);
		String[] terms = (String[])termSet.toArray(new String[termSet.size()]);
		for (int i=0; i<terms.length; i++){
			if (terms[i].length()!=terms[i].getBytes().length)
				System.err.println(terms[i]+" "+terms[i].length()+" "+terms[i].getBytes().length);
		}
	}
	
	public static void removeRedundantWords(String list1Filename, String list2Filename){
		try{
			THashSet<String> termSet1 = new THashSet<String>();
			THashSet<String> termSet2 = new THashSet<String>();
			BufferedReader br = Files.openFileReader(list1Filename);
			String str = null;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()!=0)
					termSet1.add(str);
			}
			br.close();
			br = Files.openFileReader(list2Filename);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()!=0)
					termSet2.add(str);
			}
			br.close();
			String[] terms = (String[])termSet1.toArray(new String[termSet1.size()]);
			for (int i=0; i<terms.length; i++){
				if (termSet2.contains(terms[i]))
					System.out.println(terms[i]);
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void tokeniseWordlist(String filename){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String tmpFilename = filename+".tmp";
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(tmpFilename);
			String str = null;
			THashSet<String> termSet = new THashSet<String>();
			
			while ((str=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(str, "+=-/^:,.- ");
				while (stk.hasMoreTokens()){
					String term = stk.nextToken().trim().toLowerCase();
					if (!termSet.contains(term)){
						termSet.add(term);
						bw.write(term+EOL);
					}
				}
			}
			(new File(filename)).delete();
			(new File(tmpFilename)).renameTo(new File(filename));
			bw.close();
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void splitMedlineFiles(String filename, 
			int numberOfParts,
			String outputDir
			){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			TerrierTimer timer = new TerrierTimer();
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()!=0){
					timer.start();
					System.out.print("Splitting file "+str+"...");
					splitMedlineFile(str, numberOfParts, outputDir);
					timer.setBreakPoint();
					System.out.println("Done in "+timer.toStringMinutesSeconds());
				}
			}
			br.close();
		}catch(IOException e){
			 e.printStackTrace();
		}
	}
	
	public static void distributedIndexMerging(String indexRoot, String newIndexPath, String indexPrefix){
		File fIndexRoot = new File(indexRoot);
	}
	
	public static void splitMedlineFile(String filename, 
			int numberOfParts,
			String outputDir
			){
		// determine the number of documents in the file
		int totalNumberOfDocuments = 0;
		String[] cmd = {"/usr/local/bin/bash", "-c", 
				"cat "+filename+" | grep \\<\\/MedlineCitation\\> | wc -l"};
		if (filename.endsWith(".gz"))
			cmd[2] = "cat "+filename+" | grep \\<\\/MedlineCitation\\> | wc -l";
		try{
			 Process p = Runtime.getRuntime().exec(cmd);
			 String line = null;
			 BufferedReader input =
			       new BufferedReader
			         (new InputStreamReader(p.getInputStream()));
			 while ((line = input.readLine()) != null) {
			       totalNumberOfDocuments = Integer.parseInt(line.trim());
			       //System.out.println("line: "+line);
			 }
			 input.close();
		 }catch(IOException e){
			 e.printStackTrace();
		 }
		
		// determine the number of documents in each part
		int[] numberOfDocumentsPerPart = new int[numberOfParts];
		int average = totalNumberOfDocuments/numberOfParts;
		//System.out.println("totalNumberOfDocuments: "+totalNumberOfDocuments);
		//System.out.println("average: "+average);
		Arrays.fill(numberOfDocumentsPerPart, average);
		numberOfDocumentsPerPart[numberOfParts-1] = 
				totalNumberOfDocuments - average*(numberOfParts-1);
		// start splitting
		try{
			BufferedReader br = Files.openFileReader(filename);
			String filenamePrefix = null;
			if (filename.endsWith(".gz"))
				filenamePrefix=outputDir+filename.substring(filename.lastIndexOf('/'), filename.lastIndexOf('.')+1);
			else
				filenamePrefix=outputDir+filename.substring(filename.lastIndexOf('/'), filename.length());
			int suffixLength = (""+numberOfParts).length();
			String str = null;
			String firstLine = br.readLine();
			// read until reaches fileTag
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.startsWith(fileTag))
					break;
				else if (str.startsWith(docTag)){
					firstLine = "";
					break;
				}
			}
				
			for (int i=0; i<numberOfParts; i++){
				String suffix = ""+i;
				int initialLength = suffix.length();
				for (int j=0; j<suffixLength-initialLength; j++)
					suffix = "0"+suffix;
				String outputFilename = filenamePrefix+suffix+".gz";
				boolean inCollection = false;
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
				//if (firstLine.trim().length()>0)
					//bw.write(firstLine+EOL);
				bw.write(fileTag+EOL);
				int counter = 0;
				while ((str=br.readLine())!=null){
					str = str.trim();
					if (str.startsWith(docEndTag)){
						// write the line, count++
						bw.write(str+EOL);
						counter++;
						if (counter == numberOfDocumentsPerPart[i]){
							bw.write(fileEndTag);
							break;
						}
					} else if (str.startsWith(fileEndTag)){
						bw.write(str);
						break;
					} else{
						bw.write(str+EOL);
					}
				}
				bw.close();
			}
			
			br.close();
		}catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		}
	}
	
	public static void createMeshIndex(String meshFilename){
		String outputfilename = null;
		if (meshFilename.endsWith(".gz"))
			outputfilename = meshFilename.substring(0, meshFilename.lastIndexOf('.'))+".idx.gz";
		else
			outputfilename = meshFilename+".idx.gz";
		Manager manager = new Manager(null);
		THashMap<String,THashSet<String>> entryMap = new THashMap<String,THashSet<String>>();
		THashSet<String>[] entries = null;
		try{
			// load
			BufferedReader br = Files.openFileReader(meshFilename);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputfilename);
			String line = null;
			String[] keys = null;
			while ((line=br.readLine())!=null){
				line=line.trim().toLowerCase();
				if (line.length()==0)
					continue;
				if (line.startsWith("mh =")){
					if (keys!=null){
						for (int i=0; i<keys.length; i++){
							entryMap.put(keys[i], entries[i]);
						}
					}
					String meshheading =  line.substring(line.indexOf('=')+1, line.length());
					StringBuilder sbd = new StringBuilder();
					for (int i=0; i<meshheading.length(); i++){
						char ch = meshheading.charAt(i);
						if (Character.isLetterOrDigit(ch)||Character.isSpaceChar(ch))
							sbd.append(ch);
						else
							sbd.append(' ');
					}
					meshheading = sbd.toString();
					//System.out.println("heading:"+meshheading);
					StringTokenizer stk = new StringTokenizer(meshheading, ", ");
					THashSet<String> keyStrings = new THashSet<String>();
					while (stk.hasMoreTokens()){
						String str = manager.pipelineTerm(stk.nextToken().trim());
						if (str!=null)
							keyStrings.add(str);
					}
					keys = (String[])keyStrings.toArray(new String[keyStrings.size()]);
					entries = new THashSet[keys.length];
					for (int i=0; i<entries.length; i++){
						if (entryMap.containsKey(keys[i]))
							entries[i] = (THashSet<String>)entryMap.get(keys[i]);
						else
							entries[i] = new THashSet<String>();
					}
				}else if(line.startsWith("entry =")){
					line =  line.substring(line.indexOf('=')+1, line.length());
					StringBuilder sbd = new StringBuilder();
					for (int i=0; i<line.length(); i++){
						char ch = line.charAt(i);
						if (Character.isLetterOrDigit(ch)||Character.isSpaceChar(ch))
							sbd.append(ch);
						else
							sbd.append(' ');
					}
					line = sbd.toString();
					StringTokenizer stk = new StringTokenizer(line, ",| ");
					while (stk.hasMoreTokens()){
						String entry = manager.pipelineTerm(stk.nextToken().trim());
						if (entry!=null){
							for (int i=0; i<entries.length; i++){
								if (!entry.equals(keys[i]))
									entries[i].add(entry);
							}
						}
					}
				}
			}
			if (keys!=null){
				for (int i=0; i<keys.length; i++){
					entryMap.put(keys[i], entries[i]);
				}
			}
			br.close();
			// output
			Object[] meshHeaders = entryMap.keySet().toArray();
			for (int i=0; i<meshHeaders.length; i++){
				THashSet<String> entry = entryMap.get(meshHeaders[i]);
				Object[] entryStrings = entry.toArray();
				if (entryStrings.length==0)
					continue;
				bw.write((String)meshHeaders[i]+" ");
				for (int j=0; j<entryStrings.length; j++)
					bw.write((String)entryStrings[j]+" ");
				bw.write(ApplicationSetup.EOL);
			}
			bw.close();
		}catch(IOException e){
			 e.printStackTrace();
			 System.exit(1);
		}
		System.out.println("Mesh index saved in file "+outputfilename);
	}
	
	static public void main(String[] args){
		if (args[1].equals("--splitfile")){
			// -m --splitfile <filename> <#parts> <outdir>
			splitMedlineFile(args[2], Integer.parseInt(args[3]), args[4]);
		}else if(args[1].equals("--tokenisewordlist")){
			// -m --tokenisewordlist <filename>
			tokeniseWordlist(args[2]);
		}else if (args[1].equals("--splitfiles")){
			// -m --splitfiles <filename> <#parts> <outdir>
			splitMedlineFiles(args[2], Integer.parseInt(args[3]), args[4]);
		}else if (args[1].equals("--removeredundantterms")){
			// -m --removeredundantterms <filename1> <filename2>
			removeRedundantWords(args[2], args[3]);
		}else if (args[1].equals("--checkwordlist")){
			// -m --checkwordlist <filename>
			checkWordlist(args[2]);
		}else if (args[1].equals("--repairgzipfile")){
			// -m --gzipfilename
			repairGZipFile(args[2]);
		}else if (args[1].equals("--createmeshindex")){
			// -m --createmeshindex
			createMeshIndex(args[2]);
		}
	}
}
