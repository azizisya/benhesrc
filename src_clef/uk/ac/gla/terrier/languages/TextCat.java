package uk.ac.gla.terrier.languages;
import gnu.trove.*;
import java.util.Comparator;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.UnsupportedEncodingException;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/** Language guesser. Port of text_cat (http://www.let.rug.nl/~vannoord/TextCat/) © Gertjan van Noord, 1997. 
  * This is better than text_cat, libTextCat and JTextCat, as it assumes that the encoding is correct, and works on character, not
  * on bytes. Comments marked with # are comments from the original Perl code. 
  * <h4>Usage:</h4>
  * Instantiate using the name of the directory that contains the language models to be used. 
  * Use by calling classify on the series of tokens or a string.
  */
public class TextCat {
	/** the program returns the best-scoring language together
		  with all languages which are $opt_u times worse (cf option -u).
		  If the number of languages to be printed is larger than the value
		  of this option (default: $opt_a) then no language is returned, but
		  instead a message that the input is of an unknown language is
		  printed. Default: $opt_a.
	*/	
	protected final int opt_a = 10;
	/**
	Before sorting is performed the Ngrams which occur this number
		  of times or less are removed. This can be used to speed up
		  the program for longer inputs. For short inputs you should use
		  -f 0.
	*/
	protected final int opt_f = 0;
	/**
	indicates the topmost number of ngrams that should be used.
		  If used in combination with -n this determines the size of the
		  output. If used with categorization this determines
		  the number of ngrams that are compared with each of the language
		  models (but each of those models is used completely).
	*/
	protected static final int opt_t = 400;
	/**
		determines how much worse result must be in order not to be
		  mentioned as an alternative. Typical value: 1.05 or 1.1.
	*/
	protected final double opt_u = 1.05;

	/** mapping of language name to the actual language model */
	protected THashMap<String, LanguageModel> models = new THashMap<String, LanguageModel>();
	/** directory where the language models were loaded from, and where any new language models would be written */
	protected String LMdir;
	/** maximum number of tokens considered in a stream of text for creating a language model. Specified by property
	 * <tt>language.classifier.max_terms</tt>, defaults to 100,000 tokens.
	 */
	final int MAXDOC_TOKENS = Integer.parseInt(ApplicationSetup.getProperty("language.classifier.max_terms","100000"));
	
	/** default directory to find the language models for the classifier. Specified by the <tt>language.classifier.lm_dir</tt>
	 * property, defaults to <tt>terrier/share/LanguageClassifier_LM</tt>.
	 */
	protected final static String DEFAULT_LM_DIR =
		ApplicationSetup.makeAbsolute(
				ApplicationSetup.getProperty("language.classifier.lm_dir", "LanguageClassifier_LM"), 
				ApplicationSetup.TERRIER_SHARE);
		
	
	/** create a new instance of text cat, using the language models found in the
	  * default directory (Default directory <tt>terrier/share/LanguageClassifier_LM</tt>). */
	public TextCat()
	{
		this(DEFAULT_LM_DIR);
	}
	

	/** create a new instance of text cat, using the language models found in the
	  * specified directory */
	public TextCat(String languageModelDirectory)
	{
		loadLanguageModels(LMdir = languageModelDirectory);
	}

	/** returns the most likely languages for a series of tokens */
	public String[] classify(String[] tokens)
	{
		final TObjectIntHashMap<String> results  = classifyEx(tokens);
		if (results == null || results.size() == 0)
			return new String[]{"unknown"};
		final String[] all_language_names = results.keys(new String[0]);
		Arrays.sort(all_language_names, new Comparator<String>()
			{
				 public int compare(String o1, String o2)
				 {
					final int x1 = results.get(o1);
                    final int x2 = results.get(o2);
                    if (x1 == x2)
                        return 0;
                    return (x1 > x2) ? 1 : -1;
					//return new Integer(results.get(o1)).compareTo(new Integer(results.get(o2)));
				 }
			});
		final ArrayList<String> selected_results = new ArrayList<String>();
		final TIntArrayList selected_results_scores = new TIntArrayList();
		
		double a = results.get(all_language_names[0]);
		selected_results.add(all_language_names[0]);
		selected_results_scores.add(results.get(all_language_names[0]));
		final int l = all_language_names.length;
		for(int i = 1;i<l;i++)
		{
			if (results.get(all_language_names[i]) < opt_u * a)
			{
				selected_results.add(all_language_names[i]);
				selected_results_scores.add(results.get(all_language_names[i]));
			}
		}
		if (selected_results.size() > opt_a)
			return new String[]{"unknown"};
		//System.err.println(Arrays.toString(selected_results_scores.toNativeArray()));
		return selected_results.toArray(new String[0]);
	}

