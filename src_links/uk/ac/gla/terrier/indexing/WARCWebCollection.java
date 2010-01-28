

package uk.ac.gla.terrier.indexing;
import uk.ac.gla.terrier.utility.FixedSizeInputStream;
import uk.ac.gla.terrier.utility.Files;
import java.io.PrintWriter;
import java.io.IOException;
import uk.ac.gla.terrier.utility.NullWriter;
import uk.ac.gla.terrier.utility.ApplicationSetup;


public class WARCWebCollection extends WARCCollection
{


    protected static boolean extractWebStuff =
        Boolean.parseBoolean(
            ApplicationSetup.getProperty("trecwebcollection.extract.webstuff","true")
        );
    protected static boolean useURLText =
        Boolean.parseBoolean(
            ApplicationSetup.getProperty("trecwebcollection.use.url-text", "false")
        );


    /** stores the anchor text found while parsing the documents */
    protected PrintWriter anchorTextWriter = null;

    /** stores the titles found while parsing the documents */
    protected PrintWriter titleWriter = null;

    /** stores the text of documents found while parsing the documents */
    protected PrintWriter abstractWriter = null;

    /** stores the content-type of documents */
    protected PrintWriter contentTypeWriter = null;

    /** stores the url of documents */
    protected PrintWriter urlWriter = null;

	public WARCWebCollection() { this(ApplicationSetup.COLLECTION_SPEC); }
	public WARCWebCollection(final String CollectionSpecFilename) 
	{
		super(CollectionSpecFilename); 
		if (extractWebStuff)
            openAnchorAltWriters();
	}

	public Document getDocument() 
	{
		FixedSizeInputStream fsis = new FixedSizeInputStream(is, currentDocumentBlobLength);
        fsis.suppressClose();
		//System.err.println("Parsing "+ DocProperties.get("url"));
        if (useURLText)
            return new UrlLabradorDocument(
                fsis, DocProperties,
                anchorTextWriter, titleWriter, abstractWriter, contentTypeWriter, urlWriter);
        return new LabradorDocument(
            fsis, DocProperties,
            anchorTextWriter, titleWriter, abstractWriter, contentTypeWriter, urlWriter);
	}



    /** protected method initialises the extraction writers used by the Document objects.
      * All extractors can be disabled by setting property <tt>trecwebcollection.extract.webstuff</tt> to false.
      * Individual extractors can be disabled by settings their filename to empty */
    protected void openAnchorAltWriters() {
        logger.info("WARCWebCollection opening extraction writers");
        try {
            String filenameNoExt = ApplicationSetup.TERRIER_INDEX_PATH +
                                   ApplicationSetup.FILE_SEPARATOR +
                                   ApplicationSetup.TERRIER_INDEX_PREFIX + "."; 

            /* for each writer, use a NullWriter if the filename length is 0, thus disabling that particular extraction writer */
            
            String anchorTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.anchor-text.filename","anchor-text.gz");
            anchorTextWriter =  new PrintWriter(anchorTextFilename.length() > 0
                ? Files.writeFileWriter(filenameNoExt + anchorTextFilename)
                : new NullWriter());

            String titleTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.title-text.filename","title.gz");
     
            titleWriter = new PrintWriter(titleTextFilename.length() > 0
                ? Files.writeFileWriter(filenameNoExt + titleTextFilename)
                : new NullWriter());

            String extractTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.extract-text.filename","extract-text.gz");
            abstractWriter = new PrintWriter(extractTextFilename.length() > 0
                ? Files.writeFileWriter(filenameNoExt + extractTextFilename)
                : new NullWriter());

            String contentTypeFilename = ApplicationSetup.getProperty("trecwebcollection.extract.content-type.filename","content-type.gz");
            contentTypeWriter = new PrintWriter(contentTypeFilename.length() > 0
                ? Files.writeFileWriter(filenameNoExt + contentTypeFilename)
                : new NullWriter());
            
            String urlTextFilename = ApplicationSetup.getProperty("trecwebcollection.extract.url-text.filename","url-text.gz");
            urlWriter = new PrintWriter(urlTextFilename.length() > 0
                ? Files.writeFileWriter(filenameNoExt +urlTextFilename)
                : new NullWriter());

        } catch(IOException ioe) {
            logger.error("IOException while opening anchor and alt text writers.", ioe);
        }
    }

    /** Close this TRECWebCollection object, and all enabled extractors */
    public void close() {
        super.close();
        if (extractWebStuff)
        {
        anchorTextWriter.flush();
        anchorTextWriter.close();
        titleWriter.flush();
        titleWriter.close();
        abstractWriter.flush();
        abstractWriter.close();
        contentTypeWriter.flush();
        contentTypeWriter.close();
        urlWriter.flush();
        urlWriter.close();
        }
    }
}
