import java.io.*;
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
		if( (compCode != Utils.CompTypes.AARITHMETIC) && (compCode != Utils.CompTypes.AARITHMETIC32) && (compCode != Utils.CompTypes.AARITHMETIC64) )
			throw new IllegalArgumentException(String.format("Incorrect compressor type '%s' is specified for '%s' compressor.", compCode.toString(), this.getClass().getName()));

		COMPRESSOR_CODE = compCode;
	}

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


		//InputStream sin = Utils.buildInputStream(fr, Utils.LOCK_SIZE,true, true);
		//InputStream sin0 = new BufferedInputStream(new FileInputStream(fr.origFilename), Utils.getOptimalBufferSize(fr.fileSize));

		/*CompressData cData;

		if((COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC32))
		{
			var c = new RangeAdaptCompressor(Utils.MODE.MODE32);
			cData = new CompressData(sin, sout, fr.fileSize);
			c.compress(cData);
		}
		else
		{
			var c = new RangeAdaptCompressor(Utils.MODE.MODE64);
			cData = new CompressData(sin, sout, fr.fileSize);
			c.compress(cData);
		}
		sin.close();

		 */


		RangeAdaptCompressor cp;
		if((COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC32))
			cp = new RangeAdaptCompressor(Utils.MODE.MODE32);
		else
			cp = new RangeAdaptCompressor(Utils.MODE.MODE64);

		var bm = new BlockManager();
		bm.compressFile(fr, sout, cp);

//		fr.compressedSize = cData.sizeCompressed;
//		fr.CRC32Value = cData.CRC32Value;

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
		//OutputStream sout = Utils.buildOutputStream(fr, BLOCK_SIZE, true, true);

//		CompressData uData;
		RangeAdaptUncompressor uc;
		if((COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC) || (COMPRESSOR_CODE == Utils.CompTypes.AARITHMETIC32) )
		{
			uc = new RangeAdaptUncompressor(Utils.MODE.MODE32);
//			uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize);
//			uc.uncompress(uData);
		}
		else
		{
			uc = new RangeAdaptUncompressor(Utils.MODE.MODE64);
//			uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize);
//			uc.uncompress(uData);
		}

		var bm = new BlockManager();
		bm.uncompressFile(fr, sin, sout, uc);

/*		if (uData.CRC32Value != fr.CRC32Value)
			logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

		sout.close();
*/
		logger.info(String.format("Extracting done '%s'.", fr.fileName));
	}

}

