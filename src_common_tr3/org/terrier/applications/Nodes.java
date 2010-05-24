/*
 * Created on 31 Jul 2006
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.terrier.applications;

import gnu.trove.THashMap;
import gnu.trove.THashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;

import org.terrier.utility.ApplicationSetup;


public class Nodes {
	
	protected File fNodeList = new File("/users/grad/ben/nodelist.txt");
	
	protected THashSet nodes;
	
	protected THashMap nodeServerMapping;
	
	public boolean isNodeInUse(String node){
		return !nodes.contains(node);
	}
	
	public void printWarningMessage(String node){
		System.out.println("WARNING: NODE "+node+" IS RUNNING SERVER " +
				(String)nodeServerMapping.get(node));
	}
	
	public void addNode(String node, String server){
		if (this.isNodeInUse(node)){
			
		}
		nodes.add(node);
		nodeServerMapping.put(node, server);
		try{
			FileWriter fw = new FileWriter(fNodeList, true);
			fw.write(node + " " + server + ApplicationSetup.EOL);
			fw.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public Nodes() {
		if (!fNodeList.exists()){
			try{
				fNodeList.createNewFile();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			nodes = new THashSet();
			nodeServerMapping = new THashMap();
		}
		else{
			nodes = new THashSet();
			nodeServerMapping = new THashMap();
			try{
				BufferedReader br = new BufferedReader(new FileReader(fNodeList));
				String str = null;
				while ((str=br.readLine())!=null){
					str = str.trim();
					if (str.length() == 0)
						continue;
					StringTokenizer stk = new StringTokenizer(str);
					String node = stk.nextToken();
					String server = stk.nextToken();
					nodes.add(node);
					nodeServerMapping.put(node, server);
				}
				br.close();
			}catch(IOException ioe){
				ioe.printStackTrace();
				System.exit(1);
			}
			
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
