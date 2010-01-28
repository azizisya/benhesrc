package uk.ac.gla.terrier.languages;
import gnu.trove.*;
import java.util.Arrays;
import java.util.Comparator;
import java.io.IOException;
import java.io.Writer;
class LanguageModel extends TObjectIntHashMap<String>
{
	/** the name of this language */
	String languageName = null;
	/** make a new language model without any name */
	public LanguageModel(){}
	/** make a new language model with the specified name */
	public LanguageModel(String name) { languageName = name; }	

	/** remove the those with a value less than minEntries 
		* @param minValue minimum value of the entry value for entry to be kept
		*/
	protected void trimMinValue(final int minValue)
	{
		if (minValue == 0)
			return;
		super.retainEntries(new TObjectIntProcedure<String>() {
				public boolean execute(String key, int size) {
					return size >= minValue;
				}
			}
		);
	}

	/** reduce this language model to only the topNEntries. If size if
		* less than topNEntries, then no transform occurrs.
		* @param topNEntries the number of top entries to keep. */
	protected void trimTopN(final int topNEntries)
	{
		if (this.size() <= topNEntries)
			return;

		final String[] all_tokens = super.keys(new String[0]);
		Arrays.sort(all_tokens, new Comparator<String>(){
				public int compare(String o1, String o2)
				{
					/* # adding  `or $a cmp $b' in the sort block makes sorting five
						* # times slower..., although it would be somewhat nicer (unique result)
						*/
					final int x1 = get(o1);
					final int x2 = get(o2);
					if (x1 == x2)
                        return 0;
                    return (x2 > x1) ? 1 : -1;
				}			
			});
		final int l = all_tokens.length;
		for(int i=topNEntries;i<l;i++)
			remove(all_tokens[i]);
	}

	/** return a textual representation of this language model */
	public String toString()
	{
		return Arrays.deepToString(super.keys(new String[0]));
	}

	/** write this language model out to the specified writer 
		* @param bw the file to write the model out to. */
	public void writeToFile(final Writer bw) throws IOException
	{
		final String[] all_tokens = super.keys(new String[0]);
		//sort the tokens by descending values
		Arrays.sort(all_tokens, new Comparator<String>(){
				public int compare(String o1, String o2)
				{
					/* # adding  `or $a cmp $b' in the sort block makes sorting five
						* # times slower..., although it would be somewhat nicer (unique result)
						*/
					return new Integer(get(o2)).compareTo(new Integer(get(o1)));
				}
			});
		//write out to bufferedwriter in descending rank
		for(String tok: all_tokens)
		{
			bw.write(tok + "\t" + get(tok) + "\n");
		}
	}
	/** return the name of this language */
	public String getName()
	{
		return languageName;
	}
}
