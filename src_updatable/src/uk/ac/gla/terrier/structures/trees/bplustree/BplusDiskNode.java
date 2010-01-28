package uk.ac.gla.terrier.structures.trees.bplustree;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Constructor;

import uk.ac.gla.terrier.structures.*;

public abstract class BplusDiskNode {

	
	public abstract FilePosition getFilePosition();
	
	public abstract int sizeInBytes();
	
	public abstract void toBytes(DataOutput dao);
	
	public abstract void load(DataInput dai);
	
	//public abstract void setNext(byte[] bytes);
	protected Object createObject(Class cls)
	{
		try {

	            Constructor ct = cls.getConstructor((Class[])null);
	            return ct.newInstance((Object[])null);
	         }
	         catch (Throwable e) {
	        	 e.printStackTrace();
	            return null;
	         }
	}
	
	protected static int getObjectsSize(Class cl)
	{
		//You have to provide a static sizeInBytes method in any class
		//that extends BplusKey or BplusValue
		try {
			  String methodName = "sizeInBytes";

			  Class  arguments[] = new Class[] { };
			  // get the method
			  java.lang.reflect.Method objMethod = cl.getMethod(methodName, arguments);
			  
			  //convert the returned object to an INteger then an int
			  Object result = objMethod.invoke(null, (Object[])arguments);
			  Integer retval = (Integer)result;
			 
			  
			  return retval.intValue();
			  } 
			catch (Exception e) {System.err.println("Error while calculating an objects size: ");
			    e.printStackTrace();
			    return -777;
			  }
	}
}
