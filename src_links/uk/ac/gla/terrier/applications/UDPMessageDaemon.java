package uk.ac.gla.terrier.applications;
import java.io.IOException;
import java.util.HashMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.apache.log4j.Logger;
public class UDPMessageDaemon extends Thread implements EventRegister
{

	/** the logger used for debugging and all the relevant information */
	protected static Logger logger = Logger.getRootLogger();
	/** 
	 * the port that is used for communication
	 * related to reloading the indexes
	 */
	protected static final int PORT = 4000;

	/** used for getting management messages */
	protected DatagramSocket socket = null;

	/** map from event strings to */
	protected HashMap<String, EventReceiver> eventMap = new HashMap<String,EventReceiver>(1);

	/** set to true to stop this daemon */
	protected boolean run = false;

	public UDPMessageDaemon() {
		super("UDPMessageDaemon:"+PORT); 
		this.start();
	}

	/** Register a handler for the given eventName */
	public void registerEvent(String eventName, EventReceiver receiver)
	{
		eventMap.put(eventName, receiver);
	}

	/** stop this daemon */
	public void stopRegister()
	{
		run = false;
	}
	

	/**
	 * the method that is required to be implemented
	 * for the Runnable interface. Receives UDP packages
	 * that start the reloading of indexes from a 
	 * given location.
	 */
	public void run() {
		run = true;
		try{
			socket = new DatagramSocket(PORT);
		} catch (SocketException se) {}
		while (run) {
			try {
				byte[] buf = new byte[4096];

				// receive request
				if (socket == null)
				{
					throw new IOException("Socket not defined");
				}
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				logger.debug("waiting for packet");
				socket.receive(packet);
				
				// get the data
				String input = new String(packet.getData(), 0, packet.getLength());
				logger.debug("packet received: " + input);
				//final int firstSpace = input.indexOf(" ");
				String data[] = input.split("\\s");	
				EventReceiver er = eventMap.get(data[0]);
				if (er != null)
					er.invokeEvent(data[0], data);
				else
					logger.warn("Event name "+data[0]+" is not registered");
				/*
				if (input.startsWith("RELOAD")) {
					String[] inputData = input.split("\\s+");
					synchronized(this) {
						//invalidating the query cache and reloading the 
						//data for the query manager
						//queryCache.clear();
						queryingManager.reload(inputData);
					}
				}*/

			} catch (IOException e) {
				logger.debug("ioexception while handling management socket.", e);
				if (socket != null)
					socket.close();
				try {
					socket = new DatagramSocket(PORT);
				} catch(SocketException se) {
					logger.debug("socket exception while re-opening management socket.", se);
					try{Thread.sleep(5000);}catch (Exception ex){}
				}
			}
		}
		socket.close();
		logger.info("UDPMesageDaemon shutdown");
	}
}
