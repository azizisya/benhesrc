import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.gla.terrier.querying.parser.Query;
import uk.ac.gla.terrier.querying.parser.QueryParser;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;

import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.* ;
import javax.servlet.* ;
import antlr.TokenStreamSelector;

/**
 *  
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class HTTPQuerying extends HttpServlet{
	/** display more debugging info to stderr */
	protected boolean verbose = true;
	
	/** the number of processed queries. */
	protected int matchingCount = 0;
	
	/** The query manager.*/
	protected Manager queryingManager;
	
	/** The default weighting model used.*/
	protected String wModel = ApplicationSetup.getProperty("http.model", "PL2");
	
	/** The matching model used.*/
	protected String mModel = ApplicationSetup.getProperty("http.matching", "Matching");
	
	/** The data structures used.*/
	protected Index index;
	
	/** The maximum number of presented results. */
	protected static int MAXRESULTS = 
		Integer.parseInt(ApplicationSetup.getProperty("http.output.format.length", "1000"));
		
	/**
	 * A default constructor initialises the inverted index,
	 * the lexicon and the document index structures.
	 */
	public HTTPQuerying() {
		long startLoading = System.currentTimeMillis();
		index = Index.createIndex();
		if(index == null)
		{
			System.err.println("ERROR: Failed to load indexes. Perhaps index files are missing");
			System.exit(1);
		}
		queryingManager = new Manager(index);
		long endLoading = System.currentTimeMillis();
		if (verbose)
			System.err.println("time to intialise indexes : " + ((endLoading-startLoading)/1000.0D));
		
	}

	public void doHead(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		doGet(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		doGet(req, res);
	}


	public void doGet(HttpServletRequest Request, HttpServletResponse Response)
		throws ServletException, IOException
	{
		//main request starts here!
		Response.setContentType("text/plain");
		String sQuery = (String)Request.getParameter("q");
		if (sQuery == null)
		{
			System.out.println("No query passed");
			PrintWriter out = new PrintWriter(Response.getOutputStream());
			out.println("TERRIER_TXT/1.0");
			out.println("");
			out.println("number url title snippet");
			out.println("0");
			out.println("\n");
		}
		else
		{
			System.out.println("Started processing search for q=\""+sQuery+"\" at "+(new java.util.Date()));
			//TERRIER IS NOT THREAD SAFE, SO PREVENT CONCURRENT ACCESS
			synchronized(this)
			{
				processQuery(sQuery, Response.getOutputStream());
			}
		}
	}

	/**
	 * Closes the used structures.
	 */
	public void close() {
		index.close();
	}
	
		/**
	 * According to the given parameters, it sets up the correct matching class.
	 * @param query String the query to process.
	 * @param out where to display to.
	 */
	public void processQuery(String query, OutputStream out) {
		SearchRequest srq = queryingManager.newSearchRequest();
	
			
		Pattern scopes = Pattern.compile(".*(scope(?:s):[^ ]+).*", Pattern.CASE_INSENSITIVE);
		Matcher m = scopes.matcher(query);

		//System.out.println("m.matches MATCHES: "+query.indexOf("scopes"));
		//System.out.println("m.matches MATCHES: "+m.matches());
		if (m.matches())
		{
			String control = m.group(1);
			query = query.replaceAll("scope(?:s):[^ ]+", " ");
			String kv[] = control.split(":");
			srq.setControl(kv[0], kv[1]);
			System.err.println("setting '"+kv[0] +"' to '"+ kv[1]+ "'");
		}
		
		if (verbose)
			System.out.println(query);
		try{
			Query q = QueryParser.parseQuery(query);
            srq.setQuery(q);
		} catch (Exception e) {
			System.err.println("Failed to process Q"+query+" : "+e);
			return;
		}
		
		srq.addMatchingModel(mModel, wModel);
		matchingCount++;
		queryingManager.runPreProcessing(srq);
		queryingManager.runMatching(srq);
		queryingManager.runPostProcessing(srq);
		queryingManager.runPostFilters(srq);
		printResults(srq, new PrintWriter(out));
	}
	
	
	public void printResults(SearchRequest srq, PrintWriter printwriter) {
		ResultSet resultset = srq.getResultSet();
		int ai[] = resultset.getDocids();
		double ad[] = resultset.getScores();
		int count = resultset.getResultSize();
		if(count > MAXRESULTS)
			count = MAXRESULTS;

		boolean showDocno = srq.getControl("show_docid").equals("on");
	
		
		StringBuffer stringbuffer = new StringBuffer();	
		synchronized(stringbuffer) {
			stringbuffer.append("TERRIER_TXT/1.0\n");
			stringbuffer.append(srq.getQuery());
			stringbuffer.append("\n");
			stringbuffer.append("number url title snippet");
			if (showDocno)
				stringbuffer.append(" docid");
			stringbuffer.append("\n");
			stringbuffer.append(resultset.getExactResultSize());
			stringbuffer.append("\n\n");
			printwriter.write(stringbuffer.toString());
			
			int docids[] = resultset.getDocids();
			for(int i=0;i<count;i++)
			{
				stringbuffer = new StringBuffer();
				stringbuffer.append(i);
				//stringbuffer.append("\n");
				//stringbuffer.append(docids[i]);
				stringbuffer.append("\n");
				stringbuffer.append(resultset.getMetaItem("url", i));
				stringbuffer.append("\n");
				stringbuffer.append(resultset.getMetaItem("title", i));
				stringbuffer.append("\n");
				stringbuffer.append(resultset.getMetaItem("snippet", i));
				if(showDocno)
				{
					stringbuffer.append("\n");
					stringbuffer.append(resultset.getMetaItem("docid", i));
				}
				stringbuffer.append("\n\n");
				printwriter.write(stringbuffer.toString());
			}	
		}
		printwriter.flush();
		System.err.println("Finished rendering");
	}

	public static void main(String[] args)
	{
		StringBuffer s = new StringBuffer();
		for(int i=0;i<args.length;i++)
		{
			s.append(args[i]);
			s.append(" ");
		}
		new HTTPQuerying().processQuery(s.toString(), System.out);
	}
}
