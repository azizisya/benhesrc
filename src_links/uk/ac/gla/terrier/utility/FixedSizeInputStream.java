package uk.ac.gla.terrier.utility;

import java.io.IOException;
import java.io.FilterInputStream;
import java.io.InputStream;

public class FixedSizeInputStream extends FilterInputStream
{
	/** maximum bytes to read */
	protected final long maxsize;
	/** number of bytes read so far */
	protected long size;

	/** prevent a close() from doing anyhing, like closing the underlying stream */
	protected boolean suppressClose = false;

	/** create a new FixedSizeInputStream, using in as the underlying
	  * InputStream, and maxsize as the maximum number of bytes to read. 
	  * @param in underlying InputStream to read bytes from.
	  * @param maxsize maximum number of bytes to read.
	  */
	public FixedSizeInputStream(InputStream in, long maxsize)
	{
		super(in);
		this.maxsize = maxsize;
	}

	/** Read a single byte from the underlying Reader.
	  * @return The byte read, or -1 if the end of the underlying stream has been reached
	  * or the maximum allowed number of bytes has been read from it.
	  * @throws IOException If an I/O error occurs.
	  */
	public int read() throws IOException
	{
		if (size == maxsize)
			return -1;
		final int by = in.read();
		if (by != -1)
			size++;
		//System.err.println("1. size="+size);
		return by;
	}
	
	/** Read bytes into a portion of an array. 
	  * @param cbuf Destination buffer
	  * @param off Offset at which to start storing bytes
	  * @param len  Maximum number of bytes to read 
	  * @return The number of bytes read, or -1 if the end of the stream has been reached.
	  * @throws IOException If an I/O error occurs in the underlying reader.
	  */
	public int read(byte[] cbuf, int off, int len) throws IOException
	{
		if (size == maxsize)
            return -1;
		if (size + len < maxsize)
		{
			int rtr = in.read(cbuf, off, len);
			size += rtr;
			//System.err.println("2. size="+size +" rtr="+rtr);
			return rtr;
		}
		int rtr = in.read(cbuf, off, (int)(maxsize - size));
		size += rtr;
		//System.err.println("3. size="+size +" rtr="+rtr);
		return rtr;	
	}

    /**
     * Skips n bytes from the stream. If the end of
     * the stream has been reached before reading n bytes
     * then it returns.
     * <B>NB:</B> This method uses read() internally.
     * @param n long the number of characters to skip.
     * @return long the number of characters skipped.
     * @throws IOException if there is any error while
     *       reading from the stream.
     */
    public long skip(long n) throws IOException {
        /* TODO a more efficient implementation could be made */
        long i = 0;
        for (; i < n && size < maxsize; i++) {
            this.read();
        }
        return i;
    }

	/* simple remaining implementation - marks and reset not supported */
	public boolean markSupported() { return false; }
	public void mark(int readAheadLimit) { return; }
	public void reset() throws IOException { return; }
	public void suppressClose() { suppressClose = true; }
	public void close() throws IOException
	{ 
		if (suppressClose)
			return;
		System.err.println("FixedInputStream forcing parent close"); 
		super.close();
	}
}
