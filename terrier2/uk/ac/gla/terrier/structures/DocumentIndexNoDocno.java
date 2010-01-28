package uk.ac.gla.terrier.structures;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class DocumentIndexNoDocno extends DocumentIndex {
	
	public class DocIndexEntry{
		public int length;
		public long endOffset;
		public byte endBitOffset;
	}
	
	protected ArrayList<DocIndexEntry> docs;
	
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	
	/** 
	 * The index of the last retrieved entry. When there is a request
	 * for the information about a document, given its document id, 
	 * the document id is compared with the index. If they are equal,
	 * then we have the information about the document readily available,
	 * otherwise we need to decode it and update the index.
	 */
	private int index=-1;

	public DocumentIndexNoDocno(String path, String prefix)
	{
		this(ApplicationSetup.getProperty("docidx.output.filename", ""));
	}
	public DocumentIndexNoDocno() {
		this(ApplicationSetup.getProperty("docidx.output.filename", ""));
	}
	/**
	 * A constructor for DocumentIndexInMemory that specifies the file to open.
	 * Opens the document index file and reads its contents into memory.
	 * For the document pointers file we replace the extension of the 
	 * document index file with the right default extension. 
	 * @param filename String The filename of the document index file.
	 */
	public DocumentIndexNoDocno(String filename) {
		super((filename=ApplicationSetup.getProperty("docidx.output.filename", "")));
		try {
			loadIntoMemory(filename);
			numberOfDocumentIndexEntries = docs.size();
		} catch (IOException ioe) {
			logger.fatal(
				"Input/Output exception during opening the document index file. Stack trace follows: ",ioe);
		}
	}
	/**
	 * Prints to the standard error the document index structure, 
	 * which is loaded into memory.
	 */
	public void print() {
		for (int i=0; i<numberOfDocumentIndexEntries; i++) {
			DocIndexEntry doc = docs.get(i);
			System.out.println(""
						+ i
						+ ", "
						+ doc.length
						+ ", "
						+ doc.endOffset
						+ ", "
						+ doc.endBitOffset);
		}
		
	}
	/**
	 * Returns the id of a document with a given document number.
	 * @return int The document's id, or a negative number if a document with the given number doesn't exist.
	 * @param docno java.lang.String The document's number
	 */
	public int getDocumentId(String docno) {
		logger.warn("DOCNO SEEKING NOT SUPPORTED");
		return -1;
	}
	/**
	 * Returns the length of a document with a given id.
	 * @return int The document's length
	 * @param docid the document's id
	 */
	public int getDocumentLength(int docid) {
		if (docid<0||docid>=this.numberOfDocumentIndexEntries)
			return -1;
		return docs.get(docid).length;
	}
	/**
	 * Returns the document length of the document with a given document number .
	 * @return int The document's length
	 * @param docno java.lang.String The document's number
	 */
	public int getDocumentLength(String docno) {
		logger.warn("DOCNO SEEKING NOT SUPPORTED");
		return -1;
	}
	/**
	 * Returns the number of a document with a given id.
	 * @return java.lang.String The documents number
	 * @param docid int The documents id
	 */
	public String getDocumentNumber(int docid) {
		logger.warn("DOCNO SEEKING NOT SUPPORTED");
		return null;
	}
	
	/**
	 * Returns the ending offset of the current document's
	 * entry in the direct index.
	 * @return FilePosition an offset in the direct index.
	 */
	public FilePosition getDirectIndexEndOffset() {
		return new FilePosition(this.endOffset, this.endBitOffset);
	}
	/**
	 * Returns the number of documents in the document index.
	 * @return int the number of documents in the document index.
	 */
	public int getNumberOfDocuments() {
		return numberOfDocumentIndexEntries;
	}
	
	/**
	 * Returns the starting offset of the current document's
	 * entry in the direct index.
	 * @return FilePosition an offset in the direct index.
	 */
	public FilePosition getDirectIndexStartOffset() {
		return new FilePosition(this.startOffset, this.startBitOffset);
	}
	/**
	 * Loads the data from the file into memory.
	 * @param dis java.io.DataInputStream The input stream from 
	 *			which the data are read
	 * @param numOfEntries int The number of entries to read
	 * @exception java.io.IOException An input/output exception is 
	 *			thrown if there any error while reading from disk.
	 */
	public void loadIntoMemory(String filename)
		throws java.io.IOException {
		try{
		System.err.println("Loading docidx from "+filename+"...");
		docs = new ArrayList<DocIndexEntry>();
		BufferedReader br = Files.openFileReader(filename);
		String line = null;
		while ((line=br.readLine())!=null){
			String[] tokens = line.replaceAll(" ", "").split(",");
			if (tokens.length!=5)
				continue;
			DocIndexEntry entry = new DocIndexEntry();
			entry.length = Integer.parseInt(tokens[1]);
			entry.endOffset = Long.parseLong(tokens[3]);
			entry.endBitOffset = Byte.parseByte(tokens[4]);
			this.docs.add(entry);
		}
		br.close();
		System.out.println(docs.size()+" entries loaded.");
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void convertDocIndex(String docIndexFilename){
		try{
			logger.debug("Converting document index...");
			DocumentIndexEncoded di = new DocumentIndexEncoded(docIndexFilename);
			DataOutputStream dos = new DataOutputStream(Files.writeFileStream(docIndexFilename+".nodocno"));
			int N = di.getNumberOfDocuments();
			for (int i=0; i<N; i++){
				dos.writeInt(di.getDocumentLength(i));
				dos.writeLong(di.getEndOffset());
				dos.writeByte(di.getEndBitOffset());
			}
			dos.close();
			logger.debug("Done.");
		}
		catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Seeks from the document index the i-th entry.
	 * @param i the document id.
	 * @return boolean true if it was found, otherwise
	 *		 it returns false.
	 */
	public boolean seek(int i) throws IOException {
		if (i >= numberOfDocumentIndexEntries)
			return false;
		index = i;
		if (i == 0) {
			startOffset = 0;
			startBitOffset = 0;
			docid = i;
			DocIndexEntry doc = docs.get(i);
			docLength = doc.length;
			endOffset = doc.length;
			endBitOffset = doc.endBitOffset;
		} else {
			startOffset = docs.get(i-1).endOffset;
			startBitOffset = docs.get(i-1).endBitOffset;
			startBitOffset++;
			if (startBitOffset == 8) {
				startBitOffset = 0;
				startOffset++;
			}
			docid = i;
			DocIndexEntry doc = docs.get(i);
			docLength = doc.length;
			endOffset = doc.length;
		}
		return true;
	}
	
	/**
	 * Overrides the seek(String s) method of 
	 * the super class.
	 * @param docno String the document number of the document we are seeking.
	 * @return Returns false if the given docno could not be found in the DocumentIndex
	 */
	public boolean seek(final String docno) {
		logger.warn("DOCNO SEEKING NOT SUPPORTED");
		return false;
	}
}
