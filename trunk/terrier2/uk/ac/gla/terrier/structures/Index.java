/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
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
 * The Original Code is Index.java.
 *
 * The Original Code is Copyright (C) 2004-2008 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   Vassilis Plachouras <vassilis{a.}dcs.gla.ac.uk>
 */
package uk.ac.gla.terrier.structures;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/** 
 * This class encapsulates all the Indexes at retrieval time. 
 * It is loaded by giving a path and prefix. This looks for an 
 * index properties file at path/prefix.properties. Essentially, the
 * properties file then specifies which index structures the index
 * supports. The index then loads these so they can be used in retrieval.
 * <p>
 * Arbitrary properties can be defined in the index properties files, and
 * in particular, properties are used to record index statistics and 
 * the contructor type and values of various index objects.
 * Properties defaults are chosen appropriately for Terrier 1.x indices.
 * <p>
 * The Index will apply methods on specially marked interfaces. Currently,
 * these are <a href="LegacyBitFileStructure.html">LegacyBitFileStructure</a>
 * and <a href="IndexConfigurable.html">IndexConfigurable</a>. Moreover,
 * structures implementing <a href="Closeable.html">Closeable</a> will have
 * their close method called when the Index is closed.
 * <p>
 * @author Craig Macdonald &amp; Vassilis Plachouras
 * @version $Revision: 1.1 $ 
*/
public class Index implements Closeable
{
	/** This collection statistics parses the associated index properties for each call */
	protected class UpdatingCollectionStatistics extends CollectionStatistics
	{
		public UpdatingCollectionStatistics ()
		{
			super(0,0,0,0);
		}

		@Override
		public double getAverageDocumentLength() {
			final int numDocs = getNumberOfDocuments();
			if (numDocs == 0)
				return 0.0d;
			return (double)getNumberOfTokens() / (double) numDocs;
		}

		@Override
		public int getNumberOfDocuments() {
			return Integer.parseInt(properties.getProperty("num.Documents","0"));
		}

		@Override
		public long getNumberOfPointers() {
			return Long.parseLong(properties.getProperty("num.Pointers", "0"));
		}

		@Override
		public long getNumberOfTokens() {
			return Long.parseLong(properties.getProperty("num.Pointers", "0"));
		}

		@Override
		public int getNumberOfUniqueTerms() {
			return Integer.parseInt(properties.getProperty("num.Terms", "0"));
		}
		
	}
	
	/** The logger used */
	protected static final Logger logger = Logger.getRootLogger();
	protected static String lastLoadError = null;
	/** path component of this index's location */
	protected String path;
	/** prefix component of this index's location */
	protected String prefix;
	/** properties of this index */	
	protected final Properties properties = new Properties();
	/** Cache of all opened index structures, but not input streams */
	protected final HashMap<String, Object> structureCache = new HashMap<String, Object>(10);
	/** Have the properties of this index changed, suggesting a flush() is necessary when closing */
	protected boolean dirtyProperties = false;

	/** Set to true if loading an index succeeds */
	protected boolean loadSuccess = true;
	protected String loadError = null;

	
	/** A constructor for child classes that doesnt open the file */
	protected Index(long a, long b, long c) { }
	/**
	 * A default constuctor that creates an instance of the index.
	 */
	protected Index() {
		this(ApplicationSetup.TERRIER_INDEX_PATH,
			ApplicationSetup.TERRIER_INDEX_PREFIX, false);
	}
	/**
	 * Constructs a new Index object. Don't call this method,
	 * call the createIndex(String) factory method to
	 * construct an Index object.
	 * @param path String the path in which the data structures
	 *		will be created.
	 * @param prefix String the prefix of the files to
	 *		be created.
	 * @param isNew where a new Index should be created if there is no index
	 * 		at the specified location
	 */
	protected Index(String path, String prefix, boolean isNew) {
		if (!(new File(path)).isAbsolute())
			path = ApplicationSetup.makeAbsolute(path, ApplicationSetup.TERRIER_VAR);
		
		this.path = path;
		this.prefix = prefix;
		
		boolean indexExists = loadProperties();	

		if (isNew && ! indexExists)
		{
			setIndexProperty("index.terrier.version", ApplicationSetup.TERRIER_VERSION);
			setIndexProperty("num.Documents","0");
			setIndexProperty("num.Terms", "0");
			setIndexProperty("num.Tokens", "0");
			setIndexProperty("num.Pointers", "0");
			loadUpdatingStatistics();
			dirtyProperties = true;
			loadSuccess = true;	
		}
		else
		{
			//note the order - some structures will require collection statistics, so load this first.
			loadStatistics();
			loadIndices();
		}
	}

