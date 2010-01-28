package tests;
import uk.ac.gla.terrier.compression.*;
/**
 * A class that tests the functionality of the BitFile.
 * Creation date: (03/06/2003 22:58:34)
 * @author Vassilis Plachouras
 */
public class TestCompressedFile {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) throws Exception {
		// Gets the command line parameters
		if (args.length != 1) {
			System.err.println("usage: java TestCompressedFile <filename>");
			System.exit(1);
		}
		//Start timing
		long startTime = System.currentTimeMillis();
		BitFile file = new BitFile(args[0]);
		file.writeReset();
		long startByte1 = file.getByteOffset();
		byte startBit1 = file.getBitOffset();
		file.writeGamma(1);
		file.writeUnary(2);
		file.writeGamma(1);
		file.writeUnary(1);
		file.writeGamma(3);
		file.writeUnary(2);
		file.writeGamma(1);
		file.writeUnary(1);
		//file.writeFlush();
		long endByte1 = file.getByteOffset();
		byte endBit1 = file.getBitOffset();
		System.out.println("endByte1: " + endByte1);
		System.out.println("endBit1: " + endBit1);
		endBit1--;
		if (endBit1 < 0 && endByte1 > 0) {
			endBit1 = 7;
			endByte1--;
		}
		//file.writeReset();
		long startByte2 = file.getByteOffset();
		byte startBit2 = file.getBitOffset();
		file.writeGamma(200);
		file.writeUnary(2);
		file.writeGamma(10);
		file.writeUnary(2);
		file.writeGamma(35);
		file.writeUnary(3);
		file.writeFlush();
		long endByte2 = file.getByteOffset();
		byte endBit2 = file.getBitOffset();
		System.out.println("endByte2: " + endByte2);
		System.out.println("endBit2: " + endBit2);
		endBit2--;
		if (endBit2 < 0 && endByte2 > 0) {
			endBit2 = 7;
			endByte2--;
		}
		file.writeReset();
		long startByte3 = file.getByteOffset();
		byte startBit3 = file.getBitOffset();
		file.writeGamma(1);
		file.writeUnary(2);
		file.writeGamma(1);
		file.writeUnary(1);
		file.writeGamma(3);
		file.writeUnary(2);
		file.writeGamma(1);
		file.writeUnary(1);
		//file.writeFlush();
		long endByte3 = file.getByteOffset();
		byte endBit3 = file.getBitOffset();
		System.out.println("endByte3: " + endByte3);
		System.out.println("endBit3: " + endBit3);
		endBit3--;
		if (endBit3 < 0 && endByte3 > 0) {
			endBit3 = 7;
			endByte3--;
		}
		//file.writeReset();
		long startByte4 = file.getByteOffset();
		byte startBit4 = file.getBitOffset();
		file.writeGamma(1);
		file.writeUnary(5);
		file.writeGamma(7);
		file.writeUnary(2);
		file.writeGamma(16);
		file.writeUnary(2);
		file.writeGamma(3);
		file.writeUnary(1);
		file.writeFlush();
		long endByte4 = file.getByteOffset();
		byte endBit4 = file.getBitOffset();
		System.out.println("endByte4: " + endByte4);
		System.out.println("endBit4: " + endBit4);
		endBit4--;
		if (endBit4 < 0 && endByte4 > 0) {
			endBit4 = 7;
			endByte4--;
		}
		file.close();
		//end timing
		long endTime = System.currentTimeMillis();
		System.err.println(
			"time to write file: " + ((endTime - startTime) / 1000.0D));
		file = new BitFile(args[0]);
		file.readReset(
			(long) startByte1,
			(byte) startBit1,
			(long) endByte1,
			(byte) endBit1);
		while (((file.getByteOffset() + startByte1) < endByte1)
			|| (((file.getByteOffset() + startByte1) == endByte1)
				&& (file.getBitOffset() < endBit1))) {
			System.out.println("gamma: " + file.readGamma());
			System.out.println("unary: " + file.readUnary());
		}
		System.out.println("next term");
		file.readReset(
			(long) startByte2,
			(byte) startBit2,
			(long) endByte2,
			(byte) endBit2);
		while (((file.getByteOffset() + startByte2) < endByte2)
			|| (((file.getByteOffset() + startByte2) == endByte2)
				&& (file.getBitOffset() < endBit2))) {
			System.out.println("gamma: " + file.readGamma());
			System.out.println("unary: " + file.readUnary());
		}
		System.out.println("next term");
		file.readReset(
			(long) startByte3,
			(byte) startBit3,
			(long) endByte3,
			(byte) endBit3);
		while (((file.getByteOffset() + startByte3) < endByte3)
			|| (((file.getByteOffset() + startByte3) == endByte3)
				&& (file.getBitOffset() < endBit3))) {
			System.out.println("gamma: " + file.readGamma());
			System.out.println("unary: " + file.readUnary());
		}
		System.out.println("next term");
		file.readReset(
			(long) startByte4,
			(byte) startBit4,
			(long) endByte4,
			(byte) endBit4);
		while (((file.getByteOffset() + startByte4) < endByte4)
			|| (((file.getByteOffset() + startByte4) == endByte4)
				&& (file.getBitOffset() < endBit4))) {
			System.out.println("gamma: " + file.readGamma());
			System.out.println("unary: " + file.readUnary());
		}
		file.close();
	}
}
