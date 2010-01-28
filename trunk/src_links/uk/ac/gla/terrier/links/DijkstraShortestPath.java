
package uk.ac.gla.terrier.links;
import gnu.trove.*;
import java.util.Arrays;
import uk.ac.gla.terrier.links.LinkServer;
import uk.ac.gla.terrier.structures.*;
import uk.ac.gla.terrier.utility.TerrierTimer;

/** This class implements Dijkstra's Shortest Path algorithm for finding the minimum number
  * of links between two documents. It has not been fully tested, and has a worst case
  * complexity in that it has to visit each document in the graph.
  * This may be a problem if the link graph is not wholly connected. In particular, 
  * i think it would be advisable to only add the documents to Q that have &gt;0 inlinks.<p>
  * Written by Craig Macdonald and David Hannah, 18/07/2007.<p>
  * For more information, see http://en.wikipedia.org/wiki/Dijkstra's_algorithm
  */

public class DijkstraShortestPath {
	
	/*
 1  function Dijkstra(Graph, source):
 2     for each vertex v in Graph:           // Initializations
 3         dist[v] := infinity               // Unknown distance function from s to v
 4         previous[v] := undefined
 5     dist[source] := 0                     // Distance from s to s
 6     Q := copy(Graph)                      // Set of all unvisited vertices
 7     while Q is not empty:                 // The main loop
 8         u := extract_min(Q)               // Remove best vertex from priority queue                          
 9         for each neighbor v of u:
10             alt = dist[u] + length(u, v)
11             if alt < dist[v]              // Relax (u,v)
12                 dist[v] := alt
13                 previous[v] := u
	 */
	public static void main(String args[]) throws Exception
	{
		Index i = Index.createIndex();
		LinkServer l = new LinkServer();
		int srcDocid = i.getDocumentIndex().getDocumentId(args[0]);
		int targetDocid = i.getDocumentIndex().getDocumentId(args[1]);
		System.err.println("path between " + srcDocid + "and " + targetDocid);
		int dist = shortestPath(l, i.getCollectionStatistics().getNumberOfDocuments(), srcDocid, targetDocid);
		System.err.println("Distance between "+ args[0] + " and " + args[1] + " is " + dist + " links");
	}
	
	public static int shortestPath(LinkServer ls, final int numDocs, final int source, final int Dest) throws Exception
	{
		//TODO: check that Dest has any inlinks. If none, then return -1;
		final int[] dist = new int[numDocs];
		Arrays.fill(dist, Integer.MAX_VALUE);
		dist[source] = 0;
		TIntHashSet Q = new TIntHashSet(numDocs); 
		TerrierTimer timer = new TerrierTimer();
		timer.setTotalNumber(numDocs);
		for(int i=0;i<numDocs;i++)
		{
			Q.add(i);//perhaps we should only add i if i has any inlinks.
		}
		timer.start();
		while(Q.size() > 0)
		{
			timer.setRemainingTime(numDocs - Q.size());
			if (Q.size() % 100 == 0)
				System.err.println(timer.toStringMinutesSeconds());
			int u = getMinDoc(Q.toArray(), dist);
			//System.err.println("Processing u="+u+" which has distance "+ dist[u]);
			if (u == Dest)
			{
				return dist[u] +1;
			}
			Q.remove(u);
			final int[] links_of_u = ls.getOutLinks(u);
			if (links_of_u != null)
			{
				//System.err.println("\twith "+links_of_u.length+"links");
				for(int v : links_of_u)
				{
					int alt = dist[u] +1;
					if (alt < dist[v])
					{
						dist[v] = alt;
						//System.err.println("dist["+v+"] = "+ alt);
						//previous[v] := u
					}
				}
			}
		}
		return -1;		
	}

	/** returns the document with the lowest corresponding value. Known as extract_min(Q) in the
	  * wikipedia pseudo-code. */	
	public static int getMinDoc(int[] docids, final int[] values)//extract_min
	{
		final int len = docids.length;
		final int[] docid_values = new int[len];
		for(int i=0;i<len;i++)
		{
			docid_values[i] = values[docids[i]];
			//if (docid_values[i] < 0)
			//	System.err.println(docid_values[i]);
		}
		uk.ac.gla.terrier.sorting.HeapSortInt.ascendingHeapSort(docid_values, docids);
		return docids[0];
	}
}
