/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
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
 * The Original Code is HadoopUtility.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package org.terrier.utility.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.log4j.Logger;

import org.terrier.structures.Index;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

/** Utility class for the setting up and configuring of Terrier MapReduce jobs 
  * @author Craig Macdonald
  * @version $Revision: 1.3 $
  * @since 2.2. */
public class HadoopUtility {
	
	protected static final Logger logger = Logger.getLogger(HadoopUtility.class);
	
	public static abstract class MapReduceBase<K1,V1,K2,V2,K3,V3> implements Mapper<K1,V1,K2,V2>, Reducer<K2,V2,K3,V3>
	{
		protected JobConf jc;		
		public void configure(JobConf jc) {
			this.jc = jc;
						
			//1. configure application
			try{ 
				HadoopUtility.loadTerrierJob(jc);
			} catch (Exception e) {
				throw new Error("Cannot load ApplicationSetup", e);
			}
			
			//2. configurure this class
			try{
				if (isMap(jc))
				{
					configureMap();
				} else {
					configureReduce();
				}
			} catch (Exception e) { 
				throw new Error("Cannot configure indexer", e);
			}
		}
		
		protected abstract void configureMap() throws IOException;
		protected abstract void configureReduce() throws IOException;
		
		
		public void close() throws IOException {
			if (isMap(jc))
			{
				closeMap();
			} else {
				closeReduce();
			}
		}

		protected abstract void closeMap() throws IOException;
		protected abstract void closeReduce() throws IOException;		
	}
	
	/** detect if a job is a Map job or not */
	public static final boolean isMap(JobConf jc) {
		return TaskAttemptID.forName(jc.get("mapred.task.id")).isMap();
	}

	public static void makeTerrierJob(JobConf jobConf) throws IOException
	{
		try{
			saveApplicationSetupToJob(jobConf, true);
			saveClassPathToJob(jobConf);
 		} catch (Exception e) {
			throw new WrappedIOException("Cannot HadoopUtility.makeTerrierJob", e);
		}
	}
	
	public static void loadTerrierJob(JobConf jobConf) throws IOException
	{
		try{
			HadoopPlugin.setGlobalConfiguration(jobConf);
			loadApplicationSetup(jobConf);
		} catch (Exception e) {
			 throw new WrappedIOException("Cannot HadoopUtility.loadTerrierJob", e);
		}
	}
	
	protected static void saveClassPathToJob(JobConf jobConf) throws IOException
	{
		String[] jars = findJarFiles(new String[]{
				System.getenv().get("CLASSPATH"),
				System.getProperty("java.class.path")
			});
		for (String jarFile : jars)
		{
			Path srcJarFilePath = new Path("file:///"+jarFile);
			String filename = srcJarFilePath.getName();
			Path tmpJarFilePath = makeTemporaryFile(jobConf, filename);
			FileSystem defFS = FileSystem.get(jobConf);
			defFS.copyFromLocalFile(srcJarFilePath, tmpJarFilePath);
			DistributedCache.addFileToClassPath(tmpJarFilePath, jobConf);
		}
		 DistributedCache.createSymlink(jobConf);
	}

	protected static String[] findJarFiles(String [] classPathLines)
	{
		List<String> jars = new ArrayList<String>();
		for (String locationsLine : classPathLines)
		{
			if (locationsLine == null)
				continue;
			for (String CPentry : locationsLine.split(":"))
			{
				if (CPentry.endsWith(".jar"))
					jars.add(new File(CPentry).getAbsoluteFile().toString());
			}
		}
		return jars.toArray(new String[0]);
	}

	protected static final String[] checkSystemProperties = {"file", "java", "line", "os", "path", "sun", "user"};
	protected static final Random random = new Random();

	protected static Path makeTemporaryFile(JobConf jobConf, String filename) throws IOException
	{
		FileSystem defFS = FileSystem.get(jobConf);
        Path tempFile = new Path("/tmp/"+(random.nextInt())+"-"+filename);
        defFS.deleteOnExit(tempFile);
		return tempFile;
	}
	
