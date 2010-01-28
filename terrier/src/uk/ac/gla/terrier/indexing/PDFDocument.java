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
 * The Original Code is PDFDocument.java.
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
import java.io.CharArrayWriter;
import java.io.CharArrayReader;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.pdfbox.util.PDFTextStripper;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
/** 
 * Implements a Document object for reading PDF documents. This object uses the
 * <a href="http://www.pdfbox.org">PDFBox.org</a> library, so you'll need
 * to ensure that PDFBox-0.6.7a.jar or greater is in your classpath when
 * compiling or using this document. For using this class, you will also
 * need the library <a href="http://logging.apache.org/log4j/">log4j</a>.
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class PDFDocument extends FileDocument
{
	protected static Logger logger = Logger.getRootLogger();
	/** 
	 * Constructs a new PDFDocument, which will convert the docStream
	 * which represents the file to a Document object from which an Indexer
	 * can retrieve a stream of terms.
	 * @param docStream InputStream the input stream that represents the
	 *        the document's file. 
	 */
	public PDFDocument(File f, InputStream docStream)
	{
		super(f, docStream);
	}
	/** 
	 * Returns the reader of text, which is suitable for parsing terms out of,
	 * and which is created by converting the file represented by 
	 * parameter docStream. This method involves running the stream 
	 * through the PDFParser etc provided in the org.pdfbox library.
	 * On error, it returns null, and sets EOD to true, so no terms 
	 * can be read from this document.
	 * @param docStream the input stream that represents the document's file.
	 * @return Reader a reader that is fed to an indexer.
	 */
	protected Reader getReader(InputStream docStream)
	{
		
		PDFParser parser = null; PDDocument document = null; PDFTextStripper stripper = null;
		CharArrayWriter writer = null;
		try{
			parser = new PDFParser(docStream);
			parser.parse();
			document = parser.getPDDocument();
			writer = new CharArrayWriter();
			stripper = new PDFTextStripper();
			stripper.setLineSeparator("\n");
			stripper.writeText(document, writer);
			document.close();
			writer.close();
			parser.getDocument().close();
			return new CharArrayReader(writer.toCharArray());
		}catch (Exception e){
				logger.warn("WARNING: Problem converting PDF: ",e);
			try{
				document.close();				
			}catch(Exception e1){
				logger.warn("WARNING: Problem converting PDF: ",e1);
			}
			try{
				writer.close();
			}catch(Exception e2){
				logger.warn("WARNING: Problem converting PDF: ",e2);
			}
			try{
				parser.getDocument().close();
			}catch(Exception e3){
				logger.warn("WARNING: Problem converting PDF: ",e3);	
			}
			parser = null; document = null; writer = null; stripper = null;
			EOD=true;
			return null;
		}
	}
}
