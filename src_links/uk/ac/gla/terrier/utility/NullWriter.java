package uk.ac.gla.terrier.utility;
import java.io.Writer;
import java.io.IOException;

/** A dummy writer class. All character/strings written to this class are lost.
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
public class NullWriter extends Writer
{
	public NullWriter() {}

	public Writer append(char c)
			  throws IOException
	{	return this; }

	public void flush()
					throws IOException
	{}

	public void close()
					throws IOException
	{}

	public Writer append(CharSequence csq,
					 int start,
					 int end)
			  throws IOException
	{ return this; }

	public Writer append(CharSequence csq)
			  throws IOException
	{ return this; }

	public void write(String str,
				  int off,
				  int len)
		   throws IOException
	{}
	
	public void write(String str)
		   throws IOException
	{}

	public void write(char[] cbuf,
						   int off,
						   int len)
					throws IOException
	{}

	public void write(char[] cbuf)
		   throws IOException
	{}

	public void write(int c)
		   throws IOException
	{}

}
