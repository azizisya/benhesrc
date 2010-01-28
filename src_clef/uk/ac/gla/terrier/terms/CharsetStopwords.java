package uk.ac.gla.terrier.terms;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Stopwords class implementation that uses the filename of the stopword list to 
  * deduce the encoding of the stopwords file. The encoding is deduced from the suffix
  * of the file that replaced the .txt extension at the end of the filename.
  * <p>
  * <b>Examples:</b><table>
  * <tr><td><font size="+1">Filename</font></td><td><font size="+1">Encoding</font></td></tr>
  * <tr><td><tt>stopwords</tt></td><td>System default encoding</td></tr>
  * <tr><td><tt>stopwords.txt</tt></td><td>System default encoding</td></tr>
  * <tr><td><tt>stopwords.UTF-8</tt></td><td>UTF</td></tr>
  * <tr><td><tt>stopwords.ISO-8859-2</tt></td><td>ISO-8859-2</td></tr>
  * </table>
  * <p>Valid encodings are checked used Charset.isSupported(), and are passed to the
  * constructor of the InputStreamReader() that is used to read the stopwords list.
  * @author Craig Macdonald <craigm{a.}dcs.gla.ac.uk>
  * @version $Revision: 1.2 $
  */

public class CharsetStopwords extends Stopwords
{
	public CharsetStopwords(TermPipeline n)
	{
		super(n);
	}

	public CharsetStopwords(TermPipeline n, String file)
	{
		super(n,file);
	}

	public CharsetStopwords(TermPipeline n, String[] files)
	{
		super(n,files);
	}
	
	public void loadStopwordsList(String stopwordsFilename)
	{
		stopwordsFilename = ApplicationSetup.makeAbsolute(stopwordsFilename, ApplicationSetup.TERRIER_SHARE);
		String charset = null;
		int pos = stopwordsFilename.lastIndexOf('.');
		if (pos > 0)
		{
			charset = stopwordsFilename.substring(pos+1);
			if (! charset.toLowerCase().equals("txt"))
			{
				if (! Charset.isSupported(charset))
				{
					System.err.println("WARNING: CharsetStopwords does not support"
					+charset);
					charset = null;	
				}		
			}
			else
			{
				charset = null; //force .txt to default character set
			}
		}		
		try {
			BufferedReader br = null;
			System.err.println("Loading stopwords file "+stopwordsFilename);
			FileInputStream fis = new FileInputStream(stopwordsFilename);
			if (charset == null)
			{
				br = new BufferedReader(new InputStreamReader(fis));
			}
			else
			{
				br = new BufferedReader(new InputStreamReader(fis, charset));
			}
			//stopWords = new HashSet();
			String word;
			while ((word = br.readLine()) != null)
				stopWords.add(word);
			br.close();
		} catch (IOException ioe) {
			System.err.println("ERROR: Input/Output Exception while reading stop word list. "+ioe);
			ioe.printStackTrace();
			//System.exit(1);
		} catch (Exception e) {
			System.err.println("ERROR: Exception while reading stop word list. Stack trace follows."+e);
			e.printStackTrace();
			//System.exit(1);			
		}
	}

}
