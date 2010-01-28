package uk.ac.gla.terrier.statistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.structures.*;

public abstract class ClassifyDocuments
{
	Index index;
	DirectIndex di;
	Lexicon lex;	

	public ClassifyDocuments(Index index)
	{
		this.index = index;
		this.di = index.getDirectIndex();
		this.lex = index.getLexicon();
	}

	public abstract void train(int[] docids, int[] labels) throws Exception;
	public abstract int[] classify(int[] docids) throws Exception;

}

