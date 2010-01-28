package uk.ac.gla.terrier.structures.incrementalindex2.memoryindex;

import java.util.HashSet;

import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.indexing.TRECCollection;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.InvertedIndex;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.trees.FieldDocumentTree;
import uk.ac.gla.terrier.structures.trees.FieldDocumentTreeNode;
import uk.ac.gla.terrier.terms.TermPipeline;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.FieldScore;
import java.util.Set;

public class MemoryIndex extends Index{
	

	/** Set to true if loading an index succeeds */
	protected boolean loadSuccess = false;
	
	/** The direct index to use for quering */
	protected MemoryDirectIndex DiI;
	
	/** The document index to use for retrieval */
	protected MemoryDocumentIndex DoI;
	
	/** The inverted index to use for retrieval */
	protected MemoryInvertedIndex II;

	
	/** The lexicon to use for retrieval */
	protected MemoryLexicon L;
	
	/** A negative lexicon containing updates to be made to existing entries
	 * in the global lexicon.*/
	protected MemoryLexicon NegL;
	
	/** Number of tokens indexed by this index */
	protected long numberOfTokens = 0;
	/** Number of pointers in this inverted index */
	protected long numberOfPointers = 0;
	
	/** Number of document currently in this index */
	protected int numberOfDocuments = 0;
	
	/**
	 * Toggle on whether to build direct index, leads to better
	 * indexing performance but less relevant query results.
	 */
	//private static final boolean buildDirectIndex = true;
	
	

	
	/**
	 * Creates a memoryIndex by initialising the constituent 
	 * data structures. The first docid is specified in the
	 * constructor.
	 */
	public MemoryIndex(final int startingDocumentId)
	{
		super((long)3,(long)3,(long)3);
		load_pipeline();
		System.err.println("New MemoryIndex, first doc will have id "+startingDocumentId);	
		
		L = new MemoryLexicon();
		II = new MemoryInvertedIndex();
		DoI = new MemoryDocumentIndex(startingDocumentId);
		DiI = new MemoryDirectIndex();
		
		NegL = new MemoryLexicon();
	}
	
	/** Construct a memory index. The first docid assigned will
	  * be 0.
	  */
	public MemoryIndex()
	{
		this(0);
	}
	

	
	//******************************************************************************
	// Borrowed from basic indexer
	/** 
	 * This class implements an end of a TermPipeline that adds the
	 * term to the DocumentTree. This TermProcessor does NOT have field
	 * support.
	 */
	protected class BasicTermProcessor implements TermPipeline
	{
		//term pipeline implementation
		public void processTerm(String term)
		{
			/* null means the term has been filtered out (eg stopwords) */
			if (term != null)
			{
				//add term to thingy tree
				termsInDocument.insert(term);
				//int termid = L.incrementEntry(term,TF,Nt);
				//II.addDocForTerm(termid, docid, tfs);
				numOfTokensInDocument++;
			}
		}
	}
	
	/** This class implements an end of a TermPipeline that adds the
	 *  term to the DocumentTree. This TermProcessor does have field
	 *  support.
	 */
	protected class FieldTermProcessor implements TermPipeline
	{
		public void processTerm(String term)
		{
			/* null means the term has been filtered out (eg stopwords) */
			if (term != null)
			{
				/* add term to Document tree */
				termsInDocument.insert(term,termFields);
				numOfTokensInDocument++;
			}
		}
	}
	
	/** 
	 * A private variable for storing the fields a term appears into.
	 */
	protected Set<String> termFields;
	
	/** 
	 * The structure that holds the terms found in a document.
	 */
	protected FieldDocumentTree termsInDocument;
	
	/** 
	 * The number of tokens found in the current document so far/
	 */
	protected int numOfTokensInDocument = 0;
	
	/**
	 * The first component of the term pipeline.
	 */
	protected TermPipeline pipeline_first;
	
	/**
	 * The number of documents indexed with a set
	 * of builders. If a collection consists of 
	 * more documents, then we need to create
	 * new builders and later merge the data
	 * structures. The corresponding property is
	 * <tt>indexing.max.docs.per.builder<tt> and the
	 * default value is <tt>0</tt>. If the property
	 * is set equal to zero, then there is no limit.
	 */
	protected static int MAX_DOCS_PER_BUILDER = 
		Integer.parseInt(ApplicationSetup.getProperty("indexing.max.docs.per.builder", "0"));
	

