package uk.ac.gla.terrier.links;

import gnu.trove.TIntArrayList;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author vassilis
 */
public class LinkServerStream {

	/** The name of the link index file.*/
	protected String LINK_INDEX_FILENAME;
	/** The name of the document index file.*/
	protected String DOCUMENT_INDEX_FILENAME;
	
	protected DataInputStream docIndex = null;
	
	protected DataInputStream linkIndex = null;
	
	
	protected int docid;
	protected int degree;
	protected int[] links;
	
	public boolean readNextEntry() {
		try {
			docid = docIndex.readInt();
			degree = docIndex.readInt();
			docIndex.readLong();
			links = new int[degree];
			for (int i=0; i<degree; i++) {
				links[i] = linkIndex.readInt();
			}
		} catch(EOFException eofe) {
			return false;
		} catch(IOException ioe) {
			System.err.println("IOException while reading links : "+ ioe);
			ioe.printStackTrace();
			System.exit(1);
		}
		return true;
	}
	
	public int getDocid() {
		return docid;
	}
	
	public int getDegree() {
		return degree;
	}
	
	public int[] getLinks() {
		return links;
	}
	
	
	public void close() {
		try {
			docIndex.close();
			linkIndex.close();
		} catch(IOException ioe) {
			System.out.println("IOException while closing files. exiting");
			System.exit(1);
		}
	}
	public LinkServerStream() {

		LINK_INDEX_FILENAME = ApplicationSetup.getProperty("link.index.filename","");
		DOCUMENT_INDEX_FILENAME = ApplicationSetup.getProperty("document.index.filename","");

		try {
			docIndex = new DataInputStream(new BufferedInputStream(new FileInputStream(DOCUMENT_INDEX_FILENAME)));
			linkIndex = new DataInputStream(new BufferedInputStream(new FileInputStream(LINK_INDEX_FILENAME)));
		} catch(IOException ioe) {
			System.err.println("IOException while opening files : "+ ioe);
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void createIndex() throws IOException {
		
		final int entryLength = 16;
		String INCOMING_INPUT_FILENAME;
		String INCOMING_OUTPUT_LINK_INDEX_FILENAME;
		String INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME;
		String OUTGOING_INPUT_FILENAME;
		String OUTGOING_OUTPUT_LINK_INDEX_FILENAME;
		String OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME;

		OUTGOING_INPUT_FILENAME = ApplicationSetup.getProperty("documentLinks.outgoing.input.filename","");
		OUTGOING_OUTPUT_LINK_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.outgoing.output.link.index.filename","");
		OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.outgoing.output.document.index.filename","");
		INCOMING_INPUT_FILENAME = ApplicationSetup.getProperty("documentLinks.incoming.input.filename","");
		INCOMING_OUTPUT_LINK_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.incoming.output.link.index.filename","");
		INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.incoming.output.document.index.filename","");

		final DataOutputStream outgoingDocumentIndex = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME)));
		final DataOutputStream outgoingLinkIndex = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(OUTGOING_OUTPUT_LINK_INDEX_FILENAME)));
		final DataOutputStream incomingDocumentIndex = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME)));
		final DataOutputStream incomingLinkIndex = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(INCOMING_OUTPUT_LINK_INDEX_FILENAME)));
		
		final TIntArrayList links = new TIntArrayList();	
		if (ApplicationSetup.getProperty("documentLinks.createoutgoing","false").equals("true")) {
			//A buffered reader for the input file
			BufferedReader br;
			if (OUTGOING_INPUT_FILENAME.endsWith(".gz")) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(OUTGOING_INPUT_FILENAME))));
			} else {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(OUTGOING_INPUT_FILENAME)));
			}
			String line;
			int previousFrom = -1;
			int to;
			int from;
			long where = 0L; //the byte offset in the file, to be increased by 16 every time
			while ((line = br.readLine()) != null) {
				int spaceIndex = line.indexOf(' ');
				from = Integer.parseInt(line.substring(0, spaceIndex));
				to = Integer.parseInt(line.substring(spaceIndex + 1));
				
				if (from == -1 || to == -1)
					continue;

				if (previousFrom != from) {
					if (previousFrom != -1) {
						outgoingDocumentIndex.writeInt(previousFrom);
						outgoingDocumentIndex.writeInt(links.size());
						//long where = outgoingLinkIndex.getFilePointer();
						outgoingDocumentIndex.writeLong(where);
						where+=entryLength;
						for (int i=0; i<links.size(); i++) {
							outgoingLinkIndex.writeInt( links.get(i) );
						}
						links.clear();
					}
					previousFrom = from;
					links.add(to);
				} else {
					previousFrom = from;
					links.add(to);
				}
			}
			if (links.size() > 0) {
				outgoingDocumentIndex.writeInt(previousFrom);
				outgoingDocumentIndex.writeInt(links.size());
				//long where = outgoingLinkIndex.getFilePointer();
				outgoingDocumentIndex.writeLong(where);
				where+=entryLength;
				for (int i=0; i<links.size(); i++) {
					outgoingLinkIndex.writeInt( links.get(i) );
				}
				links.clear();
			}
			br.close();
		}

		
		//the same for the incoming links
		if (ApplicationSetup.getProperty("documentLinks.createincoming","false").equals("true")) {
			BufferedReader br;
			if (INCOMING_INPUT_FILENAME.endsWith(".gz")) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(INCOMING_INPUT_FILENAME))));
			} else {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(INCOMING_INPUT_FILENAME)));
			}
			//String line;
			//int to;
			//int from;
			long where = 0L; //resetting where to be used for the new file
			int previousFrom = -1;
			int to;
			int from;
			String line;
			
			while ((line = br.readLine()) != null) {
				int spaceIndex = line.indexOf(' ');
				from = Integer.parseInt( line.substring(0, spaceIndex) );
				to =   Integer.parseInt( line.substring(spaceIndex +1) );
				
				if (from == -1 || to == -1)
					continue;

		
				if (previousFrom != from) {
					if (previousFrom != -1) {
						incomingDocumentIndex.writeInt(previousFrom);
						incomingDocumentIndex.writeInt(links.size());
						//long where = incomingLinkIndex.getFilePointer();
						incomingDocumentIndex.writeLong(where);
						where+=entryLength;
						for (int i=0; i<links.size(); i++) {
							incomingLinkIndex.writeInt( links.get(i) );
						}
						links.clear();
					}
					previousFrom = from;
					links.add(to);
				} else {
					previousFrom = from;
					links.add(to);
				}
			}
			if (links.size() > 0) {
				incomingDocumentIndex.writeInt(previousFrom);
				incomingDocumentIndex.writeInt(links.size());
				//long where = incomingLinkIndex.getFilePointer();
				incomingDocumentIndex.writeLong(where);
				where+=entryLength;
				for (int i=0; i<links.size(); i++) {
					incomingLinkIndex.writeInt( links.get(i) );
				}
				links.clear();
			}

		}
	}

		
 	public static void main(String[] args) {
 		if (args.length == 0) {
 			System.out.println("usage: LinkServerStream [-help] [-create]");
 			System.exit(1);
 		} else if (args[0].equals("-help")) {
 			System.out.println("usage: LinkServerStream [-help] [-create]");
 			System.exit(1);
 		} else if (args[0].equals("-create")) {
 			try {
	 			//LinkServerStream lServer = new LinkServerStream();
	 			//lServer.createIndex();
	 			//lServer.close();
				createIndex();
 			} catch(IOException ioe) {
 				System.err.println("IO exception while creating link index. exiting");
				System.err.println(ioe);
				ioe.printStackTrace();
 				System.exit(1);
 			}
 		}
 	}
}
