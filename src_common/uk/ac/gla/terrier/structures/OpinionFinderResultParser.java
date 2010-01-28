/*
 * Created on 10 Oct 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.structures;

import java.io.BufferedReader;
import java.io.IOException;

import uk.ac.gla.terrier.indexing.TRECFullTokenizer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class OpinionFinderResultParser {
	protected String docTag = ApplicationSetup.getProperty("TrecDocTags.doctag", "DOC");
	protected String docnoTag = ApplicationSetup.getProperty("TrecDocTags.idtag", "DOCNO");
	protected String sentTag = "MPQASENT";
	
	public void parseResultFile(String resultFilename, String outputFilename){
		// for each doc, count how many subj sentences it has and sum up the diff values
		TRECFullTokenizer tk = new TRECFullTokenizer(
				);
	}
}