	/** 
	 * The maximum number of tokens in a document. 
	 * If it is set to zero, then there is no limit 
	 * in the number of tokens indexed for a document. Set by property <tt>indexing.max.tokens</tt>.
	 */
	protected final static int MAX_TOKENS_IN_DOCUMENT = 
		Integer.parseInt(ApplicationSetup.getProperty("indexing.max.tokens", "0"));
	
	
	private final static String PIPELINE_NAMESPACE = "uk.ac.gla.terrier.terms.";
	
	protected void load_pipeline()
	{
		
		String[] pipes = ApplicationSetup.getProperty(
				"termpipelines", "Stopwords,PorterStemmer").trim()
				.split("\\s*,\\s*");
		TermPipeline next;
		
		if(FieldScore.USE_FIELD_INFORMATION)
			next =  new FieldTermProcessor();
		else
			next = new BasicTermProcessor();
		
		TermPipeline tmp;
		for(int i=pipes.length-1; i>=0; i--)
		{
			try{
				String className = pipes[i];
				if (className.length() == 0)
					continue;
				if (className.indexOf(".") < 0 )
					className = PIPELINE_NAMESPACE + className;
				Class pipeClass = Class.forName(className, false, this.getClass().getClassLoader());
				tmp = (TermPipeline) (pipeClass.getConstructor(new Class[]{TermPipeline.class}).newInstance(new Object[] {next}));
				next = tmp;
			}catch (Exception e){
				System.err.println("TermPipeline object "+PIPELINE_NAMESPACE+pipes[i]+" not found: "+e);
			}
		}
		pipeline_first = next;
	}
	
	
	protected class addTermToInvIndexProcedure implements FieldDocumentTree.FDTnodeProcedure{
		
		int docno;
		
		public addTermToInvIndexProcedure(int d)
		{
			docno = d;
		}
		
		public void execute(FieldDocumentTreeNode node)
		{
			int termid = L.incrementEntry(node.term,node.frequency,1);
			II.addDocForTerm(termid, docno, node.frequency /*tfs*/);
			
		}
	}
	

	public int[] indexDocument(final String docno, final Document doc)
	{
		numberOfDocuments++; 
		
		/* setup for parsing */
		termsInDocument = new FieldDocumentTree();
		String term; //term we're currently processing
		numOfTokensInDocument = 0;
		int numberOfPointerInDocument =0;

		//get each term in the document
		while (!doc.endOfDocument()) {
			
			if ((term = doc.getNextTerm())!=null && !term.equals("")) {
				termFields = doc.getFields();
				/* pass term into TermPipeline (stop, stem etc) */
				pipeline_first.processTerm(term);
				/* the term pipeline will eventually add the term to this object. */
			}
			if (MAX_TOKENS_IN_DOCUMENT > 0 && 
					numOfTokensInDocument > MAX_TOKENS_IN_DOCUMENT)
					break;
		}
		//if we didn't index all tokens from document,
		//we need to get to the end of the document.
		while (!doc.endOfDocument()) 
			doc.getNextTerm();
		/* we now have all terms in the DocumentTree, so we save the document tree */
		if (termsInDocument.getNumberOfNodes() == 0)
		{	/* this document is empty, add the minimum to the document index */
			//TODO: having entries for empty documents is a good idea (TM)
			//was default behaviour in 1.0.x
			//indexEmpty(docid);
		}
		else
		{	/* index this document */
			numberOfTokens += numOfTokensInDocument;
			numberOfPointerInDocument = termsInDocument.getNumberOfNodes();
			final int docid = DoI.newDocEntry(docno,numOfTokensInDocument);
			DiI.addDoc(docid,termsInDocument);
			termsInDocument.forEachNode(new addTermToInvIndexProcedure(docid));
		}
		
		return new int[]{numOfTokensInDocument, numberOfPointerInDocument}; 
	}
	
	
	public void indexCollection(final Collection collection)
	{
		final long startCollection = System.currentTimeMillis();
		int docCount = 0;
		while(collection.nextDocument())
		{
			/* get the next document from the collection */
			/* get the docno and the document */
			String docno = collection.getDocid();
			Document doc = collection.getDocument();
			
			if (doc == null)
				continue;
			docCount++;
			indexDocument(docno, doc);
		}
		long endCollection = System.currentTimeMillis();
		System.err.println("Collection took "+((endCollection-startCollection)/1000.0)+"seconds to index "
				+"("+docCount+" documents)\n");
	}
	
