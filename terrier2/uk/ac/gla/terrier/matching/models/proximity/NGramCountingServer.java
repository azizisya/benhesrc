package uk.ac.gla.terrier.matching.models.proximity;

import java.rmi.RemoteException;
import java.rmi.Remote;

public class NGramCountingServer implements Remote{
	protected String id;
	
	/**
	 * Set the server id.
	 * @param id The server id.
	 * @throws RemoteException
	 */
	public void setId(String id) throws RemoteException{
		this.id = id;
	}
		
	/**
	 * Get the id of the server.
	 * @return The server id.
	 * @throws RemoteException
	 */
	public String getId() throws RemoteException{
		return id;		
	}
	
	protected int[][] postings1;
	
	protected int[][] postings2;
	
	protected int docid;
	
	protected ProximityModel proxModel;
	
	protected int wSize;
	
	protected int docLength;
	
	public double matchingNGram;

}
