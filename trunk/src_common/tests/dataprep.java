package tests;
import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.io.*;
import gnu.trove.TIntObjectHashMap;
import uk.ac.gla.terrier.structures.Lexicon;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Vector;

public class dataprep {
	protected Index index;
	protected WeightingModel wmodel;
	protected double c;
	
	protected PrintWriter pwarff;
	protected TIntObjectHashMap<int[]> termidEntryMap;
	protected LexiconInputStream lexin;
	protected Vector<Integer> termids;
	
	public void loadIndex(String indexpath, String indexprefix){
		index = Index.createIndex(indexpath, indexprefix);
	}
	public void writeArff(String outpath, TIntObjectHashMap<Integer> doc_labels,String relation){
		try{
			pwarff = new PrintWriter(new FileWriter(outpath));
			this.writeARFFheader(relation);
			String[]data = this.getDocTermWeights(doc_labels);
			String[]label = this.getLabels(doc_labels);
			this.writeARFFdata(data, label);
			pwarff.close();
			index.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	public TIntObjectHashMap<Integer> getDocLabel(String pathdoc_label){
		TIntObjectHashMap<Integer> docs = new TIntObjectHashMap<Integer>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(pathdoc_label));
			String line = null;
			while(null!=(line=br.readLine()))
			{	
			//	System.out.println(line);
				String[] str = line.split(",");
				int docid = Integer.valueOf(str[0].trim());
				int label = Integer.valueOf(str[1].trim());
				docs.put(docid, label);
				System.out.println(docid+" "+label);
			}
			System.out.println("doc-label loaded");
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return docs;
	}
	/**
	 * get the termlist to be used as features: stored as termid-attribute id pairs
	 * termid corresponds to the termis in the index
	 * attribute id corresponds to the attribute id used for classification
	 * @param termlist
	 */
	public void loadTermlist(String termlist){
		try{
			BufferedReader br = new BufferedReader(new FileReader(termlist));
			String line = null;
			int i = 0;
			Lexicon lex = new Lexicon(index.getPath(), index.getPrefix());
			termids = new Vector<Integer>();
			while(null!=(line=br.readLine()))
			{	
				String term = line.trim();
				System.out.println(term);
				if(lex.getLexiconEntry(term)!=null){
					int termid = lex.getLexiconEntry(term).termId;
					termids.add(termid);
					i++;
				}

			}
			br.close();
			System.out.println("Termlist loaded");
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	protected void loadLexicon(){
		this.termidEntryMap = new TIntObjectHashMap<int[]>();
		try{
			lexin = new LexiconInputStream(index.getPath(), index.getPrefix());
			while (lexin.readNextEntry()!=-1){
				int[] entry = {lexin.getNt(), lexin.getTF()};
				termidEntryMap.put(lexin.getTermId(), entry);//termid-Nt,TF
			}
			lexin.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	/**
	 * Write the arff header
	 * @param relation
	 * @param lexin
	 */
	public void writeARFFheader(String relation){
		pwarff.println("@relation "+relation);
		for(int i = 0; i<termids.size(); i++){
			pwarff.println("@ATTRIBUTE "+termids.get(i)+" NUMERIC");
		}
		pwarff.println("@ATTRIBUTE class {opinioned, un-opinioned}");
		System.out.println("Attributes done");
	}
	/**
	 * Write the arff data part
	 * @param data
	 * @param label
	 */
	public void writeARFFdata(String []data, String[] label){
		pwarff.println("@data");
		for (int i = 0; i<data.length; i++){
			pwarff.print(data[i]);
			pwarff.print(label[i]+'\n');
		}
		
	}
	/**
	 * add new examples
	 * @param data
	 * @param label
	 */
	public void appendARFFdata(String[] data, String[] label){
		System.out.println("start writing data");
		for (int i = 0; i<data.length; i++){
				pwarff.print(data[i]);
				pwarff.print(label[i]+'\n');
			}
		System.out.println("data done");
	}
	/**
	 * get doc-term vec from the index
	 * @param docids
	 * @return
	 */
	public String[] getDocTermWeights(TIntObjectHashMap docs){
		int[] docids = docs.keys();
		Arrays.sort(docids);
		
		DocumentIndex docIndex = index.getDocumentIndex();
		DirectIndex directIndex = index.getDirectIndex();
		String[] data = new String[docids.length];//number of instances
		
		for(int i = 0; i<docids.length; i++){
			String instance = "";
			//directIndex.getTerms(199);
			if (docIndex.getDocumentLength(docids[i])>0){
				System.out.println(docids[i]+" "+docIndex.getDocumentLength(docids[i]));
				int[][] terms = directIndex.getTerms(docids[i]);//get the terms in the doc
				int doclength = docIndex.getDocumentLength(docids[i]);
				//terms[0][i]=termid,term[1][i]=tf
				//wmodel.score(double tf, double docLength, double n_t, double F_t, double keyFrequency)
			//	int[] termlist = termid_attridMap.keys();
				for(int j = 0; j<terms.length; j++){
					if(termids.contains(terms[0][j])){
						int termid = terms[0][j];
						int tf = terms[1][j];
						int[] entry = termidEntryMap.get(termid);
						double score = wmodel.score(tf, (double)doclength, (double)entry[0], (double)entry[1], 1d);
						int attrid = termids.indexOf(terms[0][j]);
						instance = instance + attrid+" "+score+",";
					}
				}
				
			}
			else{
				instance = "0 0,";
			}
			data[i]=instance;
		}
		return data;
	}

	/**
	 * get class labels
	 * @param labels
	 * @return
	 */
	public String[] getLabels(TIntObjectHashMap<Integer> doc_labels){
		String[] label = new String[doc_labels.size()];
		int[] keys = doc_labels.keys();
		for(int i = 0; i<doc_labels.size(); i++){
			int value = doc_labels.get(keys[i]);
			if(value<0||value>4){
				System.out.println("label error");
			}
			else if(value==1||value==0){
				label[i]="un-opinioned";
			}
			else{
				label[i]="opinioned";
			}
		}
		return label;
	}
	
	protected void getWeightingModel(){
		String namespace = "uk.ac.gla.terrier.matching.models.";
		String modelName = namespace.concat(ApplicationSetup.getProperty("trec.model", "DFRWeightingModel(In,L,B)"));
		double c = Double.parseDouble(ApplicationSetup.getProperty("c", "0.35"));
		try{
			if (modelName.indexOf("(") > 0){
				String params = modelName.substring( 
					modelName.indexOf("(")+1, modelName.indexOf(")"));
				String[] parameters = params.split("\\s*,\\s*");
				
				wmodel = (WeightingModel) Class.forName(
								modelName.substring(0,modelName.indexOf("(")))
						.getConstructor(
								new Class[]{String[].class})
						.newInstance(
								new Object[]{parameters});
			}else{						
				wmodel = (WeightingModel) Class.forName(modelName).newInstance();
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(1);
		}
		wmodel.setParameter(c);
		CollectionStatistics collStat = index.getCollectionStatistics();
		wmodel.setAverageDocumentLength(collStat.getAverageDocumentLength());
		wmodel.setNumberOfDocuments(collStat.getNumberOfDocuments());
		wmodel.setNumberOfPointers(collStat.getNumberOfPointers());
		wmodel.setNumberOfTokens(collStat.getNumberOfTokens());
		wmodel.setNumberOfUniqueTerms(collStat.getNumberOfUniqueTerms());
	}
	public void constructDocLabelMap(String pathdatamap, String pathqrels, String out)throws Exception{
		PrintWriter pw = new PrintWriter(new FileWriter(out));
		BufferedReader br1 = new BufferedReader(new FileReader(pathdatamap));
		String line = null;
		HashMap<String, String> docmap = new HashMap<String, String>();
		while(null!=(line = br1.readLine())){
			String[] str = line.split(" ");
			docmap.put(str[0].trim(), str[1].trim());
		}
		BufferedReader br2 = new BufferedReader(new FileReader(pathqrels));
		line = null;
		
		while(null!=(line = br2.readLine())){
			String[] str = line.split(" ");
			String docnum = str[2];
			String doclabel = str[3];
			String docid = docmap.get(docnum);
			pw.println(docid + ","+doclabel);
		}
		pw.close();
	}
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		if (args[0].equals("-writearff")){
			String indexpath = args[1];
			String indexprefix = args[2];
			String pathdoc_label = args[1]+"/doc_label";
			String relation  = args[3];
			String termlist = args[1]+"/wordlist.filtered.stemmed";
			String outpath = args[1]+"/"+relation+".arff";
			
			dataprep dp = new dataprep();
		//	System.out.println(pathdoc_label);
			TIntObjectHashMap doclabel = dp.getDocLabel(pathdoc_label);
			dp.loadIndex(indexpath, indexprefix);
			dp.loadLexicon();
			dp.getWeightingModel();
			System.out.println(dp.index.getPath());
			dp.loadTermlist(termlist);
			dp.writeArff(outpath, doclabel, relation);
			
		}
		if(args[0].equals("-doc_label")){
			String pathdocmap = args[1];
			String pathqrels = args[2];
			String out = args[3];
			dataprep dp = new dataprep();
			dp.constructDocLabelMap(pathdocmap, pathqrels, out);
		}
	}

}
/*
 * parameters: -writearff indexpath indexprefix relation
 * put these files under the index directory: doc_label, wordlist.filtered.stemmed
 */
