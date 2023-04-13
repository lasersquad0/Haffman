import java.io.*;
import java.nio.file.Path;
import java.util.logging.Logger;

public class HFAdaptArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final int OUTPUT_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла
	HFAdaptArchiver()
	{
		this(Utils.CompTypes.AHUFFMAN);
	}

	HFAdaptArchiver(Utils.CompTypes compCode)
	{
		if(compCode != Utils.CompTypes.AHUFFMAN)
			throw new IllegalArgumentException(String.format("Incorrect compressor type '%s' is specified for '%s' compressor.", compCode.toString(), this.getClass().getName()));

		COMPRESSOR_CODE = compCode;
	}

	@Override
	public String compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		logger.info("Using Adaptive Huffman compression algorithm.");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames, COMPRESSOR_CODE); // that needs to be before creating output stream, to avoid creating empty archive files

		String arcFilename = getArchiveFilename(filenames[0]); 	// first parameter in array is name of archive
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(arcFilename), OUTPUT_BUF_SIZE);

		fh.saveHeader(sout);

		for (int i = 0; i < fh.files.size(); i++)
		{
			compressFile(sout, fh.files.get(i));
		}

		sout.close();

		fh.updateHeaders(arcFilename); // it is important to save info into files table that becomes available only after compression

		logger.info("All files are processed.");

		return arcFilename;
	}

	private void compressFile(OutputStream sout, HFFileRec fr) throws IOException
	{
		logger.info(String.format("Starting compression '%s' ...", fr.origFilename));

		if(Utils.BLOCK_MODE)
		{
			var cp = new AdaptHuffman();
			var bm = new BlockManager();

			if (Utils.THREADS_COUNT > 1)
				bm.compressFileInThread(fr, sout, cp);
			else
				bm.compressFile(fr, sout, cp);
		}
		else
		{
			InputStream sin = new BufferedInputStream(new FileInputStream(fr.origFilename), Utils.getOptimalBufferSize(fr.fileSize));

			var cData = new CompressData(sin, sout, fr.fileSize);
			var c = new AdaptHuffman();

			c.compress(cData);

			fr.compressedSize = cData.sizeCompressed;
			fr.CRC32Value = cData.CRC32Value;

			sin.close();
		}

		printCompressionDone(fr);
		//logger.info(String.format("Compression '%s' finished.", fr.origFilename));
	}

	@Override
	public void unCompressFiles(String arcFilename) throws IOException
	{
		//logger.info(String.format("Loading archive file '%s'.", arcFilename));

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			unCompressFile(fh.files.get(i), sin);
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
			var cp = new AdaptHuffman();
			var bm = new BlockManager();
			var udata = new CompressData(sin, sout, fr.compressedSize, fr.fileSize);
			bm.uncompressFile(fr, udata, cp);

			if (udata.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, udata.CRC32Value, fr.CRC32Value));
		}
		else
		{
			var uc = new AdaptHuffman();
			var uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize);

			uc.uncompress(uData);

			if (uData.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));
		}

		sout.close();

		logger.info(String.format("Extracting done'%s'.", fr.fileName));
	}

}

