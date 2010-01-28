package uk.ac.gla.terrier.links;
import java.io.*;
import java.util.zip.*;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.Files;
class MakeScores
{
	public static void main(String args[]) throws IOException
	{	
		Index index = Index.createIndex();
		DocumentIndex doi = index.getDocumentIndex();
		final int numDocs = index.getCollectionStatistics().getNumberOfDocuments();
		double[] out = new double[numDocs];
		String line = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		int i = 0;
		while((line = br.readLine()) != null)
		{
			i++;
			String[] parts = line.trim().split("\\s+");
			if (parts.length != 2)
			{
				System.err.println("Bad line ("+i+"): "+ line);
				continue;
			}

			int docid = doi.getDocumentId(parts[0]);
			if (docid == -1 )
			{
				System.err.println("Bad docno ("+i+"): "  + line);
			}
			double value = Double.parseDouble(parts[1]);
			
			out[docid] = value;
		}

		final ObjectOutputStream oos = new ObjectOutputStream(Files.writeFileStream(args[0]));
		oos.writeObject(out);
		oos.close();
	}
}
