package uk.ac.gla.terrier.applications;

import gnu.trove.THashSet;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.matching.dsms.ProximityScoreModifierTREC2008;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.statistics.Statistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.ExpansionTerm;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.SingleLineTRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Rounding;

public class ExpandedBasedOnProx {
	/**
	 * 
	 * @param oneLineTopicFilename The topics contain no weights
	 * @param qrelsFilename The qrels should contain docids instead of docnos
	 * @param indexPath
	 * @param indexPrefix
	 * @param outputFilename The output file containing expanded queries.
	 */
	public static void expandFromPosDocuments(String oneLineTopicFilename, String qrelsFilename, String indexPath, String indexPrefix, String outputFilename){
		Index index = Index.createIndex(indexPath, indexPrefix);
		DirectIndex directIndex = index.getDirectIndex();
		Lexicon lexicon = index.getLexicon();
		Manager manager = new Manager(index);
		SingleLineTRECQuery queries = new SingleLineTRECQuery(oneLineTopicFilename);
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		ProximityScoreModifierTREC2008 modifier = new ProximityScoreModifierTREC2008();
		modifier.setIndex(index);
		int minET = 10; int maxET = 80; int intv = 5;
		StringBuffer[] buf = new StringBuffer[(maxET-minET)/intv+1];
		int[] ET = new int[(maxET-minET)/intv+1];
		for (int i=0; i<ET.length; i++){
			ET[i] = minET + intv*i;
			buf[i] = new StringBuffer();
		}
		int ngramLength = Integer.parseInt(ApplicationSetup.getProperty("proximity.ngram.length", "2"));
		double ngramC = Double.parseDouble(ApplicationSetup.getProperty("proximity.ngram.c", "1d"));
		try{
			// for each single line query
			while (queries.hasMoreQueries()){
				// parse the original query
				String queryString = queries.nextQuery();
				String queryid = queries.getQueryId();
				String[] tokens = queryString.split(" ");
				// set of original query terms
				THashSet<String> termSet = new THashSet<String>();
				// mapping from term String to query term frequency
				TObjectIntHashMap<Object> termFreqMap = new TObjectIntHashMap<Object>();
				// calculate query term frequencies
				for (int i=0; i<tokens.length; i++){
					String term = manager.pipelineTerm(tokens[i].trim().toLowerCase());
					if (term!=null){
						term = term.trim();
						if (term.length() > 0){
							termSet.add(term);
							termFreqMap.adjustOrPutValue(term, 1, 1);
						}
					}
				}
				// get the maximum freq
				int maxFreq = 0;
				String[] termStrings = (String[])termSet.toArray(new String[termSet.size()]);
				for (int i=0; i<termStrings.length; i++){
					int freq = termFreqMap.get(termStrings[i]);
					maxFreq = Math.max(freq, maxFreq);
				}
				// get the relevant docids
				String[] docnos = qrels.getRelevantDocumentsToArray(queryid);
				int[] docids = new int[docnos.length];
				for (int i=0; i<docnos.length; i++)
					docids[i] = Integer.parseInt(docnos[i]);
				// get termids in the relevant docs
				TIntHashSet termidSet = new TIntHashSet();
				for (int i=0; i<docids.length; i++){
					int[][] terms = directIndex.getTerms(docids[i]);
					for (int j=0; j<terms[0].length; j++){
						termidSet.add(terms[0][j]);
					}
				}
				// remove invalid terms
				for (int termid : termidSet.toArray())
					// A candidate term should appear in no more than 10% of documents in the collection.
					if (lexicon.getLexiconEntry(termid).n_t > index.getCollectionStatistics().getNumberOfDocuments()/10)
						termidSet.remove(termid);
				// Get the most weighted terms from the relevant docs
				ExpansionTerm[] expTerms = new ExpansionTerm[termidSet.size()];
				int counter = 0;
				System.out.println(termidSet.size()+" terms to process for query "+queryid);
				for (int termid : termidSet.toArray()){
					LexiconEntry lexEntry = lexicon.getLexiconEntry(termid);
					String term = lexEntry.term;
					double[] scores_u = new double[docids.length];
					double[] scores = new double[docids.length];
					Arrays.fill(scores_u, 0d); Arrays.fill(scores, 0d);
					for (int i=0; i<termStrings.length; i++){
						// in an unlikely case, if an original query term appears in more than 10% of documents in the collection,
						// this term is ignored from proximity weighting.
						try{
							if (lexicon.getLexiconEntry(termStrings[i]).n_t > index.getCollectionStatistics().getNumberOfDocuments()/10)
								continue;
						}catch(NullPointerException ne){
							System.err.println("term: "+term+", termStrings[i]: "+termStrings[i]);
							ne.printStackTrace();
							System.exit(1);
						}
						try{
							// if first time process the terms, cache postings
							if (counter == 0 || i == 0)
								modifier.computeFDScore(term, termStrings[i], scores, scores_u, docids, 1d, 1d, ngramC, ngramLength, index, true);
							else
								modifier.computeFDScore(term, termStrings[i], scores, scores_u, docids, 1d, 1d, ngramC, ngramLength, index, false);
						}catch(NullPointerException ne){
							System.err.println("term: "+term+", termStrings[i]: "+termStrings[i]);
							ne.printStackTrace();
							System.exit(1);
						}
					}
					double weight = Statistics.sum(scores_u);
					expTerms[counter] = new ExpansionTerm(termid);
					expTerms[counter].setWeightExpansion(weight);
					counter++;
					modifier.clearCache(termid);
					System.out.println("term "+(counter)+": "+lexEntry.term+", with weight "+weight+", "+termidSet.size()+" terms in total to process for query "+queryid);
				}
				Arrays.sort(expTerms);
				// the normaliser of the expanded terms
				double normaliser = expTerms[0].getWeightExpansion();
				System.out.println("Term with maximum weight: "+lexicon.getLexiconEntry(expTerms[0].getTermID()).term+", weight: "+expTerms[0].getWeightExpansion());
				int numberOfExpandedTerms = Math.min(ApplicationSetup.EXPANSION_TERMS, expTerms.length);
				// get mapping from original query terms to their weights
				TObjectDoubleHashMap<String> termWeightMap = new TObjectDoubleHashMap<String>();
				for (int i=0; i<termStrings.length; i++){
					// determine the query term weight
					double qtw = (double)termFreqMap.get(termStrings[i])/maxFreq;
					// append query
					termWeightMap.put(termStrings[i], qtw);
				}
				int ETCounter = 0;
				for (int i=0; i<ET.length; i++){
					while (ETCounter <= ET[i]){
						// add to the hashmap
						if (ETCounter >= expTerms.length || expTerms[ETCounter].getWeightExpansion() == 0d){
							ETCounter++;
							continue;
						}
						termWeightMap.adjustOrPutValue(lexicon.getLexiconEntry(expTerms[ETCounter].getTermID()).term,
								expTerms[ETCounter].getWeightExpansion()/normaliser, expTerms[ETCounter].getWeightExpansion()/normaliser);
						ETCounter++;
					}
					// write query to string builder
					StringBuilder currentBuf = new StringBuilder();
					currentBuf.append(queryid);
					for (String term : (String[])termWeightMap.keys(new String[termWeightMap.size()])){
						currentBuf.append(" "+term+"^"+Rounding.toString(termWeightMap.get(term), 4));
						//buf[i].append(" "+term+"^"+Rounding.toString(termWeightMap.get(term), 4));
					}
					System.out.println("Expand "+ET[i]+" terms: "+currentBuf.toString());
					buf[i].append(currentBuf.toString()+ApplicationSetup.EOL);
				}
				modifier.clearCache();
			}
			for (int i=0; i<ET.length; i++){
				BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename+"_t"+ET[i]);
				bw.write(buf[i].toString());
				bw.close();
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--positiveexpansion")){
			// --positiveexpansion oneLineTopicFilename, qrelsFilename, indexPath, indexPrefix, outputFilename
			ExpandedBasedOnProx.expandFromPosDocuments(args[1], args[2], args[3], args[4], args[5]);
		}
		
	}

}
