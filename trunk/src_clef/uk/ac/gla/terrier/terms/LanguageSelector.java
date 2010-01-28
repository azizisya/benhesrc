/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://ir.dcs.gla.ac.uk/terrier 
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * Information Retrieval Group
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is LanguageSelector.java.
 *
 * The Original Code is Copyright (C) 2004, 2005 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original author)
 */
package uk.ac.gla.terrier.terms;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.util.Hashtable;
import java.util.HashSet;
/** 
 * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk> 
 * @version $Revision: 1.1 $
 * <b>Properties</b>
 * <ul>
 * <li><tt>termpipelines.languageselector.markerlang</tt> - the marker term prefix used to change language. Defaults to "||LANG:"</li>
 * <li><tt>termpipelines.languageselector.markerdoc</tt> - the marker term used to mark a new document. Defaults to "||DOC||"</li>
 * <li><tt>termpipelines.languageselector.languages</tt> - comma delimited list of "two letter" languages to support</li>
 * <li><tt>termpipelines.LANG</tt> - the term pipeline for that "two letter" language</li>
 * <li><tt>termpipelines.languageselector.defaultlang</tt> - the default "two letter" language</li>
 * <li><tt>termpipelines.allowduplicatelangsindoc</tt> - if set false, this will prevent english (say) more than once in 1 document.</li>
 * </ul>
 */
public class LanguageSelector implements TermPipeline
{
	/** The default namespace for TermPipeline modules to be loaded from */
    public final static String PIPELINE_NAMESPACE = "uk.ac.gla.terrier.terms.";

	protected static final String LANGUAGE_MARKER = 
		ApplicationSetup.getProperty("termpipelines.languageselector.markerlang", "||LANG:");
	protected static final String DOCUMENT_MARKER = 
		ApplicationSetup.getProperty("termpipelines.languageselector.markerdoc", "||DOC||");

	/** The last component in the term pipeline. */
	protected TermPipeline lastPipeline = null;
	
	/** The currently selected language */
	protected String currentLanguage = null;

	protected Hashtable LanguagePipelines = new Hashtable();
	//maps english to en, welsh to en etc
	protected Hashtable langLanguages = new Hashtable();

	protected static final String defaultPipeline_en = "Stopwords,PorterStemmer";

	protected TermPipeline currentPipeline = null;

	protected String defaultLang = "en";

	protected HashSet languageDone = new HashSet();

	protected static boolean AllowRepeatLanguages = new Boolean(
		ApplicationSetup.getProperty("termpipelines.allowduplicatelangsindoc","false"))
		.booleanValue();
	
	/** 
	 * Makes a new stopword termpipeline object. The stopwords 
	 * file is loaded from the application setup file, 
	 * under the property <tt>stopwords.filename</tt>.
	 * @param n TermPipeline the next component in the term pipeline.
	 */
	public LanguageSelector(TermPipeline n)
	{
		lastPipeline = n;
		final String[] supportedLanguages = ApplicationSetup.getProperty("termpipelines.languageselector.languages", "en").split("\\s*,\\s*");
		for(int i=0; i<supportedLanguages.length; i++)
		{
			String thisLang = supportedLanguages[i].trim();
			String defaultPipeline = "";
			if (thisLang.equals("en"))
				defaultPipeline = defaultPipeline_en;
			String[] components = ApplicationSetup.getProperty("termpipelines."+thisLang, defaultPipeline).split("\\s*,\\s*");
			TermPipeline next = lastPipeline;
			for(int j=components.length -1;j>=0;j--)
			{
				String newPipelineName = components[j].trim();
				if (newPipelineName.length() == 0)
					continue;
				if (newPipelineName.indexOf(".") < 0 )
					newPipelineName = PIPELINE_NAMESPACE + newPipelineName;
				try{
					Class pipeClass = Class.forName(newPipelineName, false, this.getClass().getClassLoader());
					TermPipeline newPipeline = (TermPipeline)(pipeClass.getConstructor(new Class[]{TermPipeline.class}).newInstance(new Object[]{next}));
					next = newPipeline;
					if(newPipelineName.endsWith("Stopwords"))
					{
						((Stopwords)next).loadStopwordsList(
							ApplicationSetup.getProperty("stopwords.filename."+thisLang, thisLang+"-stopwords.txt"));
					}
				}catch (Exception e) {
					System.err.println("ERROR: Couldn't load stopword list called "+newPipelineName+" : "+e);
					e.printStackTrace();	
				}
			}
			LanguagePipelines.put(thisLang.toLowerCase(), next);
		}
		defaultLang = ApplicationSetup.getProperty("termpipelines.languageselector.defaultlang", null);	
		final String[] langLanguageMap = ApplicationSetup.getProperty("termpipelines.languageselector.languagemap", "english:en").split("\\s*,\\s*");
		for(int i=0;i<langLanguageMap.length;i++)
		{
			String[] langLanguage = langLanguageMap[i].split(":");
			if(langLanguage.length == 2)
			{
				langLanguages.put(langLanguage[0].toLowerCase(), langLanguage[1].toLowerCase());
			}
		}
		switchLanguage(defaultLang);
	}


	/** 
	 * Checks to see if term t is a stopword. If so, then the TermPipeline
	 * is existed. Otherwise, the term is passed on to the next TermPipeline
	 * object.
	 * @param t The term to be checked.
	 */
	public void processTerm(String t)
	{
		if(t.equals(DOCUMENT_MARKER))
		{
			if (! AllowRepeatLanguages)
				languageDone = new HashSet();
			return;				
		}

		if (t.startsWith(LANGUAGE_MARKER))
		{
			//could be english, say
			String newLang = t.substring(LANGUAGE_MARKER.length()).toLowerCase();

			//remove any encoding information that occurs in the language, we dont need it
			newLang = newLang.replaceAll("-.+$","");

			// switch to that language
			switchLanguage(newLang);
			return;
		}
		
		//check we're not ignoring terms of this document
		if (currentPipeline != null)
			//pass the term onto the next item in the currently selected term pipeline
			currentPipeline.processTerm(t);
	}

	protected void switchLanguage(String newLang)
	{
		System.err.println("Investigating switch to "+newLang);
		//map english to en, french to fr etc
		String pipelineLanguage = (String)langLanguages.get(newLang);

		//if we're not allowing repeating languages in a document, then check we've not done it before
		boolean LanguageDone = (! AllowRepeatLanguages) && languageDone.contains(pipelineLanguage);

		if(pipelineLanguage != null && ! LanguageDone && LanguagePipelines.containsKey(pipelineLanguage))
		{//find the pipeline for en
			
			//if we're not allowed to repeat in a document, then take note we've used this language
			if (! AllowRepeatLanguages)
				languageDone.add(currentLanguage);

			//we're already selected to use this language
			if (pipelineLanguage.equals(currentLanguage))
				return;

			//make the pipeline change
			currentLanguage = pipelineLanguage;
			currentPipeline = (TermPipeline)LanguagePipelines.get(pipelineLanguage);

			System.err.println("Language change - "+newLang+" - changing to lang "+currentLanguage);
		}
		else if (! LanguageDone && defaultLang != null && ! defaultLang.equals(newLang))
		{//if we have a default language, then use it
			switchLanguage(defaultLang);
		}
		else
		{	//unknown lang, or lang done
			System.err.println("Lang "+pipelineLanguage+" unknown or language ("+newLang+") done already - ignoring document");
			currentPipeline = null;
		}
		System.err.println("Language switch done");
	}
}