	/** returns the most likely languages for a string */	
	public String[] classify(String str)
	{
		return classify(str.split("0-9\\s+"));
	}

	/** counts the occurrences of each token in the specified language model */
	static class Counter implements TObjectProcedure<String> {
		int p = 0;
		int i = 0;
		LanguageModel lm = null;	
		final int maxp = opt_t;
		public boolean execute(String token)
		{
			if (lm.containsKey(token))
			{
				p += Math.abs(lm.get(token)-i);
			}
			else
			{
				p += maxp;
			}
			i++;
			return true;
		}
	}

	/** give the full classifcation output */	
	public TObjectIntHashMap<String> classifyEx(String[] tokens)
	{
		final TObjectIntHashMap<String> results = new TObjectIntHashMap<String>(models.size());
		final LanguageModel unknown = create_lm(tokens);
		if (unknown == null)
			return results;
		final int maxp = opt_t;
		models.forEachEntry(
			new TObjectObjectProcedure<String, LanguageModel>(){
				 public boolean execute(String langName, final LanguageModel lm)
				 {
					Counter countOccurrences = new Counter();
					countOccurrences.lm = lm;
					unknown.forEachKey( countOccurrences );
					results.put(langName, countOccurrences.p);
					countOccurrences = null;
					return true;
				 }
			});
		return results;
	}

	/** add a language model depicted by these tokens.
	  * @param languageName the name of the language represented by this text#
	  * @param tokens the series of tokens to use as as a language model. 
  	  * @return true if a model was successfully loaded */
	public boolean addLanguageModel(String languageName, String[] tokens) throws IOException
	{
		LanguageModel newLM = create_lm(tokens);
		if (newLM == null)
			return false;
		Writer bw = Files.writeFileWriter(LMdir + "/" + languageName + "-UTF-8.lm");
		newLM.writeToFile(bw);
		bw.close();
		models.put(languageName + "-UTF-8", newLM);
		return true;
	}

	/** add a language model depicted by this passage of text.
	  * @param languageName the name of the language represented by this text 
	  * @param passage the passage of text to use as as a language model.
	  * @return true if a model was successfully loaded */
	public boolean addLanguageModel(String languageName, String passage) throws IOException
	{
		return addLanguageModel(languageName, passage.split("0-9\\s+"));
	}

	/** returns all the languages identified by this instance */
	public String[] getLanguages()
	{
		return models.keySet().toArray(new String[0]);
	}

	/** remove the named language from this instance.
	  * @param languageName the name of the language to delete
	  * @return true if a model of that name was found and removed
	  */
	public boolean removeLanguage(String languageName)
	{
		if (models.containsKey(languageName))
		{
			models.remove(languageName);
			return true;
		}
		return false;
	}

	/** load all the language models from the specified directory 
	  * @param directory the name of the directory to load the language models from */
	protected void loadLanguageModels(final String directory)
	{
		loadLanguageModels(new File(directory));	
	}

	/** load all the language models from the specified directory
	  * @param LM_Dir the name of the directory to load the language models from */
	protected void loadLanguageModels(final File LM_Dir)
	{
		//obtain a list of language models in the specified directory
		final File[] LMs = LM_Dir.listFiles(new FileFilter(){
				public boolean accept(File path)
				{
					if (path.isDirectory())
						return false;
					if (path.getName().endsWith(".lm"))
						return true;
					return false;
				}
			}
		);
		//load in each language model	
		for (File f: LMs)
			loadLanguageModel(f);
	}

