
/*
 * Terrier - Terabyte Retriever
 * Webpage: http://ir.dcs.gla.ac.uk/terrier
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.uk
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is RunIteratorFactory.java.
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Richard McCreadie <richardm{a.}dcs.gla.ac.uk> (original author)
 *   
 */
package org.terrier.utility;
 
/**
 * Generic mutatble Wrapper class  - allows non-mutatable class
 * to be wrapped in mutatable classes, and re-accessed later.
 * @author Richard McCreadie
 * @version $Revision: 1.2 $
 * @since 2.2
 */
public class Wrapper<T> {

	public static class IntObjectWrapper<K> extends Wrapper<K>
	{
		int value;
		public IntObjectWrapper(){}
		
		public IntObjectWrapper(int v, K o)
		{
			super(o);
			value = v;
		}
		
		public int getInt()
		{
			return value;
		}
		public void setInt(int v)
		{
			value = v;
		}
	}
	
	protected T o;
	
	public Wrapper(){
		o=null;
	}
	
	public Wrapper(T O) {
		o=O;
	}
	
	public T getObject() {
		return o;
	}
	
	public void setObject(T O) {
		o=O;
	}
	
	public Wrapper<T> createWrapper(T O){
		Wrapper<T> tempWrapper = new Wrapper<T>();
		tempWrapper.setObject(O);
		return tempWrapper;
	}
}