	/** for an immultable index, use a normal collection statistics, never changes */	
	protected void loadStatistics()
	{
		structureCache.put("collectionstatistics",
			new CollectionStatistics(
				Integer.parseInt(properties.getProperty("num.Documents","0")),
				Integer.parseInt(properties.getProperty("num.Terms", "0")),
				Long.parseLong(properties.getProperty("num.Tokens", "0")),
				Long.parseLong(properties.getProperty("num.Pointers", "0"))
			));
	}

	/** for an index that is not yet built, use an UpdatingCollectionStatistics, which is slower
	  * but can support updates of the index statistics */	
	protected void loadUpdatingStatistics()
	{
		structureCache.put("collectionstatistics", new UpdatingCollectionStatistics());
	}

	/** load all index structures */
	protected void loadIndices()
	{
		boolean OK = true;
		//look for all index structures
		for (Object oKey: properties.keySet())
		{
			final String sKey = (String)oKey;
			if (sKey.matches("^index\\..+\\.class$")
				&& ! (sKey.matches("^index\\..+-inputstream.class$"))) //dont preload input streams
			{
				final String structureName = sKey.split("\\.")[1];
				Object o = getIndexStructure(structureName);
				if (o == null)
				{
					loadError = "Could not load an index structure called "+ structureName;
					OK = false;
				}
			}
		}
		if (! OK)
			this.loadSuccess = false;
	}

	/** loads in the properties file, falling back to the Terrier 1.xx log file if no properties exist. */
	protected boolean loadProperties()
	{
		try{ 
			String propertiesFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + ".properties";
			if(! allExists(propertiesFilename))
			{
				String logFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + ".log";
				if (! allExists(logFilename))
				{
					loadSuccess = false;
					loadError = "Index not found: "+propertiesFilename + " and " + logFilename + " both not found.";
					return false;
				}
				deriveIndexProperties_Terrier1xx();
				dirtyProperties = true;
			}
			else
			{
				properties.load(Files.openFileStream(propertiesFilename));
			}
			
		} catch (IOException ioe) {
			logger.error("Problem loading index properties", ioe);
			loadError = "Problem loading index properties: "+ ioe;
			return false;
		}
		return true;
	}

