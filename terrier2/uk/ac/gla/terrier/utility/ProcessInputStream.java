package uk.ac.gla.terrier.utility;

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
		int exitCode = p.exitValue();
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
