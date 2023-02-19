import java.io.*;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

public abstract class Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private static final String HF_ARCHIVE_EXT = ".hf";

	abstract void compressFiles(String[] filenames) throws IOException;
	abstract void unCompressFiles(String arcFilename) throws IOException;
	abstract void unCompressFile(HFFileRec fr, InputStream sin) throws IOException;
	//abstract void updateHeaders(HFArchiveHeader fh, String arcFilename) throws IOException;

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
			System.out.printf("%-49s %,18d %,15d %4s %7.1f%% %18s %13d%n", truncate(fr.fileName, 49), fr.fileSize, fr.compressedSize, getCompressorSym(fr.alg), ratio, dt.format(fr.modifiedDate), fr.CRC32Value);
		}

		sin.close();
	}

	protected String truncate(String str, int len)
	{
		return (str.length() > len) ? str.substring(0, len - 3) + "..." : str;
	}

	private static String getCompressorSym(byte code)
	{
		switch(code)
		{
			case 1 -> { return "HUF"; }  // Huffman alg
			case 2 -> { return "ARI"; }  // range/arithmetic alg
			case 3 -> { return "RLE"; }  // RLE alg
			case 4 -> { return "ARA"; }  // Adapting Arithmetic alg
			case 5 -> { return "A32"; }  // 32bit Arithmetic alg
			case 6 -> { return "A64"; }  // 64bit Arithmetic alg
			default -> throw new IllegalStateException("Unexpected value: " + code);
		}
	}

	protected void printCompressionDone(HFFileRec fr)
	{
		logger.info(String.format("Done compression '%s'.", fr.origFilename));
		logger.info(String.format("%-15s %,d bytes.", "File size", fr.fileSize));
		logger.info(String.format("%-15s %,d bytes.", "Compressed size", fr.compressedSize));
		logger.info(String.format("%-15s %.1f%%.", "Ratio", 100*(float)fr.compressedSize/(float)fr.fileSize));
		logger.info("------------------------------");
	}

}
