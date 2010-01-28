package uk.ac.gla.terrier.applications;
import uk.ac.gla.terrier.utility.ApplicationSetup;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import uk.ac.gla.terrier.querying.SearchRequest;

/** Parallel TREC Querying. Creates a pool of threads to service queries, resulting in faster
  * batch retrieval.
  * <p>
  * <b>Properties</b>
  * <ul>
  * <li><tt>ptq.num.threads</tt> - number of threads to have in pool. </li>
  * </ul>
  * @author Craig Macdonald
  * @version $Revision: 1.1 $
  */
public class ParallelTRECQuerying extends TRECQuerying
{
	static class QueryingThread extends Thread
	{
		TRECQuerying processor;
		public QueryingThread(Runnable r) 
		{
			super(r, "QueryingThread");
			processor = new TRECQuerying();
		}
	
		public QueryingThread()
		{
			this(null);
		}

		public TRECQuerying getProcessor()
		{
			return processor;
		}
	}

	static class QueryingThreadFactory implements ThreadFactory
	{
		public Thread newThread(Runnable r)
		{
			return new QueryingThread(r);
		}
	}
	
	protected ExecutorService queryProcessorPool;
	protected final Object resultsWritingLock = new Object();
	protected final int numThreads;
	protected static final int DEFAULT_NUM_THREADS = 
		Integer.parseInt(ApplicationSetup.getProperty("ptq.num.threads", ""+Runtime.getRuntime().availableProcessors()));

	public ParallelTRECQuerying()
	{
		this(DEFAULT_NUM_THREADS);
	}
	public ParallelTRECQuerying(int numThreads)
	{
		super();
		this.numThreads = numThreads;
		initialisePool();
	}

	protected void initialisePool()
	{
		queryProcessorPool = Executors.newFixedThreadPool(numThreads, new QueryingThreadFactory());
	}

	protected void processQueryAndWrite(final String queryId, final String query, final double cParameter, final boolean c_set) {
		queryProcessorPool./*submit*/execute(new Runnable() {
				public void run() {
					TRECQuerying proc = ((QueryingThread)Thread.currentThread()).getProcessor();
					proc.wModel = wModel;
					proc.mModel = mModel;
					SearchRequest srq = proc.processQuery(queryId, query, cParameter, c_set);
					synchronized(resultsWritingLock)
					{
						if (resultFile == null) {
							method = proc.getManager().getInfo(srq);
							resultFile = getResultFile(method);
						}
						printResults(resultFile, srq);
						matchingCount++;
					}
				}
			});
	}

	protected void finishedQueries()
	{
		this.waitForAllQueries();
		super.finishedQueries();
		this.initialisePool();
	}

	protected void waitForAllQueries()
	{
		queryProcessorPool.shutdown();
		try{
			queryProcessorPool.awaitTermination(86400,TimeUnit.SECONDS);
		} catch (Exception e) {}
	}


	public void close()
	{
		this.waitForAllQueries();
		super.close();
	}

	public static void main(String[] args)
	{
		ParallelTRECQuerying PTQ = (args.length == 1)
			? new ParallelTRECQuerying(Integer.parseInt(args[0]))
			: new ParallelTRECQuerying();
		PTQ.processQueries();
		PTQ.close();
	}
}
