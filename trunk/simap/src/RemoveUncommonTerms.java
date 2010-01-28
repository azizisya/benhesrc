

import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.structures.indexing.LexiconBuilder;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import java.io.File;
import java.io.IOException;

public class RemoveUncommonTerms
{

	public static void removeUncommonTerms(Index sourceIndex, Index destIndex, int minTF) throws IOException
	{
		String destPath = destIndex.getPath();
		String destPrefix = destIndex.getPrefix();
		InvertedIndexInputStream iiis = (InvertedIndexInputStream)sourceIndex.getIndexStructureInputStream("inverted");
		LexiconInputStream lis = (LexiconInputStream)sourceIndex.getIndexStructureInputStream("lexicon");
		
		final boolean blocks = iiis instanceof BlockInvertedIndexInputStream;
		final DirectInvertedOutputStream iios = blocks //TODO: fix num field bits
			? new BlockDirectInvertedOutputStream(destPath + '/'+ destPrefix + ApplicationSetup.IFSUFFIX, 0)
			: new DirectInvertedOutputStream(destPath + '/'+ destPrefix + ApplicationSetup.IFSUFFIX, 0);
		final LexiconOutputStream los = new LexiconOutputStream(destPath, destPrefix);	
		
		int[][] termPostings = null;
		final int[] documentLengths = new int[sourceIndex.getCollectionStatistics().getNumberOfDocuments()];
		int numberOfTerms = 0;
		while((termPostings = iiis.getNextDocuments()) != null)
		{
			lis.readNextEntry();
			if (lis.getTF() >= minTF)
			{
				iios.writePostings(termPostings, termPostings[0][0]+1);
				long endOffset = iios.getByteOffset();
		 		byte endBitOffset = iios.getBitOffset();
		 		endBitOffset--;
		 		if (endBitOffset < 0 && endOffset > 0) {
					endBitOffset = 7;
					endOffset--;
		 		}
				los.writeNextEntry(lis.getTerm(),numberOfTerms++,lis.getNt(),lis.getTF(), endOffset,endBitOffset);
			}
			else
			{
				//term to be omitted, remember the offset in document length to be made
				final int numPostings = termPostings[0].length;
				for(int i=0;i<numPostings;i++)
				{
					documentLengths[termPostings[0][i]] -= termPostings[1][i];
				}
			}
		}
		
		destIndex.addIndexStructure("inverted",
			blocks ? "uk.ac.gla.terrier.structures.BlockInvertedIndex"  : "uk.ac.gla.terrier.structures.InvertedIndex",
			"uk.ac.gla.terrier.structures.Lexicon,java.lang.String,java.lang.String","lexicon,path,prefix");

		destIndex.addIndexStructure("lexicon", 
			"uk.ac.gla.terrier.structures.Lexicon");
		destIndex.addIndexStructureInputStream("lexicon", 
			"uk.ac.gla.terrier.structures.LexiconInputStream");
		destIndex.setIndexProperty("num.Terms", ""+los.getNumberOfTermsWritten());
		destIndex.setIndexProperty("num.Pointers", ""+los.getNumberOfPointersWritten());
		destIndex.setIndexProperty("num.Tokens",""+los.getNumberOfTokensWritten());	
				
		iios.close();
	  	iiis.close();
		lis.close();
		los.close();
		
		LexiconBuilder.createLexiconIndex(destIndex);
		LexiconBuilder.createLexiconHash(destIndex);
	  

		final DocumentIndexInputStream dois = (DocumentIndexInputStream) sourceIndex.getIndexStructureInputStream("document");
		final DocumentIndexOutputStream doos = new DocumentIndexOutputStream(destPath, destPrefix);

		//TODO: strip direct file
	 	if (false && sourceIndex.hasIndexStructure("direct"))
		{

		}
		else
		{ 
			while (dois.readNextEntry() >0)
			{
				doos.addEntry(
					dois.getDocumentNumber(), 
					dois.getDocumentId(), 
					dois.getDocumentLength() + documentLengths[dois.getDocumentId()], 
					dois.getEndOffset(), dois.getEndBitOffset());
			}
		}

		//from document index builder	
		int maxDocsEncodedDocid = Integer.parseInt(ApplicationSetup.getProperty("indexing.max.encoded.documentindex.docs","5000000"));
		int numDocs = sourceIndex.getCollectionStatistics().getNumberOfDocuments();
		destIndex.setIndexProperty("num.Documents", ""+numDocs);	
		destIndex.addIndexStructure("document",
					numDocs > maxDocsEncodedDocid
					? "uk.ac.gla.terrier.structures.DocumentIndex"
					: "uk.ac.gla.terrier.structures.DocumentIndexEncoded" );
		destIndex.addIndexStructureInputStream("document", "uk.ac.gla.terrier.structures.DocumentIndexInputStream");
		//update the index properties on disk
		destIndex.flush();
	}


	public static void main(String[] args)
	{
		if (args.length != 5)
		{
			System.err.println("Usage: AddSkiplistPointers sourceIndexPath sourceIndexPrefix destIndexPath destIndexPrefix minOccurrences");
			return;
		}
		try{
			int L=1;
			Index src = Index.createIndex(args[0], args[1]);
			Index dst = Index.createNewIndex(args[2], args[3]);
			removeUncommonTerms(src, dst, Integer.parseInt(args[4]));
			src.close();
			dst.close();
		} catch(IOException e){
			System.out.println(e);
		}
	}
}
