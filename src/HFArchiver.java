import java.io.*;

public class HFArchiver
{
	final int MAX_BUF_SIZE = 1_000_000_000; // если файл >1G, то используем буфер этого размера иначе буфер размера файла
	private static final String HF_ARCHIVE_EXT = ".hf";
	//String FILE_NAME;
	String ARCHIVE_FILE_NAME;
	HFTree tree;



	HFArchiver()
	{


	}
/*	HFArchiver(String archiveFilename)
	{
		ARCHIVE_FILE_NAME = archiveFilename;

	}
*/
	public void compressFile(String filename) throws IOException
	{
		File inFile = new File(filename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		InputStream sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER); // stream только для подсчета весов и далее построения дерева Хаффмана

		tree = new HFTree(sin);
		tree.build();
		byte[] table = tree.getTable();

		sin.close();

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

		HFCompressor c = new HFCompressor(sin, sout);
		c.compress(tree);

		sout.close();
		sin.close();

		// записываем в архив размер закодированого файла в байтах
		RandomAccessFile raf = new RandomAccessFile(new File(acrFilename), "rw");
		raf.seek(fileFormat.encodedDataSizePos());
		raf.writeLong(c.encodedBytes);
		raf.close();

	}

	public void unCompressFile(String arcFilename) throws IOException
	{
		File inFile = new File(arcFilename);
		long fileLen = inFile.length();
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;

		InputStream sin = new BufferedInputStream(new FileInputStream(inFile), FILE_BUFFER);

		HFFileFormat fileFormat = new HFFileFormat();
		fileFormat.loadHeader(sin);

		tree = new HFTree(sin);
		tree.loadTable(fileFormat.hfTableSize);

		OutputStream sout = new BufferedOutputStream(new FileOutputStream(fileFormat.filenameUncomp), FILE_BUFFER);

		HFUncompressor uc = new HFUncompressor(sin, sout);
		uc.uncompress(tree, fileFormat);

		sout.close();
		sin.close();

	}


}
