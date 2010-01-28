/*
 * Created on 12-Jul-2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package uk.ac.gla.terrier.utility;

public class ExtHeapSort extends HeapSort {
	/** The size of the heap.*/
	private static int heapSize;
	/** The left child.*/
	private static int l;
	/** The right child.*/
	private static int r;
	/** The largest.*/
	private static int largest;
	/** A temporary double.*/
	private static double tmpDouble;
	/** A temporary int.*/
	private static double tmpInt;
	/** A temporary short */
	private static short tmpShort;
	/**
	 * Builds a maximum heap.
	 * @param A int[] the array which will be transformed into a heap.
	 * @param B int[] the array which will be transformed into a heap,
	 *        based on the values of the first argument.
	 */
	private static void buildMaxHeap(double[] A, double[] B, short[] C) {
		heapSize = A.length;
		for (int i = (int) Math.floor(heapSize / 2.0D); i > 0; i--)
			maxHeapify(A, B, C, i);
	}
	/**
	 * Sorts the given arrays in ascending order, using heap-sort.
	 * @param A double[] the first array to be sorted.
	 * @param B int[] the second array to be sorted, according to the
	 *        values of the first array.
	 */
	public static void ascendingHeapSort(double[] A, double[] B, short[] C) {
		buildMaxHeap(A, B, C);
		for (int i = A.length; i > 0; i--) {
			tmpDouble = A[i - 1];
			A[i - 1] = A[0];
			A[0] = tmpDouble;
			tmpInt = B[i - 1];
			B[i - 1] = B[0];
			B[0] = tmpInt;
			tmpShort = C[i - 1];
			C[i - 1] = C[0];
			C[0] = tmpShort;
			heapSize--;
			maxHeapify(A, B, C, 1);
		}
	}
	/**
	 * Sorts the given arrays in descending order, using heap-sort.
	 * @param A double[] the first array to be sorted.
	 * @param B int[] the second array to be sorted, according to the
	 *        values of the first array.
	 */
	public static void descendingHeapSort(double[] A, double[] B, short[] C) {
		ExtHeapSort.ascendingHeapSort(A, B, C);
		reverse(A, B, C, A.length);
	}
	/**
	 * Sorts the top <tt>topElements</tt> of the given array in
	 * ascending order, using heap sort.
	 * @param A double[] the first array to be sorted.
	 * @param B int[] the second array to be sorted, according to the
	 *        values of the first array.
	 * @param topElements int the number of elements to be sorted.
	 */
	public static void ascendingHeapSort(double[] A, double[] B, short[] C, int topElements) {
		buildMaxHeap(A, B, C);
		int end = A.length - topElements;
		for (int i = A.length; i > end; i--) {
			tmpDouble = A[i - 1];
			A[i - 1] = A[0];
			A[0] = tmpDouble;
			tmpInt = B[i - 1];
			B[i - 1] = B[0];
			B[0] = tmpInt;
			tmpShort = C[i - 1];
			C[i - 1] = C[0];
			C[0] = tmpShort;
			heapSize--;
			maxHeapify(A, B, C, 1);
		}
	}
	/**
	 * Reverses the elements of the two arrays, after they have
	 * been sorted.
	 * @param A double[] the first array.
	 * @param B int[] the second array.
	 * @param topElements int the number of elements to be reversed.
	 */
	private static void reverse(double[] A, double[] B, short[] C, int topElements) {
		//reversing the top elements
		int elems = topElements;
		if (elems > A.length/2)
			elems = A.length/2;
		int j;
  		double t1;
		int t2;
		final int length = A.length;
		for (int i=0; i<elems; i++) {
			j = length - i - 1;
			tmpDouble = A[i]; A[i] = A[j]; A[j] = tmpDouble;
			tmpInt = B[i]; B[i] = B[j]; B[j] = tmpInt;
			tmpShort = C[i]; C[i] = C[j]; C[j] = tmpShort;
		}
	}
	/**
	 * Sorts the top <tt>topElements</tt> of the given array in
	 * descending order, using heap sort for sorting the values
	 * in ascending order and then reversing the order of a
	 * specified number of elements.
	 * @param A double[] the first array to be sorted.
	 * @param B int[] the second array to be sorted, according to the
	 *        values of the first array.
	 * @param topElements int the number of elements to be sorted.
	 */
	public static void descendingHeapSort(double[] A, double[] B, short[] C, int topElements) {
		ascendingHeapSort(A, B, C, topElements);
		reverse(A, B, C, topElements);
	}
	/**
	 * Maintains the heap property.
	 * @param A int[] The array on which we operate.
	 * @param i int a position in the array. This number is
	 * between 1 and A.length inclusive.
	 */
	private static void maxHeapify(double[] A, double[] B, short[] C, int i) {
		l = 2 * i;
		r = 2 * i + 1;
		if (l <= heapSize && A[l - 1] > A[i - 1])
			largest = l;
		else
			largest = i;
		if (r <= heapSize && A[r - 1] > A[largest - 1])
			largest = r;
		if (largest != i) {
			tmpDouble = A[largest - 1];
			A[largest - 1] = A[i - 1];
			A[i - 1] = tmpDouble;
			tmpInt = B[largest - 1];
			B[largest - 1] = B[i - 1];
			B[i - 1] = tmpInt;
			tmpShort = C[largest -1];
			C[largest -1] = C[i - 1];
			C[i - 1] = tmpShort;
			maxHeapify(A, B, C, largest);
		}
	}

}
