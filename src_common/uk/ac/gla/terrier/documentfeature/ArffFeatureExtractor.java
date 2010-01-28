package uk.ac.gla.terrier.documentfeature;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.evaluation.TRECQrelsInMemory;
import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.TRECQuery;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.QueryUtility;
import uk.ac.gla.terrier.utility.Rounding;
import uk.ac.gla.terrier.utility.StringUtility;
import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

public class ArffFeatureExtractor implements DocumentFeatureExtractor{
	protected int labelFeatureId;
	
	protected Index index;
	
	protected DirectIndex directIndex;
	
	protected DocumentIndex docIndex;
	
	protected CollectionStatistics collStats;
	
	protected Lexicon lexicon;
	
	protected DocumentFeature[] docFeatures;
	/**
	 * qid -> (docid -> label)
	 */
	protected THashMap<String, TIntIntHashMap> labelMap; 
	
	public ArffFeatureExtractor(Index index, String[] featureNames){
		this.index = (index==null)?(null):(index);
		if (index!=null){
			this.directIndex = index.getDirectIndex();
			this.lexicon = index.getLexicon();
			this.docIndex = index.getDocumentIndex();
			this.collStats = index.getCollectionStatistics();
		}
		docFeatures = new DocumentFeature[featureNames.length];
		Index[] parameters = {this.index};
		for (int i=0; i<featureNames.length; i++){
			try{
				docFeatures[i] = (DocumentFeature) Class.forName(
						"uk.ac.gla.terrier.documentfeature."+featureNames[i])
						.getConstructor(
								new Class[]{Index.class})
						.newInstance(
								new Object[]{this.index});
			}catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	/**
	 * Each entry should look like:
	 * {1 1078552,2 4.307794,3 26.130897,4 743.333333,5 0.913805,6 2.799995,8 1381, 9 -1}
	 * for negative instance or 
	 * {1 1078552,2 4.307794,3 26.130897,4 743.333333,5 0.913805,6 2.799995,8 1381}
	 * for positive instance.
	 * where 1 is the docid feature
	 */
	/**
	public void formatDocumentFeature(int docid, String qid, TIntObjectHashMap featureMap, StringBuffer buf, int label){
		if (featureMap.size()==0)
			return;
		int[] featureIds = featureMap.keys();
		Arrays.sort(featureIds);
		buf.append("{1 "+qid+"."+docid);
		for (int i=0; i<featureIds.length; i++){
			double value = (Double)featureMap.get(featureIds[i]);
			if (value>0d){
				buf.append(","+featureIds[i]+" "+Rounding.toString(value, 10));
			}
		}
		// if (label==-1)
			// buf.append(","+labelFeatureId+" -1");
		buf.append(","+labelFeatureId+" "+label);
		buf.append("}");
	}*/
	
	public void formatDocumentFeature(int docid, String qid, TIntObjectHashMap featureMap, StringBuffer buf, int label){
		if (featureMap.size()==0)
			return;
		int[] featureIds = featureMap.keys();
		Arrays.sort(featureIds);
		buf.append("{");
		for (int i=0; i<featureIds.length; i++){
			double value = ((Double)featureMap.get(featureIds[i])).doubleValue();
			if (value>0d){
				buf.append(featureIds[i]+" "+value+",");
			}
		}
		// if (label==-1)
			// buf.append(","+labelFeatureId+" -1");
		buf.append(labelFeatureId+" "+label);
		buf.append("}");
	}
	
	protected void setLabelFeatureId(TIntObjectHashMap<Double>[] featureMaps){
		TIntHashSet featureIdSet = new TIntHashSet();
		for (int i=0; i<featureMaps.length; i++)
			featureIdSet.addAll(featureMaps[i].keys());
		int[] featureIds = featureIdSet.toArray();
		Arrays.sort(featureIds);
		this.labelFeatureId = featureIds[featureIds.length-1]+1;
	}
	
	protected int[] getUniqLabels(int[] labels){
		return (new TIntHashSet(labels)).toArray();
	}
	
	public void writeDocumentFeature(int[] docids, int[] labels, String[] qids, TIntObjectHashMap<Double>[] featureMaps, String outputFilename){
		try{
			// Do not scale feature values for arff file
			// scaleFeatureValues(posFeatureMaps, negFeatureMaps);
			// write arff data file
			
			// get the maximum feature ID
			this.setLabelFeatureId(featureMaps);
			
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			// write relation
			bw.write("@relation "+outputFilename+ApplicationSetup.EOL);
			bw.write(ApplicationSetup.EOL);
			// write attribute descruptions
			// bw.write("@attribute att_1 string");
			bw.write(ApplicationSetup.EOL);
			for (int i=0; i<labelFeatureId; i++){
				bw.write("@attribute att_"+(i+1)+" numeric"+ApplicationSetup.EOL);
			}
			StringBuilder uniqLabelBuf = new StringBuilder();
			int[] uniqLabels = this.getUniqLabels(labels);
			Arrays.sort(uniqLabels);
			uniqLabelBuf.append(uniqLabels[uniqLabels.length-1]+"");
			for (int i=uniqLabels.length-2; i>=0; i--)
				uniqLabelBuf.append(","+uniqLabels[i]);
			bw.write("@attribute class {"+uniqLabelBuf.toString()+"}"+ApplicationSetup.EOL);
			bw.write(ApplicationSetup.EOL);
			// write data with ids
			bw.write("@data"+ApplicationSetup.EOL);
			bw.write(ApplicationSetup.EOL);
			TIntObjectHashMap<String> instIdMap = new TIntObjectHashMap<String>();
			for (int i=0; i<featureMaps.length; i++){
				StringBuffer buf = new StringBuffer();
				this.formatDocumentFeature(docids[i], qids[i], featureMaps[i], buf, labels[i]);
				bw.write(buf.toString()+ApplicationSetup.EOL);
				instIdMap.put(i+1, qids[i]+"."+docids[i]);
			}
			bw.close();
			// write the rank -> id map
			String mapOutputFilename = null;
			if (outputFilename.endsWith(".gz"))
				mapOutputFilename = outputFilename.substring(0, outputFilename.lastIndexOf('.'))+".map.gz";
			else
				mapOutputFilename = outputFilename+".map.gz";
			bw = (BufferedWriter)Files.writeFileWriter(mapOutputFilename);
			int[] keys = instIdMap.keys();
			Arrays.sort(keys);
			for (int i=0; i<keys.length; i++)
				bw.write(keys[i]+" "+instIdMap.get(keys[i])+ApplicationSetup.EOL);
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * 
	 * @param query The query string.
	 * @param index Index.
	 * @return Mapping from termid to qtf
	 */
	public static TIntIntHashMap processQuery(String query, Index index){
		TIntIntHashMap map = new TIntIntHashMap();
		System.out.println("query: "+query);
		String[] tokens = query.split(" ");
		Manager manager = new Manager(index);
		Lexicon lexicon = index.getLexicon();
		for (int i=0; i<tokens.length; i++){
			String term = manager.pipelineTerm(tokens[i].toLowerCase());
			// System.out.println((i+1)+": "+tokens[i]+" >>> "+term);
			if (term!=null){
				term = term.trim();
				if (term.length()==0)
					continue;
				LexiconEntry lexEntry = null;
				try{
					lexEntry = lexicon.getLexiconEntry(term);
				}catch(StringIndexOutOfBoundsException e){
					System.err.println("term: "+term);
					e.printStackTrace();
					System.exit(1);
				}
				if (lexEntry!=null){
					map.adjustOrPutValue(lexEntry.termId, 1, 1);
				}
			}
		}
		return map;
	}
	
	public void extractAndWriteDocumentFeature(String qrelsFilename, String queryid, String outputFilename){
		//System.err.println("PLEASE ENSURE THAT DOCNOS IN THE QRELS HAVE BEEN CONVERTED TO DOCIDS");
		TRECQrelsInMemory qrels = new TRECQrelsInMemory(qrelsFilename);
		String[] posDocnos = qrels.getRelevantDocumentsToArray(queryid);
		String[] negDocnos = qrels.getNonRelevantDocumentsToArray(queryid);
		int[] posDocids = StringUtility.stringsToInts(posDocnos);
		int[] negDocids = StringUtility.stringsToInts(negDocnos);
		// get queryTermids
		try{
			extractAndWriteDocumentFeature(posDocids, negDocids, queryid,
				processQuery(((TRECQuery)Class.forName("uk.ac.gla.terrier.structures."+ApplicationSetup.getProperty("trec.topics.parser", 
						"TRECQuery")).newInstance()).getQuery(queryid), index).keys(), 
				outputFilename);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Extract document features that are stored in a given object.
	 * @param docid
	 * @param docFeature Mapping from 
	 */
	public void extractDocumentFeature(int docid, String queryid, int[] queryTermids, TIntObjectHashMap featureMap){
		for (int i=0; i<docFeatures.length; i++){
			//TerrierTimer timer = new TerrierTimer();
			//timer.start();
			docFeatures[i].extractDocumentFeature(docid, queryid, queryTermids, featureMap);
			//timer.setBreakPoint();
			//System.out.println(docFeatures[i].getInfo()+": "+timer.toStringMinutesSeconds());
		}
	}
	
	public void extractDocumentFeature(int[] docids, int[] labels, String queryid, int[] queryTermids, 
			TIntObjectHashMap<Double>[] featureMaps){
		int counter = 0;
		int totalDocs = docids.length;
		for (int i=0; i<docids.length; i++){
			if (docids[i]==-1)
				continue;
			System.out.print("processing document "+docids[i]+" with label "+labels[i]+"...");
			this.extractDocumentFeature(docids[i], queryid, queryTermids, featureMaps[i]);
			System.out.println("Done. Feature vector length: "+featureMaps[i].size()+". "
					+(++counter)+" out of "+totalDocs+" documents processed.");
		}
	}
	
	public void extractAndWriteDocumentFeature(int[] docids, int[] labels, String queryid, int[] queryTermids, String outputFilename){
		TIntObjectHashMap<Double>[] featureMaps = new TIntObjectHashMap[docids.length]; 
		for (int i=0; i<docids.length; i++)
			featureMaps[i] = new TIntObjectHashMap<Double>();
		String[] qids = new String[docids.length];
		Arrays.fill(qids, queryid);
		extractDocumentFeature(docids, labels, queryid, queryTermids, featureMaps);
		writeDocumentFeature(docids, labels, qids, featureMaps, outputFilename);
	}
	
	protected void loadLabels(String filename){
		labelMap = new THashMap<String, TIntIntHashMap>();
		try{
			BufferedReader br = Files.openFileReader(filename);
			String line = null;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				String qid = tokens[0];
				int docid = Integer.parseInt(tokens[2]);
				int label = Integer.parseInt(tokens[3]);
				if (labelMap.get(qid)==null){
					labelMap.put(qid, new TIntIntHashMap());
				}
				labelMap.get(qid).put(docid, label);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void extractAndWriteDocumentFeature(String qrelsFilename, String outputFilename, boolean cached){
		System.err.println("PLEASE ENSURE THAT DOCNOS IN THE QRELS HAVE BEEN CONVERTED TO DOCIDS");
		this.loadLabels(qrelsFilename);
		TRECQuery queries = QueryUtility.getQueryParser();
		String[] queryids = labelMap.keySet().toArray(new String[labelMap.size()]);
		TIntObjectHashMap<String> docidQidMap = new TIntObjectHashMap<String>();// assume same doc is not used for feedback for different queries
		TIntObjectHashMap<TIntObjectHashMap<Double>> feature = new TIntObjectHashMap<TIntObjectHashMap<Double>>();  // map from docid to features
		for (int i=0; i<queryids.length; i++){
			TIntIntHashMap map = labelMap.get(queryids[i]);
			if (map==null)
				continue;
			int[] docids = map.keys();
			int[] labels = new int[map.size()];
			
			for (int j=0; j<docids.length; j++)
				labels[j]=map.get(docids[j]);
			
			TIntObjectHashMap<Double>[] featureMaps = new TIntObjectHashMap[docids.length]; 
			for (int j=0; j<docids.length; j++)
				featureMaps[j] = new TIntObjectHashMap<Double>();
			// get queryTermids
			try{
				if (cached)
					this.extractDocumentFeature(docids, labels, queryids[i], null, featureMaps);
				else
					this.extractDocumentFeature(docids, labels, queryids[i], processQuery(queries.getQuery(queryids[i]), index).keys(), featureMaps);
				//extractDocumentFeature(posDocids, negDocids, processQuery(queries.getQuery(queryids[i]), index).keys(), outputFilename);
			}catch(Exception e){
				e.printStackTrace();
				System.exit(1);
			}
			for (int j=0; j<docids.length; j++){
				feature.put(docids[j], featureMaps[j]);
				docidQidMap.put(docids[j], queryids[i]);
			}
		}
		int[] docids = feature.keys(); int[] labels = new int[docids.length]; String[] qids = new String[docids.length];
		TIntObjectHashMap<Double>[] featureMaps = new TIntObjectHashMap[docids.length];
		for (int i=0; i<docids.length; i++){
			featureMaps[i] = feature.get(docids[i]);
			qids[i] = docidQidMap.get(docids[i]);
			labels[i] = labelMap.get(qids[i]).get(docids[i]);
		}
		writeDocumentFeature(docids, labels, qids, featureMaps, outputFilename);
	}
	
	public static void main(String[] args){
		String[] featureNames = ApplicationSetup.getProperty("doc.feature.names", "").split(",");
		if (args[0].equals("--alltopics")){
			// --alltopics indexpath indexprefix qrelsname outputname
			String indexpath = args[1];
			String indexprefix = args[2];
			String qrelsname = args[3];
			String outputFilename = args[4];
			ArffFeatureExtractor app = new ArffFeatureExtractor(
					Index.createIndex(indexpath, indexprefix), featureNames);
			app.extractAndWriteDocumentFeature(qrelsname, outputFilename, false);
		}else if (args[0].equals("--cached")){
			// --alltopics qrelsname outputname
			String qrelsname = args[1];
			String outputFilename = args[2];
			ArffFeatureExtractor app = new ArffFeatureExtractor(
					null, featureNames);
			app.extractAndWriteDocumentFeature(qrelsname, outputFilename, true);
		}
	}
}
