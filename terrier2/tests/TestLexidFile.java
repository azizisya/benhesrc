package tests;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
/**
 * Checks the .lexid file for any dupplicate offsets. If there
 * are any, this means that the hash function that assigns keys to
 * terms is not universal for the vocabulary considered.
 * Creation date: (11/07/2003 09:53:11)
 * @author Vassilis Plachouras
 */
public class TestLexidFile {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("usage: cmd <input lexid file>");
			System.exit(1);
		}
		File lexidFile = new File(args[0]);
		int length = (int) lexidFile.length() / 8;
		long[] termids = new long[length];
		//DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(lexidFile)));
		RandomAccessFile dis = new RandomAccessFile(lexidFile, "r");
		for (int i = 0; i < length; i++) {
			dis.seek(i * 8);
			termids[i] = dis.readLong();
			System.out.println("termd[" + i + "] = " + termids[i]);
		}
		dis.close();
		Arrays.sort(termids);
		for (int i = 1; i < length; i++) {
			if (termids[i] == termids[i - 1]) {
				System.out.println(
					"Two offsets are the same: "
						+ termids[i]
						+ " and "
						+ termids[i
						- 1]);
			}
		}
	}
}
