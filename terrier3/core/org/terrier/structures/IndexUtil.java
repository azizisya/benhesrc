/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is IndexUtil.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.structures;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.hadoop.io.Writable;

import org.terrier.structures.postings.IterablePosting;
import org.terrier.utility.ArrayUtils;
import org.terrier.utility.Files;

/** Class with handy utilities for use on an Index.
 * @since 3.0 */
public class IndexUtil {

	private final static String MAIN_USAGE = "Usage: " + IndexUtil.class.getName() + " {--printbitfile|--printlex|--printdocument|--printlist|--printmeta} structureName";
	/** Has some handy utilities for printing various index structures to System.out.
	 * <ul>
	 * <li>
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception
	{
		Index.setIndexLoadingProfileAsRetrieval(false);
		if (args.length < 2)
		{
			System.err.println(MAIN_USAGE);
			return;
		}
		final String cmd = args[0].trim();
		final String structureName = args[1].trim();
		
		//load the index
		final Index index = Index.createIndex();
		if (index == null)
		{
			System.err.println("Index not found: " + Index.getLastIndexLoadError());
			return;
		}
		
		//command loop
		if (cmd.equals("--printbitfile"))
		{
			BitPostingIndexInputStream bpiis = (BitPostingIndexInputStream) index.getIndexStructureInputStream(structureName);
			bpiis.print();
			bpiis.close();
		}
		else if (cmd.equals("--printbitentry"))
		{
			List<BitIndexPointer> pointerList = (List<BitIndexPointer>) index.getIndexStructure(args[2]);
			BitPostingIndex bpi = (BitPostingIndex) index.getIndexStructure(structureName);
			//for every docid on cmdline
			for(int argC=3;argC<args.length;argC++)
			{
				BitIndexPointer pointer = pointerList.get(Integer.parseInt(args[argC]));
				if (pointer.getNumberOfEntries() == 0)
					continue;
				System.out.print(args[argC] + " ");
				IterablePosting ip = bpi.getPostings(pointer);
				while(ip.next() != IterablePosting.EOL)
				{
					System.out.print(ip.toString());
					System.out.print(" ");
				}
				System.out.println();
			}
		}
		else if (cmd.equals("--printlex"))
		{
			LexiconUtil.printLexicon(index, structureName);
		}
		else if (cmd.equals("--printdocument"))
		{
			printDocumentIndex(index, structureName);
		}
		else if (cmd.equals("--printlist"))
		{
			Iterator<? extends Writable> in = (Iterator<? extends Writable>) index.getIndexStructureInputStream(structureName);
			while(in.hasNext())
			{
				System.out.println(in.next().toString());
			}
			IndexUtil.close(in);
		}
		else if (cmd.equals("--printlistentry"))
		{
			List<? extends Writable> list = (List<? extends Writable>) index.getIndexStructure(structureName);
			for(int argC=2;argC<args.length;argC++)
			{
				System.out.println(list.get(Integer.parseInt(args[argC])).toString());
			}
			IndexUtil.close(list);
		}
		else if (cmd.equals("--printmeta"))
		{
			printMetaIndex(index, structureName);
		}
		else
		{
			System.err.println(MAIN_USAGE);
		}
		index.close();
	}
	
	/** Force the specified object into the structure cache of the specified object, 
	 * as the given structure name
	 * @param index Index to operate on
	 * @param structureName which structure name to use
	 * @param structure which object to put into the structure cache
	 */
	public static void forceStructure(Index index, String structureName, Object structure)
	{
		index.structureCache.put(structureName, structure);
	}
	
	/** Reopen an existing index */
	public static Index reOpenIndex(Index index) throws IOException
	{
		Index rtr = null;
		String path = index.getPath();
		String prefix = index.getPrefix();
		index.close();
		rtr = Index.createIndex(path, prefix);
		return rtr;
	}
	
	/** Returns a list of the structures in the given index */
	public static String[] getStructures(Index index)
	{
		List<String> rtr = new ArrayList<String>();
		for(Object o : index.getProperties().keySet())
		{
			String key = (String)o;
			if (key.matches("index\\..+\\.class"))
			{
				key = key.replaceAll("index.", "");
				key = key.replaceFirst(".class", "");
				rtr.add(key);
			}
		}
		return rtr.toArray(new String[0]);
	}
	
	/** Move an index from one location to another */
	public static void renameIndex(String srcPath, String srcPrefix, String dstPath, String dstPrefix)
		throws IOException
	{
		final String actualPrefix = srcPrefix +'.';
		for (String filename : Files.list(srcPath))
		{
			if (filename.startsWith(actualPrefix))
			{
				final String newFilename = filename.replaceFirst(srcPrefix, dstPrefix);
				Files.rename(srcPath + "/" + filename, dstPath+"/"+ newFilename);
			}
		}
	}
	
	/** Delete an existing index */
	public static void deleteIndex(String path, String prefix)
		throws IOException
	{
		final String actualPrefix = prefix +'.';
		String[] files = Files.list(path);
		if (files == null)
			return;
		for (String filename : files)
		{
			if (filename.startsWith(actualPrefix))
			{
				Files.delete(path + "/" + filename);
			}
		}
	}
	
	/** Print the contents of the document index */
	@SuppressWarnings("unchecked")
	public static void printDocumentIndex(Index index, String structureName) throws IOException
	{
		Iterator<DocumentIndexEntry> iterator = (Iterator<DocumentIndexEntry>)index.getIndexStructureInputStream(structureName);
		int docid =0;
		while(iterator.hasNext())
		{
			DocumentIndexEntry die = iterator.next();
			System.out.println(docid +": " + die.toString());
			docid++;
		}
		close(iterator);
	}
	
