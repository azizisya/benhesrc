package uk.ac.gla.terrier.applications;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class ManageBlackLists {

	private Logger logger = Logger.getRootLogger();
	
	//the number of black lists to manage
	//should be the same with the number of query servers
	protected int numOfFiles;
	
	//an array of hashsets, for each of the query servers.
	protected TIntHashSet[] blackLists;
	
	//the path to load/save the black lists
	protected String path;
	
	//the prefix of the black lists
	protected String prefix;
	
	//the extension of the filename
	protected String extension = ".black.oos.gz";
	
	//the number of documents in each of the query servers
	//we need that in order to compute the correct local docid
	//for a given global docid
	protected int[] numOfDocs;
	
	public ManageBlackLists(String path, String prefix, int numOfFiles) {
		this.path = path;
		this.prefix = prefix; 
		this.numOfFiles = numOfFiles;
		blackLists = new TIntHashSet[numOfFiles];
		for (int i=0; i<numOfFiles; i++) 
			blackLists[i] = new TIntHashSet();
	}
	
	public void load() throws IOException {
		numOfDocs = new int[numOfFiles];
		blackLists = new TIntHashSet[numOfFiles];
		for (int i=0; i<numOfFiles; i++) {
			String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix + "_" + i + extension;
			ObjectInputStream ois = new ObjectInputStream(
				Files.openFileStream(filename));
				//new GZIPInputStream(new FileInputStream(filename)));
			try {
				numOfDocs[i] = ((Integer)ois.readObject()).intValue();
				blackLists[i] = (TIntHashSet)ois.readObject();
			} catch(ClassNotFoundException cnfe) {
				logger.error("class not found exception while loading blacklists", cnfe);
			}
			ois.close();
		}
	}
	
	public void save() throws IOException {
		for (int i=0; i<numOfFiles; i++) {
			String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix + "_" + i + extension;
			ObjectOutputStream oos = new ObjectOutputStream(Files.writeFileStream(filename));
				//new GZIPOutputStream(new FileOutputStream(filename)));
			oos.writeObject(new Integer(numOfDocs[i]));
			oos.writeObject(blackLists[i]);
			oos.close();
		}
	}
	
	public void reset(int numOfDocs[]) {
		blackLists = new TIntHashSet[numOfFiles];
		this.numOfDocs = new int[numOfFiles];
		for (int i=0; i<numOfFiles; i++) {
			blackLists[i] = new TIntHashSet();
			this.numOfDocs[i] = numOfDocs[i];
		}
	}
	
	public void add(TIntArrayList docids) {
		int length = docids.size();
		int server = 0;
		int originalDocid;
		for (int i=0; i<length; i++) {
			int docid = docids.get(i);
			originalDocid = docid;
			server = 0;
			for (int j=0; j<numOfFiles; j++) {
				if (docid >= numOfDocs[j])
					docid = docid - numOfDocs[j];
				else break; //the doc with docid is in the i-th server
				server++;
			}
			blackLists[server].add(docid);
			logger.debug("global docid " + originalDocid + " corresponds to the docid " + docid + " in server " + server);
		}
	}
	
	public void remove(TIntArrayList docids) {
		int length = docids.size();
		int server = 0;
		int originalDocid;
		for (int i=0; i<length; i++) {
			int docid = docids.get(i);
			originalDocid = docid;
			server = 0;
			for (int j=0; j<numOfFiles; j++) {
				if (docid >= numOfDocs[i])
					docid = docid - numOfDocs[i];
				else break; //the doc with docid is in the i-th server
				server++;
			}
			blackLists[server].remove(docid);
			logger.debug("global docid " + originalDocid + " corresponds to the docid " + docid + " in server " + server);
		}
	}
	
	public void print() {
		int docidOffset = 0;
		for (int i=0; i<numOfFiles; i++) {
			System.out.println("black list for query server " + i + " containing " + numOfDocs[i] + " documents.");
			int[] docids = blackLists[i].toArray();
			int localDocid; 
			int globalDocid;
			for (int j=0; j<docids.length; j++) {
				localDocid = docids[j];
				globalDocid = localDocid + docidOffset;
				System.out.println("global docid: " + globalDocid + " local docid: " + localDocid);
			}
			docidOffset += numOfDocs[i];
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length <4) {
			System.out.println("usage: ManageBlackLists option path prefix numOfServers ...");
			System.out.println("options are");
			System.out.println("-create: creates a number of empty black lists. each server contains a given number of documents");
			System.out.println("-insert: inserts the given docids to the black lists");
			System.out.println("-remove: removes the given docids from the black lists");
			System.out.println("-print:  prints the black lists");
		}

		String path = args[1];
		String prefix = args[2];
		int numOfServers = Integer.parseInt(args[3]);
		ManageBlackLists mbl = new ManageBlackLists(path, prefix, numOfServers);
		
		if (args[0].equals("-create")) {
			int[] numOfDocs = new int[numOfServers];
			for (int i=4; i<4+numOfServers; i++) {
				numOfDocs[i-4] = Integer.parseInt(args[i]);
			}
			mbl.reset(numOfDocs);
			mbl.save();
		} else if (args[0].equals("-insert")) {
			mbl.load();
			
			TIntArrayList docids = new TIntArrayList();
	
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			while((line = br.readLine()) != null) {
				docids.add(Integer.parseInt(line));
			}
			
			mbl.add(docids);
			mbl.save();
		} else if (args[0].equals("-remove")) {
			mbl.load();
			
			TIntArrayList docids = new TIntArrayList();

			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String line = null;
			while((line = br.readLine()) != null) {
				docids.add(Integer.parseInt(line));
			}
			
			mbl.remove(docids);
			mbl.save();
			
		} else if (args[0].equals("-print")) {
			mbl.load();
			mbl.print();
		}
		
	}

}
