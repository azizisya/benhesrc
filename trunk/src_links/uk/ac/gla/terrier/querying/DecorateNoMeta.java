package uk.ac.gla.terrier.querying;
/** @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
//import uk.ac.gla.terrier.links.MetaIndex;
//import uk.ac.gla.terrier.links.MetaServer2;
import java.io.IOException;

import uk.ac.gla.terrier.links.URLIndex;
import uk.ac.gla.terrier.links.URLServer;
import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;
public class DecorateNoMeta implements PostProcess, PostFilter
{
	protected URLIndex urlIndex = new URLServer();
	//protected MetaIndex metaIndex = new MetaServer2();
	protected DocumentIndex docIndex = null;

	//hand controlled, see properties
	protected boolean show_docid_early = new Boolean(
		ApplicationSetup.getProperty("postproccess.decorate.show_docid_early", "false")).booleanValue();
	/* set postproccess.decorate.show_docid_early if scope filter is enabled */
	
	protected boolean show_url_early = new Boolean(
		ApplicationSetup.getProperty("postproccess.decorate.show_url_early", "true")).booleanValue();
	/* set postproccess.decorate.show_docid_early if scope filter is enabled */

	//init at post process, postfilter stage
	protected boolean show_snippet = false;
	protected boolean show_title = false;
	protected boolean show_docid = false;
	protected boolean show_url = false;
	
	public DecorateNoMeta()
	{
	}

	public void new_query(Manager m, SearchRequest q, ResultSet rs)
	{
		show_snippet = checkControl("show_snippet", q);
		show_title = checkControl("show_title", q);
		show_docid = checkControl("show_docid", q);
		show_url = checkControl("show_url", q);
		if (show_docid)
			docIndex = m.getIndex().getDocumentIndex();
	}

	//decoration at the postfilter stage
	public byte filter(Manager m, SearchRequest q, ResultSet rs, int DocAtNumber, int DocNo)
	{
		/*if (show_snippet)
		{
			rs.addMetaItem("snippet", DocAtNumber, metaIndex.getItem("snippet", DocNo));
		}*/
		/*if (show_title)
		{
			rs.addMetaItem("title", DocAtNumber, metaIndex.getItem("title", DocNo));
		}*/
		if (!show_docid_early && show_docid)
		{
			rs.addMetaItem("docid", DocAtNumber, docIndex.getDocumentNumber(DocNo));
		}
		if (!show_url_early && show_url)
		{
			String u = "";
			try{ u = urlIndex.getURL(DocNo); }catch(IOException ioe){}
			rs.addMetaItem("url", DocAtNumber, u);
		}
		
		return FILTER_OK;
	}

	/** decoration at the postprocess stage. only decorate if required for future postfilter or postprocesses.
	  * @param manager The manager instance handling this search session.
	  * @param q the current query being processed
	  */
	public void process(Manager manager, SearchRequest q)
	{
		ResultSet rs = q.getResultSet();
		new_query(manager, q, rs);
		int docids[] = rs.getDocids();
		int resultsetsize = docids.length;
		if (show_url_early && show_url)
		{
			String[] urls = new String[resultsetsize];
			for(int i=0;i<resultsetsize;i++)
			{
				try{
					urls[i] = urlIndex.getURL(docids[i]);
				}catch(IOException ioe){
					urls[i] = "";
				}
			}
			rs.addMetaItems("url", urls);
		}

		if (show_docid_early && show_docid)
		{
			String[] documentids = new String[resultsetsize];
			for(int i=0;i<resultsetsize;i++)
			{
				documentids[i] = docIndex.getDocumentNumber(docids[i]);
			}

			System.err.println("Decorating with docnos for "+documentids.length + "result");
			if (documentids.length > 0)
				System.err.println("\tFirst docno is "+documentids[0]);

			rs.addMetaItems("docid", documentids);
		}
	}

	protected boolean checkControl(String control_name, SearchRequest srq)
	{
		String controlValue = srq.getControl(control_name);
		if (controlValue.length() == 0)
			return false;
		if (controlValue.equals("0") || controlValue.toLowerCase().equals("off")
			|| controlValue.toLowerCase().equals("false"))
			return false;
		return true;
	}
	
	/**
	 * Returns the name of the post processor.
	 * @return String the name of the post processor.
	 */
	public String getInfo()
	{
		return "Decorate";
	}
}
