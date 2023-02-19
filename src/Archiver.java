import java.io.*;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

public abstract class Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	final int MAX_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла
	final int MIN_BUF_SIZE = 1_000; // if accidentally filesize==0, use small buffer
	private static final String HF_ARCHIVE_EXT = ".hf";

	abstract void compressFiles(String[] filenames) throws IOException;
	abstract void unCompressFiles(String arcFilename) throws IOException;
	abstract void updateHeaders(HFArchiveHeader fh, String arcFilename) throws IOException;

	protected String getArchiveFilename(String arcParam)
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

	protected int getOptimalBufferSize(long fileLen)
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

		System.out.printf("%-49s %18s %15s %4s %7s %18s %13s%n", "File name", "File size", "Compressed", "Alg", "Ratio", "Modified", "CRC32");
		var dt = new SimpleDateFormat("MM/dd/yyyy HH:mm");

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			float ratio = 100*(float)fr.compressedSize/(float)fr.fileSize;
			System.out.printf("%-49s %,18d %,15d %4c %7.1f%% %18s %13d%n", truncate(fr.fileName, 49), fr.fileSize, fr.compressedSize, fr.alg, ratio, dt.format(fr.modifiedDate), fr.CRC32Value);
		}

		sin.close();
	}

	protected String truncate(String str, int len)
	{
		return (str.length() > len) ? str.substring(0, len - 3) + "..." : str;
	}

	protected void printCompressionDone(HFFileRec fr)
	{
		logger.info(String.format("Done compression '%s'.", fr.origFilename));
		logger.info(String.format("%-16s %,d bytes.", "File size:", fr.fileSize));
		logger.info(String.format("%-16s %,d bytes.", "Compressed size:", fr.compressedSize));
		logger.info(String.format("%-16s %.1f%%.", "Ratio:", 100*(float)fr.compressedSize/(float)fr.fileSize));
		logger.info("------------------------------");
	}

}
