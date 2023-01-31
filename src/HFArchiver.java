import java.io.*;
import java.text.SimpleDateFormat;
import java.util.logging.*;

public class HFArchiver
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	final int MAX_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла
	final int OUTPUT_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла
	final int MIN_BUF_SIZE = 1_000; // if accidentally filesize==0, use small buffer
	private static final String HF_ARCHIVE_EXT = ".hf";

	public void compressFile2(String[] filenames) throws IOException
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

			logger.info(String.format("Analysing file '%s'.", fr.fileName));

			File fl = new File(fr.fileName);

			int BUFFER = getOptimalBufferSize(fr.fileSize);
			InputStream sin1 = new BufferedInputStream(new FileInputStream(fl), BUFFER); // stream только для подсчета весов и далее построения дерева Хаффмана

			InputStream sin2 = new BufferedInputStream(new FileInputStream(fl), BUFFER);

			HFTree tree = new HFTree();
			tree.buildFromStream(sin1);
			sin1.close();

			fr.CRC32Value = tree.CRC32Value;

			logger.info("Starting file compression...");

			tree.saveTable(sout);

			HFCompressData cData = new HFCompressData(sin2, sout, fr.fileSize);
			HFCompressor c = new HFCompressor();

			c.compress(tree, cData);

			fr.compressedSize = cData.sizeCompressed;
			fr.lastBits = cData.lastBits;

			sin2.close();

			logger.info(String.format("Compression '%s' finished.", fr.fileName));
		}

		sout.close();

		updateHeaders(fh, arcFilename);
	}

	public void unCompressFile2(String arcFilename) throws IOException
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

			HFTree tree = new HFTree();
			tree.loadTable(sin);

			OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), getOptimalBufferSize(fr.fileSize));

			logger.info(String.format("Extracting file '%s'...", fr.fileName));

			HFUncompressor uc = new HFUncompressor();
			HFUncompressData uData = new HFUncompressData(sin, sout, fr.compressedSize, fr.lastBits);

			uc.uncompress(tree, uData);

			if (uData.CRC32Value != fr.CRC32Value)
				logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

			sout.close();

			logger.info("Done.");
		}

		sin.close();

		logger.info("All files are extracted.");
	}

	private String getArchiveFilename(String arcParam)
	{
		int index = arcParam.lastIndexOf(".");
		if(index >= 0)
		{
			String ext = arcParam.substring(index);

			if (HF_ARCHIVE_EXT.equals(ext))
				return arcParam;
		}

		return arcParam + HF_ARCHIVE_EXT;
	}

	/**
	 * записываем в архив размер закодированного потока в байтах и lastBits для каждого файла в архиве
	 * @param fh Header with correct compressedSize, lastBits and CRC32Values
	 * @param arcFilename Name of the archive
	 * @throws IOException if something goes wrong
	 */
	private void updateHeaders(HFArchiveHeader fh, String arcFilename) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(new File(arcFilename), "rw");

		var offsets = fh.getFieldOffsets();

		int pos = offsets.get("initial offset");

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);

			raf.seek(pos + offsets.get("CRC32Value"));
			raf.writeLong(fr.CRC32Value);
			raf.seek(pos + offsets.get("compressedSize"));
			raf.writeLong(fr.compressedSize);
			raf.seek(pos + offsets.get("lastBits"));
			raf.writeByte(fr.lastBits);
			pos = pos + offsets.get("HFFileRecSize") + fr.fileName.length()*2; // length()*2 because writeChars() saves each char as 2 bytes
		}

		raf.close();
	}

	private int getOptimalBufferSize(long fileLen)
	{
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		FILE_BUFFER = (fileLen == 0) ? MIN_BUF_SIZE : FILE_BUFFER; // for support of rare zero length files
		return FILE_BUFFER;
	}

	public void listFiles(String arcFilename) throws IOException
	{
		File fl = new File(arcFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), 2_000);

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		System.out.printf("%-49s %18s %15s %7s %18s %13s%n", "File name", "File size", "Compressed", "Ratio", "Modified", "CRC32");
		var dt = new SimpleDateFormat("MM/dd/yyyy HH:mm");

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			float ratio = 100*(float)fr.compressedSize/(float)fr.fileSize;
			System.out.printf("%-49s %,18d %,15d %7.1f%% %18s %13d%n", truncate(fr.fileName, 49), fr.fileSize, fr.compressedSize, ratio, dt.format(fr.modifiedDate), fr.CRC32Value);
		}

		sin.close();
	}

	private String truncate(String str, int len)
	{
		return (str.length() > len) ? str.substring(0, len - 3) + "..." : str;
	}
}
