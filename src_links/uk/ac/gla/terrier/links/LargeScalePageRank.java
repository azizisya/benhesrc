package uk.ac.gla.terrier.links;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class LargeScalePageRank {

	protected static final double DEFAULT_DAMPING_FACTOR = 0.85;
	protected static final double DEFAULT_THRESHOLD = 1E-12d;//1E-8d;

	protected double[] outdegrees = null;
	protected int[][] links = null;
	protected int nodes;
	
	protected double[] vec1 = null;
	protected double[] vec2 = null;
	protected double norm1;
	protected int iterations;
	
	protected int n;
	protected int nlinks;

	protected final double threshold;	
	protected final double dampingFactor;
	
	
	public LargeScalePageRank(){
		this(DEFAULT_THRESHOLD, DEFAULT_DAMPING_FACTOR);
	}
	
	public LargeScalePageRank(double _threshold, double _dampingFactor) {
		threshold = _threshold;
		dampingFactor = _dampingFactor;
	}
	
	public void initialise(int _nodes) {
		nodes = _nodes;
		
		vec1 = new double[nodes];
		vec2 = new double[nodes];

		double norm1 = 0.0d;
		int iterations = 0;
		
		n = vec1.length;
		
		//compute outdegrees
		int counter=1;
		outdegrees = new double[nodes];
		LinkServerStream linkServer = new LinkServerStream();
		while (linkServer.readNextEntry()) {
			
			outdegrees[linkServer.getDocid()] = linkServer.getDegree();
			if (counter++ == 1000000) {
				counter = 1;
				System.out.println("processing document " + linkServer.getDocid() + " at " + (new Date()));
				System.out.println("doc " + linkServer.getDocid() + " has " + linkServer.getDegree() + " outgoing links");
			}
		}
		linkServer.close();
		
		//initialise vec1
		Arrays.fill(vec1, 1.0d/n);
	}
	
	public static void main(String[] args) {
		//final double[] outdegrees = new double[] {3.0d, 2.0d, 1.0d, 2.0d, 1.0d};
		//final int[][] links = new int[][] { {1, 2}, {1, 3}, {1, 5}, {2, 1}, {2, 4}, {3, 5}, {4, 2}, {4, 3}, {5, 4} };
		//final int nodes = 5;
		
		//final double[] outdegrees = new double[] {2.0d, 1.0d, 1.0d, 1.0d};
		//final int[][] links = new int[][] { {1, 2}, {1, 3}, {2, 3}, {3, 1}, {4, 3}};
		//final int nodes = 4;
		
		//final double[] outdegrees = new double[] {1.0d, 2.0d};
		//final int[][] links = new int[][] {{1, 1}, {2, 2}, {2, 1}};
		//final int nodes = 2;
		if (args.length!=2) {
			System.out.println("usage: LargeScalePageRank [number of documents] [output filename]");
			System.exit(1);
		}
		LargeScalePageRank am = new LargeScalePageRank(/*1E-8d, Double.parseDouble(args[0])*/);
		System.out.println("started initialisation " + (new Date()));
		am.initialise(Integer.parseInt(args[0]) /*CollectionStatistics.getNumberOfDocuments()*/);
		System.out.println("started computing " + (new Date()));
		am.compute();
		System.out.println("started printing results  " + (new Date()));
		if (args.length == 1)
			am.printResults(); 
		else 
			am.printResults(args[1]);
	}
	
	protected void iterate() {
		int source;
		int dest;
		int outdegree;
		int[] outlinks;
		
		iterations++;
		//initialise vec2
		Arrays.fill(vec2, 0.0d);
		
		//perform one iteration
		//for (int i=0; i<nlinks; i++) {
		//	source = links[i][0]-1;
		//	dest = links[i][1]-1;
		//	vec2[dest] += vec1[source]/(1.0d + outdegrees[source]);
		//}
		int counter = 1;
		LinkServerStream linkServer = new LinkServerStream();
		while (linkServer.readNextEntry()) {
			source =linkServer.getDocid();
			outdegree = linkServer.getDegree();
			outlinks = linkServer.getLinks();
			for (int j=0; j<outdegree; j++) {
				dest = outlinks[j];
				vec2[dest] += dampingFactor * vec1[source]/(outdegrees[source]);
			}
			if (counter++ == 1000000) {
				counter = 1;
				System.out.println("processing document " + source + " at " + (new Date()));
			}
		}
		linkServer.close();
		
		//transfer probability to clones
		double addProb = (1.0d - dampingFactor) / ((double)n);
		for (int i=0; i<n; i++) {
			vec2[i] += addProb;
		}
		
		//compute sum
		//and compute norm1
		double sum = 0.0d;
		for (int i=0; i<n; i++) {
			sum += vec2[i];
		}
		norm1 = 0.0d;
		double recipSum = 1.0d / sum;
		for (int i=0; i<n; i++) {
			vec2[i] *= recipSum;
			norm1 += Math.abs(vec2[i]-vec1[i]);
		}
		
		//copy vec2 values to vec1
		System.arraycopy(vec2, 0, vec1, 0, vec1.length);
	}
	
	public void compute() {
		int iterationCounter = 1;
		do {
			System.out.println("started iteration " + (new Date()));
			iterate();
			System.out.println("iteration " + iterationCounter + " with norm1: " + norm1);
			iterationCounter++;
		} while (norm1 > threshold);
	}
	
	public void printResults() {
		System.out.println("iterations: " + iterations + " norm1: " + norm1);
		
		//print vec1
		System.out.println("vec1 (the pagerank scores) : ");
		for (int i=0; i<vec1.length; i++)
			System.out.println(vec1[i] + " ");
		System.out.println("----");


	}
	
	public void printResults(String filename) {
		printResults();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
			oos.writeObject(vec1);
			oos.close();
		} catch(IOException ioe) {
			System.err.println("IOException while saving the results to a file. exiting : "+ioe);
			ioe.printStackTrace();
			System.exit(1);
		}
	}

}
