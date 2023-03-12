import java.io.*;
import java.util.logging.Logger;

public class RangeAdaptArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	static final Utils.CompressorTypes COMPRESSOR_CODE = Utils.CompressorTypes.AARITHMETIC;
	final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file
	private ModelOrder0 model;

	@Override
	public void compressFiles(String[] filenames) throws IOException
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
	}

	public void compressFile(HFFileRec fr, OutputStream sout) throws IOException
	{
		logger.info(String.format("Starting compression '%s' ...", fr.origFilename));

		File fl = new File(fr.origFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fr.fileSize));

		var model = new ModelOrder0(RangeCompressor.BOTTOM);
		var cData = new CompressData(sin, sout, fr.fileSize);
		var c = new RangeCompressor();

		c.compress(cData, model);

		fr.compressedSize = cData.sizeCompressed;
		fr.CRC32Value = cData.CRC32Value;

		sin.close();

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

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), Utils.getOptimalBufferSize(fr.fileSize));

		var uc = new RangeUncompressor();
		var uData = new UncompressData(sin, sout, fr.compressedSize, fr.fileSize);
		var model = new ModelOrder0(RangeCompressor.BOTTOM);
		uc.uncompress(uData, model);

		if (uData.CRC32Value != fr.CRC32Value)
			logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

		sout.close();

		logger.info(String.format("Extracting '%s' done.", fr.fileName));
	}

	/**
	 * записываем в архив размер закодированного потока в байтах и lastBits для каждого файла в архиве
	 * @param fh Header with correct compressedSize, lastBits and CRC32Values
	 * @param arcFilename Name of the archive
	 * @throws IOException if something goes wrong
	 */
	/*
	@Override
	public void updateHeaders(HFArchiveHeader fh, String arcFilename) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(new File(arcFilename), "rw");

		var offsets = fh.getFieldOffsets();

		int pos = offsets.get(FHeaderOffs.InitialOffset);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			raf.seek(pos + offsets.get(FHeaderOffs.CRC32Value));
			raf.writeLong(fr.CRC32Value);

			raf.seek(pos + offsets.get(FHeaderOffs.compressedSize));
			raf.writeLong(fr.compressedSize);

			// calculate pos of the next file record, for the next loop iteration
			pos = pos + offsets.get(FHeaderOffs.FileRecSize) + fr.fileName.length()*2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}
	 */
}
