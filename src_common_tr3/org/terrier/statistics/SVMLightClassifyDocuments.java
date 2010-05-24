package org.terrier.statistics;
import java.io.BufferedReader;
import java.io.Writer;
import java.io.InputStreamReader;

import org.terrier.structures.*;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

import gnu.trove.*;

public class SVMLightClassifyDocuments extends ClassifyDocuments 
{
	protected static String svmLightPath = ApplicationSetup.getProperty("svm.light.path",null);
	protected static String svmTrainParams = ApplicationSetup.getProperty("svm.light.train.params", "");
	protected static String svmClassifyParams = ApplicationSetup.getProperty("svm.light.classify.params", "");
	protected static String svmFeatureValues = ApplicationSetup.getProperty("svm.light.feature.values", "TF");
	protected static Runtime runtime = Runtime.getRuntime();

	protected String trainingFilename;
	protected String modelFilename;
	public SVMLightClassifyDocuments(Index i)
	{
		super(i);
		trainingFilename = "training";
		modelFilename = "model";
	}	

	/** Labels should be 1 for relevant, -1 for non-relevant */
	public void train(int[] docids, int[] labels) throws Exception
	{
		svmLightOutputDocs(trainingFilename, docids, labels);
		Process p = runtime.exec(svmLightPath + "/" + "svm_learn" + " " +svmTrainParams+ " "+ trainingFilename + " " + modelFilename);
		p.waitFor();
		p.destroy();
		System.err.println("SVMLight returned "+ p.exitValue());
	}

	public int[] classify(int[] docids) throws Exception
	{
		String exampleFilename = "example";
		String outputFilename = "output";
		int[] labels = new int[docids.length];
		//Arrays.fill(labels, 0);
		svmLightOutputDocs(exampleFilename, docids, labels);
		Process p = runtime.exec(svmLightPath + "/" + "svm_classify" + " " +svmClassifyParams
			+ " " + exampleFilename + " " + modelFilename + " " + outputFilename);
		p.waitFor();
		p.destroy();
		System.err.println("SVMLight clasify returned "+ p.exitValue());
		BufferedReader br = Files.openFileReader(outputFilename);
		String line = null; int i=0;
		while((line = br.readLine()) != null)
		{
			line = line.trim();
			double result = Double.parseDouble(line);
			labels[i] = result > 0 ? 1 : -1;
		}
		return labels;
	}

   public double[] classify_scores(int[] docids) throws Exception
   {
		String exampleFilename = "example";
		String outputFilename = "output";
		int[] labels = new int[docids.length];
		//Arrays.fill(labels, 0);
		svmLightOutputDocs(exampleFilename, docids, labels);
		Process p = runtime.exec(svmLightPath + "/" + "svm_classify" + " " +svmClassifyParams
			+ " " + exampleFilename + " " + modelFilename + " " + outputFilename);
		p.waitFor();
		p.destroy();
		System.err.println("SVMLight clasify returned "+ p.exitValue());
		BufferedReader br = Files.openFileReader(outputFilename);
		double[] labels_out = new double[docids.length];
		String line = null; int i=0;
		while((line = br.readLine()) != null)
		{
			line = line.trim();
			labels_out[i] = Double.parseDouble(line);
		}
		return labels_out;
	}

	protected void svmLightOutputDocs(String filename, int[] docids, int[] labels) throws Exception
	{
		Writer w = Files.writeFileWriter(filename);
		
		for(int i=0;i<docids.length;i++)
		{
			final int docid = docids[i];
			final int label = labels[i];
			final StringBuilder line = new StringBuilder();
			line.append(label);
			line.append(" ");
			final int[][] postings = this.di.getTerms(docid);
			for (int t=0;t<postings[0].length;t++)
			{
				line.append(postings[0][t]);
				line.append(':');
				line.append(postings[1][t]);
				line.append(' ');
			}
			line.append(" # ");
			line.append(docid);
			line.append("\n");
			w.write(line.toString());
		}
		w.close();
	}

	public void close() throws Exception
	{
		Files.delete(trainingFilename);
		Files.delete(modelFilename);
		Files.delete("example");
		Files.delete("output");
	}

	public static void main(String args[]) throws Exception
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		Index index = Index.createIndex();
		MetaIndex metaidx = index.getMetaIndex();
		String key = "docno";
		while((line = br.readLine()) != null)
		{
			line = line.trim();
			String parts[] = line.split(" ");
			int number = parts.length -1;
			String qid = parts[0];
			System.err.println("line: "+line);
			
			TIntObjectHashMap<TIntHashSet> classify2sets = new TIntObjectHashMap<TIntHashSet>(3);
	
			for(int i=1;i<number+1;i++)
			{
				String[] parts2 = parts[i].split(":");
				int docid = metaidx.getDocument(key, parts2[0]);
				int label = Integer.parseInt(parts2[1]);
				TIntHashSet set = classify2sets.get(label);
				if (set == null)
				{
					classify2sets.put(label, set = new TIntHashSet());
				}
				set.add(docid);
				System.out.println("entry "+parts[i]+" decomposed to ("+parts2[0]+", "+parts2[1]+")");
			}
			SVMLightClassifyDocuments svm = new SVMLightClassifyDocuments(index);
			TIntArrayList docids_train = new TIntArrayList();
			TIntArrayList labels_train = new TIntArrayList();
			for(int label : new int[]{-1, 1})
			{
				TIntHashSet set_for_label = classify2sets.get(label);
				if (set_for_label == null)
				{
					System.err.println("No set found for label " + label);
					return;
				}
				for (int docid : set_for_label.toArray())
				{
					docids_train.add(docid);
					labels_train.add(label);
				}
			}
			svm.train(docids_train.toNativeArray(), labels_train.toNativeArray());
			TIntArrayList docids_classify = new TIntArrayList();
			TIntHashSet set_for_label0 = classify2sets.get(0);
			if (set_for_label0 == null)
			{
					System.err.println("No set found for label 0");
					return;
			}
			for (int docid : classify2sets.get(0).toArray())
			{
				docids_classify.add(docid);
			}
			int[] docids_classifyAr = docids_classify.toNativeArray();
			int[] labels = svm.classify(docids_classifyAr);
			System.out.print(qid+" "); int i=0;
			for (int docid : docids_classifyAr)
			{
				String docno = metaidx.getItem(key, docid);
				System.out.print(docno + " " + labels[i]+ " ");
				i++;
			}
			System.out.println();
			svm.close();
		}
	}

}
