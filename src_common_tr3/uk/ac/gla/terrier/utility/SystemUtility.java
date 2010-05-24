package uk.ac.gla.terrier.utility;

import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.structures.trees.*;

import org.terrier.structures.TRECQuery;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;
import org.terrier.utility.Rounding;
import org.terrier.utility.TagSet;
import org.terrier.matching.models.*;
import org.terrier.matching.models.queryexpansion.QueryExpansionModel;

import java.io.*;
import java.util.*;
//import ben.uk.ac.gla.terrier.structures.*;
/**
 * This class implements some utilities of the system.
 * @author  ben
 */
public class SystemUtility {

    public static void writeString(File fOutput, String str) throws IOException {
    	BufferedWriter bw = new BufferedWriter(new FileWriter(fOutput));
    	bw.write(str);
    	bw.close();
    }
    
    public static String stripWeights(String queryString){
		StringBuilder buf = new StringBuilder();
		String[] tokens = queryString.replaceAll("\\^", " ").split(" ");
		if (tokens.length % 2 == 0){
			// no query id
			for (int i=0; i<tokens.length; i+=2)
				buf.append(tokens[i]+" ");
		}else{
			// has query id
			for (int i=1; i<tokens.length; i+=2)
				buf.append(tokens[i]+" ");
		}
		return buf.toString().trim();
	}
    
    public static String stripNumbersAtTheEnd(String str){
    	str = str.trim();
    	String term = "";
    	boolean isNumber = false;
    	for (int i = str.length() - 1; i >=0 ; i--){
    		if (str.charAt(i)>='0' && str.charAt(i)<='9'){
    			term = str.charAt(i)+term;
    			if (!isNumber)
    				isNumber = true;
    		}
    		else if (isNumber)
    			break;
    	}
    	try{
    		while (term.charAt(0) == '0')
    			term = term.substring(1, term.length());
    	}
    	catch(Exception e){
    		System.err.println("str: " + str + ", term: " + term);
    		e.printStackTrace();
    		System.exit(1);
    	}
    	return term;
    }
    
    public static void readStringColumn(File f, int columnIndex, Vector vector){
    	try{
    		BufferedReader br = new BufferedReader(new FileReader(f));
    		String str = null;
    		while ((str = br.readLine())!=null){
    			if (str.trim().length() == 0)
    				continue;
    			StringTokenizer stk = new StringTokenizer(str.trim());
    			int counter = 0;
    			while (counter < columnIndex){
    				stk.nextToken();
    				counter++;
    			}
    			
    			vector.addElement(stk.nextToken());
    		}
    		br.close();
    	}
    	catch(IOException ioe){
    		ioe.printStackTrace();
    		System.exit(1);
    	}
    }
    
    public static String doubleArrayToString(double[] data, String separator,
    int effDec){
    	StringBuffer buffer = new StringBuffer();
    	for (int i = 0; i < data.length; i++)
    		buffer.append(Rounding.toString(data[i], effDec) + separator);
    	return buffer.toString();
    }
    
    public static double getAverageQueryLength(){
    	TRECQuery queries = new TRECQuery();
    	double[] avql = new double[queries.getNumberOfQueries()];
    	int counter = 0;
    	while (queries.hasMoreQueries()){
    		avql[counter++] = (new StringTokenizer(queries.nextQuery())).countTokens();
    	}
    	return Statistics.mean(avql);
    }
    
    public static double getMedianQueryLength(){
    	TRECQuery queries = new TRECQuery();
    	double[] avql = new double[queries.getNumberOfQueries()];
    	int counter = 0;
    	while (queries.hasMoreQueries()){
    		avql[counter++] = (new StringTokenizer(queries.nextQuery())).countTokens();
    	}
    	Arrays.sort(avql);
    	return Statistics.median(avql);
    }
	
