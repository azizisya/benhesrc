package uk.ac.gla.terrier.links;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;

/** Bog standard metaserver:
 * Two files: .meta_dat and .meta_idx
 * .meta_data - the data file, contains the data for each document
 * Records a fixed length, as are key sizes.
 * Record length and key length are defined in the meta_idx file -
 * the index file.
 * The index file for this MetaServer is a gzip'd objectstream, containing
 * the following:
 * Integer : Fixed Entry Size
 * String[] : key names
 * int[] : key lengths
 */
public class MetaServer4 implements uk.ac.gla.terrier.links.MetaIndex {
	
	private static Logger logger = Logger.getRootLogger();
	
	public static final String INDEX_SUFFIX = ".meta_idx";
	public static final String DATA_SUFFIX = ".meta_dat";
	int EntryLength;

	protected TObjectIntHashMap keyOffsets;
	protected TObjectIntHashMap keyLengths;
	protected int keyCount;
	
	protected final String path;
	protected final String prefix;
	protected final RandomAccessFile dataSource;
	
	protected FileChannel dataSourceChannel;
		
	public MetaServer4(String path, String prefix) throws IOException {
		this.path = path; this.prefix = prefix;
		loadIndex();
		dataSource = new RandomAccessFile(path + ApplicationSetup.FILE_SEPARATOR + prefix + DATA_SUFFIX, "r");
		dataSourceChannel = dataSource.getChannel();

	}

	public String getItem(String Key, int docid) throws IOException {
		byte[] bOut = read(EntryLength * docid, EntryLength);
		return new String(bOut, keyOffsets.get(Key), keyLengths.get(Key)).trim();
	}

	public String[] getItems(String[] Keys, int docid) throws IOException {
		byte[] bOut = read(EntryLength * docid, EntryLength);
		final int kCount = Keys.length;
		String[] sOut = new String[kCount];
		for(int i=0;i<kCount;i++)
		{
			sOut[i] = new String(
				bOut, 
				keyOffsets.get(Keys[i]), 
				keyLengths.get(Keys[i])).trim();
		}
		return sOut;
	}
	
	public String[] getItems(String Key, int[] docids) throws IOException {
		final int numDocs = docids.length;
		String values[] = new String[docids.length];
		for(int i=0;i<numDocs;i++)
		{
			values[i] = getItem(Key, docids[i]).trim();
		}
		return values;
	}

	public String[][] getItems(String Keys[], int[] docids) throws IOException {
		int dLen = docids.length;
		String[][] saOut = new String[dLen][];
		for(int i=0;i<dLen;i++)
		{
			saOut[i] = getItems(Keys, docids[i]);
		}
		return saOut;
	}

	/** Add an items to the metadata index, under named Key for 
	 * docid, value value.
	 * @deprecated This data structure does not support updates */
	public void addItem(String Key, int docid, String value)
	{
		//do nothing
	}

	/** Closes the underlying structures.*/
	public void close() throws IOException {
		dataSourceChannel.close();
		dataSource.close();
	}

	protected byte[] read(long offset, int bytes) throws IOException {
//		byte[] out = new byte[bytes];
//		dataSource.seek(offset);
//		dataSource.readFully(out);
//		return out;
		
		byte[] out = new byte[bytes];
		dataSourceChannel.read(MappedByteBuffer.wrap(out), offset);
		return out;
		
//		byte[] out = new byte[bytes];
//		buffer.position((int)offset);
//		buffer.get(out, 0, bytes);
//		return out;
	}

