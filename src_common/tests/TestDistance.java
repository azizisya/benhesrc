package tests;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.Server;
import uk.ac.gla.terrier.utility.SimilarityServer;
import uk.ac.gla.terrier.utility.TerrierTimer;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TIntArrayList;

import java.io.*;
import java.math.BigDecimal;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
/**
 *
 * @author jhe
 *
 */
public class TestDistance {
	
	public int finished = 0;
	
	final protected int threadNo = 10;
	
	public String hostName; 
	
	protected Server[] simServers;

    public class SimilarityThread extends Thread {
    	public double[] sim;
    	DistMat[] mat1;
    	DistMat[][] mat2;
    	TestDistance dist;
    	Server server;
    	
    	
    	public SimilarityThread(DistMat[] mat, DistMat[][] targetMat,
    			Server server,
				TestDistance dist) {
			super();
			this.mat1 = mat;
			this.mat2 = targetMat;
			this.server = server;
			this.dist = dist;
			this.start();
		}
		
        public void run() {
        	try{
        		sim = server.getSim(mat1, mat2);
        	}catch(RemoteException e){
        		e.printStackTrace();
        	}
        	dist.finished++;
        }

    }



	public static DistMat[][] dmat;
//	public static no.uib.cipr.matrix.sparse.SparseVector [] mat;
	public double[] sim;
	