	public static WeightingModel initWeightingModel(String methodName, double beta){
		WeightingModel wmodel = null;
		if (methodName.lastIndexOf('.') < 0)
			methodName = "uk.ac.gla.terrier.matching.models.".concat(methodName);
		try {
			wmodel = (WeightingModel) Class.forName(methodName).newInstance();
			wmodel.setParameter(beta);
		} 
		catch(InstantiationException ie) {
			System.err.println("Exception while loading the weighting model class:\n" + ie);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(IllegalAccessException iae) {
			System.err.println("Exception while loading the weighting model class:\n" + iae);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(ClassNotFoundException cnfe) {
			System.err.println("Exception while loading the weighting model class:\n" + cnfe);
			System.err.println("Exiting...");
			System.exit(1);
		}	
		return wmodel;
	}
	
	public static QueryExpansionModel initQueryExpansionModel(String qemethodName, 
			double beta, double avl, double numOfTokens){
		QueryExpansionModel qemodel = null;
		if (qemethodName.lastIndexOf('.') < 0)
			qemethodName = 
				"uk.ac.gla.terrier.matching.models.queryexpansion.".concat(qemethodName);
		try {
			qemodel = (QueryExpansionModel) Class.forName(qemethodName).newInstance();
		} catch(InstantiationException ie) {
			System.err.println("Exception while loading the query expansion model class:\n" + ie);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(IllegalAccessException iae) {
			System.err.println("Exception while loading the query expansion model class:\n" + iae);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(ClassNotFoundException cnfe) {
			System.err.println("Exception while loading the query expansion model class:\n" + cnfe);
			System.err.println("Exiting...");
			System.exit(1);
		}
		qemodel.setAverageDocumentLength(avl);
		qemodel.setNumberOfTokens(numOfTokens);
		return qemodel;
	}
	
	public static QueryExpansionModel initQueryExpansionModel(String qemethodName, 
			double beta){
		QueryExpansionModel qemodel = null;
		if (qemethodName.lastIndexOf('.') < 0)
			qemethodName = 
				"uk.ac.gla.terrier.matching.models.queryexpansion.".concat(qemethodName);
		try {
			qemodel = (QueryExpansionModel) Class.forName(qemethodName).newInstance();
		} catch(InstantiationException ie) {
			System.err.println("Exception while loading the query expansion model class:\n" + ie);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(IllegalAccessException iae) {
			System.err.println("Exception while loading the query expansion model class:\n" + iae);
			System.err.println("Exiting...");
			System.exit(1);
		} catch(ClassNotFoundException cnfe) {
			System.err.println("Exception while loading the query expansion model class:\n" + cnfe);
			System.err.println("Exiting...");
			System.exit(1);
		}
		return qemodel;
	}
    
    /** This method converts the data saved in the File f into Matlab compatible format. */
    public static File convertToMatlabFormat(File f){
        File fDat = null;
        try{
	        // loads the data in the given file
            BufferedReader br = new BufferedReader(new FileReader(f));
            
            String buf = br.readLine();
            StringTokenizer stk = new StringTokenizer(buf);
            int dimension = stk.countTokens();
            
            
            StringBuffer[] sb = new StringBuffer[dimension];
            for (int i=0; i<dimension; i++)
            	sb[i] = new StringBuffer();
            while(buf!=null){
                if (buf.length()!=0){
                    stk = new StringTokenizer(buf, " ");
                    for (int i=0; i<dimension; i++)
                    	sb[i].append(stk.nextToken()+" ");
                }
                buf = br.readLine();
            }
            br.close();

            // obtains the name for the output file
            String sFilename = f.getPath();
            if (sFilename.lastIndexOf('.')!=-1)
                sFilename=(sFilename.substring(0, sFilename.lastIndexOf('.'))+".mtl");
            else
                sFilename+=".mtl";
            fDat = new File(sFilename);

            // output data in Matlab compatible format
            BufferedWriter bwDat = new BufferedWriter(
                new FileWriter(fDat));
            for (int i=0; i<dimension; i++)
            	bwDat.write(sb[i].toString()+ApplicationSetup.EOL);    	
            /**
            bwDat.write(sParameter); bwDat.newLine();
            bwDat.write(sAP); bwDat.newLine();
            bwDat.write(sFQ);
            */
            bwDat.close();
            
        }
        catch(IOException ioe){
           	ioe.printStackTrace();
           	System.exit(1);
        }
        return fDat;
    }
 	/**
 	*	This method parses an evaluation result file and returns 
 	*	the average precision stores in the file.
 	* 	@param f File the evaluation result file to be parsed.
 	* 	@return double the average precision value stored in the input file.
 	*/ 
	public static double loadAveragePrecision(File f){
        double MAP = -1d;
        try{
            BufferedReader br = new BufferedReader(new FileReader(f));
            String buf = br.readLine();
            while(buf!=null){
                StringTokenizer stk = new StringTokenizer(buf, ":");
                String sTag = stk.nextToken().trim();
                if (sTag.equals("Average Precision")/*||
	                sTag.equals("Average Reciprocal Precision")*/){
                    sTag = stk.nextToken().trim();
                    MAP = Double.parseDouble(sTag);
                    break;
                }
                buf = br.readLine();
            }
            br.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
            System.exit(1);
        }
        return MAP;
    }
	
	public static double loadAllEvalMeasure(String filename, String measure){
		double result = -1;
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			while ((str=br.readLine())!=null){
				if (str.startsWith(measure)){
					StringTokenizer stk = new StringTokenizer(str);
					stk.nextToken();
					if (stk.nextToken().equals("all")){
						result = Double.parseDouble(stk.nextToken());
						break;
					}
				}
			}
			br.close();
		}catch(IOException ioe){
            ioe.printStackTrace();
            System.exit(1);
        }
		return result;
	}
	
	public static double loadPrecisionAt10(File f){
        double prec = -1d;
        try{
            BufferedReader br = Files.openFileReader(f);
            String buf = br.readLine();
            while(buf!=null){
                StringTokenizer stk = new StringTokenizer(buf, ":");
                String sTag = stk.nextToken().trim();
                if (sTag.equals("Precision at    1")/*||
	                sTag.equals("Average Reciprocal Precision")*/){
                    sTag = stk.nextToken().trim();
                    prec = Double.parseDouble(sTag);
                    break;
                }
                buf = br.readLine();
            }
            br.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
            System.exit(1);
        }
        return prec;
    }
    
    // Attention: the beta values of the returned models are not assigned.
    public static WeightingModel[] getWeightingModels(){
        ArrayList modelList = new ArrayList();
        try{
            BufferedReader methodsFile = new BufferedReader(new FileReader(ApplicationSetup.TREC_MODELS));
            String methodName;

            while ((methodName = methodsFile.readLine()) != null) {
                WeightingModel wmodel = null;
                try {
                    wmodel = (WeightingModel) Class.forName(methodName).newInstance();				
                } catch(InstantiationException ie) {
                    System.err.println("Exception while loading the weighting model class:\n" + ie);
                    System.err.println("Exiting...");
                    System.exit(1);
                } catch(IllegalAccessException iae) {
                    System.err.println("Exception while loading the weighting model class:\n" + iae);
                    System.err.println("Exiting...");
                    System.exit(1);
                } catch(ClassNotFoundException cnfe) {
                    System.err.println("Exception while loading the weighting model class:\n" + cnfe);
                    System.err.println("Exiting...");
                    System.exit(1);
                }
            
                modelList.add(wmodel);
            }
        }catch(IOException ioe){
            ioe.printStackTrace();
            System.exit(1);
        }
    
        return (WeightingModel[])modelList.toArray(new WeightingModel[modelList.size()]);
    }
    
    public static void crossValidateResults(Vector vecResults, File baseline, File fout){
    	try{
    		// count number of queries
    		int numberOfQueries = 0;
    		BufferedReader br = new BufferedReader(new FileReader(baseline));
    		String str = null;
    		while ((str=br.readLine())!=null){
    			if (str.trim().length()==0)
    				continue;
    			else
    				numberOfQueries++;
    		}
    		br.close();
    		System.out.println(numberOfQueries + " queries in total.");
    		double[][] AP = new double[numberOfQueries][vecResults.size()];
    		for (int i = 0; i < vecResults.size(); i++){
    			System.out.println("processing file " + 
    					((File)vecResults.get(i)).getPath());
    			br = new BufferedReader(new FileReader((File)vecResults.get(i)));
    			int counter = 0;
    			while ((str=br.readLine())!=null){
        			if (str.trim().length()==0)
        				continue;
        			else{
        				str = str.trim();
        				AP[counter++][i] = Double.parseDouble(
        						str.substring(str.indexOf(' ')+1, str.length()));
        			}
        		}
    			br.close();
    		}
    		double[] APBaseline = new double[numberOfQueries];
    		br = new BufferedReader(new FileReader(baseline));
    		System.out.println("processing file " + baseline.getPath());
    		int counter = 0;
			while ((str=br.readLine())!=null){
    			if (str.trim().length()==0)
    				continue;
    			else{
    				str = str.trim();
    				APBaseline[counter++] = Double.parseDouble(
    						str.substring(str.indexOf(' ')+1, str.length()));
    			}
    		}
    		br.close();
    		StringBuffer buffer = new StringBuffer();
    		String EOL = ApplicationSetup.EOL;
    		for (int i = 0; i < numberOfQueries; i++){
    			buffer.append(Rounding.toString(Statistics.mean(AP[i]), 4) + " "
    					+ Rounding.toString(APBaseline[i], 4) + EOL);
    		}
    		BufferedWriter bw = new BufferedWriter(new FileWriter(fout));
    		bw.write(buffer.toString());
    		bw.close();
    		// compute average MAP
    		double sum = 0;
    		for (int i = 0; i < AP.length; i++)
    			for (int j = 0; j < AP[i].length; j++)
    				sum += AP[i][j];
    		double meanMAP = sum/(AP.length*AP[0].length);
    		System.out.println("mean MAP: " + meanMAP);
    		// done.
    		System.out.println("output saved in file " + fout.getPath());
    	}
    	catch(IOException e){
    		e.printStackTrace();
    		System.exit(1);
    	}
    	
    }
    
	/**
	* Insert the method's description here.
 	* Creation date: (16/12/2003 12:12:23)
 	* @return int
 	*/
	public static int queryType() {
		int[] type = new int[3];// 0: narr; 1: desc; 2: title
		Arrays.fill(type, 0);
		
		/**
		QueryTagProcess queryTagProcess = new QueryTagProcess();
		String tag = "narr";
		String frTag = "fr-narr";
		if (queryTagProcess.tagWord(tag.toUpperCase())||
		queryTagProcess.tagWord(frTag.toUpperCase()))
			type[0] = 1;
			
		tag = "desc";
		frTag = "fr-desc";
		if (queryTagProcess.tagWord(tag.toUpperCase())||
			queryTagProcess.tagWord(frTag.toUpperCase()))
			type[1] = 1;
			
		tag = "title";
		frTag = "fr-title";
		if (queryTagProcess.tagWord(tag.toUpperCase())||
			queryTagProcess.tagWord(frTag.toUpperCase()))
			type[2] = 1;	
			*/
		
		TagSet tags = new TagSet(TagSet.TREC_QUERY_TAGS);
		String tag = "narr";
		String frTag = "fr-narr";
		if (tags.isTagToProcess(tag.toUpperCase())||
				tags.isTagToProcess(frTag.toUpperCase()))
			type[0] = 1;
			
		tag = "desc";
		frTag = "fr-desc";
		if (tags.isTagToProcess(tag.toUpperCase())||
				tags.isTagToProcess(frTag.toUpperCase()))
			type[1] = 1;
			
		tag = "title";
		frTag = "fr-title";
		if (tags.isTagToProcess(tag.toUpperCase())||
				tags.isTagToProcess(frTag.toUpperCase()))
			type[2] = 1;

		return type[0] * 4 + type[1] * 2 + type[2] * 1;
	}

	
	
 	
}
