package uk.ac.gla.terrier.applications;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.structures.GenoArticle;
import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.structures.LexiconEntry;
import uk.ac.gla.terrier.structures.Span;
import uk.ac.gla.terrier.utility.Files;
import uk.ac.gla.terrier.utility.PDFUtility;
import uk.ac.gla.terrier.utility.StringUtility;
import gnu.trove.TDoubleHashSet;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;

public class ScanForParaValues {
	
	public void scanForParasFromPDFFile(String pdfFilename, String paraFilename, String lexiconFilename){
		// load all parameter strings
		THashSet<String> paraStrSet = new THashSet<String>();
		try{
			BufferedReader br = Files.openFileReader(paraFilename);
			String line = null;
			while ((line=br.readLine())!=null){
				paraStrSet.add(line);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		String[] paraStrs = (String[])paraStrSet.toArray(new String[paraStrSet.size()]);
		int[] paraTermids = new int[paraStrs.length];
		
		GenoArticle article = PDFUtility.getGenoPDFArticle(pdfFilename);
		Lexicon lexicon = new Lexicon(lexiconFilename);
		for (int i=0; i<paraStrs.length; i++){
			LexiconEntry lexEntry = lexicon.getLexiconEntry(paraStrs[i].toLowerCase());
			paraTermids[i] = (lexEntry==null)?(-1):(lexEntry.termId);
		}
		Span[] spans = article.getSpans();
		int numOfSpans = spans.length;
		for (int i=0; i<numOfSpans; i++){
			spans[i].setRankby("offset");
		}
		Arrays.sort(spans);
		String separator = "_"; 
		for (int i=0; i<numOfSpans; i++){
			int foundQuantity = 0;	
			StringBuilder paraStrBuf = new StringBuilder();
			// do not look for quantities in references
			if (!spans[i].getSection().equalsIgnoreCase("references")){
				for (int j=0; j<paraTermids.length; j++){
					if (spans[i].getTermFreqMap().containsKey(paraTermids[j])){
						paraStrBuf.append(paraStrs[j]+" ");
					}
				}
				if (paraStrBuf.length()!=0){
					double[] values = this.scanForPara(spans[i].getSpanString());
					if (values.length>0){
						foundQuantity = 1;	
					}
				}
			}
			if (paraStrBuf.length()==0)
				paraStrBuf.append("No parameters found");
			// output  format:
			// quantification?(1/0) _ gene names _ section title _ snippet
			System.out.println(foundQuantity+"_"+paraStrBuf.toString()+"_"+
					spans[i].getSection()+"_"+spans[i].getSpanString());
		}
		
	}
	
	public void scanForParaFromPDFFile(String pdfFilename, String paraStr, String lexiconFilename){
		GenoArticle article = PDFUtility.getGenoPDFArticle(pdfFilename);
		Lexicon lexicon = new Lexicon(lexiconFilename);
		LexiconEntry lexEntry = lexicon.getLexiconEntry(paraStr);
		if (lexEntry==null)
			return;
		int paraTermid = lexEntry.termId;
		Span[] spans = article.getSpans();
		int numOfSpans = spans.length;
		for (int i=0; i<numOfSpans; i++){
			spans[i].setRankby("offset");
		}
		Arrays.sort(spans);
		for (int i=0; i<numOfSpans; i++){
			/**
			if (spans[i].getType().equals("TXT")){
				// if span contains parameter string
				if (spans[i].getTermFreqMap().containsKey(paraTermid)){
					double[] values = scanForPara(paraStr, spans[i].getSpanString());
					if (values.length>0){
						System.out.println(spans[i].getSection()+" "+spans[i].getSpanString());
						// print values
						for (int j=0; j<values.length; j++){
							// System.out.println(values[j]);
						}
					}
				}
			}*/
			System.out.println(spans[i].getSection()+" "+spans[i].getSpanString());
		}
		
	}
	
	public double[] scanForPara(String text){
		TDoubleHashSet valueSet = new TDoubleHashSet();
		char[] chs = text.toCharArray();
		// int numberOfChars = chs.length;
		String[] tokens = text.toLowerCase().replaceAll(",", "").split(" ");
		int numberOfTokens = tokens.length;
		for (int i=0; i<numberOfTokens; i++){
			if (StringUtility.isNumber(tokens[i])){
				valueSet.add(Double.parseDouble(tokens[i]));
			}
		}
		
		return valueSet.toArray();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args[0].equals("--test")){
			// --test pdfFilename parameterString lexiconFilename
			ScanForParaValues app = new ScanForParaValues();
			app.scanForParaFromPDFFile(args[1], args[2], args[3]);
		}else if (args[0].equals("--scanparas")){
			// --scanparas pdfFilename paraFilename lexiconFilename
			ScanForParaValues app = new ScanForParaValues();
			app.scanForParasFromPDFFile(args[1], args[2], args[3]);
		}

	}

}
