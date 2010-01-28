/*
 * Created on 28 Apr 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import uk.ac.gla.terrier.matching.ResultSet;
import uk.ac.gla.terrier.querying.SearchRequest;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GenoBaselineQuerying extends TRECQuerying {
	
	protected boolean genoOutputFormat = Boolean.parseBoolean(ApplicationSetup.getProperty("geno.format", "true"));
	
	/**
	 * 
	 */
	public GenoBaselineQuerying() {
		// TODO Auto-generated constructor stub
		genoOutputFormat = Boolean.parseBoolean(ApplicationSetup.getProperty("geno.format", "true"));
	}

	/**
	 * @param i
	 */
	public GenoBaselineQuerying(Index i) {
		super(i);
		// TODO Auto-generated constructor stub
		genoOutputFormat = Boolean.parseBoolean(ApplicationSetup.getProperty("geno.format", "true"));
	}
	
	/*public void convertCornellResultFile(String resultFilename){
		final Runtime r = Runtime.getRuntime();
		try{
			File fResult = new File(resultFilename);
			File fCornell = new File(resultFilename+".cornell");
			File fTemp = new File(resultFilename+".tmp");
			fResult.renameTo(fCornell);
			Process conversion = r.exec("java -jar "+locatorJar+" -l "+htmlCollDir+" -basename "+basename+" -batch "+fCornell);
			String rline;
			String line = null;
            BufferedReader rbr = new BufferedReader(new InputStreamReader(conversion.getInputStream()));
            BufferedReader br = Files.openFileReader(fCornell);
            BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(fTemp);
            String queryid = "";
            while ((rline = rbr.readLine())!=null && (line=br.readLine())!=null) {
            	// System.out.println(rline);
            	if (line.startsWith("#")){
            		queryid = line.substring(line.indexOf(' '), line.length()).trim();
            		line = br.readLine();
            	}
                if (rline.endsWith("tag")) {
                	StringTokenizer stk = new StringTokenizer(rline);
                	stk.nextToken();
                	String pmid = stk.nextToken();
                	String rank = stk.nextToken();
                	String score = stk.nextToken();
                	String offset = stk.nextToken();
                	String length = stk.nextToken();
                	//bw.wr
                }
            }
            rbr.close();	rbr = null; bw.close(); bw = null; br.close(); br = null;
            try{
            	conversion.waitFor(); //wait for completion
            } catch (InterruptedException ie) {
            } finally {
            	conversion.destroy();
            }
            int exitCode = conversion.exitValue();
            System.err.println("exit code was " + exitCode);
            // add query ids
            // delete temp files
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}*/
	
	/** 
	 * Prints the results for the given search request, 
	 * using the specified destination. 
	 * @param pw PrintWriter the destination where to save the results.
	 * @param q SearchRequest the object encapsulating the query and the results.
	 */
	public void printResults(final PrintWriter pw, final SearchRequest q) {
		final ResultSet set = q.getResultSet();
		final DocumentIndex docIndex = index.getDocumentIndex();
		final int[] docids = set.getDocids();
		final double[] scores = set.getScores();
		//logger.debug("result size: "+set.getResultSize());
		final int maximum = 
			RESULTS_LENGTH > set.getResultSize() || RESULTS_LENGTH == 0
			? set.getResultSize()
			: RESULTS_LENGTH;
		//logger.debug("maximum: "+maximum);
		//if the minimum number of documents is more than the
		//number of documents in the results, aw.length, then
		//set minimum = aw.length

		//if (minimum > set.getResultSize())
		//	minimum = set.getResultSize();
		final String iteration = ITERATION + "0";
		final String queryId = q.getQueryID();
		final String methodExpanded = " " + method + ApplicationSetup.EOL;
		StringBuilder sbuffer = new StringBuilder();
		//the results are ordered in descending order
		//with respect to the score. 
		int limit = 10000;
		int counter = 0;
		int pmidRankCounter = 0;
		THashSet<String> pmidSet = new THashSet<String>();
		for (int i = 0; i < maximum; i++) {
			if (scores[i] <= 0d){
				if (logger.isDebugEnabled()){
					String docno = docIndex.getDocumentNumber(docids[i]);
					logger.debug(docno+": "+scores[i]);
				}
				continue;
			}
			String docno = docIndex.getDocumentNumber(docids[i]);
			String[] tokens = docno.split("-");
			String pmid = tokens[0];
			String offset = tokens[1]; // sentid in cornell format
			String length = tokens[2]; // type in cornell format
			if (genoOutputFormat)
				sbuffer.append(queryId+" "+pmid+" "+(i+1)+" "+scores[i]+" "+offset+" "+length+methodExpanded);
			else {
				//sbuffer.append(queryId+" "+iteration+" "+docno+" "+i+" "+scores[i]+methodExpanded);
				if (!pmidSet.contains(pmid)){
					sbuffer.append(queryId+" "+iteration+" "+pmid+" "+(pmidRankCounter++)+" "+scores[i]+methodExpanded);
					pmidSet.add(pmid);
				}
			}/*else if(outputFormat.equals("cornell")){
				if (i == 0)
					sbuffer.append("# "+queryId+ApplicationSetup.EOL);
				sbuffer.append(pmid+"\t"+offset+"\t"+i+"\t"+scores[i]+ApplicationSetup.EOL);
			}*/
			counter++;
			if (counter%limit==0){
				pw.write(sbuffer.toString());
				sbuffer=null; sbuffer = new StringBuilder();
				pw.flush();
			}
		}
		pw.write(sbuffer.toString());
		pw.flush();
	}
	
	public static void main(String[] args){
		GenoBaselineQuerying querying = new GenoBaselineQuerying();
		boolean cset = true;
		double c = 0d;
		if (args.length == 0){
			if (ApplicationSetup.getProperty("c", "null").equals("null"))
				cset = false;
			else
				c = Double.parseDouble(ApplicationSetup.getProperty("c", null));
		}else{
			c = Double.parseDouble(args[0]);
		}
		querying.processQueries(c, cset);
	}

}
