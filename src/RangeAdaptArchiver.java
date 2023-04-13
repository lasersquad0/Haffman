import java.io.*;
import java.nio.file.Path;
import java.util.logging.Logger;

public class RangeAdaptArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file

	RangeAdaptArchiver()
	{
		this(Utils.CompTypes.AARITHMETIC);
	}
	RangeAdaptArchiver(Utils.CompTypes compCode)
	{
		if( (compCode != Utils.CompTypes.ABITARITHMETIC) && (compCode != Utils.CompTypes.AARITHMETIC) && (compCode != Utils.CompTypes.AARITHMETIC32) && (compCode != Utils.CompTypes.AARITHMETIC64) )
			throw new IllegalArgumentException(String.format("Incorrect compressor type '%s' is specified for '%s' compressor.", compCode.toString(), this.getClass().getName()));

		COMPRESSOR_CODE = compCode;
	}

	private Utils.MODE mode()
	{
		if ((COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC32))
			return Utils.MODE.MODE32;
		else
			return Utils.MODE.MODE64;
	}

	@Override
	public String compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		logger.info("Using Adaptive Arithmetic Range compression algorithm.");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames, COMPRESSOR_CODE); // that needs stay here before creating output stream, to avoid creating empty archive files

		String arcFilename = getArchiveFilename(filenames[0]); 	// first parameter in array is name of archive
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(arcFilename), OUTPUT_BUF_SIZE);

		fh.saveHeader(sout);

		for (int i = 0; i < fh.files.size(); i++)
		{
			compressFile(fh.files.get(i), sout);
		}

		sout.close();

		fh.updateHeaders(arcFilename); // it is important to save info into files table that becomes available only after compression

		logger.info("All files are processed.");

		return arcFilename;
	}

	public void compressFile(HFFileRec fr, OutputStream sout) throws IOException
	{
		logger.info(String.format("Starting compression '%s' ...", fr.origFilename));

		if(Utils.BLOCK_MODE)
		{
			BlockCompressable cp;
			if(COMPRESSOR_CODE == Utils.CompTypes.ABITARITHMETIC)
				cp = new RangeBitsCompressor();
			else
				cp = new RangeAdaptCompressor(mode());

			var bm = new BlockManager();
			if (Utils.THREADS_COUNT > 1)
				bm.compressFileInThread(fr, sout, cp);
			else
				bm.compressFile(fr, sout, cp);
		}
		else
		{
			InputStream sin = new BufferedInputStream(new FileInputStream(fr.origFilename), Utils.getOptimalBufferSize(fr.fileSize));

			Compressor cp;
			if(COMPRESSOR_CODE == Utils.CompTypes.ABITARITHMETIC)
				cp = new RangeBitsCompressor();
			else
				cp = new RangeAdaptCompressor(mode());

			var cData = new CompressData(sin, sout, fr.fileSize);
			cp.compress(cData);

			sin.close();

			fr.compressedSize = cData.sizeCompressed;
			fr.CRC32Value = cData.CRC32Value;
		}

		printCompressionDone(fr);
		//logger.info(String.format("Compression '%s' done.", fr.origFilename));
	}

	@Override
	public void unCompressFiles(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s'.", arcFilename));

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			unCompressFile(fr, sin);
		}

		sin.close();

		logger.info("All files are extracted.");
	}

	@Override
	public void unCompressFile(HFFileRec fr, InputStream sin) throws IOException
	{
		logger.info(String.format("Extracting file '%s'...", fr.fileName));

		Path p = Path.of(Utils.OUTPUT_DIRECTORY, fr.fileName);
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(p.toFile()), Utils.getOptimalBufferSize(fr.fileSize));

		if(Utils.BLOCK_MODE)
		{
			var uc = new RangeAdaptCompressor(mode());
			var bm = new BlockManager();
			var udata = new CompressData(sin, sout, fr.compressedSize, fr.fileSize);
			bm.uncompressFile(fr, udata, uc);

			if (udata.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, udata.CRC32Value, fr.CRC32Value));
		}
		else
		{
			Compressor uc;
			if(COMPRESSOR_CODE == Utils.CompTypes.ABITARITHMETIC)
				uc = new RangeBitsCompressor();
			else
				uc = new RangeAdaptCompressor(mode());

			var uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize);
			uc.uncompress(uData);

			if (uData.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));
		}

		sout.close();

		logger.info(String.format("Extracting done '%s'.", fr.fileName));
	}

}

