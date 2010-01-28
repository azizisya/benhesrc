/*
 * Created on 2 May 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.querying;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class GenoPMIDFilter implements PostFilter {
	
	protected THashMap<String, THashSet<String>> queryidDocnosMap;
	
	protected DocumentIndex docindex;
	
	protected TIntObjectHashMap<String> spanidPmidMap = new TIntObjectHashMap<String>();

	private void loadDocumentFilter(){
		System.out.println("loading documents...");
		queryidDocnosMap = new THashMap<String, THashSet<String>>();
		String filterFilename = ApplicationSetup.getProperty("geno.doc.resultfile", "");
		int depth = ApplicationSetup.EXPANSION_DOCUMENTS;
		try{
			BufferedReader br = Files.openFileReader(filterFilename);
			String str = null;
			String currentQueryid = "";
			THashSet<String> docnoSet = null;
			int counter = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				String[] tokens = str.split("\\s* \\s*");
				String queryid = tokens[0];
				String pmid = tokens[2];
				if (currentQueryid.length() == 0){
					docnoSet = new THashSet<String>();
					currentQueryid = queryid;
					docnoSet.add(pmid);
					counter++;
				}else if (queryid.equals(currentQueryid)){
					counter++;
					if (counter <= depth || depth == 0)
						docnoSet.add(pmid);
				}else{
					queryidDocnosMap.put(currentQueryid, docnoSet);
					docnoSet = new THashSet<String>();
					currentQueryid = queryid;
					docnoSet.add(pmid);
					counter = 1;
				}
			}
			queryidDocnosMap.put(currentQueryid, docnoSet);
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
			
	public void new_query(Manager m, SearchRequest srq, ResultSet rs)
	{
		if (queryidDocnosMap == null)
			this.loadDocumentFilter();
		this.docindex = m.getIndex().getDocumentIndex();
	}

	/**
	  * Called for each result in the resultset, used to filter out unwanted results.
	  * @param m The manager controlling this query
	  * @param srq The search request being processed
	  * @param DocAtNumber which array index in the resultset have we reached
	  * @param DocNo The document number of the currently being procesed result.
	  */
	public byte filter(Manager m, SearchRequest srq, ResultSet rs, int DocAtNumber, int DocNo)
	{
		THashSet<String> docnoSet = this.queryidDocnosMap.get(srq.getQueryID());
		if (docnoSet == null)
			return FILTER_OK;
		
		String pmid = null;
		if (spanidPmidMap.contains(DocNo))
			pmid = spanidPmidMap.get(DocNo);
		else{
			pmid = this.docindex.getDocumentNumber(DocNo).split("-")[0];
			this.spanidPmidMap.put(DocNo, pmid);
		}
		
		if (docnoSet.contains(pmid))
		{
			return FILTER_OK;
		}
		return FILTER_REMOVE;
	}
	
}
