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
 * The Original Code is MSExcelDocument.java.
 *
 * The Original Code is Copyright (C) 2004-2009 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.indexing;


import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.CharArrayWriter;
import java.io.CharArrayReader;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** Implements a Document object for a Microsoft Excel spreadsheet.
 *  Uses HSSF and POIFS subparts of the Jakarta-POI project. This means
 *	that to use or compile this module, you must have the 
 *	poi-?.?.?-final-*.jar in your classpath. <p>
 *  A bug in the current stable POI library seems to mean that large
 *  Excel files cannot be parsed - see the MAXFILESIZE field to control
 *  the maximum file size that this class will attempt to read.
 *  @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
 *  @version $Revision: 1.1 $
 */
public class MSExcelDocument extends FileDocument
{
	protected static Logger logger = Logger.getRootLogger();
	/** Size of 1MB in bytes */
	protected static final int MEGABYTE = 1048576;

	/** Maximum file size that this class will attempt to open. Set to 0
	  * to ignore. Set by propery <tt>indexing.excel.maxfilesize.mb</tt>,
	  * default 0.5 */
	protected static final long MAXFILESIZE = (long)((float)MEGABYTE *
		Float.parseFloat(
			ApplicationSetup.getProperty("indexing.excel.maxfilesize.mb", "0.5")
			));

	/** Construct a new MSExcelDocument Document object
	  * @param f the file that is opened for this
	  * @param docStream the actual stream of the open file */
	public MSExcelDocument(File f, InputStream docStream)
	{
		super(f, docStream);
	}
	/** Get the reader appropriate for this InputStream. This involves
		converting the Excel document to a stream of words. On failure
		returns null and sets EOD to true, so no terms can be read from
		the object. 
		Uses the property <tt>indexing.excel.maxfilesize.mb</tt> to 
		determine if the file is too big to open
		@param docStream */
	protected Reader getReader(InputStream docStream)
	{
		
		if (MAXFILESIZE > 0 && file.length() > MAXFILESIZE)
		{	
			
			logger.warn("WARNING: Excel document "+file.getName()+
				" is too large for POI. Ignoring.");
			EOD = true;
			return null;	
		}
		try
		{
			CharArrayWriter writer = new CharArrayWriter();
			//opening the file system
			POIFSFileSystem fs = new POIFSFileSystem(docStream);
			//opening the work book
			HSSFWorkbook workbook = new HSSFWorkbook(fs);
			
			for (int i = 0; i < workbook.getNumberOfSheets(); i++ )
			{
				//got the i-th sheet from the work book
				HSSFSheet sheet = workbook.getSheetAt(i);
				
				Iterator rows = sheet.rowIterator();
				while( rows.hasNext() ) {
					
					HSSFRow row = (HSSFRow) rows.next();
					Iterator cells = row.cellIterator();
					while( cells.hasNext() ) {
						HSSFCell cell = (HSSFCell) cells.next();
						switch ( cell.getCellType() ) {
							case HSSFCell.CELL_TYPE_NUMERIC:
								String num = Double.toString(cell.getNumericCellValue()).trim();
								if(num.length() > 0) {
									writer.write(num + " ");
								}
								break;
							case HSSFCell.CELL_TYPE_STRING:
								String text = cell.getStringCellValue().trim();
								if(text.length() > 0) {
									writer.write(text + " ");
								}
								break;
						}
					}
				}
			}
			return new CharArrayReader(writer.toCharArray());
		}
		catch(Exception e )
		{
			logger.warn("WARNING: Problem converting excel document"+e);
			EOD = true;
			return null;
		}
	}
}
