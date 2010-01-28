package uk.ac.gla.terrier.structures.incrementalindex;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import gnu.trove.TIntArrayList;
import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** @author John Kane &amp; Craig Macdonald */
public class IncrementalIndexProperties {
	
	protected Properties Prop;

	public static final int Max_number_Of_Document_Memory_Segment = 
		Integer.parseInt(
				ApplicationSetup.getProperty("incrementalindex.initial.docspersegment", "1000"));	
	public static final int Max_number_Of_Segments = 
		Integer.parseInt(
				ApplicationSetup.getProperty("incrementalindex.maxsegments", "8"));
	
	public int currentNumberOfSegments = 0;
	/** Contains the segment Ids for all currently active segments. -1 means this slot empty */
	protected int[] currentSegmentIds = new int[Max_number_Of_Segments];
	
	/** We need to have unique segment Ids. */
	public int nextSegmentId = 0;
	
	public int nextDocumentId = 0;
	
	public long numberOfPointers = 0;
	public long numberOfTokens = 0;
	public int numberOfTerms = 0;
	
	public boolean hasBeenSaved = false;
	
	public int[] getCurrentSegmentIds()
	{
		return currentSegmentIds;
	}

	public int addSegment(int segmentId)
	{
		for(int i=0;i<Max_number_Of_Segments;i++)
		{
			if (currentSegmentIds[i] == -1)
			{
				currentSegmentIds[i] = segmentId;
				currentNumberOfSegments++;				
				return i;
			}
		}
		return -1;
		//currentSegmentIds.add(segmentId);
		//return currentSegmentIds.size() -1;
	}

	public int mergedCurrentSegments(int segmentId1, int segmentId2, int newSegmentId)
	{
		int newAt = -1;
		for(int i=0;i<Max_number_Of_Segments;i++)
		{
			if (currentSegmentIds[i] == segmentId1)
			{
				currentSegmentIds[i] = newSegmentId;
				newAt = i;
			}
			else
			{
				currentSegmentIds[i] = -1;
				currentNumberOfSegments--;
			}
		}
		return newAt;
		
		/*int index1 = currentSegmentIds.indexOf(segmentId1);
		currentSegmentIds.remove(index1);
		int index2 = currentSegmentIds.indexOf(segmentId2);
		currentSegmentIds.remove(index2);
		currentSegmentIds.insert(newSegmentId, index1);
		return currentSegmentIds.indexOf(newSegmentId);
		*/
	}
	
	/**
	 * The position in the Global lexicon inner node file
	 * of the root node of the B+ tree that constitutes the lexicon.
	 */
	public FilePosition GlobalLexiconRoot;
	
	/**
	 * The position in the lexicon leaf node file of
	 * the first leaf node.
	 */
	public FilePosition GlobalLexiconFirstLeaf;
	
	/**
	 * The position in the Global document index inner node file
	 * of the root node of the B+ tree that constitutes the document index.
	 */
	public FilePosition GlobalDocIndexRoot;
	
	/**
	 * The position in the document index leaf node file of
	 * the first leaf node.
	 */
	public FilePosition GlobalDocIndexFirstLeaf;
	
	public int globalLexiconHeight = 0;
	
	public int globalDocIndexHeight = 0;
	
	public int globalLexiconNumberOfValues = 0;
	
	public int globalDocIndNumberOfValues = 0;
	
	
	public String indexPath;
	public String indexPrefix;
	
	public IncrementalIndexProperties()
	{
		this(ApplicationSetup.TERRIER_INDEX_PATH, ApplicationSetup.TERRIER_INDEX_PREFIX);
	}
	
	public IncrementalIndexProperties(String indexPath, String indexPrefix)
	{
		this.indexPath = indexPath; this.indexPrefix = indexPrefix;
		Prop = new Properties();
		Arrays.fill(currentSegmentIds, -1);
		
		String propertiesFile = indexPath + ApplicationSetup.FILE_SEPARATOR + indexPrefix + ".properties";
		File f = new File(propertiesFile);
		if(f.exists())
		{
			System.out.println("found");
			try{
			Prop.loadFromXML(new FileInputStream(propertiesFile));
			//Max_number_Of_Segments = Integer.parseInt(Prop.getProperty("Max_number_Of_Segments","6"));
			currentNumberOfSegments = Integer.parseInt(Prop.getProperty("currentNumberOfSegments","4"));
			currentSegmentIds = StringToArray(Prop.getProperty("currentNumberOfSegments",""),-1,Max_number_Of_Segments);
			
			GlobalLexiconRoot = new FilePosition(Integer.parseInt(Prop.getProperty("GlobalLexiconRoot","0")),(byte)0);
			GlobalDocIndexRoot = new FilePosition(Integer.parseInt(Prop.getProperty("GlobalDocIndexRoot","0")),(byte)0);
			
			
			GlobalLexiconFirstLeaf = new FilePosition(Integer.parseInt(Prop.getProperty("GlobalLexiconFirstLeaf","0")),(byte)0);
			GlobalDocIndexFirstLeaf = new FilePosition(Integer.parseInt(Prop.getProperty("GlobalDocIndexFirstLeaf","0")),(byte)0);
			
			globalLexiconHeight = Integer.parseInt(Prop.getProperty("globalLexiconHeight","0"));
			globalDocIndexHeight = Integer.parseInt(Prop.getProperty("globalDocIndexHeight","0"));
			
			globalLexiconNumberOfValues = Integer.parseInt(Prop.getProperty("globalLexiconNumberOfValues","0"));
			globalDocIndNumberOfValues = Integer.parseInt(Prop.getProperty("globalDocIndNumberOfValues","0"));
			
			hasBeenSaved = Boolean.parseBoolean(Prop.getProperty("hasBeenSaved","false"));
			
			}
			catch(IOException e){e.printStackTrace();}
		}

	}

