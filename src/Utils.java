import java.io.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Utils
{
	static final String APP_LOGGER_NAME = "ROMALogger";
	public enum MODE {MODE32, MODE64}
	public enum CompTypes { NONE, HUFFMAN, AHUFFMAN, RLE, ARITHMETIC, ARITHMETIC32, ARITHMETIC64, AARITHMETIC, AARITHMETIC32, AARITHMETIC64, ABITARITHMETIC }
	public static String[] CompTypeSymbols = { "NONE", "HUF", "AHUF", "RLE", "ARI", "ARI32", "ARI64", "AARI", "AARI32", "AARI64", "BITARI" };


	public static int BLOCK_SIZE = 1 << 16;
	public static int THREADS_COUNT = 1;
	public static String OUTPUT_DIRECTORY = "";
	public static boolean VERBOSE = false;
	public static boolean BLOCK_MODE = true; // use block mode where possible by default

	private static final int MAX_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла
	private static final int MIN_BUF_SIZE = 1_000; // if accidentally filesize==0, use small buffer


	public static int getOptimalBufferSize(long fileLen)
	{
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		FILE_BUFFER = (fileLen == 0) ? MIN_BUF_SIZE : FILE_BUFFER; // for support of rare zero length files
		return FILE_BUFFER;
	}

	// compares sorted copies of two arrays. be accurate with arrays sizes because copies are created.
	static boolean compareBlocks(byte[] b1, byte[] b2)
	{
		byte[] b11 = Arrays.copyOf(b1, b1.length);
		byte[] b22 = Arrays.copyOf(b2, b2.length);
		Arrays.sort(b11);
		Arrays.sort(b22);
		return Arrays.equals(b11, b22);
	}


	private static final byte[] writeBuffer = new byte[4];
	static void writeInt(OutputStream o, int v) throws IOException
	{
		writeBuffer[0] = (byte)(v >>> 24);
		writeBuffer[1] = (byte)(v >>> 16);
		writeBuffer[2] = (byte)(v >>>  8);
		writeBuffer[3] = (byte)(v);
		o.write(writeBuffer, 0, 4);
	}

	static int readInt(InputStream in) throws IOException
	{
		int ch1 = in.read();
		int ch2 = in.read();
		int ch3 = in.read();
		int ch4 = in.read();
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new EOFException("End of file reached.");
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));
	}

	static void saveBuf(byte[] b, int len, String filename) throws IOException
	{
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(filename), Utils.getOptimalBufferSize(b.length));
		sout.write(b,0, len);
		sout.close();
	}

	static void saveBuf(ByteArrayOutputStream bs, String filename) throws IOException
	{
		OutputStream sout = new BufferedOutputStream(new FileOutputStream(filename), Utils.getOptimalBufferSize(bs.size()));
		bs.writeTo(sout);
		sout.close();
	}

	private static ThreadPoolExecutor pool;
	static synchronized ThreadPoolExecutor getThreadPool()
	{
	//	if(pool == null)
//		{
		ThreadPoolExecutor	pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Utils.THREADS_COUNT - 1);
			pool.setKeepAliveTime(20, TimeUnit.SECONDS); // need for shutdown pool without calling terminate, otherwise app stays in memory after main thread exited.
			pool.allowCoreThreadTimeOut(true);
	//	}

		return pool;
	}

	static int parseNumber(String s) throws NumberFormatException
	{
		int factor = 1;
		s = s.toUpperCase();
		if(s.lastIndexOf("K") > 0)
		{
			s = s.substring(0, s.length()-1);
			factor = 1_024;
		}
		else if(s.lastIndexOf("M") > 0)
		{
			s = s.substring(0, s.length()-1);
			factor = 1024*1024;
		}

		return Integer.parseInt(s) * factor;
	}

	/*
	public static OutputStream buildOutputStream(HFFileRec fr, int blockSize, boolean addBWT, boolean addMTF) throws FileNotFoundException
	{
		OutputStream out = new BufferedOutputStream(new FileOutputStream(fr.fileName), Utils.getOptimalBufferSize(fr.fileSize));
		if(addBWT)
			out = new BWTFilterOutputStream(out, blockSize);
		if(addMTF)
			out = new MTFFilterOutputStream(out, blockSize);

		return out;
	}

	public static InputStream buildInputStream(HFFileRec fr, int blockSize, boolean addBWT, boolean addMTF) throws FileNotFoundException
	{
		InputStream in = new BufferedInputStream(new FileInputStream(fr.origFilename), Utils.getOptimalBufferSize(fr.fileSize));
		if(addBWT)
			in = new BWTFilterInputStream(in, blockSize);
		if(addMTF)
			in = new MTFFilterInputStream(in, blockSize);

		return in;
	}
*/
}

