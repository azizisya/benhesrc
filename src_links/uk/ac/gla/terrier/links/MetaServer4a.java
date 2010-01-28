package uk.ac.gla.terrier.links;
import gnu.trove.TLongArrayList;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
/** Metaserver - with variable-size entry lengths using Deflate ZLib compression:
 * Two files: .meta_dat and .meta_idx
 * .meta_data - the data file, contains the data for each document
 * Records a fixed length, as are key sizes.
 * Record length and key length are defined in the meta_idx file -
 * the index file.
 * The index file for this MetaServer is a gzip'd objectstream, containing
 * the following:
 * long[] : offsets of each entry, size NoDocs
 * Integer: compressionlevel 
 * Integer: record uncompressed size
 * String[] : key names
 * int[] : key lengths
 * 
 * 
 * thoughts: instead of having fixed lengths, we should have variable length fields and
 *           save a preprocessed text, split into sentences, instead of the text itself
 *           this will save time for computing the snippets. we can also ignore sentences 
 *           that are empty, or 1 word long - wicked
 */
public class MetaServer4a extends MetaServer4 {
	
	private static Logger logger = Logger.getRootLogger();
	protected long[] docid2offsets;
	protected int compressionLevel;
	protected int recordLength;
	protected long fileLength;
	
	public MetaServer4a(String path, String prefix)
		throws IOException
	{
		super(path, prefix);
		fileLength = (new File(path + ApplicationSetup.FILE_SEPARATOR + prefix + DATA_SUFFIX)).length();
		
		int offsetsLength = docid2offsets.length;
	}
	
	public String getItem(String Key, int docid)
        throws IOException
    {
		Inflater unzip = new Inflater();		
		unzip.setInput(
			read(docid2offsets[docid],
					(docid+1)==docid2offsets.length ? (int)(fileLength-docid2offsets[docid])
					                                : (int)(docid2offsets[docid+1] - docid2offsets[docid])));
					
        byte[] bOut = new byte[recordLength];
		try {
			unzip.inflate(bOut);
		} catch(DataFormatException dfe) {
			dfe.printStackTrace();
		}
		return new String(bOut, keyOffsets.get(Key), keyLengths.get(Key)).trim();
    }

