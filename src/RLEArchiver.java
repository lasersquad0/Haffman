import java.io.*;
import java.util.logging.*;

public class RLEArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	final int OUTPUT_BUF_SIZE = 100_000_000; // buffer for output archive file

	@Override
	public void compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.fillFileRecs(filenames); // that needs to be before creating output stream, to avoid creating empty archive files

		String arcFilename = getArchiveFilename(filenames[0]); 	// first parameter in array is name of archive
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(arcFilename), OUTPUT_BUF_SIZE);

		fh.saveHeader(sout);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			File fl = new File(fr.fileName);
			InputStream sin = new BufferedInputStream(new FileInputStream(fl), getOptimalBufferSize(fr.fileSize));

			logger.info("Starting file compression...");

			var cData = new CompressData(sin, sout, fr.fileSize);
			RLECompressor c = new RLECompressor();

			c.compress(cData);

			fr.compressedSize = cData.sizeCompressed;
			fr.CRC32Value = cData.CRC32Value;

			sin.close();

			logger.info(String.format("Compression '%s' done.", fr.fileName));
		}

		sout.close();

		updateHeaders(fh, arcFilename);
	}

	@Override
	public void unCompressFiles(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s'.", arcFilename));

		File fl = new File(arcFilename);
		long fileLen = fl.length();
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), getOptimalBufferSize(fileLen));

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), getOptimalBufferSize(fr.fileSize));

			logger.info(String.format("Extracting file '%s'...", fr.fileName));

			RLEUncompressor uc = new RLEUncompressor();
			UncompressData uData = new UncompressData(sin, sout, fr.compressedSize, fr.fileSize);

			uc.uncompress(uData);

			if (uData.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

			sout.close();

			logger.info(String.format("Extracting '%s' done.", fr.fileName));
		}

		sin.close();

		logger.info("All files are extracted.");
	}

	/**
	 * записываем в архив размер закодированного потока в байтах и lastBits для каждого файла в архиве
	 * @param fh Header with correct compressedSize, lastBits and CRC32Values
	 * @param arcFilename Name of the archive
	 * @throws IOException if something goes wrong
	 */
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
			//raf.seek(pos + offsets.get("lastBits"));
			//raf.writeByte(fr.lastBits);
			pos = pos + offsets.get(FHeaderOffs.FileRecSize) + fr.fileName.length()*2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}

}

