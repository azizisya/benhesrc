package uk.ac.gla.terrier.utility;

import java.io.BufferedWriter;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.Reader;

import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.util.PDFTextStripper;

import uk.ac.gla.terrier.querying.Manager;
import uk.ac.gla.terrier.structures.GenoArticle;

public class PDFUtility {
	public static GenoArticle getGenoPDFArticle(String pdfFilename){
		Manager manager = new Manager();
		PDFParser parser = null; PDDocument document = null; PDFTextStripper stripper = null;
		CharArrayWriter writer = null;
		GenoArticle article = null;
		try{
			parser = new PDFParser(Files.openFileStream(pdfFilename));
			parser.parse();
			document = parser.getPDDocument();
			writer = new CharArrayWriter();
			stripper = new PDFTextStripper();
			stripper.setLineSeparator("\n");
			stripper.writeText(document, writer);
			document.close();
			writer.close();
			parser.getDocument().close();
			Reader br = new CharArrayReader(writer.toCharArray());
			StringBuilder sb = new StringBuilder();
			sb.ensureCapacity(100000);
			int ch=0;
			boolean metEOL = false;
			boolean firstXLine = true;
			int lineCounter = 0;
			while ((ch=br.read())!=-1){
				if ((char)ch == '\n'){
					metEOL = true;
					continue;
				}
				else if ((char)ch == '-'){
					int next = br.read();
					if ((char)next == '\n'){
						continue;
					}else
						sb.append((char)ch);
				}
				if (metEOL){
					if (Character.isUpperCase(ch) && Character.isLetterOrDigit(sb.charAt(sb.length()-1)) && !firstXLine)
						sb.append(". ");
					else
						sb.append(" ");
					metEOL = false;
					lineCounter++;
					if (firstXLine && lineCounter > 7 ) firstXLine = false;
				}
				sb.append((char)ch);
			}
			//BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(outputFilename);
			//bw.write(sb.toString());
			//bw.close();
			char[] chars = new char[sb.length()];
			sb.getChars(0, sb.length()-1, chars, 0);
			//printGivenSentences(GenoArticle.getSentencesFromText(chars, "extDoc"));
			article = new GenoArticle(chars, manager, "extDoc");
			// article.dumpSpans(manager.getIndex().getLexicon());
		}catch (Exception e){
			e.printStackTrace();
			System.exit(1);
			/*logger.warn("WARNING: Problem converting PDF: ",e);
			try{
				document.close();				
			}catch(Exception e1){
				logger.warn("WARNING: Problem converting PDF: ",e1);
			}
			try{
				writer.close();
			}catch(Exception e2){
				logger.warn("WARNING: Problem converting PDF: ",e2);
			}
			try{
				parser.getDocument().close();
			}catch(Exception e3){
				logger.warn("WARNING: Problem converting PDF: ",e3);	
			}
			parser = null; document = null; writer = null; stripper = null;*/
		}
		return article;
	}
}
