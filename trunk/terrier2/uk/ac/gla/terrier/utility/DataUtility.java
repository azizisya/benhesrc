package uk.ac.gla.terrier.utility;

import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class DataUtility {
	/**
	 * Load content into an array in which each element contains one line of content.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Object[] loadObject(String filename) throws IOException{
		BufferedReader br = Files.openFileReader(filename);
		String line = null;
		ArrayList<String> list = new ArrayList<String>();
		while ((line=br.readLine())!=null){
			list.add(line.trim());
		}
		br.close();
		return list.toArray();
	}
	/**
	 * Load integers stored in a file into an array in which each element corresponds to one line.
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static int[] loadInt(String filename) throws IOException{
		BufferedReader br = Files.openFileReader(filename);
		String line = null;
		TIntArrayList list = new TIntArrayList();
		while ((line=br.readLine())!=null){
			list.add(Integer.parseInt(line));
		}
		br.close();
		return list.toNativeArray();
	}
	/**
	 * Each of two given files contains a column of strings. Find the strings that are in parFile but not in set	File.
	 * @param setFile The whole string set.
	 * @param parFile The partial string set.
	 */
	public static void findNotIn(String setFile, String parFile){
		try{
			THashSet<String> strSet = new THashSet<String>(); 
			BufferedReader br = Files.openFileReader(setFile);
			String str = null;
			while ((str=br.readLine())!=null){
				strSet.add(str);
			}
			br.close();
			br = Files.openFileReader(parFile);
			BufferedWriter bw = (BufferedWriter)Files.writeFileWriter(parFile+".rest");
			while ((str=br.readLine())!=null){
				if (!strSet.contains(str))
					bw.write(str+ApplicationSetup.EOL);
			}
			bw.close();
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		System.out.println("Done. Saved in file "+parFile+".rest");
	}
	/**
	 * 
	 * @param filename Data filename
	 * @param keyLocation index of the key column
	 * @param valueLocation index of the value column. index is zero-based.
	 * @return
	 */
	public static TIntIntHashMap loadIntIntHashMap(String filename, int keyLocation, int valueLocation){
		TIntIntHashMap map = new TIntIntHashMap();
		String line = null;
		try{
			BufferedReader br = Files.openFileReader(filename);
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				map.put(Integer.parseInt(tokens[keyLocation]), Integer.parseInt(tokens[valueLocation]));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return map;
	}
	
	/**
	 * 
	 * @param filename Data filename
	 * @param keyLocation index of the key column
	 * @param valueLocation index of the value column. index is zero-based.
	 * @return
	 */
	public static TIntHashSet loadIntHashSet(String filename, int column){
		TIntHashSet ints = new TIntHashSet();
		String line = null;
		try{
			BufferedReader br = Files.openFileReader(filename);
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<column; i++)
					stk.nextToken();
				ints.add(Integer.parseInt(stk.nextToken()));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return ints;
	}
	
	/**
	 * 
	 * @param filename Data filename
	 * @param keyLocation index of the key column
	 * @param valueLocation index of the value column. index is zero-based.
	 * @return
	 */
	public static int[] loadInt(String filename, int column){
		TIntArrayList values = new TIntArrayList();
		String line = null;
		try{
			BufferedReader br = Files.openFileReader(filename);
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				for (int i=0; i<column; i++)
					stk.nextToken();
				values.add(Integer.parseInt(stk.nextToken()));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return values.toNativeArray();
	}
	
	/**
	 * 
	 * @param filename Data filename
	 * @param keyLocation index of the key column
	 * @return
	 */
	public static TIntIntHashMap loadIntSizeHashMap(String filename, int keyLocation){
		TIntIntHashMap map = new TIntIntHashMap();
		String line = null;
		try{
			BufferedReader br = Files.openFileReader(filename);
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				int id = Integer.parseInt(tokens[keyLocation]);
				map.adjustOrPutValue(id, 1, 1);
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return map;
	}
	
	/**
	 * 
	 * @param filename Data filename
	 * @param keyLocation index of the key column
	 * @param valueLocation index of the value column. index is zero-based.
	 * @return
	 */
	public static TIntDoubleHashMap loadIntDoubleHashMap(String filename, int keyLocation, int valueLocation){
		TIntDoubleHashMap map = new TIntDoubleHashMap();
		String line = null;
		try{
			BufferedReader br = Files.openFileReader(filename);
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				map.put(Integer.parseInt(tokens[keyLocation]), Double.parseDouble(tokens[valueLocation]));
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return map;
	}
	
	/**
	 * 
	 * @param filename Data filename
	 * @param keyLocation index of the key column
	 * @param valueLocation index of the value column. index is zero-based.
	 * @return
	 */
	public static TIntDoubleHashMap loadIntDoubleHashMap(String filename, int keyLocation, int valueLocation, int topX){
		TIntDoubleHashMap map = new TIntDoubleHashMap();
		String line = null;
		try{
			BufferedReader br = Files.openFileReader(filename);
			int counter = 0;
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				map.put(Integer.parseInt(tokens[keyLocation]), Double.parseDouble(tokens[valueLocation]));
				if (++counter == topX)
					break;
			}
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
		return map;
	}
	
	/**
	 * 
	 * @param filename
	 * @param index zero based.
	 */
	public static void printMeanValue(String filename, int index){
		try{
			BufferedReader br = Files.openFileReader(filename);
			String str = null;
			double sum = 0; int counter = 0;
			while ((str=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(str);
				for (int i=0; i<index; i++)
					stk.nextToken();
				sum += Double.parseDouble(stk.nextToken());
				counter++;
			}
			br.close();
			System.out.println(filename+", mean of column "+index+": "+sum/counter);
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}		  
	}
}
