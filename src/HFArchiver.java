import java.io.*;
import java.util.logging.*;

public class HFArchiver
{
	private final static Logger logger = Logger.getLogger("HFLogger");
	final int MAX_BUF_SIZE = 1_000_000_000; // если файл >1G, то используем буфер этого размера иначе буфер размера файла
	private static final String HF_ARCHIVE_EXT = ".hf";
	HFTree tree;

	protected void compressFileInternal(InputStream inTree, HFFileData fileData) throws IOException
	{
		logger.info(String.format("Analysing file '%s', calc weights, building HF table.", fileData.fnUncompressed));

		tree = new HFTree(inTree);
		tree.build();
		byte[] table = tree.getTable();
		inTree.close();

		logger.info("Analysis finished. HF table is build.");

		fileData.CRC32Value = tree.CRC32Value;
		fileData.tableSize = (short)table.length;
		fileData.saveHeader();
		fileData.sout.write(table);

		logger.info("Starting data compression...");

		HFCompressor c = new HFCompressor(fileData.sin, fileData.sout);
		c.compress(tree);
		fileData.sout.flush();
		fileData.fzCompressed = c.encodedBytes;
		fileData.lastBits = c.lastBits;

		logger.info("Compression finished.");

	}

	public void compressFile(String filename) throws IOException
	{
		File inFile = new File(filename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		InputStream sin1 = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER); // stream только для подсчета весов и далее построения дерева Хаффмана

		String acrFilename = filename.substring(0, filename.lastIndexOf(".")) + HF_ARCHIVE_EXT;
		InputStream sin2 = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(acrFilename), FILE_BUFFER);

		HFFileData fileData = new HFFileData();
		fileData.fnUncompressed = filename;
		fileData.fzUncompressed = fileLen;
		fileData.fnCompressed = acrFilename;
		fileData.sin = sin2;
		fileData.sout = sout;
//		fileData.CRC32Value = tree.CRC32Value;
//		fileData.hfTableSize = (short)table.length;

		compressFileInternal(sin1, fileData);

		sout.close();
		sin2.close();
		sin1.close();

		// записываем в архив размер закодированного файла в байтах и lastBits
		RandomAccessFile raf = new RandomAccessFile(new File(acrFilename), "rw");
		raf.seek(fileData.encodedDataSizePos());
		raf.writeLong(fileData.fzCompressed);
		raf.writeByte(fileData.lastBits);
		raf.close();
	}

	public void unCompressFile(String arcFilename) throws IOException
	{
		logger.info(String.format("Loading archive file '%s' and HF table.", arcFilename));

		File inFile = new File(arcFilename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;

		InputStream sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);

		HFFileData fileData = new HFFileData();
		fileData.sin = sin;
		fileData.fnCompressed = arcFilename;
		fileData.loadHeader();

		tree = new HFTree(sin);
		tree.loadTable(fileData.tableSize);

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fileData.fnUncompressed), FILE_BUFFER);
		fileData.sout = sout;

		logger.info("File and HF table are loaded.");
		logger.info("Uncompressing...");

		HFUncompressor uc = new HFUncompressor();
		uc.uncompress(tree, fileData);

		sout.close();
		sin.close();

		logger.info("Uncompressing is done.");
	}

}