	/** For an index created by Terrier 1.xx, this reads the legacy .log file, deriving the correct
	  * settings for various properties. 
	  * @return true if .log file was read OK, false otherwise */	
	protected boolean deriveIndexProperties_Terrier1xx() throws IOException
	{
		// open the log file, get first line
		String logFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LOG_SUFFIX;
		final BufferedReader br = Files.openFileReader(logFilename);
		String line = br.readLine();
		if (line == null)
		{
			logger.warn(loadError = "Index .log file ("+logFilename+") corrupt - file has zero bytes");
			return false;
		}

		//parse the collection statistics
		final String[] statistics = line.trim().split("\\s+");
		if (statistics.length != 4)
		{
			logger.warn(loadError = "Index .log file ("+logFilename+") corrupt - first line does not have 4 distinct statistics");
			return false;
		}
		properties.setProperty("num.Documents", statistics[0]);
		properties.setProperty("num.Tokens", statistics[1]);
		properties.setProperty("num.Terms", statistics[2]);
		properties.setProperty("num.Pointers", statistics[3]);
		
		line = br.readLine();
		if (line == null) //Terrier 1.0beta index support
		{//these indexes did not have class names declared. Assume non-block indices
			properties.setProperty("index.terrier.version", "1.0beta");
			properties.setProperty("index.lexicon.class", "uk.ac.gla.terrier.structures.Lexicon");
			properties.setProperty("index.lexicon-inputstream.class", "uk.ac.gla.terrier.structures.LexiconInputStream");
			
			properties.setProperty("index.document.class", "uk.ac.gla.terrier.structures.DocumentIndexEncoded");
			properties.setProperty("index.document-inputstream.class", "uk.ac.gla.terrier.structures.DocumentIndexInputStream");
			
			properties.setProperty("index.direct.class", "uk.ac.gla.terrier.structures.DirectIndex");
			properties.setProperty("index.direct-inputstream.class", "uk.ac.gla.terrier.structures.DirectIndexInputStream");
			
			properties.setProperty("index.inverted.class", "uk.ac.gla.terrier.structures.InvertedIndex");
			properties.setProperty("index.inverted-inputstream.class", "uk.ac.gla.terrier.structures.InvertedIndexInputStream");
		}
		else
		{
			//the .log file has the various index structures named. Use these to make guesses about the correct
			properties.setProperty("index.terrier.version", "1.0.0");
			//property settings
			final String[] classes = line.split("\\s+");
			//derive the Lexicon classes
			properties.setProperty("index.lexicon.class", classes[0]);
			if (classes[0].endsWith(".Lexicon"))
				properties.setProperty("index.lexicon-inputstream.class", "uk.ac.gla.terrier.structures.LexiconInputStream");
			else if (classes[0].endsWith(".BlockLexicon"))
				properties.setProperty("index.lexicon-inputstream.class", "uk.ac.gla.terrier.structures.BlockLexiconInputStream");
			else if (classes[0].endsWith(".UTFLexicon"))
				properties.setProperty("index.lexicon-inputstream.class", "uk.ac.gla.terrier.structures.UTFLexiconInputStream");
			else if (classes[0].endsWith(".UTFBlockLexicon"))
				properties.setProperty("index.lexicon-inputstream.class", "uk.ac.gla.terrier.structures.UTFBlockLexiconInputStream");
			//document index	
			properties.setProperty("index.document.class", classes[1]);
			properties.setProperty("index.document-inputstream.class", "uk.ac.gla.terrier.structures.DocumentIndexInputStream");

			//direct index
			properties.setProperty("index.direct.class", classes[2]);
			if (classes[2].endsWith(".DirectIndex"))
				properties.setProperty("index.direct-inputstream.class", "uk.ac.gla.terrier.structures.DirectIndexInputStream");
			else if (classes[2].endsWith(".BlockDirectIndex")) 
				properties.setProperty("index.direct-inputstream.class", "uk.ac.gla.terrier.structures.BlockDirectIndexInputStream");
			//inverted index	
			properties.setProperty("index.inverted.class", classes[3]);
			if (classes[3].endsWith(".InvertedIndex"))
				properties.setProperty("index.inverted-inputstream.class", "uk.ac.gla.terrier.structures.InvertedIndexInputStream");
			else if (classes[2].endsWith(".BlockInvertedIndex"))
				properties.setProperty("index.inverted-inputstream.class", "uk.ac.gla.terrier.structures.BlockInvertedIndexInputStream");	
			
		}
		
		//direct index stuff
		properties.setProperty("index.direct.parameter_types", "uk.ac.gla.terrier.structures.DocumentIndex,java.lang.String,java.lang.String");
		properties.setProperty("index.direct.parameter_values","document,path,prefix");
		properties.setProperty("index.direct-inputstream.parameter_types", "uk.ac.gla.terrier.structures.DocumentIndexInputStream,java.lang.String,java.lang.String");
		properties.setProperty("index.direct-inputstream.parameter_values","document-inputstream,path,prefix");

		properties.setProperty("blocks.directindex.countdelta","0");
		
		//inverted index stuff
		properties.setProperty("index.inverted.parameter_types", "uk.ac.gla.terrier.structures.Lexicon,java.lang.String,java.lang.String");
		properties.setProperty("index.inverted.parameter_values","lexicon,path,prefix");
		properties.setProperty("index.inverted-inputstream.parameter_types", "java.lang.String,java.lang.String,uk.ac.gla.terrier.structures.LexiconInputStream");
		properties.setProperty("index.inverted-inputstream.parameter_values", "path,prefix,lexicon-inputstream");

		properties.setProperty("blocks.invertedindex.countdelta","0");
		
		properties.setProperty("index.structures.LegacyBitFileStructure", "true");
		return true;
	}

