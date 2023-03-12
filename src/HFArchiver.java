import java.io.*;
import java.util.logging.*;

public class HFArchiver extends Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final int OUTPUT_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла

	HFArchiver()
	{
		this(Utils.CompTypes.HUFFMAN);
	}
	HFArchiver(Utils.CompTypes compCode)
	{
		if(compCode != Utils.CompTypes.HUFFMAN)
			throw new IllegalArgumentException(String.format("Incorrect compressor type '%s' is specified for '%s' compressor.", compCode.toString(), this.getClass().getName()));

		COMPRESSOR_CODE = compCode;
	}

	@Override
	public void compressFiles(String[] filenames) throws IOException
	{
		if(filenames.length < 2)
			throw new IllegalArgumentException("There are no files to compress. Exiting...");

		logger.info("Using Huffman compression algorithm.");

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
	}

	private void compressFile(OutputStream sout, HFFileRec fr) throws IOException
	{
		logger.info(String.format("Analysing file '%s'.", fr.origFilename));

		File fl = new File(fr.origFilename);

		int BUFFER = Utils.getOptimalBufferSize(fr.fileSize);
		InputStream sin1 = new BufferedInputStream(new FileInputStream(fl), BUFFER); // stream только для подсчета весов и далее построения дерева Хаффмана

		InputStream sin2 = new BufferedInputStream(new FileInputStream(fl), BUFFER);

		HFTree tree = new HFTree();
		tree.buildFromStream(sin1);
		sin1.close();

		fr.CRC32Value = tree.CRC32Value;

		logger.info("Starting file compression...");

		tree.saveTable(sout);

		CompressData cData = new CompressData(sin2, sout, fr.fileSize, tree);
		HFCompressor c = new HFCompressor();

		c.compress(cData);

		fr.compressedSize = cData.sizeCompressed;

		sin2.close();

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

		HFTree tree = new HFTree();
		tree.loadTable(sin);

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fr.fileName), Utils.getOptimalBufferSize(fr.fileSize));

		var uc = new HFUncompressor();
		var uData = new CompressData(sin, sout, fr.compressedSize, fr.fileSize, tree);

		uc.uncompress(uData);

		if (uData.CRC32Value != fr.CRC32Value)
			logger.warning(String.format("CRC values for file '%s' are not equal: %d and %d", fr.fileName, uData.CRC32Value, fr.CRC32Value));

		sout.close();

		logger.info(String.format("Extracting done '%s'.", fr.fileName));
	}

}

