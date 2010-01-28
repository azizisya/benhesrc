package uk.ac.gla.terrier.structures;
import java.util.Enumeration;
import java.io.IOException;

import org.apache.log4j.Logger;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
public class JDBMHashtable implements PersistentHashtable
{
	/** The logger used */
	private static Logger logger = Logger.getRootLogger();
	private RecordManager database;
	private HTree table;
	private static final String TABLENAME = "JDBMHashtable";
	private static final boolean AutoCommit = false;
	protected String filename;	
	public JDBMHashtable(String Filename)
	{
		filename = Filename;
		try{
			database = RecordManagerFactory.createRecordManager(filename);
			long recid = database.getNamedObject(TABLENAME);
			if (recid==0)
			{
				table = HTree.createInstance(database);
				database.setNamedObject(TABLENAME, table.getRecid());
			}
			else
			{
				table = HTree.load(database, recid);
			}
		}catch (IOException ioe){
			logger.fatal("Couldn't open a JDBM : ",ioe);
		}
	}
	public void clear()
	{
		//try{
			//TODO
		//}catch (IOException ioe){
		//	System.err.println("Couldn't clear JDBM : "+ioe);
		//}
	}
	public boolean containsKey(String key)
	{
		try{
			return table.get(key) != null;
		}catch(IOException ioe){
			return false;
		}
	}
	public boolean equals(Object o)
	{
		if (! (o instanceof JDBMHashtable))
			return false;
		return ((JDBMHashtable)o).filename.equals(filename);
	}
	public String get(String key)
	{
		try{
			return (String)table.get(key);
		}catch(IOException ioe){
			logger.error("JDBM problem : ",ioe);
			return null;
		}
	}
	public boolean isEmpty()
	{
		return size() == 0;
	}
	public void put(String key, String value)
	{
		try{	
			table.put(key, value);
			if (AutoCommit) database.commit();
		}catch (IOException ioe){
			logger.error("JDBM problem : ",ioe);
		}
	}
	public void remove(String key)
	{
		try{
			table.remove(key);
			if (AutoCommit) database.commit();
		}catch (IOException ioe){
			logger.error("JDBM problem : ",ioe);
		}
	}
	public int size()
	{
		int count =0;
		try{
			FastIterator keys = table.keys();
			while(keys.next() != null)
			{
				count ++;
			}
		}catch(Exception e){}
		return count;
	}
	public Enumeration keys()
	{
		return null;//return database.keys();
	}
	public Enumeration values()
	{
		return null;//return database.elements();
	}
	public void close()
	{
		try{
			database.commit();
			database.close();
		}catch (IOException ioe){
			logger.error("Failed to commit changes to JDBM : ",ioe);
		}
	}
	
	public void commit() {
		try {
			database.commit();
		} catch (IOException ioe){
			logger.error("Failed to commit changes to JDBM : ",ioe);
		}
	}
}
