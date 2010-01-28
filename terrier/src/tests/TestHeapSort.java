package tests;
import uk.ac.gla.terrier.utility.HeapSort;
/**
 * Tests the functionality of the uk.ac.gla.terrier.sorting.HeapSort class
 * Creation date: (05/08/2003 09:55:33)
 * @author Vassilis Plachouras
 */
public class TestHeapSort {
	/**
	 * Starts the application.
	 * @param args an array of command-line arguments
	 */
	public static void main(java.lang.String[] args) {
		// Insert code to start the application here.
		double[] test = new double[] { 3, 5, 2, 1, 6, 7, 9, 8, 0, 4 };
		int[] testIds = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		short[] testShorts = new short[test.length];
		System.err.println("Input: scores, docids");
		for (int i = 0; i < test.length; i++)
			System.err.print("\t" + test[i]);
		System.err.println();
		for (int i = 0; i < test.length; i++)
			System.err.print("\t" + testIds[i]);
		System.err.println();
		System.err.println("--------------");
		System.err.println("Output: Sorted ascending score, top 6 elements");
		HeapSort.ascendingHeapSort(test, testIds, testShorts, 6);
		for (int i = 0; i < test.length; i++)
			System.err.print("\t" + test[i]);
		System.err.println();
		for (int i = 0; i < test.length; i++)
			System.err.print("\t" + testIds[i]);
		System.err.println();
		System.err.println("--------------");
		System.err.println("Output: Sorted descending score, top 6 elements");
		HeapSort.descendingHeapSort(test, testIds, testShorts, 6);
		for (int i = 0; i < test.length; i++)
			System.err.print("\t" + test[i]);
		System.err.println();
		for (int i = 0; i < test.length; i++)
			System.err.print("\t" + testIds[i]);
		System.err.println();

	}
}
