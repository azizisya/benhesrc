package org.terrier.structures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;

public class TRECResult {
	protected String resultFilename = null;
	
	THashSet queryidSet = new THashSet();
	// A mapping from queryid to TRECQueryResult
	THashMap queryidResultMap = new THashMap();
	
	public class TRECQueryResult{
		String queryid;
		THashSet docnosSet = new THashSet();
		TObjectIntHashMap docnoRankMap = new TObjectIntHashMap();
		TIntObjectHashMap rankDocnoMap = new TIntObjectHashMap();
		String[] rankedDocnos;
		
		public TRECQueryResult(String queryid, String[] docnos, TObjectIntHashMap docnoRankMap){
			this.queryid = queryid;
			this.docnoRankMap = (TObjectIntHashMap)docnoRankMap.clone();
			int N = docnos.length;
			rankedDocnos = new String[N];
			for (int i=0; i<N; i++){
				docnosSet.add(docnos[i]);
				int rank = docnoRankMap.get(docnos[i]);
				rankDocnoMap.put(rank, docnos[i]);
				rankedDocnos[rank] = docnos[i];
			}
		}
		
		public String[] getRankedDocnos(){
			return (String[])rankedDocnos.clone();
		}
		
		public int getNumberOfRetrievedDocuments(){
			return this.docnosSet.size();
		}
	}
	
	public TRECResult(String filename){
		this.resultFilename = filename;
		this.loadResultFile();
	}
	
	public String[] getQueryids(){
		return (String[])queryidSet.toArray(new String[queryidSet.size()]);
	}
	
	public String[] getRankedDocnos(String queryid){
		TRECQueryResult result = (TRECQueryResult)queryidResultMap.get(queryid);
		return result.getRankedDocnos();
	}
	
	public int getNumberOfRetrievedDocuments(String queryid){
		TRECQueryResult result = (TRECQueryResult)queryidResultMap.get(queryid);
		return result.getNumberOfRetrievedDocuments();
	}
	
	protected void loadResultFile(){
		try{
			BufferedReader br = new BufferedReader(new FileReader(new File(this.resultFilename)));
			String preQueryid = "";
			String str = null;
			TObjectIntHashMap docnoRankMap = new TObjectIntHashMap();
			THashSet docnoSet = new THashSet();
			int rank = 0;
			while ((str=br.readLine())!=null){
				str = str.trim();
				if (str.length()==0)
					continue;
				StringTokenizer stk = new StringTokenizer(str);
				String queryid = stk.nextToken();
				stk.nextToken();
				String docno = stk.nextToken();
				if (!queryid.equals(preQueryid)){
					if (preQueryid.length()>0){
						this.queryidSet.add(preQueryid);
						String[] docnos = (String[])docnoSet.toArray(new String[docnoSet.size()]);
						this.queryidResultMap.put(preQueryid, new TRECQueryResult(preQueryid, docnos, docnoRankMap));
						docnos = null;
						docnoRankMap = new TObjectIntHashMap();
						docnoSet = new THashSet();
						rank = 0;
					}
					preQueryid = queryid;
				}
				docnoRankMap.put(docno, rank++);
				docnoSet.add(docno);
			}
			this.queryidSet.add(preQueryid);
			String[] docnos = (String[])docnoSet.toArray(new String[docnoSet.size()]);
			this.queryidResultMap.put(preQueryid, new TRECQueryResult(preQueryid, docnos, docnoRankMap));
			docnos = null;
			docnoRankMap.clear(); docnoRankMap = null;
			docnoSet.clear(); docnoSet = null;
			br.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