	public void WriteTFIDF(DocumentIndex doci, DirectIndex di,
			Lexicon lex,InvertedIndex ii,String filepath){
	
		int numdoc = doci.getNumberOfDocuments();
	//	System.out.println(numdoc);
		
		try{
			PrintWriter pw = new PrintWriter(new FileWriter(filepath+"/tdmat.txt"));	
			for (int i = 0; i<numdoc; i++){
			
				//String str = String.valueOf(i); //the doc number
				String str= i+":";
				int doclen = doci.getDocumentLength(i);
				//System.out.println("doclenth:"+doclen);
				if(doclen==0){
					str = i+":(0,0)";
				}
				else{
					int[][] termids = di.getTerms(i);
					//System.out.println(termids.length);
					//System.out.println(termids[0].length);
				
					for (int j = 0; j<termids[0].length; j++){
						double tf = (double)termids[1][j]/doclen;
						int[][] docs = ii.getDocuments(termids[0][j]);
						double idf = (double)numdoc/docs[0].length;
				
						double tfidf = tf*Math.log10(idf);
						BigDecimal b = new BigDecimal(tfidf);
						double rtfidf = b.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
						if(rtfidf!=0){
							str = str+"("+termids[0][j]+","+rtfidf+")";
						}
					}
				}
				pw.println(str);
				System.out.println("doc "+str);
			}
			pw.close();
			}
			catch(IOException e){
				e.printStackTrace();
			}
	
	}
	public void writeDistance(String pathmatrix, String pathdist, int startdoc, int enddoc){
		try{	
			PrintWriter pw = new PrintWriter(new FileWriter(pathdist+"/dist"+startdoc+"-"+enddoc+".txt"));
			String matfile = pathmatrix+"/tdmat.txt";
			//System.out.println(matfile);
			//get the number of lines in the file
			String[] cmd1 = {"/bin/bash","-c","wc -l "+matfile};
			Process p = Runtime.getRuntime().exec(cmd1);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str = input.readLine();
			if(str!=null){
				//System.out.println(str);
				str = str.replace(matfile, "").trim();
			}//if
			input.close();
			p.destroy();
			int numlines = Integer.valueOf(str);
			//int size = cs.getNumberOfUniqueTerms();
			for(int i = startdoc; i<enddoc-1; i++){
				no.uib.cipr.matrix.sparse.SparseVector vec1 = readVector(matfile, i+1);
				for(int j = i+1; j<numlines; j++){
					no.uib.cipr.matrix.sparse.SparseVector vec2 = readVector(matfile, j+1);
					//get cosine similarity between the two vecs
					double sim = cosineSimilarity(vec1,vec2);	
				//	double dist = vec1.add(-1,vec2).norm(no.uib.cipr.matrix.Vector.Norm.TwoRobust);
					BigDecimal b = new BigDecimal(sim);
					double rsim = b.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
					pw.println(i+","+j+";"+rsim);
					System.out.println(i+","+j+";"+sim);
				}
			}
			
			pw.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	/**
	 * This method write the distance between the doc pairs into file
	 * @param pathmatrix
	 * @param pathdist
	 */
	public void writeDistance(String pathmatrix, String pathdist,boolean norm){
		try{
			
			PrintWriter pw = new PrintWriter(new FileWriter(pathdist+"/dist.txt"));
			String matfile = pathmatrix+"/tdmat.txt";
			//System.out.println(matfile);
			//get the number of lines in the file
			String[] cmd1 = {"/bin/bash","-c","wc -l "+matfile};
			Process p = Runtime.getRuntime().exec(cmd1);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str = input.readLine();
			if(str!=null){
				//System.out.println(str);
				str = str.replace(matfile, "").trim();
			}//if
			input.close();
			p.destroy();
			int numlines = Integer.valueOf(str);
			//int size = cs.getNumberOfUniqueTerms();
			for(int i = 0; i<numlines-1; i++){
				no.uib.cipr.matrix.sparse.SparseVector vec1 = readVector(matfile, i+1);
				if(norm){vec1 = normalize(vec1);}
				for(int j = i+1; j<numlines; j++){
					no.uib.cipr.matrix.sparse.SparseVector vec2 = readVector(matfile, j+1);
					if(norm){vec2 = normalize(vec2);}
					//get cosine similarity between the two vecs
					double sim = cosineSimilarity(vec1,vec2);
	
				//	double dist = vec1.add(-1,vec2).norm(no.uib.cipr.matrix.Vector.Norm.TwoRobust);
					pw.println(i+","+j+";"+sim);
					System.out.println(i+","+j+";"+sim);
				}
			}
			
			pw.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	
	}
	public double cosineSimilarity(no.uib.cipr.matrix.sparse.SparseVector vec1,
			no.uib.cipr.matrix.sparse.SparseVector vec2){
		//int[] ix1 = vec1.getIndex();
		//double[] value1 = vec1.getData();
		//int[] ix2 = vec2.getIndex();
		//double[] value2 = vec2.getData();

		//int len = 0;
		//if(ix1[ix1.length-1]>ix2[ix2.length-1]){
		//	len = ix1[ix1.length-1]+1;
		//}
		//else{
		//	len = ix2[ix2.length-1]+1;
		//}
		//construct the dense vector
	//	no.uib.cipr.matrix.Vector dvec1 = new no.uib.cipr.matrix.sparse.SparseVector(len,ix1,value1);
	//	no.uib.cipr.matrix.Vector dvec2 = new no.uib.cipr.matrix.sparse.SparseVector(len,ix2,value2);
		
		double prod = vec1.dot(vec2);	
		double nvec1 = vec1.norm(no.uib.cipr.matrix.Vector.Norm.TwoRobust);
		double nvec2 = vec2.norm(no.uib.cipr.matrix.Vector.Norm.TwoRobust);
	//	System.out.println(nvec1);
	//	System.out.println(nvec2);
		return prod/(nvec1*nvec2);
	}
	/**
	 * This method read one line from the matrix file and returns the termid-tfidf pair vector
	 * @param file
	 * @param numline
	 * @return
	 */
	public no.uib.cipr.matrix.sparse.SparseVector readVector(String file, int numline){
	//	no.uib.cipr.matrix.sparse.SparseVector vec = new no.uib.cipr.matrix.sparse.SparseVector(size);
		no.uib.cipr.matrix.sparse.SparseVector vec=null;
		String comm = "head -"+numline+" "+file+"|tail -1";
		String[] cmd = {"/bin/bash","-c",comm};		
	//	int numele = 0;
		try{
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str = input.readLine();
			if(str!=null){
				String[] strline = str.split(":");
				String[] vecstr = strline[1].replace("(", "").replace(")", ";").split(";");
				int[] index = new int[vecstr.length];
				double[] value = new double[vecstr.length];
				for(int i = 0; i<vecstr.length; i++){
					String[] data = vecstr[i].split(",");

					index[i] = Integer.valueOf(data[0]);
					value[i] = Double.valueOf(data[1]);
				}
				vec = new no.uib.cipr.matrix.sparse.SparseVector(vecstr.length,index, value);
				//System.out.println(str);
			}
			input.close();
			p.destroy();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return vec;
	}
	/**
	 * this method normalize a vector to unit length(2norm)
	 * @param vec
	 * @return
	 */
	public no.uib.cipr.matrix.sparse.SparseVector normalize(no.uib.cipr.matrix.sparse.SparseVector vec){
		no.uib.cipr.matrix.sparse.SparseVector vec1 = vec.scale(1/vec.norm(no.uib.cipr.matrix.Vector.Norm.TwoRobust));
		return vec1;
	}
	/**
	 * only used when the matrix isn't too large...
	 * @param pathmatrix
	 * @deprecated
	 */
	public void loadMatix(String pathmatrix){
		String matfile = pathmatrix+"/tdmat.txt";
		//System.out.println(matfile);
		try{
			//get the number of lines in the file
			String[] cmd1 = {"/bin/bash","-c","wc -l "+matfile};
			Process p = Runtime.getRuntime().exec(cmd1);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str = input.readLine();
			if(str!=null){
				//System.out.println(str);
				str = str.replace(matfile, "").trim();
			}//if
			int numlines = Integer.valueOf(str);
			//System.out.println(numlines);
			
			FileReader fs = new FileReader(matfile);
			BufferedReader br = new BufferedReader(fs);
			dmat=new DistMat[numlines][];
			for(int i = 0; i<numlines; i++){
				String str2 = br.readLine();
				if(str!=null){
					//System.out.println(str2);
					String[] strline = str2.split(":");					
					String[] strdata = strline[1].replace("(", "").replace(")",";").split(";");
					dmat[i]=new DistMat[strdata.length];
					for(int j = 0;j<strdata.length; j++){
						if(strdata[j]!=null){
							String[] d = strdata[j].split(",");
							dmat[i][j] = new DistMat();
						//	dmat[i][j].docid = Integer.valueOf(strline[0]);
							dmat[i][j].termid = Integer.valueOf(d[0]);
							dmat[i][j].tfidf = Double.valueOf(d[1]);
						}//if
					}//j
				}//if
				if(i%1000==0){
					System.out.println(i+"docs");
				}
			}//i
		}//try
		catch(IOException e){
			e.printStackTrace();
		}
	}
	/**
	 * \z
	 * @param pathmatrix
	 * @param pathdocs
	 */
	public void loadMatrix(String pathmatrix, String pathdocs){
		//read the file containing the docs retreived
		try{
			BufferedReader br = new BufferedReader(new FileReader(pathdocs));
			BufferedReader brmat = new BufferedReader(new FileReader(pathmatrix));
			String line = null;
			String vec = null;
			
			//get the number of docs
			String[] cmd = {"/bin/bash","-c","head -2 "+pathdocs+"|tail -1"};
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String str = input.readLine();
			p.destroy();
			
			int numlines = 0;
			System.out.println(str);
			if(str!=null&& str.startsWith("<docs:")){
				String strnum = str.replace("<docs:", "").replace(">","").trim();
				System.out.println(strnum);
				numlines = Integer.valueOf(strnum);
			}//if
			else{
				System.out.println("file "+pathdocs+" corrupted, info of number of docs lost.");
			}
			dmat = new DistMat[numlines][];
			int count = 0;
			int ix = 0;
			int c = 0;
			while((null!=(line=br.readLine()))){
				if(!line.startsWith("<")){
				//	System.out.println(line);
					int docn = Integer.valueOf(line.trim());
					while((count<=docn)&&(null!=(vec=brmat.readLine()))){
						count++;
					}
					if(count==docn+1){
						//vec = brmat.readLine();
						//System.out.println(vec);
						String[] strline = vec.split(":");	//System.out.println(strline[0]);
						if(Integer.valueOf(strline[0])==docn){
							
							String[] vecstr = strline[1].replace("(", "").replace(")", ";").split(";");
						//	int[] index = new int[vecstr.length];
						//	double[] value = new double[vecstr.length];
							dmat[c]=new DistMat[vecstr.length];
							for(int i = 0; i<vecstr.length; i++){
								String[] data = vecstr[i].split(",");
								dmat[c][i]=new DistMat();
								dmat[c][i].termid = Integer.parseInt(data[0]);
								dmat[c][i].tfidf = Double.parseDouble(data[1]);
							}
							
							c++;
							if(c%10000==0){
								System.out.println("#"+c+"#");
							}
						}
						
						else{
							System.out.println("docno doesn't match:"+docn+":"+count);
						}//else
					}//if count==docn
				}//if ! <query
			}//while
			br.close();
			brmat.close();
		}//try
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Bind the servers
	 *
	 */
	protected void bindServers() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		String name = null;
		final int rmi_port =
		Integer.parseInt(ApplicationSetup.getProperty("terrier.rmi.port", "1099"));
		simServers = new Server[threadNo];
		try {
			for (int i=0; i<threadNo; i++) {
				name = "//" + hostName+":"+rmi_port + "/DistMatch-"+i;
				simServers[i] = bindOneServer(name);
				System.err.println("Bound server " + name);
			}
		} catch(Exception e) {
			e.printStackTrace();			
		}
	}
	
	protected Server bindOneServer(String name)
    {
		System.err.println("name: "+name);
        Server rtr = null;
        while(rtr == null)
        {
        	try{
                rtr = (Server) Naming.lookup(name);
                if (rtr == null)//null isn't an allowed answer
                        throw new Exception("Naming.lookup returned null");
            }
            catch (Exception e) {
               e.printStackTrace();
               System.exit(1);
            }
        }
        return rtr;
    }
	
	public void startServers(){
		for (int i=0; i<threadNo; i++){
			String[] args = {""+i};
			SimilarityServer.main(args);
		}
		try{
			Thread.sleep(1000);
		}catch(Exception e){}
	}
	
	public void similairty(String pathout){
		//this.startServers();
		this.bindServers();
		try{
			BufferedWriter pw = (BufferedWriter)Files.writeFileWriter(pathout);
			
			if(dmat==null){
				System.out.println("Matrix not loaded");
			}
			else{
				//	int n = dmat.length*(dmat.length-1)/2;
				//sim = new double[5000];
				//double[] sim = new double[threadNo];
				//int id = 0;
				int count = 0;
				this.finished = 0;
				//int[] ids = new int[threadNo];
				SimilarityThread[] threads = new SimilarityThread[threadNo];
				TerrierTimer timer = new TerrierTimer();
				timer.start();
				String EOL = ApplicationSetup.EOL;
				TDoubleArrayList sims = new TDoubleArrayList();
				for(int i = 0; i<dmat.length-1; i++){
					int numberPerThread = (dmat.length-i-1)/threadNo;
					int current = i+1;
					if (dmat.length-i>threadNo*2){
						for (int j=0; j<threadNo; j++){
							threads[j] = (j!=threadNo-1)?(new SimilarityThread(dmat[i],
								this.getDistMat(current, current+numberPerThread-1)
								, simServers[j], this)):
									(new SimilarityThread(dmat[i],
											this.getDistMat(current, dmat.length-1)
											, simServers[j], this));
							current+=(numberPerThread);
						}
						while (this.finished<threadNo){
							try{
					            Thread.sleep(1);
					        }catch (Exception e) {/* dont care */}
						}
						for (int k = 0; k < threadNo; k++){
							sims.add(threads[k].sim);
							threads[k]=null;
						}
						count++;
						if (count%10==0){
							double[] sim = sims.toNativeArray();
							int number = sim.length;
							for(int n = 0; n<number; n++){
								BigDecimal b = new BigDecimal(sim[n]);
								double score = b.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
								pw.write(score+EOL);
							}
							timer.setBreakPoint();
							System.out.println("###"+count+"###"+timer.toStringMinutesSeconds());
							sims.clear();
							sims = new TDoubleArrayList();
						}
						
						finished = 0;
					}else{
						for (int j=i+1; j<dmat.length-1;j++)
							sims.add(this.getSim(dmat[i], dmat[j]));
						count++;
					}
						
				}//i
				if(sims.size()!=0){
					double[] sim = sims.toNativeArray();
					int number = sim.length;
					for(int n = 0; n<number; n++){
						BigDecimal b = new BigDecimal(sim[n]);
						double score = b.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
						pw.write(score+ApplicationSetup.EOL);
					}
					timer.setBreakPoint();
					System.out.println("###"+count+"###"+timer.toStringMinutesSeconds());
					sims.clear();
				}
			}
			pw.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public double getSim(DistMat[] mat1, DistMat[] mat2){
		double sim = 0;
		int idi = 0;
		int idj = 0;
		double sum = 0;
		double sumi = 0;
		double sumj = 0;
		while(idi<mat1.length && idj<mat2.length){
			if(mat1[idi].termid<mat2[idj].termid){
				sumi = sumi+mat1[idi].tfidf*mat1[idi].tfidf;
				idi++;
			}
			else if(mat1[idi].termid>mat2[idj].termid){
				sumj = sumj+mat2[idj].tfidf*mat2[idj].tfidf;
				idj++;
			}
			else{
				sumi = sumi+mat1[idi].tfidf*mat1[idi].tfidf;
				sumj = sumj+mat2[idj].tfidf*mat2[idj].tfidf;
				sum = sum+mat1[idi].tfidf*mat2[idj].tfidf;
				idi++;
				idj++;
			}
		}
		sim=sum/(Math.sqrt(sumi)*Math.sqrt(sumj));
		return sim;
	}
	
	public void singlePassSimilairty(String pathout){
		try{
			PrintWriter pw = new PrintWriter(new FileWriter(pathout));
			
			if(dmat==null){
				System.out.println("Matrix not loaded");
			}
			else{
				//	int n = dmat.length*(dmat.length-1)/2;
				sim = new double[5000];
				int count = 0;
				int id = 0;
				for(int i = 0; i<dmat.length-1; i++){
					for(int j = i+1; j<dmat.length; j++){
					//calculate the dot product
						int idi = 0;
						int idj = 0;
						double sum = 0;
						double sumi = 0;
						double sumj = 0;
						while(idi<dmat[i].length && idj<dmat[j].length){
							if(dmat[i][idi].termid<dmat[j][idj].termid){
								sumi = sumi+dmat[i][idi].tfidf*dmat[i][idi].tfidf;
								idi++;
							}
							else if(dmat[i][idi].termid>dmat[j][idj].termid){
								sumj = sumj+dmat[j][idj].tfidf*dmat[j][idj].tfidf;
								idj++;
							}
							else{
								sumi = sumi+dmat[i][idi].tfidf*dmat[i][idi].tfidf;
								sumj = sumj+dmat[j][idj].tfidf*dmat[j][idj].tfidf;
								sum = sum+dmat[i][idi].tfidf*dmat[j][idj].tfidf;
								idi++;
								idj++;
							}
						}//while
						sim[id]=sum/(Math.sqrt(sumi)*Math.sqrt(sumj));
						count++;
						id++;
						if(count%5000==0){
							for(int n = 0; n<sim.length; n++){
								BigDecimal b = new BigDecimal(sim[n]);
								double score = b.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
								pw.println(score);
							}
							sim = new double[5000];
							id = 0;
							System.out.println("###"+count+"###");
						}
					}//j
				}//i
				if(id!=0){
					for(int n = 0; n<id; n++){
						BigDecimal b = new BigDecimal(sim[n]);
						double score = b.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
						pw.println(score);
					}
				}
			}
			pw.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private DistMat[][] getDistMat(int start, int end){
		DistMat[][] mat = new DistMat[end-start+1][];
		int counter = 0;
		for (int i=start; i<=end; i++){
			mat[counter++]  = dmat[i];
		}
		return mat;
	}
	
	/**
	 * This method calculate the euclidean distance between two docs
	 * @deprecated
	 * @param doc1
	 * @param doc2
	 * @return
	 */
	public double distance(DistMat[] doc1,DistMat[] doc2){
		double dist = 0;
		int count1 = 0; 
		int count2 = 0; 
		int c = 0;
		double sum = 0;
		while(count1<doc1.length||count2<doc2.length){
			if(doc1[count1].termid<doc2[count2].termid){
				sum += Math.pow(doc1[count1].tfidf,2);
				c = count1+1;
				if(doc1[c]!=null){
					System.out.println(count1);
					count1++;
					System.out.println("doc1:"+count1);
				}
			}
			else if(doc1[count1].termid==doc2[count2].termid){
				sum += Math.pow((doc1[count1].tfidf-doc2[count2].tfidf),2);
				c = count1+1;
				if(doc1[c]!=null){
					count1++;
					System.out.println("doc1:"+count1);
				}
				c = count2+1;
				if(doc2[c]!=null){
					count2++;
					System.out.println("doc2:"+count2);
				}
			}
			else if(doc1[count1].termid < doc2[count2].termid){
				sum += Math.pow(doc2[count2].tfidf,2);
				c = count2+1;
				if(doc2[c]!=null){
					count2++;
					System.out.println("doc2:"+count2);
				}
			}
		}
		dist = Math.sqrt(sum);
		return dist;
	}
	
	/**
	 * this method normalize the given vector
	 * @deprecated
	 * 
	 */
	public DistMat[] normalization(DistMat[] termvec){
		DistMat[] vec = new DistMat[termvec.length];
		double sum = 0;
		for(int i = 0; i < termvec.length; i++){
			sum += Math.pow(termvec[i].tfidf,2);
		}
		double norm = Math.sqrt(sum);
		for(int j = 0; j<termvec.length;j++){
			vec[j] = new DistMat();
	//		vec[j].docid = termvec[j].docid;
			vec[j].termid = termvec[j].termid;
			vec[j].tfidf = termvec[j].tfidf/norm;
		}
		return vec;
	}
	/**
	 * -mt write the doc by term matrix, tfidf as weights
	 * -dist write the doc-doc distance into the file
	 * 
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*if(args[0].equals("-m")){
			String indexpath = args[1];
			String filepath = args[2];
			DocumentIndex doci = new DocumentIndex(indexpath+"/data.docid"); 
			DirectIndex di = new DirectIndex(doci,indexpath+"/data.df");
			Lexicon lex = new Lexicon(indexpath+"/data.lex");
			InvertedIndex ii = new InvertedIndex(lex,indexpath+"/data.if");
			TestDistance dis = new TestDistance();
			dis.WriteTFIDF(doci,di,lex,ii,filepath);
		}
		if(args[0].equals("-d")){
			String matrixpath = args[1];
			String filepath = args[2];
			TestDistance dist = new TestDistance();
			dist.writeDistance(matrixpath, filepath, false);
			
		}
		if(args[0].equals("-spd")){
			String matrixpath = args[1];
			String filepath = args[2];
			TestDistance dist = new TestDistance();
			dist.writeDistance(matrixpath, filepath, Integer.valueOf(args[3]), Integer.valueOf(args[4]));
		}*/
		if(args[1].equals("--computesimilarity")){
			String pathdocs = args[2];
			String pathmatrix = args[3];
			TestDistance dist = new TestDistance();
			dist.hostName = args[5];
			dist.loadMatrix(pathmatrix, pathdocs);
			dist.singlePassSimilairty(args[4]);
		}
		
	}

}