    public String[] getItems(String[] Keys, int docid) throws IOException {
		Inflater unzip = new Inflater();
		unzip.setInput(
				read(docid2offsets[docid],
						(docid+1)==docid2offsets.length ? (int)(fileLength-docid2offsets[docid])
						                                : (int)(docid2offsets[docid+1] - docid2offsets[docid])));
		byte[] bOut = new byte[recordLength];
		try {
			unzip.inflate(bOut);
		} catch(DataFormatException dfe) {
			dfe.printStackTrace();
		}
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

	protected void loadIndex() throws IOException {
	    ObjectInputStream ois = new ObjectInputStream(
			Files.openFileStream(path+ApplicationSetup.FILE_SEPARATOR+prefix+INDEX_SUFFIX));
			//new GZIPInputStream(new FileInputStream(path+ApplicationSetup.FILE_SEPARATOR+prefix+INDEX_SUFFIX)));
	    
	    String[] keyNames = null;
	    int[] keyLens = null;
	    
	    try {
			//1. (long[]) length (numDocs+1) - offsets in file
			docid2offsets = (long[])ois.readObject();
			//2. int - how much zlib was used
			compressionLevel = ois.readInt();
			//3. int - how big each record was before compression
			recordLength = ois.readInt();
			
			//4. key names
			keyNames = (String[])ois.readObject();
			//5. lengths of each key
			keyLens = (int[])ois.readObject();
	    	logger.debug("docid2offsets.length: " + docid2offsets.length + " compressionLevel: " + compressionLevel + " recordLength: " + recordLength);
	    } catch(ClassNotFoundException cnfe) {
	    	//shouldn't be here
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
	
	protected byte[] read(long offset, int bytes) throws IOException {		
		byte[] out = new byte[bytes];
		dataSourceChannel.read(MappedByteBuffer.wrap(out), offset);
		return out;
		
//		int selectedBuffer;
//		int offsetInBuffer;
//		//System.out.println("offset: " + offset);
//		final int offsetsLength = docid2offsets.length;
//		if (offset < docid2offsets[(offsetsLength/2)]) {
//			selectedBuffer = 0;
//			offsetInBuffer = (int)offset;
//		} else {
//			selectedBuffer = 1;
//			offsetInBuffer = (int) (offset - docid2offsets[(offsetsLength/2)]);
//		} 
//		//System.out.println("selectedBuffer: " + selectedBuffer + " offsetInBuffer: " + offsetInBuffer);
//		buffers[selectedBuffer].position(offsetInBuffer);
//		byte[] out = new byte[bytes];
//		buffers[selectedBuffer].get(out);
//		return out;
	}

	
	public static void createMetaServer(String path, String prefix, String[] keyNames, int[] keyLens, BufferedReader[][] input) {
		ObjectOutputStream indexOutput = null;
		DataOutputStream dataOutput = null;
		int compressionLevel = 5;
		int entryLength = 0; 
		TLongArrayList offsets = null;
		int howManyOffsetsFinished = 0;
		try {
			String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix;
			indexOutput = new ObjectOutputStream(
				Files.writeFileStream(filename + INDEX_SUFFIX));
				//new GZIPOutputStream(new FileOutputStream(filename + INDEX_SUFFIX)));
			dataOutput = new DataOutputStream(
				Files.writeFileStream(filename + DATA_SUFFIX));
				//new BufferedOutputStream(new FileOutputStream(filename + DATA_SUFFIX)));
						
			
			
			for (int i=0; i<keyLens.length; i++) {
				entryLength += keyLens[i];
			}
			ByteArrayOutputStream baos = null;
			Deflater zip = new Deflater();
			zip.setLevel(compressionLevel);
			offsets = new TLongArrayList();
			byte[] buffer = new byte[2*entryLength];//for compressing
			byte[] spaces = new byte[entryLength];//for padding
			long currentOffset = 0;//the offset in the file, increasing for each entry
			Arrays.fill(spaces, (byte)32);
			                 		
			
			
			
			boolean moreData = false;
			int fileToRead = 0; //max value is input[0].length;
			do {
				//creating the data
				String[] data = new String[input.length];
				for (int i=0; i<input.length; i++) {
					moreData = ((data[i] = input[i][fileToRead].readLine())!=null);
					//System.out.println(fileToRead + " " + moreData);
					if (!moreData && (fileToRead+1)==input[i].length) {
						//System.out.println("breaking");
						break; 
					} else if (!moreData && fileToRead+1<input[i].length) {
						fileToRead++;
						//System.out.println("to the next file");
						moreData = ((data[i] = input[i][fileToRead].readLine())!=null);
					}
					int idx = data[i].indexOf(' ');
					try{
						data[i] = (idx==-1)?("NA"):(data[i].substring(idx));
					}catch(StringIndexOutOfBoundsException e){
						System.err.println(data[i]);
						e.printStackTrace();
						System.exit(1);
					}
					data[i] = data[i].substring(0, Math.min(data[i].length(), keyLens[i]));
					//System.out.print(data[i] + " ");
				}
				//System.out.println();
				if (!moreData) 
					break;
				if (data[2].matches("^\\s+$") || data[2].equals("")) {
					data[2] = data[1].substring(0, Math.min(data[1].length(), keyLens[2]));
				}
				baos = new ByteArrayOutputStream(entryLength);
				for (int i=0; i<input.length; i++) {
					byte[] bytesToWrite = data[i].getBytes();
					int numberOfBytesToWrite = bytesToWrite.length;
					baos.write(bytesToWrite, 0, Math.min(numberOfBytesToWrite, keyLens[i]));
					if (numberOfBytesToWrite < keyLens[i]) 
						baos.write(spaces, 0, keyLens[i]-numberOfBytesToWrite);
				}
				zip.reset();
				zip.setInput(baos.toByteArray());
				offsets.add(currentOffset);
				howManyOffsetsFinished++;
				
				zip.finish();
				int numOfCompressedBytes = zip.deflate(buffer);
				if (numOfCompressedBytes == 0) {
					zip.reset();
					zip.setInput(baos.toByteArray());
					zip.finish();
					numOfCompressedBytes = zip.deflate(buffer);
				}
				//System.out.println("current offset: " + currentOffset + " numOfCompressedBytes: " + numOfCompressedBytes);
				
				currentOffset += numOfCompressedBytes;
				dataOutput.write(buffer, 0, numOfCompressedBytes);
			} while (moreData);
			//assuming url, abstract, title
			//dataOutput.close();
			//finished with the data
			
			//creating the index
			//indexOutput.writeObject(offsets.toNativeArray());
			//indexOutput.writeObject(new Integer(compressionLevel));
			//indexOutput.writeObject(new Integer(entryLength));
			//indexOutput.writeObject(keyNames);
			//indexOutput.writeObject(keyLens);
			//indexOutput.close();
			//finished with the index

		} catch(IOException ioe) {
			ioe.printStackTrace();
		} finally {
			logger.debug("howManyOffsetsFinished: " + howManyOffsetsFinished);
			try {
				dataOutput.close();
				//finished with the data
				
				//creating the index
				indexOutput.writeObject(offsets.toNativeArray());
				indexOutput.writeInt(compressionLevel);
				indexOutput.writeInt(entryLength);
				indexOutput.writeObject(keyNames);
				indexOutput.writeObject(keyLens);
				indexOutput.close();
			} catch(IOException ioe) {
				logger.error("ioexception while closing the metadata server.", ioe);
			}
		}
	}

	public static void main(String[] args) {
		//calls the method, so that the class is loaded and 
		//the logger is initialised.
		ApplicationSetup.setupFilenames();
		try {
		if (args.length > 0) {
			if (args[0].equals("create")) {
				//params
				//path prefix numOfKeys numOfInputFiles [keyname keylen keyinput]+
				String path = args[1];
				String prefix = args[2];
				int numOfKeys = Integer.parseInt(args[3]);
				int numOfInputFiles = Integer.parseInt(args[4]);
				String[] keyNames = new String[numOfKeys];
				int[] keyLens = new int[numOfKeys];
				BufferedReader[][] inputs = new BufferedReader[numOfKeys][numOfInputFiles];
				
				int argIndex = 5;
				for (int i=0; i<numOfKeys; i++) {
					logger.debug(args[argIndex]); keyNames[i] = args[argIndex++];
					logger.debug(args[argIndex]); keyLens[i] = Integer.parseInt(args[argIndex++]);

					for (int j=0; j<numOfInputFiles; j++) {
						logger.debug(args[argIndex]);
						String filename = args[argIndex++];
						inputs[i][j] = Files.openFileReader(filename);
					}
				}
				MetaServer4a.createMetaServer(path, prefix, keyNames, keyLens, inputs);
				for (int i=0; i<numOfKeys; i++) {
					for (int j=0; j<numOfInputFiles; j++) {
						inputs[i][j].close();						
					}
				}	
				final Index i = Index.createIndex(path, prefix);
				if (i != null)
				{
					i.addIndexStructure("meta", "uk.ac.gla.terrier.links.MetaServer4a");
					i.addIndexStructure("metacache", "uk.ac.gla.terrier.links.MetaCache", "", "");
					i.flush();
					i.close();
				}
			} else if (args[0].equals("test")) {
				//params
				//path prefix keyname docid
				String path = args[1];
				String prefix = args[2];
				String keyname = args[3];
				int docid = Integer.parseInt(args[4]);
				MetaServer4a metaServer = new MetaServer4a(path, prefix);
				System.out.println(keyname + " " + docid + ": " + metaServer.getItem(keyname, docid));
				metaServer.close();
			} else if (args[0].equals("test2")) {
				//params
				//path prefix keyname docno
				String path = args[1];
				String prefix = args[2];
				String keyname = args[3];
				String docno = args[4];
				Index index = Index.createIndex(path, prefix);
				int docid = index.getDocumentIndex().getDocumentId(docno);
				System.out.println("docno: "+docno+", docid: "+docid);
				MetaServer4a metaServer = new MetaServer4a(path, prefix);
				System.out.println(keyname + " " + docno + ": " + metaServer.getItem(keyname, docid));
				metaServer.close(); index.close();
			}
			else if (args[0].equals("random")) {
				//params
				//path prefix keyname maxdocs numofrandomtries 
				System.out.println("args[0]:" + args[0]);
				String path = args[1];
				String prefix = args[2];
				String keyname = args[3];
				int maxDocs = Integer.parseInt(args[4]);
				int numOfRandomTries = Integer.parseInt(args[5]);
				Random random = new Random();
				System.out.println("numOfRandomTries: " + numOfRandomTries);
				System.out.println("maxDocs: " + maxDocs);
				MetaServer4a metaServer = new MetaServer4a(path, prefix);
				long startTime = System.currentTimeMillis();
				for (int i=0; i<numOfRandomTries; i++) {
					int docid = random.nextInt(maxDocs);
					metaServer.getItem(keyname, docid);
					//System.out.println(keyname + " " + docid + ": " + metaServer.getItem(keyname, docid));
				}
				long endTime = System.currentTimeMillis();
				System.out.println("time spent: " + ((endTime-startTime)/1000.0d));
				metaServer.close();
			}
		}
		} catch(IOException ioe) {
			logger.error("IOException while creating the metadata server.", ioe);
		}
	}
	
}