	public void indexCollections(Collection[] collections)
	{
		final int l = collections.length;
		for(int i=0;i<l;i++)
			indexCollection(collections[i]);
	}
	

	
	// End of theft
	//*******************************************************************************************************************
	
	
	/**
	 * Closes the In-memory index, writing it to disk?
	 * It should perform a flush to disk on MemoryDirectIndex,
	 * MemoryInvertedIndex, MemoryDocumentIndex and memoryLexicon
	 */
	public void close() {
		DiI.close();
		DoI.close();
		II.close();
		L.close();
		NegL.close();
	}

	
	//Getters
	/**
	 * Returns the MemoryDirectIndex
	 */
	public DirectIndex getDirectIndex() {
		return DiI;
	}

	/**
	 * Returns the MemoryDocumentIndex
	 */
	public DocumentIndex getDocumentIndex() {
		return DoI;
	}

	/**
	 * Returns the MemoryInvertedIndex
	 */
	public InvertedIndex getInvertedIndex() {
		return II;
	}

	/**
	 * Returns the MemoryLexiconIndex
	 */
	public Lexicon getLexicon() {
		return L;
	}
	
	public MemoryLexicon getNegativeLexicon()
	{
		return NegL;
	}
	
	
	public int getNumberOfDocuments()
	{
		return numberOfDocuments;
	}
	
	public long getNumberOfPointers()
	{
		return numberOfPointers;
	}
	
	public int getNumberOfTerms()
	{
		//TODO: remove cast once Lexicon API has been fixed, ie method no longer returns long
		return (int)(L.getNumberOfLexiconEntries());
	}
	
	public long getNumberOfTokens()
	{
		return numberOfTokens;
	}
	
	public void flush(){
		//does nothing.
	}
	/*
	public void flush(){
		System.err.println("Flushing to disk "+DoI.getNumberOfDocuments());
		FilePosition[] positions = DiI.flush(currentNumberOfSegments);
		//DoI.flush(positions);

		long startTime = System.currentTimeMillis();
		GDoI.merge(currentNumberOfSegments,DoI,positions);
		long gdoitime = System.currentTimeMillis();
		System.err.println("Time taken to flush document index: "+((gdoitime-startTime)/1000));

		int docidCounter = MemoryDocumentIndex.docidCounter;
		DoI = new MemoryDocumentIndex();
		MemoryDocumentIndex.docidCounter = docidCounter;

		
		//Merge the Memory lexicon entries into the global lexicon
		startTime = System.currentTimeMillis();
		GL.merge(L);
		long glmergetime = System.currentTimeMillis();
		System.err.println("Time taken to flush lexicon ("+L.getNumberOfLexiconEntries()+" terms): "+((glmergetime-startTime)/1000));
		positions = II.flush(currentNumberOfSegments);
		long postingswrite = System.currentTimeMillis();
		System.err.println("Time taken to flush II ("+L.getNumberOfLexiconEntries()+" terms, "+L.getNumberOfPointers()+" pointers): "+((postingswrite-glmergetime)/1000));	
		GL.merge(currentNumberOfSegments,L,positions);
		glmergetime = System.currentTimeMillis();
		System.err.println("Time taken to flush lexicon2 ("+L.getNumberOfLexiconEntries()+" terms): "+((glmergetime-postingswrite)/1000));
		//L.flush(positions);
		//System.out.println("*************************************************");
		//L.print();
		//System.out.println("*************************************************");
		//GL.print();
		//System.out.println("*************************************************");
		L = new MemoryLexicon();
		
		II = new MemoryInvertedIndex();
		DiI = new MemoryDirectIndex();
		
		SII.addInvertedIndex(currentNumberOfSegments);
		SDI.addDirectIndex(currentNumberOfSegments);
		
		//SII.print();
		
		//System.err.println("Here "+GDoI.getNumberOfDocuments());
		
		CollectionStatistics.createCollectionStatistics(
			GDoI.getNumberOfDocuments(), numberOfTokens,
			GL.getNumberOfLexiconEntries(), GL.getNumberOfPointers(),
			new String[] {
				"uk.ac.gla.terrier.structures.Lexicon",
				"uk.ac.gla.terrier.structures.DocumentIndexEncoded",
				"uk.ac.gla.terrier.structures.DirectIndex",
				"uk.ac.gla.terrier.structures.InvertedIndex"}
			);
		
		currentNumberOfSegments++;
	}
	*/
	
	public void print()
	{
		System.out.println("This is the Document Index, \n");
		//L.print();
		//System.out.println("Here is the memory lexicon: \n");
		DoI.print();
		
	}
	
	public static void main (String args[])
	{
		MAX_DOCS_PER_BUILDER = 1000;
		MemoryIndex mi = new MemoryIndex();
		mi.indexCollection( new TRECCollection());
		//mi.flush();
		mi.close();
	}
	

}
