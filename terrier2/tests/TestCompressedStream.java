package tests;
import java.io.*;
import uk.ac.gla.terrier.compression.*;
/**
 * A class that tests the functionality of the BitFile.
 * Creation date: (03/06/2003 22:58:34)
 * @author Vassilis Plachouras
 */
public class TestCompressedStream {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) throws IOException {
		// Gets the command line parameters
		if (args.length != 1) {
			System.err.println("usage: java TestCompressedFile <filename>");
			System.exit(1);
		}
		/*
			//Start timing
			long startTime = System.currentTimeMillis();
		    
		    BitOutputStream file = new BitOutputStream(args[0]);
		    file.writeGamma(1);
		    file.writeUnary(2);
		    file.writeGamma(1);
		    file.writeUnary(1);
		    file.writeGamma(3);
		    file.writeUnary(2);
		    file.writeGamma(1);
		    file.writeUnary(1);
		
		    file.writeGamma(200);
		    file.writeUnary(2);
		    file.writeGamma(10);
		    file.writeUnary(2);
		    file.writeGamma(35);
		    file.writeUnary(3);
		
		    file.writeGamma(1);
		    file.writeUnary(2);
		    file.writeGamma(1);
		    file.writeUnary(1);
		    file.writeGamma(3);
		    file.writeUnary(2);
		    file.writeGamma(1);
		    file.writeUnary(1);
		
			file.writeGamma(1);
		    file.writeUnary(5);
		    file.writeGamma(7);
		    file.writeUnary(2);
		    file.writeGamma(16);
		    file.writeUnary(2);
		    file.writeGamma(3);
		    file.writeUnary(1);
		
			file.close();
		
			//end timing
			long endTime = System.currentTimeMillis();
		
			System.err.println("time to write file: " + ((endTime - startTime)/1000.0D));
		*/
		BitInputStream inStream =
			new BitInputStream(args[0]);
		while (true) {
			int in = inStream.readGamma();
			if (in == -1)
				break;
			System.out.println("" + in);
			in = inStream.readUnary();
			if (in == -1)
				break;
			System.out.println("" + in);
		}
		inStream.close();
	}
}