	protected void loadIndex() throws IOException {

		String[] keyNames = null;
		int[] keyLens = null;
		
		ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(path + ApplicationSetup.FILE_SEPARATOR + prefix+INDEX_SUFFIX)));
		try {
			//1. length of 1 entry	
			EntryLength = ((Integer)ois.readObject()).intValue();
			//2. key names
			keyNames = (String[])ois.readObject();
			//3. lengths of each key
			keyLens = (int[])ois.readObject();
		} catch (ClassNotFoundException cnfe) {
			//we should never be here
		}
		//finished with index file
		ois.close();

		//now build the keyname and lengths into 2 maps:
		// keyname -> length & keyname -> offsets
		keyCount = keyNames.length;
		keyLengths = new TObjectIntHashMap(keyCount);
		keyOffsets = new TObjectIntHashMap(keyCount);
		int cumulativeOffset = 0;
		for(int i=0;i<keyCount;i++)
		{
			keyLengths.put(keyNames[i], keyLens[i]);
			keyOffsets.put(keyNames[i], cumulativeOffset);
			cumulativeOffset += keyLens[i];
		}
	}
	

	public static void createMetaServer(String path, String prefix, String[] keyNames, int[] keyLens, BufferedReader[] input) {
		try {
			String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix;
			ObjectOutputStream indexOutput = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename + INDEX_SUFFIX)));
			DataOutputStream dataOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + DATA_SUFFIX)));
			
			//creating the index
			int entryLength = 0; 
			for (int i=0; i<keyLens.length; i++) {
				entryLength += keyLens[i];
			}
			
			byte[] spaces = new byte[entryLength];
			Arrays.fill(spaces, (byte)32);
			                 		
			
			indexOutput.writeObject(new Integer(entryLength));
			indexOutput.writeObject(keyNames);
			indexOutput.writeObject(keyLens);
			indexOutput.close();
			//finished with the index
			
			boolean moreData = false;
			do {
			//creating the data
			String[] data = new String[input.length];
			for (int i=0; i<input.length; i++) {
				moreData = ((data[i] = input[i].readLine())!=null);
				if (!moreData)
					break;
				data[i] = data[i].substring(data[i].indexOf(' '));
				data[i] = data[i].substring(0, Math.min(data[i].length(), keyLens[i]));
			}
			if (!moreData) 
				break;
			if (data[2].matches("^\\s+$") || data[2].equals("")) {
				data[2] = data[1].substring(0, Math.min(data[1].length(), keyLens[2]));
			}
			for (int i=0; i<input.length; i++) {
				byte[] bytesToWrite = data[i].getBytes();
				int numberOfBytesToWrite = bytesToWrite.length;
				dataOutput.write(bytesToWrite, 0, Math.min(numberOfBytesToWrite, keyLens[i]));
				if (numberOfBytesToWrite < keyLens[i]) 
					dataOutput.write(spaces, 0, keyLens[i]-numberOfBytesToWrite);
			}
			} while (moreData);
			//assuming url, abstract, title
			dataOutput.close();
			//finished with the data
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
		if (args.length > 0) {
			logger.info("args[0] = " + args[0]);
			if (args[0].equals("create")) {
				//params
				//path prefix numOfKeys [keyname keylen keyinput]+
				String path = args[1];
				String prefix = args[2];
				int numOfKeys = Integer.parseInt(args[3]);
				String[] keyNames = new String[numOfKeys];
				int[] keyLens = new int[numOfKeys];
				BufferedReader[] inputs = new BufferedReader[numOfKeys];
				
				for (int i=0; i<numOfKeys; i++) {
					keyNames[i] = args[4+i*3];
					keyLens[i] = Integer.parseInt(args[4+i*3+1]);
					inputs[i] = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[4+i*3+2]))));
				}
				MetaServer4.createMetaServer(path, prefix, keyNames, keyLens, inputs);
				for (int i=0; i<numOfKeys; i++) {
					inputs[i].close();
				}
			} else if (args[0].equals("test")) {
				//params
				//path prefix keyname docid
				String path = args[1];
				String prefix = args[2];
				String keyname = args[3];
				int docid = Integer.parseInt(args[4]);
				MetaServer4 metaServer = new MetaServer4(path, prefix);
				logger.info(keyname + " " + docid + ": " + metaServer.getItem(keyname, docid));
				metaServer.close();
			} else if (args[0].equals("random")) {
				//params
				//path prefix keyname maxdocs numofrandomtries 
				logger.info("args[0]:" + args[0]);
				String path = args[1];
				String prefix = args[2];
				String keyname = args[3];
				int maxDocs = Integer.parseInt(args[4]);
				int numOfRandomTries = Integer.parseInt(args[5]);
				Random random = new Random();
				logger.info("numOfRandomTries: " + numOfRandomTries);
				logger.info("maxDocs: " + maxDocs);
				MetaServer4 metaServer = new MetaServer4(path, prefix);
				for (int i=0; i<numOfRandomTries; i++) {
					int docid = random.nextInt(maxDocs);
					logger.info(keyname + " " + docid + ": " + metaServer.getItem(keyname, docid));
				}
				metaServer.close();
			}
		}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
}
