/*
 * Terrier - Terabyte Retriever 
 * Webpage: http://terrier.org/
 * Contact: terrier{a.}dcs.gla.ac.uk
 * University of Glasgow - Department of Computing Science
 * http://www.gla.ac.uk/
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
 * The Original Code is ProcessInputStream.java
 *
 * The Original Code is Copyright (C) 2004-2010 the University of Glasgow.
 * All Rights Reserved.
 *
 * Contributor(s):
 *   Craig Macdonald <craigm{a.}dcs.gla.ac.uk> (original contributor)
 */
package org.terrier.utility;

import java.io.InputStream;
import java.io.IOException;


public class ProcessInputStream extends InputStream
{
	protected final InputStream in;
	protected final Process p;
	public ProcessInputStream(String command, String file) throws IOException
	{
		p = Runtime.getRuntime().exec(command + " " + file);
		in = p.getInputStream();
	}
	
	public void close() throws IOException
	{
		in.close();
		try{
			p.waitFor(); //wait for completion
		} catch (InterruptedException ie) {
		} finally {
			p.destroy();
		}
		//int exitCode = p.exitValue();
	}

	public int read() throws IOException
	{
		return in.read();
	}

	public int read(byte[] b, int off, int len) throws IOException
	{
		return in.read(b,off,len);
	}

	public int read(byte[] b) throws IOException
	{
		return in.read(b);
	}

	public int available() throws IOException
	{
		return in.available();
	}

	public void mark(int readlimit) {}
	public void reset() throws IOException {}
	public boolean markSupported() {return false; }
	public long skip(long n) throws IOException
	{
		return in.skip(n);
	}
}