	public static int[] StringToArray(String property, int defaultValue, int defaultSize)
	{
		int rtr[];
		if (property == null || property.equals(""))
		{
			rtr = new int[defaultSize];
			Arrays.fill(rtr, -1);
			return rtr;
		}
		final String[] parts = property.split("\\s*,\\s*");
		final int l = parts.length;
		rtr = new int[l];
		Arrays.fill(rtr, -1);
		for(int i=0;i<l;i++)
		{
			rtr[i] = Integer.parseInt(parts[i]);
		}
		return rtr;	
	}
	
	public static String ArrayToString(int[] a) 
	{
		StringBuilder out = new StringBuilder();
		final int l = a.length;
		for(int i=0;i<l;i++)
		{
			out.append(a[i]);
			if(i != (l-1))
				out.append(",");
		}
		return out.toString();
	}
	
	public void close()
	{
		save();
	}
	
	public void save()
	{
		
		Prop.setProperty("Max_number_Of_Segments",String.valueOf(Max_number_Of_Segments));
		Prop.setProperty("currentNumberOfSegments",String.valueOf(currentNumberOfSegments));
		
		Prop.setProperty("GlobalLexiconRoot",GlobalLexiconRoot == null ? "0" : String.valueOf(GlobalLexiconRoot.Bytes));
		Prop.setProperty("GlobalDocIndexRoot",GlobalDocIndexRoot == null ? "0" : String.valueOf(GlobalDocIndexRoot.Bytes));
		Prop.setProperty("currentSegmentIds", ArrayToString(currentSegmentIds));
		
		if(GlobalLexiconFirstLeaf != null)
			Prop.setProperty("GlobalLexiconFirstLeaf",GlobalLexiconFirstLeaf == null ? "0" : String.valueOf(GlobalLexiconFirstLeaf.Bytes));
		
		if(GlobalDocIndexFirstLeaf != null)
			Prop.setProperty("GlobalDocIndexFirstLeaf",GlobalDocIndexFirstLeaf == null ? "0" : String.valueOf(GlobalDocIndexFirstLeaf.Bytes));
		
		
		Prop.setProperty("globalLexiconHeight",String.valueOf(globalLexiconHeight));
		Prop.setProperty("globalDocIndexHeight",String.valueOf(globalDocIndexHeight));
		Prop.setProperty("globalDocIndNumberOfValues",String.valueOf(globalDocIndNumberOfValues));
		Prop.setProperty("globalLexiconNumberOfValues",String.valueOf(globalLexiconNumberOfValues));
		
		Prop.setProperty("hasBeenSaved", "true");
		
		
		try {
			String propertiesFile = indexPath + ApplicationSetup.FILE_SEPARATOR + indexPrefix + ".properties";
			Prop.storeToXML(new FileOutputStream(propertiesFile), 
				"The properties that govern the behaviour of the incremental index");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public int getNextSegmentId()
	{
		return nextSegmentId++;
	}
	
	public int getMaxNumberOfSegments()
	{
		return Max_number_Of_Segments;
	}
	
	public static void main(String[] args) {
		
		File f = new File(ApplicationSetup.TERRIER_INDEX_PATH
			+ ApplicationSetup.FILE_SEPARATOR
			+ ApplicationSetup.TERRIER_INDEX_PREFIX
			+ ".properties");
		if(f.exists())
			f.delete();
		
		
		IncrementalIndexProperties iip = new IncrementalIndexProperties();
		
		//iip.Max_number_Of_Segments = 8;
		iip.currentNumberOfSegments = 5;
		iip.GlobalDocIndexRoot = new FilePosition(10,(byte)10);
		iip.GlobalLexiconRoot = new FilePosition(15,(byte)15);
		iip.GlobalDocIndexFirstLeaf = new FilePosition(20,(byte)0);
		iip.GlobalLexiconFirstLeaf = new FilePosition(25,(byte)0);
		iip.close();
		
		iip = new IncrementalIndexProperties();
		//System.out.println(iip.Max_number_Of_Segments);
		System.out.println(iip.currentNumberOfSegments);
		System.out.println(iip.GlobalDocIndexRoot);
		System.out.println(iip.GlobalLexiconRoot);
		System.out.println(iip.GlobalDocIndexFirstLeaf);
		System.out.println(iip.GlobalLexiconFirstLeaf);

	}

}
