package uk.ac.gla.terrier.applications;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.GZIPOutputStream;

import uk.ac.gla.terrier.indexing.Collection;
import uk.ac.gla.terrier.indexing.Document;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/** Automagically create docno2url and id2url.gz from Collection object. */
public class ExtractURLLists
{
	public static void main(String args[])
	{
		Collection collection = null;
		String collectionName = ApplicationSetup.getProperty("trec.collection.class", "TRECCollection");
		if (collectionName.indexOf('.') == -1)
			collectionName = "uk.ac.gla.terrier.indexing."+collectionName;
		try{
			collection = (Collection) Class.forName(collectionName).newInstance();
		} catch (Exception e) {
			System.err.println("ERROR: Collection class named "+ collectionName + " not found");
			System.exit(1);
		}

		try{
			String path = ApplicationSetup.TERRIER_INDEX_PATH;
			PrintWriter docno2url = new PrintWriter(new FileWriter(new File(path, "docno2url")));
			PrintWriter id2url = new PrintWriter(new OutputStreamWriter(
					new GZIPOutputStream(new FileOutputStream(new File(path, "id2url.gz")))));
			int docid=0;
			while(collection.nextDocument())
			{
				Document d = collection.getDocument();
				String url = d.getProperty("url");
				String docno = collection.getDocid();
				if (url == null)
					url = "";
				docno2url.println(docno + " " + url);
				id2url.println(docid + " "+ url);
				docid++;
			}
			docno2url.close();
			id2url.close();
		}catch (Exception e){
			System.err.println("Exception "+e);
			e.printStackTrace();
		}
	}
}
