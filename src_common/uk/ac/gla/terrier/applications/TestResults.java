/*
 * Created on 2005-6-14
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import java.io.File;
import java.util.*;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.SystemUtility;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestResults {
	

	public static void main(String[] args) {
		// -res -xv [filenames]... [baseline] [outputFile]
		if (args[1].equalsIgnoreCase("-xv")){
			Vector vecFiles = new Vector();
			for (int i = 2; i < args.length-2; i++)
				vecFiles.addElement(new File(ApplicationSetup.TREC_RESULTS, args[2]));
			File baseline = new File(ApplicationSetup.TREC_RESULTS, args[args.length-2]);
			File fout = new File(ApplicationSetup.TREC_RESULTS, args[args.length-1]);
			SystemUtility.crossValidateResults(vecFiles, baseline, fout);
		}
	}
}
