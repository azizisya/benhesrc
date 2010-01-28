package uk.ac.gla.terrier.links;

import gnu.trove.TLongArrayList;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

import org.apache.log4j.Logger;

import uk.ac.gla.terrier.utility.ApplicationSetup;
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
public class MetaServer4b extends MetaServer4
{
	protected long[] docid2offsets;
	protected int compressionLevel;
	protected int recordLength;

	protected LinkedList<Inflater> inflaters = new LinkedList<Inflater>();
	
	protected LinkedList<byte[]> byteBuffers = new LinkedList<byte[]>();
	
	protected LinkedList<byte[]> readBuffers = new LinkedList<byte[]>();
	
	protected LinkedList<PooledObject> pool = new LinkedList<PooledObject>();
	
	protected int listSize = 10;
	
	protected RandomAccessFile[] dataSources = null;
	protected FileChannel[] dataSourceChannels = null;
	protected long[] fileLengths = null;
	protected int numOfDataSources;
	protected int numOfDocsPerFile;
	
	public MetaServer4b(String path, String prefix) throws IOException {
		super(path, prefix);
		//fileLength = (new File(path + ApplicationSetup.FILE_SEPARATOR + prefix + DATA_SUFFIX)).length();
		
		dataSource.close();
		dataSourceChannel.close();
		
		String filenamePrefix = path + ApplicationSetup.FILE_SEPARATOR + prefix;
		dataSources = new RandomAccessFile[numOfDataSources];
		dataSourceChannels = new FileChannel[numOfDataSources];
		fileLengths = new long[numOfDataSources];
		for (int i=0; i<numOfDataSources; i++) {
			String filename = filenamePrefix + "_" + i + DATA_SUFFIX;
			System.out.println("filename: " + filename);
			dataSources[i] = new RandomAccessFile(filename, "r");
			dataSourceChannels[i] = dataSources[i].getChannel();
			fileLengths[i] = dataSources[i].length();
		}
		
		for (int i=0; i<listSize; i++) {
			PooledObject po = new PooledObject();
			po.unzip = new Inflater();
			po.byteBuffer = new byte[recordLength];
			po.readBytes = new byte[2*recordLength];
			pool.add(po);
			//inflaters.add(new Inflater());
			//byteBuffers.add(new byte[recordLength]);
			//readBuffers.add(new byte[2*recordLength]);
		}
	}
	
	protected void read(long offset, int bytes, int channel, byte[] out) throws IOException {
		//byte[] out = readBuffers.poll();
		dataSourceChannels[channel].read(MappedByteBuffer.wrap(out), offset);
		//return out;
	}
	
	public String getItem(String Key, int docid) throws IOException {
		//PooledObject po = pool.getFirst(); 
		Inflater unzip = new Inflater();
		//Inflater unzip = inflaters.poll();		
		//Inflater unzip = po.unzip;
		//unzip.reset();
		
		//byte[] bOut = byteBuffers.poll();
		//byte[] bOut = po.byteBuffer;
		byte[] bOut = new byte[recordLength];
		
		final int channel = docid / numOfDocsPerFile;
		
		int length;
		
		if ((docid+1 == docid2offsets.length) || docid2offsets[docid]>docid2offsets[docid+1]) {
			length = (int)(fileLengths[channel] - docid2offsets[docid]); 
		} else {
			length = (int)(docid2offsets[docid+1] - docid2offsets[docid]); 
		}

		//byte[] readBytes = po.readBytes;
		byte[] readBytes = new byte[2*recordLength];
		
		read(docid2offsets[docid], length, channel, readBytes); 
		
		unzip.setInput(readBytes, 0, length);
        
		try {
			unzip.inflate(bOut);
		} catch(DataFormatException dfe) {
			dfe.printStackTrace();
			System.err.println("" + docid + " " + channel + " " + docid2offsets[docid] + " " + length);
		}
		String toReturn = new String(bOut, keyOffsets.get(Key), keyLengths.get(Key)).trim();
		//inflaters.addLast(unzip);
		//byteBuffers.addLast(bOut);
		//readBuffers.addLast(readBytes);
		//pool.addLast(po);
		return toReturn;
    }

    public String[] getItems(String[] Keys, int docid) throws IOException {
		//Inflater unzip = inflaters.poll();
    	//PooledObject po = pool.getFirst();
    	//Inflater unzip = po.unzip;
		//unzip.reset();
    	Inflater unzip = new Inflater();

		int channel = docid / numOfDocsPerFile;
		
		int length;
		
		if ((docid+1 == docid2offsets.length) || docid2offsets[docid]>docid2offsets[docid+1]) {
			length = (int)(fileLengths[channel] - docid2offsets[docid]); 
		} else {
			length = (int)(docid2offsets[docid+1] - docid2offsets[docid]); 
		}
		
		//byte[] readBytes = po.readBytes;
		byte[] readBytes = new byte[2*recordLength];
		read(docid2offsets[docid], length, channel, readBytes); 
		unzip.setInput(readBytes, 0, length);

		//byte[] bOut = po.byteBuffer; //byteBuffers.poll();
		byte[] bOut = new byte[recordLength];
		try {
			unzip.inflate(bOut);
		} catch(DataFormatException dfe) {
			dfe.printStackTrace();
			System.err.println("" + docid + " " + channel + " " + docid2offsets[docid] + " " + length);
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
        //inflaters.addLast(unzip);
        //byteBuffers.addLast(bOut);
        //readBuffers.addLast(readBytes);
        //pool.addLast(po);
        return sOut;
    }

	protected void loadIndex() throws IOException {
	    ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(path+ApplicationSetup.FILE_SEPARATOR+prefix+INDEX_SUFFIX)));
	    
