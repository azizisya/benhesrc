package uk.ac.gla.terrier.applications;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.links.MetaIndex;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.querying.parser.Query;
import uk.ac.gla.terrier.querying.parser.QueryParser;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.ArrayUtils;

/**
 *  
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class HttpQuerying extends HttpServlet {
	/** the logger used for debugging and all the relevant information */
	protected static Logger logger = Logger.getRootLogger();
	
	/** the logger used for the queries */
	protected static Logger queryLogger = Logger.getLogger(HttpQuerying.class);
	
	/** the number of processed queries. */
	protected int matchingCount = 0;
	
	/** The query manager.*/
	protected Manager queryingManager;
	
	/** The default weighting model used.*/
	protected String wModel = "PL2";
	
	/** The matching model used.*/
	protected String mModel = "Matching";

	/** The name of the manager object that handles the queries. Set by property 
	  * <tt>httpquerying.manager</tt>, defaults to Manager. */
    protected static String managerName = ApplicationSetup.getProperty("httpquerying.manager", 
		"Manager"
		/*"uk.ac.gla.terrier.distr.querying.DistributedManager"*/);
	
	/** The data structures used.*/
	protected Index index;
	
	/** The maximum number of presented results. */
	protected static int MAXRESULTS = 
		Integer.parseInt(ApplicationSetup.getProperty("matching.results_to_display","10"));

	/** should docnos be displayed */
	protected boolean SHOWDOCNO = 
		Boolean.parseBoolean(ApplicationSetup.getProperty("httpquerying.show.docno", "false"));

	/** when we started trying to load the index and manager */
	long startLoading = 0;
	
	/** thread for loading a manager */
	protected class ManagerLoader implements Runnable
	{
		public void run()
		{
			Manager dm = null;
			startLoading = System.currentTimeMillis();
			while (dm == null)
			{
				logger.info("Loading manager ("+managerName+") in ManagerLoader");
				try{
					dm = loadManager(); //new DistributedManager(wModel);
					logger.debug("after constructing the manager");
				} catch (Exception e) {
					logger.warn("Problem loading manager: try again in 30 seconds ", e);
					dm = null;				
				} catch (Error e) {
					logger.warn("Problem loading manager: try again in 30 seconds ", e);
					dm = null;
				} catch (Throwable e) {
					logger.warn("Problem loading manager: try again in 30 seconds ", e);
					dm = null;
				}
				if (dm== null)
				try{ Thread.sleep(30000);} catch (Exception e) {}
			}
			queryingManager = dm;
		
			//are reload events handled by the Manager?	
			if (queryingManager instanceof EventReceiver)
				((EventReceiver)queryingManager).selfRegisterEvents(er);
			else//use our own simple reloader
				new SimpleReloadManager().selfRegisterEvents(er);	
	

			index = dm.getIndex();
			long endLoading = System.currentTimeMillis();
			logger.info("time to intialise indexes and manager : " + ((endLoading-startLoading)/1000.0D));			
		}	
	}

	/** reloads the Manager in response to a RELOAD event */
	class SimpleReloadManager implements EventReceiver
	{
		public void selfRegisterEvents(EventRegister er)
		{
			er.registerEvent("RELOAD", this);
		}

		public void invokeEvent(String eventName, String[] data)
    	{
        	if (eventName.equals("RELOAD"))
	        {
				queryingManager = null;
    	    	managerLoaderThread = new Thread(new ManagerLoader(), "ManagerLoader-Reload");
		        managerLoaderThread.start();
        	}
	    }
	}

	/** loads the manager until it loads successfully. This means once the Manager can load, the system is running */
	protected Thread managerLoaderThread = null;
	/** recieves event notification by udp packets, such as being informed to reload the index */
	protected EventRegister er = null;
		
	/**
	 * A default constructor initialises the inverted index,
	 * the lexicon and the document index structures.
	 */
	public HttpQuerying() {
		logger.debug("in HttpQuerying constructor");

		//we dont want to wait until a Manager can be loaded. 
		//Use a thread to continually retry to load the manager
		//until it becomes available
		managerLoaderThread = new Thread(new ManagerLoader(), "ManagerLoader");
		managerLoaderThread.start();

		//this thread handles messages like reload
		er = new UDPMessageDaemon();
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
		logger.debug("Querystring passed to servlet was '"+Request.getQueryString()+"'");
		String sQuery = (String)Request.getParameter("q");
		String sSetProperty = (String)Request.getParameter("propertyset");
		
		String format = Request.getParameter("fmt");
		if (format!=null && format.equals("txt"))
			Response.setContentType("text/plain");
		else 
			Response.setContentType("text/xml");

		PrintWriter out = new PrintWriter(Response.getOutputStream());
		//delay the first few queries while the manager is starting up
		if (queryingManager == null && System.currentTimeMillis() - startLoading < 5000)
		{
			//force manager thread to check again
			managerLoaderThread.interrupt();
			for(int i=0;i<9&&queryingManager == null;i++)
			{
				try{ Thread.sleep(500);} catch (Exception e) {}
				logger.warn("Sleeping in hope that manager is starting");
			}
		}
		if (queryingManager == null) {
			logger.warn("Manager not yet loaded");
			if (format!=null && format.equals("txt")) {
				out.println("TERRIER_TXT/2.0");
				out.println("");
				out.println("number url title snippet");
				out.println("0");//number of hits
				out.println("0");//number of hits after filtering
				out.println("0");//start
				out.println("0");//end
				out.println("0");//total number of pages
				out.println("0");//current page
				out.println("\n");
			} else {
				out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
				out.println("<terrier:response xmlns:terrier=\"http://ir.dcs.gla.ac.uk/terrier/xml1.2\">");
				out.println("<terrier:action>QUERY</terrier:action>");
				out.println("<terrier:status>ERROR</terrier:status>");
				out.println("<terrier:warning>Manager is null: not yet loaded</terrier:warning>");
				out.println("<terrier:query>");
				out.print("</terrier:query>");
				out.println("</terrier:response>");
			}
			out.flush();
		}
		else if (sSetProperty != null && sSetProperty.length() > 0) {
			 Properties p = new Properties();
			for (Enumeration e =  Request.getParameterNames() ; e.hasMoreElements() ;) 
			{
		 		String keyName = (String)e.nextElement();
				if (keyName.equals("propertyset"))
					continue;
				p.setProperty(keyName, (String)Request.getParameter(keyName));
			}
			boolean ok = false;
			try{
				queryingManager.setProperties(p);
				ok = true;
			} catch (Exception e) {
				out.println("Problem: " + e);
				e.printStackTrace(out);
				logger.error("Problem setting property using propertyset: ", e);
			}
			if (ok)
				out.println("OK");
			out.flush();
			
		}
		else if (sQuery == null) {
			logger.debug("No query passed");
			if (format!=null && format.equals("txt")) {
				out.println("TERRIER_TXT/2.0");
				out.println("");
				out.println("number url title snippet");
				out.println("0");//number of hits
				out.println("0");//number of hits after filtering
				out.println("0");//start
				out.println("0");//end
				out.println("0");//total number of pages
				out.println("0");//current page
				out.println("\n");				
			} else {
				out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
				out.println("<terrier:response xmlns:terrier=\"http://ir.dcs.gla.ac.uk/terrier/xml1.2\">");
				out.println("<terrier:action>QUERY</terrier:action>");
				out.println("<terrier:status>SUCCESS</terrier:status>");
				out.println("<terrier:warning>EMPTY</terrier:warning>");
				out.println("<terrier:query>");
				out.print("</terrier:query>");
				out.println("</terrier:response>");	
			}
			out.flush();
		}
		else
		{
			Date date = new Date();
			queryLogger.info(date.getTime() + " " + Request.getRemoteAddr() + " " + sQuery);
			
			// do not alter the query string it requests searching for related documents.
			if (!sQuery.startsWith("related:")){
				// if starts with :, replace the first column with space
				sQuery = sQuery.replaceAll("^:| :", " ");
				logger.debug(sQuery);
				// replace the first +, or multiple +, with ++
				//sQuery = sQuery.replaceAll("^\\+| \\+", "\\+\\+");
				//logger.debug(sQuery);
				// temporiary replace all + with : 
				sQuery = sQuery.replaceAll("\\+", " ");
				logger.debug(sQuery);
				// replace two or more 
				//sQuery = sQuery.replaceAll(":{2,}?", " \\+");
				//logger.debug(sQuery);
				
				//sQuery = sQuery.replaceAll(":", " ");
				//logger.debug(sQuery);
			}
			
			if (logger.isDebugEnabled())
				logger.debug("Started processing search for q=\""+sQuery+"\" at "+date);
		
			try{	
				processQuery(sQuery, Response.getOutputStream(), Request);
			} catch (Throwable e) {
				doError(out, e, format);
			}
		}
	}

	protected void doError(PrintWriter out, Throwable t, String fmt)
	{
		logger.error("Querying failed because ", t);
		doError(out, t.getMessage(), fmt);
	}

	protected void doError(PrintWriter out, String t, String format)
	{
		if (format!=null && format.equals("txt")) {
				out.println("TERRIER_TXT/2.0");
				out.println("");
				out.println("number url title snippet");
				out.println("0");//number of hits
				out.println("0");//number of hits after filtering
				out.println("0");//start
				out.println("0");//end
				out.println("0");//total number of pages
				out.println("0");//current page
				out.println("\n");
		}
		else
		{
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
			out.println("<terrier:response xmlns:terrier=\"http://ir.dcs.gla.ac.uk/terrier/xml1.2\">");
			out.println("<terrier:action>QUERY</terrier:action>");
			out.println("<terrier:status>ERROR</terrier:status>");
			out.println("<terrier:warning>"+t+"</terrier:warning>");
			out.println("<terrier:query>");
			out.print("</terrier:query>");
			out.println("</terrier:response>");
		}
		out.flush();
	}

	/**
	 * Closes the used structures.
	 */
	public void close() {
		//if (index != null)
		//	index.close();
	}
	
	/**
	 * According to the given parameters, it sets up the correct matching class.
	 * @param query String the query to process.
	 * @param out where to display to.
	 */
	public void processQuery(String query, OutputStream out, HttpServletRequest request) {
		SearchRequest srq = queryingManager.newSearchRequest();
		srq.setStartedProcessingTime(System.currentTimeMillis());
		//query = query.replaceAll("\\+", " \\+");
		
		if (logger.isDebugEnabled())
			logger.debug("query string: " + request.getQueryString());
		srq.setOriginalQuery(query);
		if (logger.isDebugEnabled())
			logger.debug(query);
		
		//the start and end can be controlled from the query controls
		//or parameters of the HTTP request. The parameters of the 
		//HTTP request override the controls of the query
		//
		//when using the controls, both start and end must be 
		//specified. when using the parameters, it is necessary
		//to specify start. If end is not specified, then it 
		//takes a default value equal to start + 9.
		
		String startValue = (String)request.getParameter("start");
		String endValue = (String)request.getParameter("end");
		boolean isStartParameterSpecified = false;
		if (startValue != null) { //override the query controls
			isStartParameterSpecified = true;
			srq.setControl("start", startValue);
		}

		if (endValue != null) {
			srq.setControl("end", endValue);
		} else if (isStartParameterSpecified) {
			srq.setControl("end", String.valueOf(Integer.parseInt(startValue) + 9));
		}

		logger.debug("values before: " + startValue + " " + endValue);
		logger.debug("values before: " + srq.getControl("start") + " " + srq.getControl("end"));

		
		//is it a find-related-documents query of the form related:docid
		if (query.matches("related:\\d+")) {
			Matcher m = Pattern.compile("(\\b)(related):(\\d+)(\\b)").matcher(query);
			int docid = -1;
			String title = "";
			if (m.find()) {
				docid = Integer.parseInt(m.group(3));
				try {
					title = ((MetaIndex)(queryingManager.getIndex().getIndexStructure("meta"))).getItem("title",docid);
				} catch(IOException ioe) {
					logger.error("input/output exception while getting title of document.", ioe);
				}
				String[] titleTerms = title.trim().split("\\W+");
				query = titleTerms[0];
				for (int i=1; i<Math.min(5,titleTerms.length); i++) {
					query += " " + titleTerms[i];
				}
				logger.debug("finding related docs:" + query + ":");
			}
		}
		
		try{
			//remove dots and double quotes from the query
			query = query.replaceAll(/*"\\.|\""*/  "\\.", " ");
			//query = query.replaceAll("^:| :", " ");
			String[] parts  = query.split("\\s+");
			if (parts.length > 20)
			{
				String[] n = new String[20];
				System.arraycopy(parts, 0, n, 0, 20);	
				query = ArrayUtils.join(n, " ");
				logger.warn("Query was too long ("+parts.length+") , cropping");
			}
			Query q = QueryParser.parseQuery(query);
			srq.setQuery(q);
		} catch (Exception e) {
			logger.error("Failed to parse Q "+query+" : ",e);
			throw new Error("Failed to parse Q "+query+" : ",e);
		}
		srq.addMatchingModel(mModel, wModel);
		matchingCount++;
		
		logger.debug("values before: " + srq.getControl("start") + " " + srq.getControl("end"));
				
		queryingManager.runPreProcessing(srq);

		logger.debug("values before: " + srq.getControl("start") + " " + srq.getControl("end"));
		
		queryingManager.runMatching(srq);
		
		logger.debug("values before: " + srq.getControl("start") + " " + srq.getControl("end"));
		
		queryingManager.runPostProcessing(srq);
		
		logger.debug("values before: " + srq.getControl("start") + " " + srq.getControl("end"));
		
		queryingManager.runPostFilters(srq);
		
		logger.debug("values before: " + srq.getControl("start") + " " + srq.getControl("end"));
		
		try {
			printResults(query, srq, new PrintWriter(new OutputStreamWriter(out, "UTF8")), request);
		} catch(UnsupportedEncodingException uee) {
			logger.debug("unsupported encoding exception", uee);
		}
	}
	
	
	public void printResults(String query, SearchRequest srq, PrintWriter printwriter, HttpServletRequest request) {
		int startI = Integer.parseInt(srq.getControl("start"));
		int endI = Integer.parseInt(srq.getControl("end"));
		ResultSet resultset = srq.getResultSet();

		int count = resultset.getResultSize();
		logger.debug("count is: " + count);
		
		//if there are more results than the maximum
		//number of results to display, then adjust 
		//the end variable accordingly
		if(count > MAXRESULTS) {
			count = MAXRESULTS;
			endI = startI + count - 1;
		}
		
		if (logger.isDebugEnabled())
			logger.debug("num of docs after filtering: " + srq.getNumberOfDocumentsAfterFiltering());
		
		int pageSize = endI - startI + 1;//count;			
		int numOfDocumentsAfterFiltering = srq.getNumberOfDocumentsAfterFiltering();
		
		//if there are no retrieved documents because there was no matching
		//documents, or because of setting the start/end wronlgy, then
		//both the number of pages and the current page are set equal to zero.
		int numOfPages = 0;
		int currentPage = 0;
		if (pageSize > 0) {
			numOfPages = (int)Math.ceil(1.0d*numOfDocumentsAfterFiltering / (pageSize));
			currentPage = 1 + startI / pageSize;
		}
				
		StringBuilder stringbuffer = new StringBuilder();
		final int docids[] = resultset.getDocids();
		if (SHOWDOCNO)
			index = queryingManager.getIndex();
		final DocumentIndex docIndex = (SHOWDOCNO && index != null) ? index.getDocumentIndex() : null;
		if (SHOWDOCNO && docIndex == null)
		{
			logger.warn("Disabled printing docids, because document index not available");
			SHOWDOCNO = false;
		}
		long endProcessingTime; 
		String format = request.getParameter("fmt");
		endProcessingTime = System.currentTimeMillis();
		if (format!=null && format.equals("txt")) {

			stringbuffer.append("TERRIER_TXT/2.0\n");
			stringbuffer.append(srq.getQuery());
			stringbuffer.append("\n");
			if (SHOWDOCNO)
				stringbuffer.append("number docno url title snippet\n");
			else
				stringbuffer.append("number url title snippet\n");
			stringbuffer.append(resultset.getExactResultSize());
			stringbuffer.append("\n");
			stringbuffer.append(numOfDocumentsAfterFiltering);
			stringbuffer.append("\n");
			stringbuffer.append(startI);
			stringbuffer.append("\n");
			stringbuffer.append(startI+count-1);
			stringbuffer.append("\n");
			stringbuffer.append(numOfPages);
			stringbuffer.append("\n");
			stringbuffer.append(currentPage);
			stringbuffer.append("\n\n");
			for(int i=0; i<count; i++) {
				stringbuffer.append(startI+i);
				stringbuffer.append("\n");
				if (SHOWDOCNO)
				{
					stringbuffer.append(docIndex.getDocumentNumber(docids[i]));
					stringbuffer.append("\n");	
				}
				stringbuffer.append(resultset.getMetaItem("url", i));
				stringbuffer.append("\n");
				stringbuffer.append(resultset.getMetaItem("title", i));
				stringbuffer.append("\n");
				stringbuffer.append(resultset.getMetaItem("snippet", i));
				stringbuffer.append("\n\n");
			}	
			
		} else {//xml by default
			stringbuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
			stringbuffer.append("<terrier:response xmlns:terrier=\"http://ir.dcs.gla.ac.uk/terrier/xml1.2\">\n");
			stringbuffer.append("<terrier:action>QUERY</terrier:action>\n<terrier:status>SUCCESS</terrier:status>\n");
			if (count == 0) {
				stringbuffer.append("<terrier:warning>EMPTY</terrier:warning>\n<terrier:responsedata>\n<terrier:query>");
				stringbuffer.append(request.getParameter("q"));
				stringbuffer.append("</terrier:query>");
			} else {
				stringbuffer.append("<terrier:responsedata>\n<terrier:control name=\"start\">");
				stringbuffer.append(startI);
				stringbuffer.append("</terrier:control>\n<terrier:control name=\"end\">");
				stringbuffer.append(endI);
				stringbuffer.append("</terrier:control>\n<terrier:query>");
				stringbuffer.append(request.getParameter("q"));
				stringbuffer.append("</terrier:query>\n<terrier:hits>");
				stringbuffer.append(resultset.getExactResultSize());
				stringbuffer.append("</terrier:hits>\n<terrier:totalpages>");
				stringbuffer.append(numOfPages);
				stringbuffer.append("</terrier:totalpages>\n<terrier:page>");
				stringbuffer.append(currentPage);
				stringbuffer.append("</terrier:page>\n<terrier:time>");
				stringbuffer.append(endProcessingTime-srq.getStartedProcessingTime());
				stringbuffer.append("ms</terrier:time>\n");
				
				for(int i=0; i<count; i++) {
					stringbuffer.append("<terrier:hit>\n<terrier:rank>");
					stringbuffer.append(startI+i);
					stringbuffer.append("</terrier:rank>\n<terrier:url>");
					stringbuffer.append(resultset.getMetaItem("url", i));
					stringbuffer.append("</terrier:url>\n<terrier:title><![CDATA[");
					stringbuffer.append(resultset.getMetaItem("title", i));
					stringbuffer.append("]]></terrier:title>\n<terrier:extract><![CDATA[");
					stringbuffer.append(resultset.getMetaItem("snippet", i));
					stringbuffer.append("]]></terrier:extract>\n<terrier:docid>");
					stringbuffer.append(docids[i]);
					stringbuffer.append("</terrier:docid>\n</terrier:hit>");
				}				
			}
			stringbuffer.append("</terrier:responsedata>\n</terrier:response>\n");
		}
		if (logger.isDebugEnabled())
			logger.debug("caching: " + query.toLowerCase().trim());
		printwriter.write(stringbuffer.toString());
		printwriter.flush();
		if (logger.isDebugEnabled())
			logger.debug("Processed query in " + 
					(endProcessingTime-srq.getStartedProcessingTime()) + "ms");
	}

	protected static Manager loadManager() throws Exception {
            Manager rtr =null;
            if (managerName.indexOf('.') == -1)
                managerName = "uk.ac.gla.terrier.querying."+managerName;
            //assume default constructor is available
            rtr = (Manager)(Class.forName(managerName).newInstance());
            return rtr;
        }

}
