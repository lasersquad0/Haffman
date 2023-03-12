import java.io.*;
import java.util.HashMap;
import java.util.logging.Logger;

public class ArchiveManager
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);

	private static HashMap<Utils.CompTypes,Archiver> pool = null;

	private static void initPool()
	{
		if(pool != null) return;

		pool = new HashMap<>();
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
	}

	public static void uncompressFiles(String arcFilename) throws IOException
	{
		if(pool == null) initPool();

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			Utils.CompTypes comp = Utils.CompTypes.values()[fr.alg];
			Archiver arc = pool.get(comp);
			arc.unCompressFile(fr, sin);
		}

		sin.close();

		logger.info("All files are extracted.");
	}

}