	/** Does this index have an index structure with the specified name?
	  * @param structureName name of the required structure
	  * @return true if the index has an appropriately named structure */	
	public boolean hasIndexStructure(String structureName)
	{
		return properties.containsKey("index."+structureName+".class");
	}
	
	/** Does this index have an index structure input stream with the specified name?
	 * @param structureName name of the required structure
	 * @return true if the index has an appropriately named structure */ 
	public boolean hasIndexStructureInputStream(String structureName)
	{
		return properties.containsKey("index."+structureName+"-inputstream.class");
	}

	/** Obtains the named index structure, using an already loaded one if possible.
	  * @param structureName name of the required structure
	  * @return desired object or null if not found */	
	public Object getIndexStructure(String structureName)
	{
		Object rtr = structureCache.get(structureName);
		if (rtr != null)
			return rtr;
		rtr = loadIndexStructure(structureName);
		if (rtr != null)
			structureCache.put(structureName, rtr);
		return rtr;
	}
	
	/** Load a new instance of the named index structure.
	  * @param structureName name of the required structure
	  * @return desired object or null if not found */
	protected Object loadIndexStructure(String structureName)
	{	
		try{
			// figure out the correct class
			String structureClassName = properties.getProperty("index."+structureName+".class");
			if (structureClassName == null)
			{
				logger.error("This index ("+this.toString()+") doesnt have an index structure called "+ structureName);
				return null;//TODO exceptions?
			}
			//obtain the class definition for the index structure
			Class indexStructureClass = null;
			try{
				indexStructureClass = Class.forName(structureClassName, false, this.getClass().getClassLoader());
			} catch (ClassNotFoundException cnfe) {
				logger.error("This index ("+this.toString()+") references an unknown index structure class: "+structureName+ " looking for "+ structureClassName);
				return null;//TODO exceptions?
			}

			//build up the constructor parameter type array
			final ArrayList<Class> paramTypes = new ArrayList<Class>(5);

			final String typeList = properties.getProperty("index."+structureName+".parameter_types", "java.lang.String,java.lang.String").trim();
			Object rtr = null;
			//for objects with constructor arguments
			if (typeList.length() > 0)
			{
				final String[] types = typeList.split("\\s*,\\s*");
				for (String t: types)
				{
					paramTypes.add(Class.forName(t));
				}
				Class[] param_types = paramTypes.toArray(EMPTY_CLASS_ARRAY);
	
				//build up the constructor parameter value array
				String[] params = properties.getProperty("index."+structureName+".parameter_values", "path,prefix").split("\\s*,\\s*");
				Object[] objs = new Object[paramTypes.size()];
				int i=0;
				for (String p: params)
				{
					if (p.equals("path"))
						objs[i] = path;
					else if (p.equals("prefix"))
						objs[i] = prefix;
					else if (p.equals("index"))
						objs[i] = this;
					else if (p.endsWith("-inputstream"))//no caching for input streams
						 objs[i] = loadIndexStructure(p);
					else
						objs[i] = getIndexStructure(p);
					i++;
				}

				//get the index structure using the appropriate constructor with correct parameters
				rtr = indexStructureClass.getConstructor(param_types).newInstance(objs);
			}
			else
			{	//no constructor arguments
				rtr = indexStructureClass.newInstance();
			}
		
			//Special case hacks
			//1. set the Index properties if desired
			if (rtr instanceof IndexConfigurable)
			{
				((IndexConfigurable)rtr).setIndex(this);
			}
			//2. reopen the index structure if it is a legacy structure, ie an index from Terrier 1.0.x or Terrier 1.1.x
			if (Boolean.parseBoolean(properties.getProperty("index.structures.LegacyBitFileStructure", "false"))
				&& rtr instanceof LegacyBitFileStructure)
			{
				logger.debug("Structure "+structureName+" in legacy format, applying LegacyBitFileStructure "
					+"to change BitFile endian-ness" );
				((LegacyBitFileStructure)rtr).reOpenLegacyBitFile();
			}
			// we're done
			return rtr;		
			
		} catch (Throwable t) {
			logger.error("Couldn't load an index structure called "+structureName, t);
			return null;
		}
	}

