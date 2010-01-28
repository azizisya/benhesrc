package uk.ac.gla.terrier.learning.structures;

import java.io.BufferedReader;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import uk.ac.gla.terrier.utility.Files;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;

public class WekaPredictResults {
	protected static Logger logger = Logger.getRootLogger();
	protected class Entry implements Comparable<Entry>{
		String key;
		// lable of the instance
		int label;
		// the predicted label
		int predLabel;
		// confidence value
		double conf;
		
		int rank=0;
		
		public void setRank(int value){
			this.rank = value;
		}
		
		public int getRank(){
			return this.rank;
		}
		
		public Entry(String key, int label, int predLabel, double conf) {
			super();
			this.key = key;
			this.label = label;
			this.predLabel = predLabel;
			this.conf = conf;
		}
		
		public int compareTo(Entry o) {
			if (o.conf > this.conf) {
				return 1;
			}
			else if (o.conf < this.conf) {
				return -1;
			}
			else if (o.getRank() > this.conf){
				return -1;
			}else
				return 1;
		}
	}
	
	protected THashMap<String, Entry> entryMap;
	
	protected void createEntryMap(){
		this.entryMap = new THashMap<String, Entry>();
	}
	
	public WekaPredictResults(String predictFilename, String idMapFilename){
		this.loadPrediction(predictFilename, idMapFilename);
	}
	
	protected Entry[] getPredictedEntries(int predictedLabel){
		THashSet<Entry> entries = new THashSet<Entry>();
		String[] keys = entryMap.keySet().toArray(new String[entryMap.size()]);
		for (int i=0; i<keys.length; i++){
			Entry entry = entryMap.get(keys[i]);
			if (entry.predLabel == predictedLabel)
				entries.add(entry);
		}
		return entries.toArray(new Entry[entries.size()]);
	}
	
	public String[] getPredictedEntriesDescending(int predictedLabel){
		THashSet<Entry> entries = new THashSet<Entry>();
		String[] keys = entryMap.keySet().toArray(new String[entryMap.size()]);
		for (int i=0; i<keys.length; i++){
			Entry entry = entryMap.get(keys[i]);
			if (entry.predLabel == predictedLabel)
				entries.add(entry);
		}
		Entry[] entryArray = entries.toArray(new Entry[entries.size()]);
		Arrays.sort(entryArray);
		keys = new String[entryArray.length];
		for (int i=0; i<keys.length; i++)
			keys[i] = entryArray[i].key;
		return keys;
	}
	
	protected void loadPrediction(String predictFilename, String idMapFilename){
		// load id map
		logger.debug("Loading id map from "+idMapFilename);
		TIntObjectHashMap<String> idmap = new TIntObjectHashMap<String>();
		try{
			String line = null;
			BufferedReader br = Files.openFileReader(idMapFilename);
			while ((line=br.readLine())!=null){
				String[] tokens = line.split(" ");
				idmap.put(Integer.parseInt(tokens[0]), tokens[1]);
			}
			br.close(); br = null;
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		logger.debug("Done. "+idmap.size()+" ids loaded.");
		
		logger.debug("Loading entries from "+predictFilename);
		this.createEntryMap();
		try{
			String line = null;
			BufferedReader br = Files.openFileReader(predictFilename);
			while ((line=br.readLine())!=null){
				StringTokenizer stk = new StringTokenizer(line);
				boolean correct = (stk.countTokens()==4);
				int inst = Integer.parseInt(stk.nextToken());
				int label = Integer.parseInt(stk.nextToken().split(":")[1]);// 1:1 or 2:-1
				int predLabel = Integer.parseInt(stk.nextToken().split(":")[1]);// 1:1 or 2:-1
				if (!correct)
					stk.nextToken(); // skip + if incorrect
				double conf = Double.parseDouble(stk.nextToken()); // get confidence value
				entryMap.put(idmap.get(inst), new Entry(idmap.get(inst), label, predLabel, conf));
			}
			br.close(); br = null;
		}catch(IOException e){
			e.printStackTrace();
			System.exit(1);
		}
		idmap.clear(); idmap = null;
		logger.debug("Done. "+entryMap.size()+" entries loaded.");
	}
}
