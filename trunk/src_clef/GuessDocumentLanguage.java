import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.indexing.CollectionFactory;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.languages.TextCat;
import java.util.ArrayList;
import java.util.Arrays;
public class GuessDocumentLanguage
{
	public static void main(String args[])
    {
        final long startCollection = System.currentTimeMillis();
        int docCount = 0;
        final Collection c =  CollectionFactory.loadCollections();
        final TextCat tc = new TextCat();
        final int MAXDOC_TOKENS = Integer.parseInt(ApplicationSetup.getProperty("language.classifier.max_terms","100000"));
        Document d = null;
        while( c.nextDocument() ){
            final ArrayList<String> tokens = new ArrayList<String>();
            d = c.getDocument();
            while(! d.endOfDocument())
            {
				String t = d.getNextTerm();
				if (t != null)
					tokens.add(t);
				if (tokens.size() == MAXDOC_TOKENS)
					break;
            }
			System.out.println(c.getDocid() +" "+ Arrays.deepToString(tc.classify(tokens.toArray(new String[0]))));
            docCount++;
        }
        System.err.println("Collection finished, closing it");
        c.close();
        final long endCollection = System.currentTimeMillis();
        System.err.println("Collection "+((endCollection-startCollection)/1000.0d) +"seconds to index "
                +"("+docCount+" documents)\n");
    }
}