	/** Return the input stream associated with the specified structure of this index
	  * @param structureName  The name of the structure of which you want the inputstream. Eg "lexicon"
	  * @return Required structure, or null if not found */
	public Object getIndexStructureInputStream(String structureName)
	{
		//no caching on inputstreams
		return loadIndexStructure(structureName+"-inputstream");
	}
	
	/**
	 * Closes the data structures of the index.
	 */
	public void close() {
		//invoke the close methods on all currently open index structures
		for (Object o : structureCache.values())
		{
			if (o instanceof Closeable)
				((Closeable)o).close();
		}
		structureCache.clear();
		flushProperties();	
	}

	/** Write any dirty properties down to disk */
	protected void flushProperties()
	{
		if (dirtyProperties)
		{
			final String propertiesFilename = path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.PROPERTIES_SUFFIX;
			if ((Files.exists(propertiesFilename) && ! Files.canWrite(propertiesFilename))||
				(! Files.exists(propertiesFilename) && ! Files.canWrite(path)))
			{
				logger.warn("Could not write to index properties at "+propertiesFilename 
					+ " because you do not have write permission on the index - some changes may be lost");
				return;
			}
			try{
				final OutputStream outputStream = Files.writeFileStream(propertiesFilename); 
				properties.store(outputStream,"");
				outputStream.close(); 
			} catch (IOException ioe) {
				logger.warn("Could not write to index properties at "+propertiesFilename + " - some changes may be lost", ioe);
			}
		}
	}

	/** Write any dirty data structures down to disk */
	public void flush()
	{
		flushProperties();
	}

	/** Returns the InvertedIndex to use for this index */	
	public InvertedIndex getInvertedIndex()
	{
		return (InvertedIndex)getIndexStructure("inverted");
	}
	/** Return the DirectIndex associated with this index */
	public DirectIndex getDirectIndex()
	{
		return (DirectIndex)getIndexStructure("direct");
	}
	/** Return the Lexicon associated with this index */
	public Lexicon getLexicon()
	{
		return (Lexicon)getIndexStructure("lexicon");
	}
	/** Return the DocumentIndex associated with this index */
	public DocumentIndex getDocumentIndex()
	{
		return (DocumentIndex)getIndexStructure("document");
	}
	
	public CollectionStatistics getCollectionStatistics()
	{
		return (CollectionStatistics)getIndexStructure("collectionstatistics");
	}


	/** Returns a String representation of this index */
	public String toString()
	{
		return "Index("+path+","+prefix+")";
	}

	
	/** Returns the path of this index */
	public String getPath()
	{
		return path;
	}

	/** Returns the prefix of this index */
	public String getPrefix()
	{
		return prefix;
	}
	
	/** add an index structure to this index. Structure will be called structureName, and instantiated by a class
	 * 	called className. Instantiation parameters are "String,String", which are "path,prefix".
	 * @param structureName
	 * @param className
	 */
	public void addIndexStructure(String structureName, String className)
	{
		properties.setProperty("index."+structureName + ".class", className);
		properties.setProperty("index."+structureName+".parameter_types", "java.lang.String,java.lang.String");
		properties.setProperty("index."+structureName+".parameter_values", "path,prefix");
		dirtyProperties = true;
	}
	
	/** add an index structure to this index. Structure will be called structureName, and instantiated by a class
	 * 	called className. Instantiation type parameters or values are non-default.
	 */ 
	public void addIndexStructure(String structureName, String className, String[] paramTypes, String[] paramValues)
	{
		properties.setProperty("index."+structureName + ".class", className);
		properties.setProperty("index."+structureName+".parameter_types", join(paramTypes, ","));
		properties.setProperty("index."+structureName+".parameter_values",join(paramValues,","));
		dirtyProperties = true;
	}
	/** add an index structure to this index. Structure will be called structureName, and instantiated by a class
	 * 	called className. Instantiation type parameters or values are non-default.
	 */ 
	public void addIndexStructure(String structureName, String className, String paramTypes, String paramValues)
	{
		properties.setProperty("index."+structureName + ".class", className);
		properties.setProperty("index."+structureName+".parameter_types", paramTypes);
		properties.setProperty("index."+structureName+".parameter_values",paramValues);
		dirtyProperties = true;
	}
	
