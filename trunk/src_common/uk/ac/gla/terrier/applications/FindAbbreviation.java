/*
 * Created on 27 Nov 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.applications;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.SingleLineTRECQuery;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;

public class FindAbbreviation {
	
	protected THashMap<String, THashSet<String>> abbrMap;
	
	public void parseCollectionFile(String collectionFilename){
		try{
			BufferedReader br = Files.openFileReader(collectionFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.replaceAll("\\.", " ").replaceAll(",", " ")
				.replaceAll("\"", " ").replaceAll("\'", " ")
				.replaceAll("\\<", " ").replaceAll("\\>", " ").replaceAll("\\/", " ")
				.replaceAll(":", " ").trim().split(" ");
				//for (int i=1; i<tokens.length-1; i++)
					//System.out.print(tokens[i]+" ");
				//System.exit(0);
				for (int i=1; i<tokens.length-1; i++){
					// start of possible short form
					if (tokens[i].startsWith("(")){
						String strInPar = null;
						int abbStart = i;
						// if not a one word abbr.
						if (tokens[i].endsWith(")")){
							strInPar = tokens[i].substring(1, tokens[i].length()-1);
						}else{
							strInPar = tokens[i].substring(1, tokens[i].length());
							while ((++i)<tokens.length-1){
								if (tokens[i].endsWith(")")){
									strInPar+=" "+tokens[i].substring(0, tokens[i].length()-1);
									break;
								}else{
									strInPar+=" "+tokens[i].substring(0, tokens[i].length());
								}
							}
						}
						if (!isValidShortForm(strInPar))
							continue;
						int A = strInPar.replaceAll(" ", "").length();
						int longFormLength = Math.min(A+5, A*2);
						String longForm = "";
						for (int j=abbStart-longFormLength; j<abbStart; j++){
							if (j<1){
								continue;
							}
							longForm+=tokens[j]+" ";
							//System.out.println(longForm);
						}
						longForm = findBestLongForm(strInPar, longForm);
						if (longForm!=null){
							// if the long form contains (, ignore
							for (int j=0; j<longForm.length(); j++){
								if (longForm.charAt(j)=='('){
									longForm = null;
									break;
								}
							}
							if (longForm!=null)
								System.out.println(strInPar+", "+longForm);
						}
					}
				}
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private void loadCleanedAbbrList(String filename){
		abbrMap = new THashMap<String, THashSet<String>>();
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(",");
				THashSet<String> longForms = new THashSet<String>();
				for (int i=1; i<tokens.length; i++)
					longForms.add(tokens[i].trim());
				abbrMap.put(tokens[0].trim(), longForms);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Get the query parser that is being used.
	 * @return The query parser that is being used.
	 */
	static protected TRECQuery getQueryParser(String topicFile)
	{
		String TopicsParser = ApplicationSetup.getProperty("trec.topics.parser", "TRECQuery");
		TRECQuery rtr = null;
		try{
			Class queryingClass	= Class.forName(
				TopicsParser.indexOf('.') > 0 
				? TopicsParser
				: "uk.ac.gla.terrier.structures."+TopicsParser);
			if (topicFile != null)
			{
				Class[] types = {String.class};
				Object[] params = {topicFile};
				rtr = (TRECQuery)
					queryingClass
					.getConstructor(types)
					.newInstance(params)
					;
			}
			else
			{
				rtr = (TRECQuery) queryingClass.newInstance();
			}
		//} catch (ClassNotFoundException cnfe) {

		} catch (Exception e) {
			e.printStackTrace();
		}
		return rtr;
	}
	
	public void expandQueries(String abbrListFilename, String queryFilename){
		this.loadCleanedAbbrList(abbrListFilename);
		String outputFilename = queryFilename+".expanded";
		Lexicon lexicon = Index.createIndex().getLexicon();
		Manager manager = new Manager(null);
		TRECQuery queries = getQueryParser(queryFilename);
		if (queries.getNumberOfQueries()<=0)
			queries = new SingleLineTRECQuery(queryFilename);
		THashSet<String> expandedQueryidSet = new THashSet<String>();
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			String[] queryids = queries.getQueryids();
			// for each query
			for (int i=0; i<queryids.length; i++){
				String query = queries.getQuery(queryids[i]).toLowerCase();
				//System.out.println(queryids[i]+" "+query);
				String[] queryTerms = query.split(" ");
				// from the first query term
				StringBuilder sb = new StringBuilder();
				boolean expanded = false;
				for (int j=0; j<queryTerms.length; j++){
					// match the three term at most
					String shortForm = queryTerms[j];
					//String longForm = null;
					/*while ((longForm=expandString(shortForm))==null){
						if (j!=queryTerms.length-1)
							shortForm += " "+queryTerms[++j];
						else
							break;
					}*/
					if (shortForm.trim().length()==0) continue;
					sb.append(" "+shortForm+"^1.0");
					String processed = manager.pipelineTerm(shortForm);
					if (processed==null)
						continue;
					else if (!lexicon.findTerm(processed))
						continue;
					else if (lexicon.getNt()>1000)
						continue;
					else {
						String longForm = expandString(shortForm);						
						if (longForm!=null){
							sb.append(" "+longForm);
							expanded = true;
							expandedQueryidSet.add(queryids[i]);
							System.out.println(shortForm+": "+longForm);
						}
					}
				}
				System.out.println(queryids[i]+" "+sb.toString().trim());
				if (expanded)
					bw.write(queryids[i]+" "+sb.toString().trim()+ApplicationSetup.EOL);
				// write the expanded query
			}
			bw.close();
			bw = (BufferedWriter)Files.writeFileWriter(queryFilename+".nonexpanded");
			String[] ids = (String[])expandedQueryidSet.toArray(new String[expandedQueryidSet.size()]);
			Arrays.sort(ids);
			for (int i=0; i<ids.length; i++)
				bw.write(ids[i]+" "+queries.getQuery(ids[i])+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Expanded queries saved in "+outputFilename);
	}
	
	public String expandString(String query){
		THashSet<String> longFormSet = abbrMap.get(query);
		if (longFormSet == null)
			return null;
		// accept two variations at most
		//else if (longFormSet.size()>3)
			//return null;
		else{
			THashSet<String> expandedTermSet = new THashSet<String>();
			Object[] longForms = longFormSet.toArray();
			for (int i=0; i<longForms.length; i++){
				String[] tokens = ((String)longForms[i]).split(" ");
				for (int j=0; j<tokens.length; j++){
					// do not accept numbers
					boolean number = true;
					tokens[j] = tokens[j].trim();
					for (int k=0; k<tokens[j].length(); k++){
						if (!Character.isDigit(tokens[j].charAt(k))){
							number = false;
							break;
						}
					}
					if (!number)
						expandedTermSet.add(tokens[j]);
				}
			}
			StringBuilder sb = new StringBuilder();
			longForms = expandedTermSet.toArray();
			for (int i=0; i<longForms.length; i++)
				sb.append((String)longForms[i]+"^0.1 ");
			return sb.toString().trim();
		}
	}
	
	public void cleanAbbreviationList(String filename){
		// maps from each short form to all possible corresponding long forms 
		THashMap<String, THashSet<String>> map = new THashMap<String, THashSet<String>>();
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			while ((line=br.readLine())!=null){
				if (line.indexOf(',')<0)
					continue;
				// replace -+ with -
				// all to lower case
				line = line.replaceAll("-+", "-").replaceAll(" +", " ").toLowerCase();
				// strings can only have alphanumetic letters and hyphens

				String shortForm = cleanString(line.substring(0, line.indexOf(',')).trim());
				// length of the short form should be betweeb 3 and 10
				if (shortForm.replaceAll(" ", "").length()<3||shortForm.replaceAll(" ", "").length()>10)
					continue;
				String longForm = cleanString(line.substring(line.indexOf(',')+1, 
						line.length()).trim());				
				// long form can't start with short form
				if (longForm.startsWith(shortForm))
					continue;
				if (map.containsKey(shortForm)){
					map.get(shortForm).add(longForm);
				}else{
					THashSet<String> set = new THashSet<String>();
					set.add(longForm.replaceAll("-+", " "));
					map.put(shortForm, set);
				}
			}
			br.close();
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(filename+".clean");
			String[] shortForms = (String[])map.keySet().toArray(new String[map.size()]);
			Arrays.sort(shortForms);
			
			for (int i=0; i<shortForms.length; i++){
				Object[] longForms = map.get((String)shortForms[i]).toArray();
				// each short form should have no more than 10 corresponding long forms
				if (longForms.length>10)
					continue;
				StringBuffer buf = new StringBuffer();
				buf.append((String)shortForms[i]+", ");
				// accept no more than 5 long forms for each short form
				int limit = Math.min(5, longForms.length);
				for (int j=0; j<limit-1; j++)
					buf.append((String)longForms[j]+", ");
				buf.append((String)longForms[limit-1]+ApplicationSetup.EOL);
				bw.write(buf.toString());
				buf = null;
			}
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		
	}
	
	private String cleanString(String str){
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<str.length(); i++){
			char ch = str.charAt(i);
			// term can't start or end with hyphen
			if (ch == '-' && 
					(
							(i>0 && (Character.isSpaceChar(str.charAt(i-1)))||
							(i<str.length()-1 && Character.isSpaceChar(str.charAt(i+1))))||
							(i==str.length()-1)||(i==0)
					)
				)
				continue;
			else if (Character.isLetterOrDigit(ch) || ch=='-'
				|| Character.isSpaceChar(ch))
				sb.append(ch);
		}
		return sb.toString().replaceAll("-", " ").trim();
	}
	
	public boolean isValidShortForm(String str){
		// two words at most
		if (str.split(" ").length>2)
			return false;
		// length is between 2 and 10
		else if (str.length()<2||str.length()>10)
			return false;
		// starts with letter or digit
		else if (!Character.isLetterOrDigit(str.charAt(0)))
			return false;
		// at least one char is letter
		else{
			for (int i=0; i<str.length(); i++)
				if (Character.isLetter(str.charAt(i)))
					return true;
		}
		return false;
	}
	
	/** 
	*	Method findBestLongForm takes as input a short-form and a long-  
	*	form candidate (a list of words) and returns the best long-form 
	*	that matches the short-form, or null if no match is found. 
	**/   
	public String findBestLongForm(String shortForm, String longForm) { 
		int sIndex;     // The index on the short form 
		int lIndex;     // The index on the long form   
		char currChar;  // The current character to match 
	 
		sIndex = shortForm.length() - 1;  // Set sIndex at the end of the 
	                              // short form 
		lIndex = longForm.length() - 1;   // Set lIndex at the end of the 
		                                // long form 
		  for ( ; sIndex >= 0; sIndex--) {  // Scan the short form starting  
			  // from end to start 
			  // Store the next character to match. Ignore case 
			  currChar = Character.toLowerCase(shortForm.charAt(sIndex)); 
			  // ignore non alphanumeric characters 
		    if (!Character.isLetterOrDigit(currChar)) 
		      continue; 
		    // Decrease lIndex while current character in the long form 
		    // does not match the current character in the short form. 
		    // If the current character is the first character in the 
		    // short form, decrement lIndex until a matching character  
		    // is found at the beginning of a word in the long form. 
		    while ( 
		((lIndex >= 0) && 
		(Character.toLowerCase(longForm.charAt(lIndex)) != currChar)) 
		|| 
		      ((sIndex == 0) && (lIndex > 0) && 
		(Character.isLetterOrDigit(longForm.charAt(lIndex - 1))))) 
		          lIndex--; 
		    // If no match was found in the long form for the current 
		// character, return null (no match). 
		    if (lIndex < 0) 
		      return null; 
		    // A match was found for the current character. Move to the 
		    // next character in the long form. 
		    lIndex--; 
		  } 
		  // Find the beginning of the first word (in case the first  
		  // character matches the beginning of a hyphenated word).  
		  lIndex = longForm.lastIndexOf(" ", lIndex) + 1; 
		  // Return the best long form, the substring of the original 
		// long form, starting from lIndex up to the end of the original  
		// long form. 
		  return longForm.substring(lIndex); 
	} 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FindAbbreviation app = new FindAbbreviation();
		if (args[0].equals("--parsecollectionfile"))
			app.parseCollectionFile(args[1]);
		else if (args[0].equals("--cleanabbrlist"))
			app.cleanAbbreviationList(args[1]);
		else if (args[0].equals("--expandqueries"))
			// --expandqueries abbrListFilename queryFilename
			app.expandQueries(args[1], args[2]);
	}

}
