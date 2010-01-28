package uk.ac.gla.terrier.links;
import gnu.trove.TIntArrayList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import uk.ac.gla.terrier.utility.ApplicationSetup;
/**
 * This class creates and provides access to the links of a document.
 * It can be used for the incoming, and the outgoing links.
 * <h3>Input Data Format</h3>
 * &nbsp;<I>So I'll never have to figure it out again!</I><p>
 * INCOMING_LINKS - Sorted on the first field :<br/>
 *  <tt>targetdocid sourcedocid</tt><br/>
 * OUTGOING LINKS - Sorted on the first field : <br />
 *  <tt>sourcedocid targetdocid</tt><br/>
 * 
 *
 * @author Vassilis Plachouras
 * @date 30/03/2003
 */
public class LinkServer implements LinkIndex {
	/**
	 * The entry length in bytes in the document index file.
	 * The structure of the file is <docid(int)> <indegree(int)> <link-pointer(long)>
	 */
	protected static final int entryLength = 16;
	/** The name of the incoming links input file, as specified for the createIndex method.*/
	protected static String INCOMING_INPUT_FILENAME;
	/** The name of the incoming links output link index file.*/
	protected static String INCOMING_OUTPUT_LINK_INDEX_FILENAME;
	/** The name of the incoming links output document index file.*/
	protected static String INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME;
	/** The name of the outgoing links input file, as specified for the createIndex method.*/
	protected static String OUTGOING_INPUT_FILENAME;
	/** The name of the outgoing links output link index file.*/
	protected static String OUTGOING_OUTPUT_LINK_INDEX_FILENAME;
	/** The name of the outgoing links output document index file.*/
	protected static String OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME;
	/** The file containing the incoming links.*/
	protected RandomAccessFile incomingLinkIndex;
	/** The file containing the document ids for the incoming links.*/
	protected RandomAccessFile incomingDocumentIndex;
	/** The file containing the outgoing links.*/
	protected RandomAccessFile outgoingLinkIndex;
	/** The file containing the document ids for the outgoing links.*/
	protected RandomAccessFile outgoingDocumentIndex;
	/** The array containing the links.*/
	protected int[] linkIds;
	/** The number of links.*/
	protected int numberOfLinks;
	/** The document id, which the links we are looking for.*/
	protected int foundId;
	static {
		OUTGOING_INPUT_FILENAME = ApplicationSetup.getProperty("documentLinks.outgoing.input.filename","");
		OUTGOING_OUTPUT_LINK_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.outgoing.output.link.index.filename","");
		OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.outgoing.output.document.index.filename","");
		INCOMING_INPUT_FILENAME = ApplicationSetup.getProperty("documentLinks.incoming.input.filename","");
		INCOMING_OUTPUT_LINK_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.incoming.output.link.index.filename","");
		INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME = ApplicationSetup.getProperty("documentLinks.incoming.output.document.index.filename","");
	}
	/**
	 * The default constructor.
	 * @throws IOException Throws IOException if the link index and document index files are not found.
	 */
	public LinkServer() throws IOException {
		outgoingLinkIndex = new RandomAccessFile(OUTGOING_OUTPUT_LINK_INDEX_FILENAME, "rw");
		outgoingDocumentIndex = new RandomAccessFile(OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME, "rw");
		incomingLinkIndex = new RandomAccessFile(INCOMING_OUTPUT_LINK_INDEX_FILENAME, "rw");
		incomingDocumentIndex = new RandomAccessFile(INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME, "rw");
		numberOfLinks = 0;
		linkIds = null;
		foundId = -1;
	}
	/**
	 * A constructor that specifies the names of the input files.
	 * @param inputFilename The name of the input file.
	 * @throws IOException Throws IOException if the link index and document index files are not found.
	 */
	public LinkServer(String outgoingInputFilename, String incomingInputFilename) throws IOException {
		OUTGOING_INPUT_FILENAME = outgoingInputFilename;
		INCOMING_INPUT_FILENAME =  incomingInputFilename;
		outgoingLinkIndex = new RandomAccessFile(OUTGOING_OUTPUT_LINK_INDEX_FILENAME, "rw");
		outgoingDocumentIndex = new RandomAccessFile(OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME, "rw");
		incomingLinkIndex = new RandomAccessFile(INCOMING_OUTPUT_LINK_INDEX_FILENAME, "rw");
		incomingDocumentIndex = new RandomAccessFile(INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME, "rw");
		numberOfLinks = 0;
		linkIds = null;
		foundId = -1;
	}
	/**
	 * A constructor that specifies the name of the input file,
	 * as well as the names of the output files.
	 * @param outgoingInputFilename The name of the outgoing link input file.
	 * @param outgoingLinkIndexFilename The name of the outgoing link index file.
	 * @param outgoingDocumentIndexFilename The name of the document index file for outgoing links.
	 * @param incomingInputFilename The name of the incoming link input file.
	 * @param incomingLinkIndexFilename The name of the incoming link index file.
	 * @param incomingDocumentIndexFilename The name of the document index file for incoming links.
	 * @throws IOException Throws IOException if the link index and document index files are not found.
	 */
	public LinkServer(String outgoingInputFilename, String outgoingLinkIndexFilename, String outgoingDocumentIndexFilename,
										  String incomingInputFilename, String incomingLinkIndexFilename, String incomingDocumentIndexFilename) throws IOException {
		OUTGOING_INPUT_FILENAME = outgoingInputFilename;
		OUTGOING_OUTPUT_LINK_INDEX_FILENAME = outgoingLinkIndexFilename;
		OUTGOING_OUTPUT_DOCUMENT_INDEX_FILENAME = outgoingDocumentIndexFilename;
		INCOMING_INPUT_FILENAME = incomingInputFilename;
		INCOMING_OUTPUT_LINK_INDEX_FILENAME = incomingLinkIndexFilename;
		INCOMING_OUTPUT_DOCUMENT_INDEX_FILENAME = incomingDocumentIndexFilename;
		incomingLinkIndex = new RandomAccessFile(incomingLinkIndexFilename, "rw");
		incomingDocumentIndex = new RandomAccessFile(incomingDocumentIndexFilename, "rw");
		outgoingLinkIndex = new RandomAccessFile(outgoingLinkIndexFilename, "rw");
		outgoingDocumentIndex = new RandomAccessFile(outgoingDocumentIndexFilename, "rw");
		numberOfLinks = 0;
		linkIds = null;
		foundId = -1;
	}
	/**
	 * The method that creates the file structures. It takes as an input
	 * the name of a file, that contains in each line a pair of numbers,
	 * that is a pair of document ids. The first number is the source document
	 * id and the second is the destination document id, for the outgoing links. The
	 * opposite holds for the incoming links.
	 * NB: This documentation doesnt make sense, so refer to the clarified version at the top of the class, in the javadoc
	 * @throws IOException Throws IOException if there is any problem while reading or writing to files.
	 */
	public void createIndex() throws IOException {
		final TIntArrayList links = new TIntArrayList();
		if (ApplicationSetup.getProperty("documentLinks.createoutgoing","false").equals("true")) {
			System.err.println("Building outgoing link index");
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
			while ((line = br.readLine()) != null) {
				int spaceIndex = line.indexOf(' ');
				from = (new Integer(line.substring(0, spaceIndex))).intValue();
				to = (new Integer(line.substring(spaceIndex + 1))).intValue();

				if (from == -1 || to == -1)
					continue;

				if (previousFrom != from) {
					if (previousFrom != -1) {
						outgoingDocumentIndex.writeInt(previousFrom);
						outgoingDocumentIndex.writeInt(links.size());
						long where = outgoingLinkIndex.getFilePointer();
						outgoingDocumentIndex.writeLong(where);
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
				long where = outgoingLinkIndex.getFilePointer();
				outgoingDocumentIndex.writeLong(where);
				for (int i=0; i<links.size(); i++) {
					outgoingLinkIndex.writeInt( links.get(i) );
				}
				links.clear();
			}
			br.close();
			System.err.println("Finished building outgoing link index");
		}
		

		
		if (ApplicationSetup.getProperty("documentLinks.createincoming","false").equals("true")) {
			System.err.println("Building incoming link index");
			BufferedReader br;
			//the same for the incoming links
			if (INCOMING_INPUT_FILENAME.endsWith(".gz")) {
				br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(INCOMING_INPUT_FILENAME))));
			} else {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(INCOMING_INPUT_FILENAME)));
			}
			String line;
			int previousFrom = -1;
			int to;
			int from;
			while ((line = br.readLine()) != null) {
				int spaceIndex = line.indexOf(' ');
				from = Integer.parseInt(line.substring(0, spaceIndex));
				to =   Integer.parseInt(line.substring(spaceIndex + 1));

				if (from == -1 || to == -1)
					continue;
				
				if (previousFrom != from) {
					if (previousFrom != -1) {
						incomingDocumentIndex.writeInt(previousFrom);
						incomingDocumentIndex.writeInt(links.size());
						long where = incomingLinkIndex.getFilePointer();
						incomingDocumentIndex.writeLong(where);
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
				long where = incomingLinkIndex.getFilePointer();
				incomingDocumentIndex.writeLong(where);
				for (int i=0; i<links.size(); i++) {
					incomingLinkIndex.writeInt( links.get(i) );
				}
				links.clear();
			}
			System.err.println("Finished building outgoing link index");
			br.close();
		}

	}
	/**
	 * Returns an array of integers, that represent the document ids of
	 * the documents that are linked to the document id passed as parameter.
	 * If the document with the given id does not have any links, then the
	 * returned value is false, else it is true.
	 * @param id The document id.
	 * @throws IOException Throws an IOException if there is any problem.
	 */
	public boolean seekLinks(RandomAccessFile docIndex, RandomAccessFile linkIndex, int id) throws IOException {
		foundId = id;
		//perform a binary search in the document index file
		long begin = 0;
		long end = docIndex.length()/entryLength - 1;
		while (begin <= end) {
			if (begin == end) {
				docIndex.seek(begin*entryLength);
				int docid = docIndex.readInt();
				if (id == docid) {
					foundId = docid;
					numberOfLinks = docIndex.readInt();
					long pointer = docIndex.readLong();
					linkIndex.seek(pointer);
					linkIds = new int[numberOfLinks];
					for (int j=0; j<numberOfLinks; j++) {
						linkIds[j] = linkIndex.readInt();
					}
					return true;
				} else {
					numberOfLinks = 0;
					foundId = docid;
					linkIds = null;
					return false;
				}
			}
			long mid = (begin + end) / 2;
			float fmid = (begin + end) / 2.0F;
			if (fmid - mid >= 0.5) {
				mid++;
			}
			docIndex.seek(mid*entryLength);
			int docid = docIndex.readInt();
			if (docid > id)
				end = mid - 1;
			else
				begin = mid;
		}
		return false;
	}
	/**
	 * Returns the number of outgoing links for a specific document.
	 * If the document does not have any links, then the returned
	 * value is 0.
	 * @param docid The docid of the document we are looking for.
	 */
	public int getNumberOfOutLinks(int docid) throws IOException {
		if (seekLinks(outgoingDocumentIndex, outgoingLinkIndex, docid))
			return numberOfLinks;
		return 0;
	}
	/**
	 * Returns the number of incoming links for a specific document.
	 * If the document does not have any links, then the returned
	 * value is 0.
	 * @param docid The docid of the document we are looking for.
	 */
	public int getNumberOfInLinks(int docid) throws IOException {
		if (seekLinks(incomingDocumentIndex, incomingLinkIndex, docid))
			return numberOfLinks;
		return 0;
	}
	/**
	 * Returns the number of outgoing links for a specific document within
	 * a given set of documents. If the document does not have
	 * any links in the specified set of documents, then the returned
	 * value is 0
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents containing their docids as Integers.
	 */
	public int getNumberOfOutLinks(int docid, HashSet docids) throws IOException {
		int numberOfLinksInSet = 0;
		if (!seekLinks(outgoingDocumentIndex, outgoingLinkIndex,docid))
			return 0;
		for (int i=0; i< numberOfLinks; i++) {
			if (docids.contains(new Integer(linkIds[i]))) {
				numberOfLinksInSet++;
			}
		}
		return numberOfLinksInSet;
	}
	/**
	 * Returns the number of incoming links for a specific document within
	 * a given set of documents. If the document does not have any links in the
	 * specified set of documents, then the returned value is 0.
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents containing their docids as Integers.
	 */
	public int getNumberOfInLinks(int docid, HashSet docids) throws IOException {
		int numberOfLinksInSet = 0;
		if (!seekLinks(outgoingDocumentIndex, outgoingLinkIndex,docid))
			return 0;
		for (int i=0; i< numberOfLinks; i++) {
			if (docids.contains(new Integer(linkIds[i]))) {
				numberOfLinksInSet++;
			}
		}
		return numberOfLinksInSet;
	}
	/**
	 * Return the outgoing links of the document we were looking for. If the
	 * document does not have any links, then the returned value is null.
	 * @param docid The docid of the document we are looking for.
	 */
	public int[] getOutLinks(int docid) throws IOException {
		if (!seekLinks(outgoingDocumentIndex, outgoingLinkIndex, docid))
			return null;
		return linkIds;
	}
	/**
	 * Return the incoming links of the document we were looking for. If the
	 * document does not have any links, then the returned value is null.
	 * @param docid The docid of the document we are looking for.
	 */
	public int[] getInLinks(int docid) throws IOException {
		if (!seekLinks(incomingDocumentIndex, incomingLinkIndex, docid))
			return null;
		return linkIds;
	}
	/**
	 * Return the outgoing links of the document we were looking for, within a
	 * given set of documents. If the document does not have any links, then
	 * the returned value is null.
	 * @param docid The docid of the document we are looking for.
	 * @param docids The set of documents containing their docids as Integers.
	 */
	 public int[] getOutLinks(int docid, HashSet docids) throws IOException {
		int numberOfLinksInSet = 0;
		if (!seekLinks(outgoingDocumentIndex, outgoingLinkIndex, docid))
			return null;
		int[] tmp = new int[numberOfLinks];
		for (int i=0; i<numberOfLinks; i++ ) {
			if (docids.contains(new Integer(linkIds[i]))) {
				tmp[numberOfLinksInSet++] = linkIds[i];
			}
		}
		if (numberOfLinksInSet==0)
			return null;
		int[] linksWithInSet = new int[numberOfLinksInSet];
		for (int i=0; i<linksWithInSet.length; i++) {
			linksWithInSet[i] = tmp[i];
		}
		return linksWithInSet;
	 }
	 /**
	  * Return the incoming links of the document we were looking for, within a
	  * given set of documents. If the document does not have any links, then the
	  * returned value is null.
	  * @param docid The docid of the document we are looking for.
	  * @param docids The set of documents containing their docids as Integers.
	 */
	 public int[] getInLinks(int docid, HashSet docids) throws IOException {
		 int numberOfLinksInSet = 0;
		 if (!seekLinks(incomingDocumentIndex, incomingLinkIndex, docid))
			 return null;
		int[] tmp = new int[numberOfLinks];
		for (int i=0; i<numberOfLinks; i++ ) {
			if (docids.contains(new Integer(linkIds[i]))) {
				tmp[numberOfLinksInSet++] = linkIds[i];
			}
		}
		if (numberOfLinksInSet==0)
			return null;
		int[] linksWithInSet = new int[numberOfLinksInSet];
		for (int i=0; i<linksWithInSet.length; i++) {
			linksWithInSet[i] = tmp[i];
		}
		return linksWithInSet;
	 }
 	public void close() throws IOException {
		outgoingLinkIndex.close();
		outgoingDocumentIndex.close();
		incomingLinkIndex.close();
		incomingDocumentIndex.close();
	}
 	
 	public static void main(String[] args) {
 		if (args[0].equals("-help")) {
 			System.out.println("usage: LinkServer [-help] [-create] [-getindegree] [-getoutdegree] [-getinlinks] [-getoutlinks]");
 			System.exit(1);
 		} else if (args[0].equals("-create")) {
 			try {
	 			LinkServer lServer = new LinkServer();
	 			lServer.createIndex();
	 			lServer.close();
 			} catch(IOException ioe) {
 				System.err.println("IO exception while creating link index. exiting");
				System.err.println(ioe);
				ioe.printStackTrace();
 				System.exit(1);
 			}
 		} else if (args[0].equals("-getindegree")) {
 			int docid = Integer.parseInt(args[1]);
 			try {
	 			LinkServer lServer = new LinkServer();
	 			System.out.println("indegree for docid " + docid + " is " + lServer.getNumberOfInLinks(docid));
	 			lServer.close();
 			} catch(IOException ioe) {
 				System.err.println("IO exception while reading link index. exiting");
				System.err.println(ioe);
                ioe.printStackTrace();
 				System.exit(1);
 			}
 		} else if (args[0].equals("-getoutdegree")) {
 			int docid = Integer.parseInt(args[1]);
 			try {
	 			LinkServer lServer = new LinkServer();
	 			System.out.println("outdegree for docid " + docid + " is " + lServer.getNumberOfOutLinks(docid));
	 			lServer.close();
 			} catch(IOException ioe) {
 				System.err.println("IO exception while reading link index. exiting");
				System.err.println(ioe);
                ioe.printStackTrace();
 				System.exit(1);
 			}
 		} else if (args[0].equals("-getinlinks")) {
 			int docid = Integer.parseInt(args[1]);
 			try {
	 			LinkServer lServer = new LinkServer();
	 			System.out.print("inlinks for docid " + docid + " : ");
	 			int[] inlinks = lServer.getInLinks(docid);
	 			for (int i=0; i<inlinks.length; i++) 
	 				System.out.print(inlinks[i] + " ");
	 			System.out.println();
	 			lServer.close();
 			} catch(IOException ioe) {
 				System.err.println("IO exception while reading link index. exiting");
				System.err.println(ioe);
                ioe.printStackTrace();
 				System.exit(1);
 			}
 		} else if (args[0].equals("-getoutlinks")) {
 			int docid = Integer.parseInt(args[1]);
 			try {
	 			LinkServer lServer = new LinkServer();
	 			System.out.print("outlinks for docid " + docid + " : ");
	 			int[] outlinks = lServer.getOutLinks(docid);
	 			for (int i=0; i<outlinks.length; i++) 
	 				System.out.print(outlinks[i] + " ");
	 			System.out.println();
	 			lServer.close();
 			} catch(IOException ioe) {
 				System.err.println("IO exception while reading link index. exiting");
				System.err.println(ioe);
	            ioe.printStackTrace();
 				System.exit(1);
 			}
 		}

 	}
}
