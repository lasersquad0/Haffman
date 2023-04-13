import java.io.*;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ArchiveManager
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);

	private static HashMap<Utils.CompTypes,Archiver> pool = null;

	private static void initArchiversPool()
	{
		if(pool == null)
			pool = new HashMap<>();
		else
			pool.clear();

		pool.put(Utils.CompTypes.NONE, null);
		pool.put(Utils.CompTypes.HUFFMAN, new HFArchiver());
		pool.put(Utils.CompTypes.AHUFFMAN, new HFAdaptArchiver());
		pool.put(Utils.CompTypes.RLE, new RLEArchiver());
		pool.put(Utils.CompTypes.ARITHMETIC, new RangeArchiver());
		pool.put(Utils.CompTypes.ARITHMETIC32, new RangeArchiver(Utils.CompTypes.ARITHMETIC32));
		pool.put(Utils.CompTypes.ARITHMETIC64, new RangeArchiver(Utils.CompTypes.ARITHMETIC64));
		pool.put(Utils.CompTypes.AARITHMETIC,  new RangeAdaptArchiver());
		pool.put(Utils.CompTypes.AARITHMETIC32, new RangeAdaptArchiver(Utils.CompTypes.AARITHMETIC32));
		pool.put(Utils.CompTypes.AARITHMETIC64, new RangeAdaptArchiver(Utils.CompTypes.AARITHMETIC64));
		pool.put(Utils.CompTypes.ABITARITHMETIC, new RangeAdaptArchiver(Utils.CompTypes.ABITARITHMETIC));
	}

	public static void uncompressFiles(String arcFilename) throws IOException
	{
		initArchiversPool();

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		boolean useThreads = (Utils.THREADS_COUNT > 1) && (fh.files.size() > 1);

		if(useThreads) // we have several files in archive, uncompress them in threads
		{
			ThreadPoolExecutor threadsPool = Utils.getThreadPool(); //(ThreadPoolExecutor) Executors.newFixedThreadPool(Utils.THREADS_COUNT - 1);

			for (int i = 0; i < fh.files.size(); i++)
			{
				HFFileRec fr = fh.files.get(i);
				Archiver arc = pool.get(Utils.CompTypes.values()[fr.alg]);

				InputStream sin2 = arc.extractToTempFile(fr, sin);
				threadsPool.submit(new Runnable() {
					@Override
					public void run()
					{
						try
						{
							arc.unCompressFile(fr, sin2);
						} catch (IOException e)
						{
							throw new RuntimeException(e);
						}
					}
				});
			}

			threadsPool.shutdown();

			try
			{
				while(!threadsPool.awaitTermination(1, TimeUnit.SECONDS)); // wait till all tasks have been finished
			} catch (InterruptedException e)
			{
				throw new RuntimeException(e);
			}

		}
		else // either we have one file in archive or THREADS_COUNT==1
		{
			for (int i = 0; i < fh.files.size(); i++)
			{
				HFFileRec fr = fh.files.get(i);
				Archiver arc = pool.get(Utils.CompTypes.values()[fr.alg]);
				arc.unCompressFile(fr, sin);
			}
		}

		sin.close();

		logger.info("All files are extracted.");
	}

}

