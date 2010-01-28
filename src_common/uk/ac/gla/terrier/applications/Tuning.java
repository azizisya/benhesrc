/*
 * Created on 2005-2-23
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.tuning.NETuning;
import uk.ac.gla.terrier.tuning.ParameterTuning;
import uk.ac.gla.terrier.utility.SystemUtility;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Tuning {

	public static void main(String[] args) {
		String methodName = args[1];
		String tuningType = args[2];
		String samplingType = args[3];
		int numberOfSamples = 200;
		if (args.length > 4)
			numberOfSamples = Integer.parseInt(args[4]);
		ParameterTuning tuning = null;
		String packagePrefix = "uk.ac.gla.terrier.smooth.tuning.";
		boolean recognised = false;
		if (tuningType.equalsIgnoreCase("-NE")){
			tuning = new NETuning(methodName, Index.createIndex());
			recognised = true;
		}
		if (tuningType.equalsIgnoreCase("-corr")){
			System.err.println("Correlation tuning is not implemented. Exist...");
			recognised = true;
			System.exit(1);
//				tuning = (ParameterTuning)Class.forName(packagePrefix+
//					"CorrelationTuning").newInstance();
		}
		if(!recognised){
			System.err.println("Unrecognised tuning type. Exist...");
			System.exit(1);
		}
		
		if (samplingType.equalsIgnoreCase("-real")){
			double parameter = tuning.tuneRealTRECQuery();
			System.out.println("-------------------------------------");
			System.out.println("Normalisation method: " + tuning.method.getInfo());
			System.out.println("query type: " + SystemUtility.queryType());
			System.out.println("Estimated setting: " + parameter);
		}
		if (samplingType.equalsIgnoreCase("-sim")){
			double parameter = tuning.tuneSampling(numberOfSamples);
			System.out.println("-------------------------------------");
			System.out.println("Normalisation method: " + tuning.method.getInfo());
			System.out.println("query type: " + SystemUtility.queryType());
			System.out.println("Estimated setting: " + parameter);
		}
	}
}
