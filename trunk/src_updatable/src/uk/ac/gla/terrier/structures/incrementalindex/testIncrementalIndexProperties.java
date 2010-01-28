package uk.ac.gla.terrier.structures.incrementalindex;

import java.io.File;

import uk.ac.gla.terrier.structures.FilePosition;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import junit.framework.TestCase;

public class testIncrementalIndexProperties extends TestCase {
	
	public static void testProp()
	{
		File f = new File(ApplicationSetup.TERRIER_INDEX_PATH + ApplicationSetup.FILE_SEPARATOR + ApplicationSetup.TERRIER_INDEX_PREFIX + ".properties");
		if(f.exists())
			f.delete();
		
		
		IncrementalIndexProperties iip = new IncrementalIndexProperties();
		
		//iip.setMaxNumberOfSegments(8);
		iip.currentNumberOfSegments = 5;
		iip.GlobalDocIndexRoot = new FilePosition(10,(byte)10);
		iip.GlobalLexiconRoot = new FilePosition(15,(byte)15);
		iip.GlobalDocIndexFirstLeaf = new FilePosition(20,(byte)0);
		iip.GlobalLexiconFirstLeaf = new FilePosition(25,(byte)0);
		iip.globalLexiconHeight = 6;
		iip.globalLexiconNumberOfValues = 1234;
		iip.globalDocIndexHeight = 87;
		iip.globalDocIndNumberOfValues = 19876;
		iip.close();
		
		iip = new IncrementalIndexProperties();
		assertTrue(iip.getMaxNumberOfSegments() == 8);
		assertTrue(iip.currentNumberOfSegments == 5);
		assertTrue(iip.GlobalDocIndexRoot.Bytes == 10);
		assertTrue(iip.GlobalLexiconRoot.Bytes == 15);
		assertTrue(iip.GlobalDocIndexFirstLeaf.Bytes == 20);
		assertTrue(iip.GlobalLexiconFirstLeaf.Bytes == 25);
		assertTrue(iip.globalLexiconHeight == 6);
		assertTrue(iip.globalLexiconNumberOfValues == 1234);
		assertTrue(iip.globalDocIndexHeight == 87);
		assertTrue(iip.globalDocIndNumberOfValues == 19876);
	}

}
