package uk.ac.gla.terrier.utility;

import java.io.IOException;
import java.io.FilterReader;
import java.io.Reader;

/** Reader filter class that stops reading after a given number of characters.
  * Beware when using this class that while this class will never use more characters than
  * permitted from the underlying Reader, that underlying Readers may buffer internally, causing
  * unused characters not to be returned to the underlying InputStream */
public class FixedSizeReader extends FilterReader
{
	/** maximum characters to read */
	protected final long maxsize;
	/** number of characters read so far */
	protected long size;
	/** create a new FixedSizeReader, using in as the underlying
	  * Reader, and maxsize as the maximum number of characters to read. 
	  * @param in underlying Reader to read characters from.
	  * @param maxsize maximum number of bytes to read.
	  */
	public FixedSizeReader(Reader in, long maxsize)
	{
		super(in);
		this.maxsize = maxsize;
	}

	/** Read a single character from the underlying Reader.
	  * @return The character read, as an integer in the range 0 to 65535 (0x00-0xffff), or -1 if the end of the underlying stream has been reached
	  * or the maximum allowed number of characters has been read from it.
	  * @throws IOException If an I/O error occurs.
	  */
	public int read() throws IOException
	{
		if (size == maxsize)
			return -1;
		final int ch = in.read();
		if (ch != -1)
			size++;
		return ch;
	}
	
	/** Read characters into a portion of an array. 
	  * @param cbuf Destination buffer
	  * @param off Offset at which to start storing characters
	  * @param len  Maximum number of characters to read 
	  * @return The number of characters read, or -1 if the end of the stream has been reached.
	  * @throws IOException If an I/O error occurs in the underlying reader.
	  */
	public int read(char[] cbuf, int off, int len) throws IOException
	{
		if (size == maxsize)
            return -1;
		if (size + len < maxsize)
		{
			int rtr = in.read(cbuf, off, len);
			size += rtr;
			return rtr;
		}
		int rtr = in.read(cbuf, off, (int)(maxsize - size));
		size += rtr;
		return rtr;	
	}

    /**
     * Skips n characters from the stream. If the end of
     * the stream has been reached before reading n characters,
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
	public void mark(int readAheadLimit) throws IOException { return; }
	public void reset() throws IOException { return; }
	
}
