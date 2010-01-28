/*
 * Created on May 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package uk.ac.gla.terrier.links;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.DocumentIndexEncoded;

/**
 * @author Vassilis Plachouras
 *
 * creates the outlinks if given the inlinks and vice versa 
 * and replaces the document numbers with the docids. 
 */
public class LinkPreparator {

	static DocumentIndex docIndex;
	public static void main(String[] args) throws IOException {
		if (args.length!=4) {
			System.out.println("Usage: LinkPreparator -swap/noswap -docnos/docids [input filename] [output filename]");
			System.out.println("Example: LinkPreparator swap docids : will swap src/dest and replace docnos with docids (loads of memory)");
			System.out.println("Example: LinkPreparator noswap docids : will not swap src/dest and will output docnos (not much memory)");
			System.exit(1);
		}
		
		boolean swap = false;
		if (args[0].equals("-swap")) {
			swap = true;
		} else if (args[0].equals("-noswap")) {
			swap = false;
		} else {
			System.out.println("Usage: LinkPreparator -swap/noswap -docnos/docids [input filename] [output filename]");
			System.out.println("Example: LinkPreparator swap docids : will swap src/dest and replace docnos with docids (loads of memory)");
			System.out.println("Example: LinkPreparator noswap docids : will not swap src/dest and will output docnos (not much memory)");
			System.exit(1);
		}
		
		boolean docnos = false;
		if (args[1].equals("-docnos")) {
			docIndex = null;
			docnos = true;
		} else if (args[1].equals("-docids")) {
			docIndex = new DocumentIndexEncoded();
			docnos = false;
		}
		
		
		String line = null;
		String[] parts = null;
		String dest = null;
		String source = null;
		int spaceIndex = 0;
		BufferedReader input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[2]))));
		PrintWriter output = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(args[3]))));
		
//		while ((line = input.readLine())!=null) {
//			spaceIndex = line.indexOf(" ");
//			if (swap) {
//				dest = line.substring(0, spaceIndex); 
//				source = line.substring(spaceIndex+1);
//			} else {
//				source = line.substring(0, spaceIndex); 
//				dest = line.substring(spaceIndex+1);				
//			}
//			if (docnos) {
//				output.println(source + " " + dest);
//			} else {
//				output.println(docIndex.getDocumentId(source) + " " + docIndex.getDocumentId(dest));
//			}
//		}
		
		if (swap) {
			if (docnos) {
				while ((line = input.readLine())!=null) {
					parts = line.split("\\s+");
					dest = parts[0]; source = parts[1];	
					output.println(source + " " + dest);
				}
			} else { //no docnos
				while ((line = input.readLine())!=null) {
					parts = line.split("\\s+");
                    dest = parts[0]; source = parts[1];
					output.println(getDocid(source) + " " + getDocid(dest));
				}
			}
		} else { //no swap
			if (docnos) {
				while ((line = input.readLine())!=null) {
					parts = line.split("\\s+");
                    dest = parts[1]; source = parts[0];
					output.println(source + " " + dest);
				}
			} else { //no docnos
				while ((line = input.readLine())!=null) {
					parts = line.split("\\s+");
                    dest = parts[1]; source = parts[0];
					output.println(getDocid(source) + " " + getDocid(dest));
				}
			}
		}
		
		
		
		output.close();
		input.close();
		if (!docnos)
			docIndex.close();
	}

	final static int maxCacheSize = 10000;
	final static TObjectIntHashMap cache = new TObjectIntHashMap(maxCacheSize);
	public static int getDocid(String docno)
	{
		int r;
		if ( (r = cache.get(docno))== 0)
		{
			r = docIndex.getDocumentId(docno);
			cache.put(docno, r);
		}
		else
		{
			if (cache.size() >= maxCacheSize)
			{
				cache.clear();
			}
		}
		return r;
	}
}
