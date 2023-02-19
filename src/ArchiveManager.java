import java.io.*;
import java.util.HashMap;
import java.util.logging.Logger;

public class ArchiveManager
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);

	private static HashMap<Utils.CompressorTypes,Archiver> pool = null;

	private static void initPool()
	{
		if(pool != null) return;

		pool = new HashMap<>();
		pool.put(Utils.CompressorTypes.NONE, null);
		pool.put(Utils.CompressorTypes.HUFFMAN, new HFArchiver());
		pool.put(Utils.CompressorTypes.ARITHMETIC, new RangeArchiver());
		pool.put(Utils.CompressorTypes.RLE, new RLEArchiver());
		pool.put(Utils.CompressorTypes.AARITHMETIC,new RangeAdaptArchiver());
		pool.put(Utils.CompressorTypes.ARITHMETIC32, null);
		pool.put(Utils.CompressorTypes.ARITHMETIC64, null);
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

			Utils.CompressorTypes comp = Utils.CompressorTypes.values()[fr.alg];
			Archiver arc = pool.get(comp);
			arc.unCompressFile(fr, sin);
		}

		sin.close();

		logger.info("All files are extracted.");
	}

}

