import java.io.*;
import java.time.LocalTime;
import java.util.logging.*;

public class HFArchiver
{
	final int MAX_BUF_SIZE = 1_000_000_000; // если файл >1G, то используем буфер этого размера иначе буфер размера файла
	private static final String HF_ARCHIVE_EXT = ".hf";
	HFTree tree;


	public void compressFile(String filename) throws IOException
	{
		var log = Logger.getGlobal();
		log.log(Level.INFO, "Analysing file, calculating weights.");
		//printTime("Analysing file, calculating weights.");

		File inFile = new File(filename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		InputStream sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER); // stream только для подсчета весов и далее построения дерева Хаффмана

		tree = new HFTree(sin);
		tree.build();
		byte[] table = tree.getTable();

		sin.close();

		log.log(Level.INFO, "File is analysed. Huffman table is build.");
		//printTime("File is analysed. Huffman table is build.");

		HFFileFormat fileFormat = new HFFileFormat();
		fileFormat.filenameUncomp = filename;
		fileFormat.fileSizeUncomp = fileLen;
		fileFormat.CRC32Value = tree.CRC32Value;
		fileFormat.hfTableSize = (short)table.length;

		String acrFilename = filename.substring(0, filename.lastIndexOf(".")) + HF_ARCHIVE_EXT;
		sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(acrFilename), FILE_BUFFER);

		fileFormat.saveHeader(sout);
		sout.write(table);

		log.log(Level.INFO, "Archive file header and HF table are saved. Starting comression...");
		//printTime("Archive file header and HF table are saved. Starting comression...");

		HFCompressor c = new HFCompressor(sin, sout);
		c.compress(tree);

		sout.close();
		sin.close();

		// записываем в архив размер закодированого файла в байтах
		RandomAccessFile raf = new RandomAccessFile(new File(acrFilename), "rw");
		raf.seek(fileFormat.encodedDataSizePos());
		raf.writeLong(c.encodedBytes);
		raf.close();

		log.log(Level.INFO, "Compression finished.");
		//printTime("Compression finished.");
	}

	public void unCompressFile(String arcFilename) throws IOException
	{
		var log = Logger.getGlobal();
		log.log(Level.INFO, "Loading archive file and HF table.");
		//printTime("Loading archive file and HF table.");

		File inFile = new File(arcFilename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;

		InputStream sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);

		HFFileFormat fileFormat = new HFFileFormat();
		fileFormat.loadHeader(sin);

		tree = new HFTree(sin);
		tree.loadTable(fileFormat.hfTableSize);

		log.log(Level.INFO, "File and HF table are loaded. Starting uncompressing...");
		//printTime("File and HF table are loaded. Starting uncompressing...");

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fileFormat.filenameUncomp), FILE_BUFFER);

		HFUncompressor uc = new HFUncompressor(sin, sout);
		uc.uncompress(tree, fileFormat);

		sout.close();
		sin.close();

		log.log(Level.INFO, "Uncompressing is done.");
		//printTime("Uncompressing is done.");


	}

	/*
	private static void printTime(String msg)
	{
		LocalTime tm = LocalTime.now();
		System.out.format("%tH:%tM:%tS:%tL ", tm,tm,tm,tm);
		System.out.println(msg);
	}

	 */



}
