package uk.ac.gla.terrier.links;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream;

public class LargeScaleIndegree {	
	public static void main(String[] args) throws IOException {
		if (args.length!=2) {
			System.out.println("usage: LargeScaleIndegree [# of docs] [output filename]");
			System.exit(1);
		}
		
		int numOfDocs = Integer.parseInt(args[0]);
		double[] indegrees = new double[numOfDocs];
		
		LinkServerStream linkServer = new LinkServerStream();
		int docid;
		double indegree;
		while (linkServer.readNextEntry()) {
			docid =linkServer.getDocid();
			indegree = linkServer.getDegree();
			indegrees[docid] = indegree;
		}
		linkServer.close();
		
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(args[1])));
		oos.writeObject(indegrees);
		oos.close();
	}
	
}