	/** load in the language model defined in file f, and adds it to this instance. Calls readLanguageModel internally,
	  * then adds the model into the currently instantiated model table (models).
	  * @param f the file to be loaded. */	
	public void loadLanguageModel(final File f)
	{
		String name = f.getName();
		name = name.replaceAll(".lm$","");
		LanguageModel lm = readLanguageModel(f);
		if(lm != null && lm.size() > 0)
			models.put(lm.getName(), lm);
	}

	/** load the language model for a given file. If the filename contains a hyphen, then
	  * this is taken as the encoding of the model file. Default encoding is ISO-8859-1
	  * @param f the files to load the model from
	  * @return the language model from the given file
	 */	
	protected static LanguageModel readLanguageModel(final File f)
	{
		LanguageModel lm = null;
		String encoding = null;
		try{
			String filename = f.getName().replaceAll(".lm$","");
			String fparts[] = filename.split("-",2);
			//we use the filename to guess the encoding
			if (fparts.length > 1)
			{
				encoding = fparts[1];
			}
			else
			{
				encoding = "ISO-8859-1";
			}
			//System.err.println(filename + " has encoding " + encoding);
			final BufferedReader br = Files.openFileReader(f, encoding);
			String line = null;
			int rank =1;
			lm = new LanguageModel(fparts[0]);
			//for each line in the saved model
			while((line = br.readLine()) != null)
			{
				line = line.trim();
				final String[] parts = line.split("\t");
				lm.put(parts[0], rank++/*Integer.parseInt(parts[1])*/);
			}
			br.close();
		} catch (UnsupportedEncodingException uee) {
			System.err.println("Failed to load language model at " + f +" because the encoding "+ encoding + " is not supported");
			return null;
		} catch (IOException ioe) {
			System.err.println("Failed to load language model at "+ f + " : " + ioe);
			ioe.printStackTrace();
			return null;
		}
		return lm;
	}

	/** create a language model from a series of tokens.
	  * the created language models are trimmed by minValue (opt_f)
	  * and TopN ngrams (opt_t).
	  * @return the generated model, or null if no model was created
	  */	
	protected LanguageModel create_lm(final String[] tokens)
	{
		final LanguageModel ngram = new LanguageModel();
		if (tokens.length == 0)
			return null;
		int tokenCount=0;
		for(String word : tokens)
		{
			tokenCount++;
			word = new StringBuilder(word.length()+2)
				.append("_").append(word).append("_").toString();
			//System.err.println("word="+word);
			int len = word.length();
			final int flen = len;
			for(int i=0;i<flen;i++)
			{
				if (len > 4)
					ngram.adjustOrPutValue(word.substring(i, i+5), 1, 1);
				if (len > 3)
					ngram.adjustOrPutValue(word.substring(i, i+4), 1, 1);
				if (len > 2)
					ngram.adjustOrPutValue(word.substring(i, i+3), 1, 1);
				if (len > 1)
					ngram.adjustOrPutValue(word.substring(i, i+2), 1, 1);
	
				ngram.adjustOrPutValue(word.substring(i, i+1), 1, 1);
				len--;
			}
			if (tokenCount >= MAXDOC_TOKENS)
				break;
		}
		/* # as suggested by Karel P. de Vos, k.vos@elsevier.nl, we speed up
		 * # sorting by removing singletons
		 * # however I have very bad results for short inputs, this way
		 */
		ngram.trimMinValue(opt_f);
		/* # sort the ngrams, and spit out the $opt_t frequent ones. */
		ngram.trimTopN(opt_t);
		if (ngram.size() == 0)
			return null;
		return ngram;
	}

	public static void main(String[] args)
	{
		String[] unknown = new String[args.length -1];
		System.arraycopy(args, 1, unknown, 0, args.length -1);
		System.out.println(Arrays.deepToString(new TextCat(args[0]).classify(unknown)));
	}
	
	
}


