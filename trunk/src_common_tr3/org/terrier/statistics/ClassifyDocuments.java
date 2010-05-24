package org.terrier.statistics;
import org.terrier.structures.*;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;


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

