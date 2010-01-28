
package uk.ac.gla.terrier.utility;

import java.rmi.Remote;
import java.rmi.RemoteException;

import uk.ac.gla.terrier.structures.DistMat;


/**
 * This class provides a contract for running a matching server in
 * a distributed setting.
 * @author Ben He(ben@dcs.gla.ac.uk)
 * @version $Revision: 1.1 $
 */
public interface Server extends Remote{

	public abstract double getSim(DistMat[] mat1, DistMat[] mat2) throws RemoteException;
	
	public abstract double[] getSim(DistMat[] mat, DistMat[][] targetMat) throws RemoteException;
	
	/**
	 * Stop the server. This will not kill RMI registry process.
	 * @throws RemoteException if there is an error in the level of RMI.
	 */
	public void stopServer() throws RemoteException;
	
}
