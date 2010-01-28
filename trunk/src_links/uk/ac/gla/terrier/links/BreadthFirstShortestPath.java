package uk.ac.gla.terrier.links;

import gnu.trove.*;
import java.util.*;

import uk.ac.gla.terrier.links.LinkServer;
import uk.ac.gla.terrier.links.LinkIndex;

/**
 * This class implements a breadth first search of the link graph (outgoing links)
 * too find the shortest path between two documents. As the number of links grows
 * exponentially with distance from the source, it is highly recommended to set a
 * low value of depth when searching. 
 * @author David Hannah
 */
public class BreadthFirstShortestPath {
	
	/** Find the shortest distance between two documents, by breadth first search.
	 * Ensure that a low depth is set.
	 * @param ls The LinkServer containing the link graph 
	 * @param source the source document id
	 * @param dest the target document id
	 * @param depth the maximum depth that we wish to search the link graph to
	 * @return the distance between the source and target documents, or -1 if no
	 * path was found at less than the required depth.	 
	 * @throws Exception
	 */
	public static int ShortestPath(final LinkIndex ls, final int source, final int dest, final int depth) throws Exception{
		return ShortestPath(ls, source, new int[]{dest}, depth)[0];
	}
	
	/** Find the shortest distance between a single source document and several destination documents
	 * , by breadth first search.<br/>
	 * Ensure that a low depth is set.<br/>
	 * Also ensure that no Document Id is not duplicated in the destination array.
	 * @param ls The LinkServer containing the link graph 
	 * @param source the source document id
	 * @param dests the target document id
	 * @param depth the maximum depth that we wish to search the link graph to
	 * @return the distance between the source and target documents, or -1 if no
	 * path was found at less than the required depth.	 
	 * @throws Exception
	 */
	public static int[] ShortestPath(final LinkIndex ls, final int source, final int[] dests, final int depth) throws Exception{
		
		final int[] out = new int[dests.length];
		Arrays.fill(out, -1);
		
		TIntIntHashMap docidIndex = new TIntIntHashMap();
		
		//Put Index of Destination Doc into HashMap
		for(int i=0; i<dests.length; i++){
			docidIndex.put(dests[i], i);
		}
		
		//Check if Source if the Dest
			//return 0;
		if( docidIndex.containsKey(source) ){
			final int val = docidIndex.get(source);
			out[val] = 0;
		}
		
		//Get Number of Out Going Links from Source.
		//Put Out Going on Queue, distance 1
		final int first_links[] = ls.getOutLinks(source);
		if (first_links == null)
			return out;
		
		//this queue will always be sorted by distance
		final Queue<int[]> Q = new LinkedList<int[]>();
		final TIntHashSet seen = new TIntHashSet();
		
		
		for( int docid :  first_links)
			Q.offer(new int[]{docid, 1});	
		//Add Source to Visited set
		seen.add(source);
		int[] dd;
		int lastDist = 0;
		
		while( (dd = Q.poll()) != null ){
			
			//Get doc at head of the Queue
			if (dd[1] == depth)
				break;
			
			final int docid = dd[0];
			if (lastDist != dd[1])
			{
				System.err.println("Now working at distance "+ dd[1]);
				lastDist = dd[1];
			}
			
			//Check if it is dest
			if( docidIndex.containsKey(docid) ){
				final int val = docidIndex.get(docid);
				docidIndex.remove(docid);
				out[val] = dd[1];
			
				if(docidIndex.size() == 0)
					return out;
			}
			if (dd[1] == depth -1)
				continue;
			
			//Get Out Going Links for docid
			final int[] links = ls.getOutLinks(docid);
			if (links != null)
			{
				final int nextDist = dd[1]+1;
				for(int link : links){ //for each outgoing link
					if (! seen.contains(link))
					{
						//Put on Queue, at Dist + 1;
						Q.offer(new int[]{link, nextDist});
						seen.add(link);
					}
				}
			}
		}
		return out;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		int source = Integer.parseInt( args[0] );
		int dest = Integer.parseInt( args[1] );
		int depth = Integer.parseInt( args[2] );
		int dist = ShortestPath( new LinkServer(), source, dest, depth);
		System.out.println("Distance between id "+source + " and id"+dest +	" was "+dist );
	}

}
