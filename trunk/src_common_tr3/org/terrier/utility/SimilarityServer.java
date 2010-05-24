
package org.terrier.utility;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.terrier.structures.DistMat;
import org.terrier.utility.ApplicationSetup;


public class SimilarityServer extends UnicastRemoteObject implements Server
{private static final long serialVersionUID = 200603101232L;
	
	public SimilarityServer() throws RemoteException{super();}
	
	public double getSim(DistMat[] mat1, DistMat[] mat2) throws RemoteException{
		double sim = 0;
		int idi = 0;
		int idj = 0;
		double sum = 0;
		double sumi = 0;
		double sumj = 0;
		while(idi<mat1.length && idj<mat2.length){
			if(mat1[idi].termid<mat2[idj].termid){
				sumi = sumi+mat1[idi].tfidf*mat1[idi].tfidf;
				idi++;
			}
			else if(mat1[idi].termid>mat2[idj].termid){
				sumj = sumj+mat2[idj].tfidf*mat2[idj].tfidf;
				idj++;
			}
			else{
				sumi = sumi+mat1[idi].tfidf*mat1[idi].tfidf;
				sumj = sumj+mat2[idj].tfidf*mat2[idj].tfidf;
				sum = sum+mat1[idi].tfidf*mat2[idj].tfidf;
				idi++;
				idj++;
			}
		}
		sim=sum/(Math.sqrt(sumi)*Math.sqrt(sumj));
		return sim;
	}
	
	public double[] getSim(DistMat[] mat, DistMat[][] targetMat) throws RemoteException{
		int N = targetMat.length;
		double[] sim = new double[N];
		for (int i=0; i<N; i++){
			sim[i] = this.getSim(mat, targetMat[i]);
		}
		return sim;
	}
	
	public static void main(String[] args) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		String hostName = null;
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (java.net.UnknownHostException e) {
		}
		final int rmi_port = Integer.parseInt(ApplicationSetup.getProperty("terrier.rmi.port", "1099"));
		String matchName = "//"+hostName+":"+rmi_port+"/DistMatch-"+args[0];
		System.err.println(matchName);
		// initialise the server
		SimilarityServer match = null;
		try {
			
			match = new SimilarityServer();
			Naming.rebind(matchName, (Server)match);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.gc();
	}
	/**
	 * Stop the server.
	 * @throws RemoteException
	 */
	public void stopServer() throws RemoteException{
		System.gc();
		System.err.println("Stop server now.");
		System.exit(0);
	}
}
