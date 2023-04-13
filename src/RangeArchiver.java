import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;

public class RangeArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file


	RangeArchiver()
	{
		this(Utils.CompTypes.ARITHMETIC);
	}
	RangeArchiver(Utils.CompTypes compCode)
	{
		if( (compCode != Utils.CompTypes.ARITHMETIC) && (compCode != Utils.CompTypes.ARITHMETIC32) && (compCode != Utils.CompTypes.ARITHMETIC64) )
			throw new IllegalArgumentException(String.format("Incorrect compressor type '%s' is specified for '%s' compressor.", compCode.toString(), this.getClass().getName()));

		COMPRESSOR_CODE = compCode;
	}

	private Utils.MODE mode()
	{
		if ((COMPRESSOR_CODE == Utils.CompTypes.ARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.ARITHMETIC32))
			return Utils.MODE.MODE32;
		else
			return Utils.MODE.MODE64;
	}


	@Override
	public String compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		logger.info("Using Arithmetic Range compression algorithm.");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames, COMPRESSOR_CODE); // that needs stay before creating output stream, to avoid creating empty archive files

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
		logger.info(String.format("Analysing file '%s'.", fr.origFilename));

		File fl = new File(fr.origFilename); // TODO возможно хранить File в HFFileRec потому как fillFileRecs тоже создаются File
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), Utils.getOptimalBufferSize(fr.fileSize));

		var model = new ModelOrder0Fixed();
		model.calcWeights(fr.origFilename);
		var cData = new CompressData(sin, sout, fr.fileSize, model);

		logger.info("Starting compression...");

		var c = new RangeCompressor32(mode());
		model.rescaleTo(c.BOTTOM);
		model.saveFreqs(sout);

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;
		fr.CRC32Value = cData.CRC32Value;

		sin.close();

		printCompressionDone(fr);
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

		var model = new ModelOrder0Fixed();
		model.loadFreqs(sin); //здесь читаем таблицы symbols и cumFreq из потока

		Path p = Path.of(Utils.OUTPUT_DIRECTORY, fr.fileName);
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(p.toFile()), Utils.getOptimalBufferSize(fr.fileSize));
		var uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize, model);

		RangeCompressor32 c;
		if((COMPRESSOR_CODE == Utils.CompTypes.ARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.ARITHMETIC32) )
			c = new RangeCompressor32(Utils.MODE.MODE32);
		else
			c = new RangeCompressor32(Utils.MODE.MODE64);

		c.uncompress(uData);

		if (uData.CRC32Value != fr.CRC32Value)
			logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

		sout.close();

		logger.info(String.format("Extracting done '%s'.", fr.fileName));
	}

	/** Extracts archived stream from archive file, saves it as temporary file
	 *  and returns back InputStream to the temporary file.
	 *  this is required for multithreaded uncompressing
	 * @param sin - archive file input stream
	 * @return temporary file as InputStream
	 */
	@Override
	public InputStream extractToTempFile(HFFileRec fr, InputStream sin) throws IOException
	{
		File f = File.createTempFile("range", null);
		int bufSize = Utils.getOptimalBufferSize(fr.compressedSize);
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(f), bufSize);

		var model = new ModelOrder0Fixed();
		model.loadFreqs(sin); // reading symbols and cumFreq tables from the stream
		model.saveFreqs(sout);

		long totalRead = 0;
		while (totalRead < fr.compressedSize)
		{
			int toRead = (int)Math.min((long)bufSize, fr.compressedSize - totalRead); // we need to read EXACTLY fr.compressedSize bytes from a stream
			byte[] b = sin.readNBytes(toRead); // TODO may consume much memory since array b is allocated for each readNBytes call
			sout.write(b);
			totalRead += toRead;
		}

		sout.close();
		f.deleteOnExit();

		return new BufferedInputStream(new FileInputStream(f), bufSize);
	}


}


