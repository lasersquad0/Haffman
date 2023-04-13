import java.io.*;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

public abstract class Archiver
{
	private final static Logger logger = Logger.getLogger(Utils.APP_LOGGER_NAME);
	private static final String HF_ARCHIVE_EXT = ".hf";
	protected Utils.CompTypes COMPRESSOR_CODE; // see default constructor

	abstract String compressFiles(String[] filenames) throws IOException;
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

		System.out.printf("%-46s %18s %15s %7s %10s %6s %7s %18s %13s%n", "File name","File size","Compressed","Blocks","Block size","Alg","Ratio","Modified","CRC32");
		var dt = new SimpleDateFormat("MM/dd/yyyy HH:mm");

		for (int i = 0; i < fh.files.size(); i++)
		{
			HFFileRec fr = fh.files.get(i);
			float ratio = (float)fr.fileSize/(float)fr.compressedSize;
			System.out.printf("%-46s %,18d %,15d %,7d %,10d %6s %7.2f %18s %13d%n", truncate(fr.fileName, 46),fr.fileSize,fr.compressedSize,fr.blockCount,fr.blockSize,getCompressorSym(fr.alg), ratio, dt.format(fr.modifiedDate), fr.CRC32Value);
		}

		if(Utils.VERBOSE)
		{
			for (int i = 0; i < fh.files.size(); i++)
			{
				HFFileRec fr = fh.files.get(i);
				System.out.printf("\n---------- List of blocks for '%s' ----------\n", fr.fileName);
				System.out.printf("%-4s %10s %12s %7s %13s %7s\n", "#", "Compressed","Uncompressed","Ratio", "BWT Line", "Flags");

				for (int j = 0; j < fr.blockCount; j++)
				{
					int cBlockSize = Utils.readInt(sin);
					int uBlockSize = Utils.readInt(sin);
					int bwtLineNum = Utils.readInt(sin);
					byte bflags = (byte) sin.read();

					assert cBlockSize > 0;
					assert uBlockSize > 0;

					sin.skipNBytes(cBlockSize);
					float ratio = (float)fr.blockSize/(float)cBlockSize;
					System.out.printf("%,-4d %10d %12d %7.2f %13d %7d\n", j, cBlockSize, uBlockSize, ratio, bwtLineNum, bflags);
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
		logger.info(String.format("%-18s %,d bytes", "Block size", fr.blockSize));
		logger.info("------------------------------");
	}

	/** Extracts archived stream from archive file, saves it as temporary file
	 *  and returns back InputStream to the temporary file.
	 *  this is required for multithreaded uncompressing
	 * @param sin - archive file input stream
	 * @return temporary file as InputStream
	 */
	public InputStream extractToTempFile(HFFileRec fr, InputStream sin) throws IOException
	{
		File f = File.createTempFile("range", null);
		int bufSize = Utils.getOptimalBufferSize(fr.compressedSize);
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(f), bufSize);

		for (int j = 0; j < fr.blockCount; j++)
		{
			int cBlockSize = Utils.readInt(sin);
			int uBlockSize = Utils.readInt(sin);
			int bwtLineNum = Utils.readInt(sin);
			byte bflags = (byte) sin.read();

			assert cBlockSize > 0;
			assert uBlockSize > 0;
			assert bwtLineNum >= 0;

			byte[] b = sin.readNBytes(cBlockSize); // TODO may consume much memory since array b is allocated for each readNBytes call i.e. for each Block.

			Utils.writeInt(sout, cBlockSize);
			Utils.writeInt(sout, uBlockSize);
			Utils.writeInt(sout, bwtLineNum);
			sout.write(bflags);
			sout.write(b);
		}

		sout.close();
		f.deleteOnExit();

		return new BufferedInputStream(new FileInputStream(f), bufSize);
	}

}
