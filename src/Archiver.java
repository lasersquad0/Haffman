import java.io.*;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

public abstract class Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private static final String HF_ARCHIVE_EXT = ".hf";
	protected Utils.CompTypes COMPRESSOR_CODE; // see default constructor

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


	public void listFiles(String arcFilename, boolean verbose) throws IOException
	{
		File fl = new File(arcFilename);
		InputStream sin = new BufferedInputStream(new FileInputStream(fl), 2_000);

		HFArchiveHeader fh = new HFArchiveHeader();
		fh.loadHeader(sin);

		System.out.printf("%-46s %18s %15s %7s %6s %7s %18s %13s%n", "File name", "File size", "Compressed", "Blocks", "Alg", "Ratio", "Modified", "CRC32");
		var dt = new SimpleDateFormat("MM/dd/yyyy HH:mm");

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			float ratio = (float)fr.fileSize/(float)fr.compressedSize;
			System.out.printf("%-46s %,18d %,15d %,7d %6s %7.2f %18s %13d%n", truncate(fr.fileName, 46), fr.fileSize, fr.compressedSize, fr.blockCount, getCompressorSym(fr.alg), ratio, dt.format(fr.modifiedDate), fr.CRC32Value);
		}

		if(verbose)
		{
			for (int i = 0; i < fh.files.size(); i++)
			{
				HFFileRec fr = fh.files.get(i);
				System.out.printf("\n---------- List of blocks for '%s' ----------\n", fr.fileName);
				System.out.printf("%-4s %10s %14s %7s %7s\n", "#", "Compressed", "Uncompressed", "Line", "Flags");

				for (int j = 0; j < fr.blockCount; j++)
				{
					int cBlockSize = Utils.readInt(sin);
					int uBlockSize = Utils.readInt(sin);
					assert cBlockSize > 0;
					assert uBlockSize > 0;

					int bwtLineNum = Utils.readInt(sin);
					byte bflags = (byte) sin.read();
					sin.skipNBytes(cBlockSize);

					System.out.printf("%,-4d %10d %14d %7d %7d\n", j, cBlockSize, uBlockSize, bwtLineNum, bflags);
				}
			}
			System.out.println();
		}

		sin.close();
	}

	protected String truncate(String str, int len)
	{
		return (str.length() > len) ? str.substring(0, len - 3) + "..." : str;
	}

	private static String getCompressorSym(byte code)
	{
		for (Utils.CompTypes ct: Utils.CompTypes.values())
		{
			if(code == ct.ordinal())
				return Utils.CompTypeSymbols[ct.ordinal()];
		}
		return "<?>"; // unknown compressor type

	}

	protected void printCompressionDone(HFFileRec fr)
	{
		logger.info("Done compression.");
		logger.info(String.format("%-18s '%s'.", "File name", fr.origFilename));
		logger.info(String.format("%-18s %,d bytes.", "File size", fr.fileSize));
		logger.info(String.format("%-18s %,d bytes.", "Compressed size", fr.compressedSize));
		logger.info(String.format("%-18s %s.", "Compression method", Utils.CompTypeSymbols[fr.alg]));
		logger.info(String.format("%-18s %.2f.", "Ratio", (float)fr.fileSize/(float)fr.compressedSize));
		logger.info(String.format("%-18s %,d", "Blocks", fr.blockCount));
		logger.info(String.format("%-18s %,d bytes", "Block size", Utils.BLOCK_SIZE));
		logger.info("------------------------------");
	}

}
