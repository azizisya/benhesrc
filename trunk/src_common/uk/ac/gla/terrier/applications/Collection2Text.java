package uk.ac.gla.terrier.applications;
import java.io.BufferedWriter;
import java.io.IOException;

import uk.ac.gla.terrier.utility.ApplicationSetup;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.indexing.*;

public class Collection2Text
{
	public static void main(String args[])
	{
		try{
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(args[0]);
			final TRECWebCollection3 collection = new TRECWebCollection3();
			LabradorTextDocument doc = null;
			int counter = 0;
			while(collection.nextDocument())
			{	
				doc = (LabradorTextDocument)collection.getDocument();
				 //System.out.println("<DOC>\n<DOCNO>"+collection.getDocid().trim()+"</DOCNO>\n\n");
				String docS = doc.getText();
				 //System.out.print(docS);
				bw.write(collection.getDocid().trim()+" "+docS);
				if (docS.lastIndexOf(".") < docS.length() -2 )
					//System.out.println(".");
					bw.write(".");
				//else
					//System.out.println();
				System.out.println("</DOC>\n\n");
				bw.write(ApplicationSetup.EOL);
				counter++;
			}
			
			System.err.println("Finished dumping Collection2Text");
			System.err.println(counter+" documents processed.");
			bw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