	/** Delete the named structure from the specified index.
	 * Deletes files as well.
	 * @param index - index to operate on
	 * @param structureName name of structure to delete
	 * @return true if structure was found and deleted, false otherwise
	 */
	public static boolean deleteStructure(Index index, String structureName) throws IOException
	{
		boolean found = false;
		List<String> toRemove = new ArrayList<String>();
		for(Object o : index.getProperties().keySet())
		{
			String key = (String)o;
			if (key.startsWith("index."+structureName + "."))
			{
				toRemove.add(key);
				found = true;
			}			
		}
		for(String key : toRemove)
			index.getProperties().remove(key);
		
		for(String file : Files.list(index.getPath()))
		{
			if (file.startsWith(index.getPrefix() + "." + structureName + "."))
			{
				Files.delete(index.getPath() + "/" + file);
			}
		}
		return found;
	}
	
	/** Copies an index structure from one index to another.
	 * @param sourceIndex
	 * @param destIndex
	 * @param sourceStructureName
	 * @param destinationStructureName
	 * @throws IOException if an IO problem occurs
	 */
	public static boolean copyStructure(Index sourceIndex, Index destIndex, String sourceStructureName, String destinationStructureName) throws IOException
	{
		boolean found = false;
		for(Object o : sourceIndex.getProperties().keySet())
		{
			String key = (String)o;
			if (key.startsWith("index."+sourceStructureName + "."))
			{
				destIndex.setIndexProperty(
						key.replaceFirst("^index\\."+sourceStructureName + "\\.", 
						"index." + destinationStructureName + "."), sourceIndex.getProperties().getProperty(key));
				found = true;
			}			
		}
		for(String file : Files.list(sourceIndex.getPath()))
		{
			if (file.startsWith(sourceIndex.getPrefix() + "." + sourceStructureName + "."))
			{
				Files.copyFile(
					sourceIndex.getPath() + "/" + file, 
					destIndex.getPath() + "/" + file.replaceFirst(
						sourceIndex.getPrefix() + "\\." + sourceStructureName, 
						destIndex.getPrefix() + "." + sourceStructureName));
			}
		}
		return found;
	}
	
	/** Print the contents of the meta index */
	@SuppressWarnings("unchecked")
	public static void printMetaIndex(Index index, String structureName) throws IOException
	{
		Iterator<String[]> inputStream = (Iterator<String[]>)index.getIndexStructureInputStream(structureName);
		while(inputStream.hasNext())
		{
			System.out.println(ArrayUtils.join(inputStream.next(), ", "));
		}
		IndexUtil.close(inputStream);
	}
	
	/** Rename a structure within a given index. 
	 * @return Returns true iff a structure was successfully renamed.
	 */
	public static boolean renameIndexStructure(Index index, String sourceStructureName, String destinationStructureName) throws IOException
	{
		final String actualSourcePrefix = index.getPrefix() +'.' + sourceStructureName+".";
		final String actualDestinationPrefix = index.getPrefix() +'.' + destinationStructureName + ".";
		final String path = index.getPath();
		for (String filename : Files.list(index.getPath()))
		{
			if (filename.startsWith(actualSourcePrefix))
			{
				//System.err.println("Renaming "+ index.getPath() + '/' + filename + " to " + index.getPath() + '/' + filename.replaceFirst(actualSourcePrefix, actualDestinationPrefix));
				Files.delete(path + '/' + filename.replaceFirst(actualSourcePrefix, actualDestinationPrefix));
				Files.rename(
					path + '/' + filename, 
					path + '/' + filename.replaceFirst(actualSourcePrefix, actualDestinationPrefix));
			}
		}
		//boolean found = false;
		Properties p = index.getProperties();
		Set<String> toRemove = new HashSet<String>();
		Map<String,String> toAdd = new HashMap<String,String>();
		for(Object o : p.keySet())
		{
			String key = (String)o;
			if (key.startsWith("index."+sourceStructureName + "."))
			{
				toAdd.put(
						key.replaceFirst("index."+sourceStructureName + "\\.", 
								"index." + destinationStructureName + "."), 
						p.getProperty(key));
				toRemove.add(key);
				//System.err.println("new key is " + key.replaceFirst("index."+sourceStructureName + "\\.", 
				//		"index." + destinationStructureName + "."));
			}
			if (key.startsWith("index."+sourceStructureName + "-inputstream."))
			{
				toAdd.put(
						key.replaceFirst("index."+sourceStructureName + "-inputstream\\.", 
								"index." + destinationStructureName + "-inputstream."), 
						p.getProperty(key));
				toRemove.add(key);		
			}
			
		}
		boolean OK = false;
		for(String k : toRemove)
		{
			//System.err.println("Removing property " + k);
			p.remove(k);
		}
		for(Map.Entry<String,String> e : toAdd.entrySet())
		{
			//System.err.println("Setting property " + e.getKey());
			p.setProperty(e.getKey(), e.getValue());
			OK = true;
		}
		index.dirtyProperties = true;
		index.flush();
		return OK;
	}
	
	/** Configures an object with the index, if the object implements IndexConfigurable */
	public static void configure(Index index, Object o)
	{
		if (o instanceof IndexConfigurable)
		{
			((IndexConfigurable)o).setIndex(index);
		}
	}
	
	/** Check to see if an object is closeable, and if so, close it. Propagate
	 * any exception thrown.
	 * @param o object to check for being closeable.
	 * @throws IOException if exception thrown while closing.
	 */
	public static void close(Object o) throws IOException
	{
		if (o instanceof Closeable)
			((Closeable)o).close();
	}

}
