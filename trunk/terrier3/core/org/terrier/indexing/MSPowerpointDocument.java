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
 * The Original Code is MSPowerpointdocument.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package org.terrier.indexing;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.util.LittleEndian;
/** Implements a Document object for reading Microsoft Powerpoint files.
 *  This implementation uses the Jakarta-POI (POIFS) library, so to compile
 * 	or use this module, you must have the poi-?.?./-final-*.jar in your 
 * 	classpath. 
 *  @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *  @version $Revision: 1.14 $
 */
public class MSPowerpointDocument extends FileDocument
{
	protected static final Logger logger = Logger.getRootLogger();
	/** Constructs a new MSPowerpointDocument object for the passed InputStream
	 *  @param filename the file that has been opened
	 * 	@param docStream the stream of the file */
	public MSPowerpointDocument(String filename, InputStream docStream) 
	{
		super(filename, docStream);
	}
	
	/** This class implements a POIFSReaderListener, which means is can
	 * 	can attach to events from the POI libraries, and write the associated
	 *  data to a writer object */
	static class PowerpointExtractor implements POIFSReaderListener
	{
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		public void processPOIFSReaderEvent(POIFSReaderEvent event)
		{
			try{
				if(!event.getName().equalsIgnoreCase("PowerPoint Document"))
					return;
				DocumentInputStream input = event.getStream();
				byte[] buffer = new byte[input.available()];
				input.read(buffer, 0, input.available());
				for(int i=0; i<buffer.length-20; i++)
				{
					long type = LittleEndian.getUShort(buffer,i+2);
					long size = LittleEndian.getUInt(buffer,i+4);
					if(type==4008)
					{
						writer.write(buffer, i + 4 + 1, (int) size +3);
						i = i + 4 + 1 + (int) size - 1;
					}
				}
			}
			catch (Exception e)
			{
				logger.fatal(e.getMessage(),e);
			}
		}
	}
	
	/** This method returns the Reader for the @param docStream
	 * 	file stream. This involves loading and converting the powerpoint
	 *  document.
	 *  On failure, returns null, and sets EOD to true, so no terms can 
	 *  be read from this object.
	 */
	protected Reader getReader(InputStream docStream)
	{
		try{
			POIFSReader reader = new POIFSReader();
			PowerpointExtractor ppe = new PowerpointExtractor();
			reader.registerListener(ppe);
			reader.read(docStream);
			return new StringReader(ppe.writer.toString());			
		} catch (Exception e) {
			logger.warn("WARNING: Problem convering MS Powerpoint document",e);
			EOD = true;
			return null;
		}
	}
}