	 /** tell the index about a new input stream index structure it provides. */
	public void addIndexStructureInputStream(String structureName, String className)
	{
		addIndexStructure(structureName+"-inputstream", className);
	}
	
	 /** tell the index about a new input stream index structure it provides. */
	public void addIndexStructureInputStream(String structureName, String className, String[] paramTypes, String[] paramValues)
	{
		addIndexStructure(structureName+"-inputstream", className, paramTypes, paramValues);
	}
	
	/** tell the index about a new input stream index structure it provides. */
	public void addIndexStructureInputStream(String structureName, String className, String paramTypes, String paramValues)
	{
		addIndexStructure(structureName+"-inputstream", className, paramTypes, paramValues);
	}
	
	/** set an arbitrary property in the index 
 	  * @param key Key to of the property to set
 	  * @param value Value of the property to set */
	public void setIndexProperty(String key, String value)
	{
		properties.setProperty(key, value);
		dirtyProperties = true;
	}
	
	/** get an arbitrary property in the index 
 	  * @param key Key of the property to get 
 	  * @param defaultValue value of the property to use if property is not set 
 	  * @return Value of the property */
	public String getIndexProperty(String key, String defaultValue)
	{
		return properties.getProperty(key, defaultValue);
	}

	/** get an arbitrary int property from the index */
	public int getIntIndexProperty(String key, int defaultValue)
	{
		String rtr = properties.getProperty(key, null);
		if (rtr== null)
			return defaultValue;
		return Integer.parseInt(rtr);
	}
	

	/**
	 * Factory method for load an index. This method should be used in order to
	 * load an existing index in the applications.
	 * @param path String the path in which the 
	 *		data structures will be created. 
	 * @param prefix String the prefix of the files
	 *		to be created.
	 */
	public static Index createIndex(String path, String prefix) {
		Index i = new Index(path, prefix, false);
		if (! i.loadSuccess)
		{
			lastLoadError = i.loadError;
			return null;
		}
		return i;
	}
	
	/**
	 * Factory method create a new index. This method should be used in order to
	 * load a new index in the applications.
	 * @param path String the path in which the 
	 *		data structures will be created. 
	 * @param prefix String the prefix of the files
	 *		to be created.
	 */
	public static Index createNewIndex(String path, String prefix) {
		Index i = new Index(path, prefix, true);
		if (! i.loadSuccess)
		{
			lastLoadError = i.loadError;
			return null;
		}
		return i;
	}

	/** Returns the last warning given by an index being loaded. */
	public static String getLastIndexLoadError()
	{
		return lastLoadError;
	}

	/** Returns true if it is likely that an index exists at the specified location
 	  * @param path
 	  * @param prefix
 	  * @return true if a .properties or a .log files exists */
	public static boolean existsIndex(String path, String prefix) {
		if (!(new File(path)).isAbsolute())
			path = ApplicationSetup.makeAbsolute(path, ApplicationSetup.TERRIER_VAR);
		return allExists(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.PROPERTIES_SUFFIX)
			|| allExists(path + ApplicationSetup.FILE_SEPARATOR + prefix + ApplicationSetup.LOG_SUFFIX);
	}

	
	/** 
	 * Factory method for creating an 
	 * index. This method should be used in order to
	 * load an existing index in the applications.
	 */
	public static Index createIndex() {
		return createIndex(
			ApplicationSetup.TERRIER_INDEX_PATH,
			ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	
	/** returns true if all named files exist */
	protected static boolean allExists(String... files)
	{
		for(int i=0;i<files.length;i++)
		{
			if (! Files.exists(files[i]))
			{
				logger.debug("Files  "+files[i] + " doesn't exist");
				return false;
			}
		}
		return true;
	}

	/** joins a series of strings together with a delimiter */
	protected static String join(String[] input, String joinString)
	{
		StringBuilder rtr = new StringBuilder();
		int i = input.length;
		for(String s : input)
		{
			rtr.append(s);
			if (i > 0)
				rtr.append(joinString);
		}
		return rtr.toString();
	}

	/** empty class array */	
	protected static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
}
