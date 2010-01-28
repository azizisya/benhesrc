/*
 * Created on 2004-8-19
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.distr.structures;

import java.io.IOException;
import java.util.Arrays;

import uk.ac.gla.terrier.structures.Lexicon;
import uk.ac.gla.terrier.utility.ApplicationSetup;

/**
 * @author ben
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GlobalLexicon extends Lexicon{
	
	public GlobalLexicon (String filename){
		super(filename);
	}
	
	public int[] getGlobalInformation(String _term) {
		int[] info = new int[2];

		Arrays.fill(buffer, (byte) 0);
		Arrays.fill(bt, (byte) 0);
		byte[] tmp = _term.getBytes();
		for (int i = 0; i < tmp.length; i++)
			bt[i] = tmp[i];

		long begin = 0;
		long end = numberOfLexiconEntries;
		int termLength = ApplicationSetup.STRING_BYTE_LENGTH;
		while (begin <= end) {
			//System.out.println("begin:"+begin+", end:" + end);
			if (begin == end) {
				boolean found = true;
				try {
					lexiconFile.seek(begin * lexiconEntryLength);
					lexiconFile.readFully(buffer, 0, termLength);
				} catch (IOException ioe) {
					System.out.println(
						"Input/Output exception while handling buffered data. Stack trace follows.");
					ioe.printStackTrace();
					System.exit(1);
				}

				for (int i = 0; i < termLength; i++) {
					if (buffer[i] != bt[i]) {
						found = false;
						break;
					}
				}

				if (found) {
					term = _term;
					try {
						lexiconFile.readInt();
						info[0] = lexiconFile.readInt();
						info[1] = lexiconFile.readInt();
					} catch (IOException ioe) {
						System.out.println(
							"Input/Output exception while handling buffered data. Stack trace follows.");
						ioe.printStackTrace();
						System.exit(1);
					}
					return info;
				} else {
					return info;
				}
			} else {
				//find the next entry to check
				long mid = (begin + end) / 2;
				double dmid = (begin + end) / 2.0;
				if ((dmid - mid) >= 0.5)
					mid++;
				try {
					lexiconFile.seek(mid * lexiconEntryLength);
					lexiconFile.readFully(buffer, 0, termLength);
				} catch (IOException ioe) {
					System.err.println(
						"Input/Output exception while reading from lexicon file. Stack trace follows.");
					ioe.printStackTrace();
					System.exit(1);
				}
				int compareResult = 0;

				for (int i = 0; i < termLength; i++) {
					if (bt[i] < buffer[i]) {
						compareResult = -1;
						break;
					} else if (bt[i] > buffer[i]) {
						compareResult = 1;
						break;
					}
				}

				if (compareResult == -1)
					end = mid - 1;
				else
					begin = mid;
			}
		}
		return info;
	}
}
