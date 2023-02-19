public class Utils
{
	static final String APP_LOGGER_NAME = "ROMALogger";

	public enum CompressorTypes {NONE, HUFFMAN, ARITHMETIC, RLE, AARITHMETIC, ARITHMETIC32, ARITHMETIC64}
	private static final int MAX_BUF_SIZE = 100_000_000; // если файл >100M, то используем буфер этого размера иначе буфер размера файла
	private static final int MIN_BUF_SIZE = 1_000; // if accidentally filesize==0, use small buffer
	public static int getOptimalBufferSize(long fileLen)
	{
		int FILE_BUFFER = (fileLen < MAX_BUF_SIZE) ? (int) fileLen : MAX_BUF_SIZE;
		FILE_BUFFER = (fileLen == 0) ? MIN_BUF_SIZE : FILE_BUFFER; // for support of rare zero length files
		return FILE_BUFFER;
	}

}