	    String[] keyNames = null;
	    int[] keyLens = null;
	    
	    try {
			//1. (long[]) length (numDocs+1) - offsets in file
			docid2offsets = (long[])ois.readObject();
			//2. int - how much zlib was used
			compressionLevel = ((Integer)ois.readObject()).intValue();
			//3. int - how big each record was before compression
			recordLength = ((Integer)ois.readObject()).intValue();
			
			//4. key names
			keyNames = (String[])ois.readObject();
			//5. lengths of each key
			keyLens = (int[])ois.readObject();
			//6. number of files
			numOfDataSources = ((Integer)ois.readObject()).intValue();
			//7. number of documents per metadata file
			numOfDocsPerFile = ((Integer)ois.readObject()).intValue();
	    	Logger.getRootLogger().debug("docid2offsets.length: " + docid2offsets.length + " compressionLevel: " + compressionLevel + " recordLength: " + recordLength);
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
		
	public static void createMetaServer(String path, String prefix, String[] keyNames, int[] keyLens, BufferedReader[][] input, int numOfDocsPerFile) {
		int numOfDataSources = 0;
		int docid = 0;
		ObjectOutputStream indexOutput = null;
		DataOutputStream dataOutput = null;
		int compressionLevel = 5;
		int entryLength = 0; 
		TLongArrayList offsets = null;
		int howManyOffsetsFinished = 0;
		try {
			String filename = path + ApplicationSetup.FILE_SEPARATOR + prefix;
			indexOutput = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename + INDEX_SUFFIX)));
			dataOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + "_" + numOfDataSources + DATA_SUFFIX)));
			
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
					data[i] = data[i].substring(data[i].indexOf(' '));
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
				
				docid++;
				if (docid >= numOfDocsPerFile) {
					docid = 0;
					currentOffset = 0;
					dataOutput.flush();
					dataOutput.close();
					numOfDataSources++;
					dataOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename + "_" + numOfDataSources + DATA_SUFFIX)));
				}
			} while (moreData);

		} catch(IOException ioe) {
			ioe.printStackTrace();
		} finally {
			System.err.println("howManyOffsetsFinished: " + howManyOffsetsFinished);
			try {
				dataOutput.close();
				//finished with the data
				
				//creating the index
				indexOutput.writeObject(offsets.toNativeArray());
				indexOutput.writeObject(new Integer(compressionLevel));
				indexOutput.writeObject(new Integer(entryLength));
				indexOutput.writeObject(keyNames);
				indexOutput.writeObject(keyLens);
				System.err.println("writing " + (numOfDataSources+1));
				indexOutput.writeObject(new Integer(numOfDataSources+1));
				indexOutput.writeObject(new Integer(numOfDocsPerFile));
				indexOutput.close();
			} catch(IOException ioe) {
				System.err.println("ioexception while closing the metadata server: " + ioe);
			}
		}
	}

	public static void main(String[] args) {
		try {
		if (args.length > 0) {
			if (args[0].equals("create")) {
				//params
				//path prefix numOfKeys numOfInputFiles numOfDocsPerFile [keyname keylen keyinput]+
				String path = args[1];
				String prefix = args[2];
				int numOfKeys = Integer.parseInt(args[3]);
				int numOfInputFiles = Integer.parseInt(args[4]);
				int numOfDocsPerFile = Integer.parseInt(args[5]);
				String[] keyNames = new String[numOfKeys];
				int[] keyLens = new int[numOfKeys];
				BufferedReader[][] inputs = new BufferedReader[numOfKeys][numOfInputFiles];
				
				int argIndex = 6;
				for (int i=0; i<numOfKeys; i++) {
					System.out.println(args[argIndex]); keyNames[i] = args[argIndex++];
					System.out.println(args[argIndex]); keyLens[i] = Integer.parseInt(args[argIndex++]);

					for (int j=0; j<numOfInputFiles; j++) {
						System.out.println(args[argIndex]); inputs[i][j] = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(args[argIndex++]))));						
					}
				}
				MetaServer4b.createMetaServer(path, prefix, keyNames, keyLens, inputs, numOfDocsPerFile);
				for (int i=0; i<numOfKeys; i++) {
					for (int j=0; j<numOfInputFiles; j++) {
						inputs[i][j].close();						
					}
				}
			} else if (args[0].equals("test")) {
				//params
				//path prefix keyname docid
				String path = args[1];
				String prefix = args[2];
				String keyname = args[3];
				int docid = Integer.parseInt(args[4]);
				MetaServer4b metaServer = new MetaServer4b(path, prefix);
				System.out.println(keyname + " " + docid + ": " + metaServer.getItem(keyname, docid));
				metaServer.close();
			} else if (args[0].equals("random")) {
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
				MetaServer4b metaServer = new MetaServer4b(path, prefix);
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
			ioe.printStackTrace();
		}
	}
	
	/** Closes the underlying structures.*/
	public void close() throws IOException {
		for (int i=0; i<numOfDataSources; i++) {
			dataSourceChannels[i].close();
			dataSources[i].close();
		}
	}
	
}

class PooledObject {
	
	public Inflater unzip;
	
	public byte[] byteBuffer;
	
	public byte[] readBytes;
}