	protected static void saveApplicationSetupToJob(JobConf jobConf, boolean getFreshProperties) throws Exception
	{
		// Do we load a fresh properties File?
		//TODO fix, if necessary
		//if (getFreshProperties)
		//	loadApplicationSetup(new Path(ApplicationSetup.TERRIER_HOME));
		
		FileSystem remoteFS = FileSystem.get(jobConf);
		URI remoteFSURI = remoteFS.getUri();
		//make a copy of the current application setup properties, these may be amended
		//as some files are more globally accessible
		final Properties propertiesDuringJob = new Properties();
		Properties appProperties = ApplicationSetup.getProperties();
		for (Object _key: appProperties.keySet())
		{
			String key = (String)_key;
			propertiesDuringJob.put(key, appProperties.get(key));
		}

		//the share folder is needed during indexing, save this on DFS
		if (Files.getFileSystemName(ApplicationSetup.TERRIER_SHARE).equals("local"))
		{
			Path tempTRShare = makeTemporaryFile(jobConf, "terrier.share");
			propertiesDuringJob.setProperty("terrier.share", remoteFSURI.resolve(tempTRShare.toUri()).toString());
			//logger.info("Copying terrier share/ directory to shared storage area ("+remoteFSURI.resolve(tempTRShare.toUri()).toString()+")");
			if (Files.exists(ApplicationSetup.TERRIER_SHARE))
			{
				logger.info("Copying terrier share/ directory ("+ApplicationSetup.TERRIER_SHARE+") to shared storage area ("+remoteFSURI.resolve(tempTRShare.toUri()).toString()+")");
				FileUtil.copy(
						FileSystem.getLocal(jobConf), new Path(ApplicationSetup.TERRIER_SHARE),
						remoteFS, tempTRShare,
						false, false, jobConf);
			}
			else
			{
				logger.warn("No terrier.share folder found at "+ApplicationSetup.TERRIER_SHARE+", copying skipped");
			}
		}

		//copy the terrier.properties content over
		Path tempTRProperties = makeTemporaryFile(jobConf, "terrier.properties");
		logger.debug("Writing terrier properties out to DFS "+tempTRProperties.toString());
		OutputStream out = remoteFS.create(tempTRProperties);
		remoteFS.deleteOnExit(tempTRProperties);
		propertiesDuringJob.store(out, "Automatically generated by HadoopPlugin.saveApplicationSetupToJob()");
		out.close();
		out = null;
		DistributedCache.addCacheFile(tempTRProperties.toUri().resolve(new URI("#terrier.properties")), jobConf);
		DistributedCache.createSymlink(jobConf);
	
		//copy the non-JVM system properties over as well
		Path tempSysProperties = makeTemporaryFile(jobConf, "system.properties");	
		DataOutputStream dos = FileSystem.get(jobConf).create(tempSysProperties);
		logger.debug("Writing system properties out to DFS "+tempSysProperties.toString());
		for (Object _propertyKey : System.getProperties().keySet())
		{
			String propertyKey = (String)_propertyKey;
			if (! startsWithAny(propertyKey, checkSystemProperties))
			{
				dos.writeUTF(propertyKey);
				dos.writeUTF(System.getProperty(propertyKey));
			}
		}
		dos.writeUTF("FIN");
		dos.close();
		dos = null;
		DistributedCache.addCacheFile(tempSysProperties.toUri().resolve(new URI("#system.properties")), jobConf);
	}

	protected static Path findCacheFileByFragment(JobConf jc, String name) throws IOException
	{
		URI[] ps = DistributedCache.getCacheFiles(jc);
		URI defaultFS = FileSystem.getDefaultUri(jc);
		if (ps == null)
			return null;
		for (URI _p : ps)
		{
			final URI p = defaultFS.resolve(_p);
			if (p.getFragment().equals(name))
			{
				logger.debug("Found matching path in DistributedCache in search for "+name+" : " +new Path(p.getScheme(), p.getAuthority(), p.getPath()).toString());
				return new Path(p.getScheme(), p.getAuthority(), p.getPath());
			}
		}
		return null;
	}
	
	protected static void loadApplicationSetup(JobConf jobConf) throws IOException
	{
		logger.info("Reloading Application Setup");
		//we dont use Terrier's IO layer here, because it is not yet initialised
		FileSystem sharedFS = FileSystem.get(jobConf);
		Path terrierPropertiesFile =  findCacheFileByFragment(jobConf, "terrier.properties");	
		Path systemPropertiesFile = findCacheFileByFragment(jobConf, "system.properties");
	
		if (systemPropertiesFile != null && sharedFS.exists(systemPropertiesFile))
		{
			DataInputStream dis = sharedFS.open(systemPropertiesFile);
			while(true)
			{
				String key = dis.readUTF();
				if (key.equals("FIN"))
					break;
				String value = dis.readUTF();
				System.setProperty(key, value);
			}
			dis.close();
		}
		else
		{
			logger.warn("No system.properties file found at "+systemPropertiesFile);
		}

		if (terrierPropertiesFile != null && sharedFS.exists(terrierPropertiesFile))
		{
			ApplicationSetup.configure(sharedFS.open(terrierPropertiesFile));
		}
		else
		{
			throw new java.io.FileNotFoundException("No terrier.properties file found at "+terrierPropertiesFile);
		}
	}
	
	public static Index fromHConfiguration(Configuration c)
	{
		return Index.createIndex(c.get("terrier.index.path"), c.get("terrier.index.prefix"));
	}
	
	/** Puts the specified index onto the given Hadoop job configuration */
	public static void toHConfiguration(Index i, Configuration c)
	{
		c.set("terrier.index.path", i.getPath());
		c.set("terrier.index.prefix", i.getPrefix());
	}
	
	/**
	 * Returns true if source contains any of the Strings held in checks. Case insensitive.
	 * @param source String to check
	 * @param checks Strings to check for
	 * @return true if source starts with one of checks, false otherwise.
	 */
	protected static boolean startsWithAny(String source, String[] checks) {
		for (String s:checks) {
			if (source.toLowerCase().startsWith(s.toLowerCase())) return true;
		}
		return false;
	}
}
