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

/*
	protected void compressFileInternal(HFTree tree, HFCompressData cData) throws IOException
	{
		logger.info("Analysis finished. HF table is build.");

		tree.saveTable(cData.sout);

		logger.info("Starting data compression...");

		HFCompressor c = new HFCompressor();
		c.compress(tree, cData);

		logger.info("Compression finished.");
	}
*/
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
/*
	public void compressFile(String filename) throws IOException
	{
		logger.info(String.format("Analysing file '%s', calc weights, building HF table.", filename));

		File inFile = new File(filename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		//FILE_BUFFER = (fileLen == 0) ? MIN_BUF_SIZE : (int)fileLen; // for future support of zero length files
		InputStream sin1 = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER); // stream только для подсчета весов и далее построения дерева Хаффмана
		InputStream sin2 = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);

		String acrFilename = filename.substring(0, filename.lastIndexOf(".")) + HF_ARCHIVE_EXT;
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(acrFilename), FILE_BUFFER);

		HFTree tree = new HFTree();
		tree.buildFromStream(sin1);

		HFArchiveHeader fileData = new HFArchiveHeader();
		fileData.fnameUncompressed = filename;
		fileData.sizeUncompressed = fileLen;
		fileData.fnameCompressed = acrFilename;
		fileData.CRC32Value = tree.CRC32Value;
		fileData.saveHeader(sout);

		HFCompressData cData = new HFCompressData(sin2, sout);
		compressFileInternal(tree, cData);

		sout.close();
		sin2.close();
		sin1.close();

		// записываем в архив размер закодированного потока в байтах и lastBits
		RandomAccessFile raf = new RandomAccessFile(new File(acrFilename), "rw");
		raf.seek(14);
		raf.writeLong(cData.sizeCompressed);
		raf.writeByte(cData.lastBits);
		raf.close();
	}
*/
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

	private int getOptimalBufferSize(long fileLen)
	{
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		FILE_BUFFER = (fileLen == 0) ? MIN_BUF_SIZE : FILE_BUFFER; // for support of rare zero length files
		return FILE_BUFFER;
	}
/*
	public void unCompressFile(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s' and HF table.", arcFilename));

		File inFile = new File(arcFilename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;

		InputStream sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);

		HFArchiveHeader fileData = new HFArchiveHeader();
		fileData.loadHeader(sin);

		HFTree tree = new HFTree();
		tree.loadTable(sin);

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fileData.fnameUncompressed), FILE_BUFFER);

		logger.info("File and HF table are loaded.");
		logger.info("Uncompressing...");

		HFUncompressor uc = new HFUncompressor();
		HFUncompressData uData = new HFUncompressData(sin, sout, fileData.sizeCompressed, fileData.lastBits);

		uc.uncompress(tree, uData);

		long v = fileData.CRC32Value;
		if(uData.CRC32Value != v)
			logger.warning(String.format("CRC values are not equal: %d and %d", uData.CRC32Value, v));

		sout.close();
		sin.close();

		logger.info("Uncompressing is done.");
	}
*/
	public void listFiles(String arcFilename) throws IOException
	{
		File fl = new File(arcFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), 2_000);

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		System.out.printf("%-70s %20s %18s %10s %20s %15s%n", "File name", "File size", "Compressed", "Ratio", "Modified", "CRC32");
		var dt = new SimpleDateFormat("MM/dd/yyyy HH:mm");

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			float ratio = 100*(float)fr.compressedSize/(float)fr.fileSize;
			System.out.printf("%-70s %,20d %,18d %10.1f%% %20s %15d%n", fr.fileName, fr.fileSize, fr.compressedSize, ratio, dt.format(fr.modifiedDate), fr.CRC32Value);
		}

		sin.close();
	}
}
