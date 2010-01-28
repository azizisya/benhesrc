package tests;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

import uk.ac.gla.terrier.matching.models.WeightingModel;
import uk.ac.gla.terrier.structures.CollectionStatistics;
import uk.ac.gla.terrier.structures.DirectIndex;
import uk.ac.gla.terrier.structures.DocumentIndex;
import uk.ac.gla.terrier.structures.Index;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconInputStream;
import uk.ac.gla.terrier.structures.Matrix;
import uk.ac.gla.terrier.structures.indexing.MatrixBuilder;
import uk.ac.gla.terrier.terms.PorterStemmer;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.TerrierTimer;

public class BuildMatrix {
	
	protected Index index;
	
	protected WeightingModel wmodel;
	
	protected double c;
	
	
	protected TIntObjectHashMap<int[]> termidEntryMap;
	
	public BuildMatrix(){
		System.err.println("Creating index...");
		index = Index.createIndex();
		System.err.println("IndexCreated.");
		this.getWeightingModel();
		System.err.println("Loading lexicon...");
		this.loadLexicon();
		System.err.println("Lexicon loaded.");
	}
	
	public BuildMatrix(boolean loadmatrix){
		index = Index.createIndex();
	}
	
	protected int[][] constructDocVector(int[][] terms, int docLength){
		int[][] vector = new int[3][docLength];// [termid, unscale, scale]
		int n = terms[0].length;
		//double maxScore = 0d;
		for (int i=0; i<n; i++){
			//lex.findTerm(terms[0][i]);
			int[] entry = termidEntryMap.get(terms[0][i]);
			double score = wmodel.score((double)terms[1][i], (double)docLength,(double) entry[0], (double)entry[1], 1d);
			//maxScore = Math.max(score, maxScore);
			BigDecimal bd = new BigDecimal(""+score, new MathContext(4)); // rounding?
			vector[0][i] = terms[0][i];
			vector[1][i] = bd.unscaledValue().intValue();
			vector[2][i] = bd.scale();
		}
		//System.out.println("maxscore: "+maxScore);
		return vector;
	}
	// load lexicon entries in memory to speed up process. This could cause outofmemory exception.
	protected void loadLexicon(){
		this.termidEntryMap = new TIntObjectHashMap<int[]>();
		try{
			LexiconInputStream lexin = new LexiconInputStream(index.getPath(), index.getPrefix());
			while (lexin.readNextEntry()!=-1){
				int[] entry = {lexin.getNt(), lexin.getTF()};
				termidEntryMap.put(lexin.getTermId(), entry);
			}
			lexin.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public Matrix getMatrix(){
		return new Matrix(
				index.getPath(), index.getPrefix()
				);
	}
	
	public void printMatrix(Matrix matrix){
		try{
			matrix.print();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	private void loadFilter(String filename, TIntHashSet whiteTermidSet){
		try{
			BufferedReader br = Files.openFileReader(filename);
			Lexicon lexicon = index.getLexicon();
			String str = null;
			PorterStemmer stemmer = new PorterStemmer(null);
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0) continue;
				str = stemmer.stem(str);
				if (lexicon.findTerm(str))
					whiteTermidSet.add(lexicon.getTermId());
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public void buildDocTermMatrixWithFilter(String filterFilename){
		DocumentIndex docIndex = index.getDocumentIndex();
		DirectIndex directIndex = index.getDirectIndex();
		MatrixBuilder matrixBuilder = new MatrixBuilder(index.getPath(), index.getPrefix());
		TIntHashSet whiteTermidSet = new TIntHashSet();
		this.loadFilter(filterFilename, whiteTermidSet);
		System.err.println("Data structure initialised.");
		try{
			int docCounter = 0;
			int pointerCounter = 0;
			int N = index.getCollectionStatistics().getNumberOfDocuments();
			TerrierTimer timer = new TerrierTimer(); timer.setTotalNumber(index.getCollectionStatistics().getNumberOfPointers());
			for (int i=0; i<N; i++){
				int docLength = docIndex.getDocumentLength(i);
				if (docLength > 0){
					//System.err.println("doclength: "+docIndex.getDocumentLength(i));
					int[][] terms = directIndex.getTerms(i);
					pointerCounter+=terms[0].length;
					TIntIntHashMap map = new TIntIntHashMap();
					for (int j=0; j<terms[0].length; j++){
						if (whiteTermidSet.contains(terms[0][j])){
							map.put(terms[0][j], terms[1][j]);
						}
					}
					terms[0] = map.keys(); terms[1] = map.getValues();
					//System.err.println("Constructing vector");
					int[][] vector = this.constructDocVector(terms, docLength);
					map.clear(); map = null;
				}else
					matrixBuilder.addVector(null);
				docCounter++;
				timer.setRemainingTime(pointerCounter);
				if (docCounter%2500==0) 
					System.out.println("Processed "+(i+1)+" documents. "
							+timer.getPercentage()+"% finished.");
			}
			//matrixBuilder.flushBuffer();
			matrixBuilder.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		docIndex.close();
	}
	
	
	
	public void buildDocTermMatrix(){
		DocumentIndex docIndex = index.getDocumentIndex();
		DirectIndex directIndex = index.getDirectIndex();
		MatrixBuilder matrixBuilder = new MatrixBuilder(index.getPath(), index.getPrefix());
		System.err.println("Data structure initialised.");
		try{
			int docCounter = 0;
			int pointerCounter = 0;
			int N = index.getCollectionStatistics().getNumberOfDocuments();
			TerrierTimer timer = new TerrierTimer(); timer.setTotalNumber(index.getCollectionStatistics().getNumberOfPointers());
			for (int i=0; i<N; i++){
				int docLength = docIndex.getDocumentLength(i);
				if (docLength > 0){
					//System.err.println("doclength: "+docIndex.getDocumentLength(i));
					int[][] terms = directIndex.getTerms(i);
					//System.err.println("Constructing vector");
					int[][] vector = this.constructDocVector(terms, docLength);
				}else
					matrixBuilder.addVector(null);
				docCounter++;
				timer.setRemainingTime(pointerCounter);
				if (docCounter%2500==0) 
					System.out.println("Processed "+(i+1)+" documents. "
							+timer.getPercentage()+"% finished.");
			}
			//matrixBuilder.flushBuffer();
			matrixBuilder.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		docIndex.close();
	}
	
	public void convertDocTermMatrix(String strMatrixFilename){
		
		MatrixBuilder matrixBuilder = new MatrixBuilder(index.getPath(), index.getPrefix()); 
		try{
			BufferedReader br = Files.openFileReader(strMatrixFilename);
			int docCounter = 0;
			int pointerCounter = 0;
			String str = null;
			while((str=br.readLine())!=null){
				str = str.substring(str.indexOf(':'), str.length());
				String[] entry = str.replaceAll("(", " ").replaceAll(")", " ").replaceAll(",", " ").split(" ");
				int n = entry.length/2;
				int[][] vector = new int[3][n];
				for (int i=0; i<n; i++){
					vector[0][i] = Integer.parseInt(entry[i*2]);
					BigDecimal bd = new BigDecimal(entry[i*2+1]);
					vector[1][i] = bd.unscaledValue().intValue();
					vector[2][i] = bd.scale();
				}
				matrixBuilder.addVector(vector);
				
				
				docCounter++;
				if (docCounter%2500==0) 
					System.out.println("Processed "+(docCounter+1)+" documents.");
			}
			matrixBuilder.close();
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	

	protected void getWeightingModel(){
		String namespace = "uk.ac.gla.terrier.matching.models.";
		String modelName = namespace.concat(ApplicationSetup.getProperty("trec.model", "TF_IDF"));
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
		}
		wmodel.setParameter(c);
		CollectionStatistics collStat = index.getCollectionStatistics();
		wmodel.setAverageDocumentLength(collStat.getAverageDocumentLength());
		wmodel.setNumberOfDocuments(collStat.getNumberOfDocuments());
		wmodel.setNumberOfPointers(collStat.getNumberOfPointers());
		wmodel.setNumberOfTokens(collStat.getNumberOfTokens());
		wmodel.setNumberOfUniqueTerms(collStat.getNumberOfUniqueTerms());
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[1].equals("--builddoctermmatrix")){
			BuildMatrix matrix = new BuildMatrix();
			matrix.buildDocTermMatrix();
		}else if(args[1].equals("--printmatrix")){
			BuildMatrix matrix = new BuildMatrix(true);
			matrix.printMatrix(matrix.getMatrix());
		}else if (args[1].equals("--buildfiltereddoctermmatrix")){
			BuildMatrix matrix = new BuildMatrix();
			matrix.buildDocTermMatrixWithFilter(args[2]);
		}
	}

}
