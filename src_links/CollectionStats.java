import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 *  
 * @author Craig Macdonald
 * @version $Revision: 1.1 $
 */
public class CollectionStats extends HttpServlet{
	CollectionStatistics cs = null;

	public CollectionStats()
	{
		try{
			cs = Index.createIndex().getCollectionStatistics();
		} catch (Exception e) { }
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
		PrintWriter out = new PrintWriter(Response.getOutputStream());
		out.println(cs.getNumberOfDocuments());
		out.println(cs.getNumberOfPointers());
		out.println(cs.getNumberOfTokens());
		out.println(cs.getNumberOfUniqueTerms());
		out.println("\n");
		out.println("terrier.setup="+System.getProperty("terrier.setup","NONE SET"));
		out.flush();
	}
}
