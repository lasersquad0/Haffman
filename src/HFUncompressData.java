import java.io.InputStream;
import java.io.OutputStream;

public class HFUncompressData {
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // may be to compare with original size
	long CRC32Value;
	byte lastBits;
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();

	public HFUncompressData(InputStream sin, OutputStream sout, long compressedSize, byte lastBits)
	{
		this.sin = sin;
		this.sout = sout;
		sizeCompressed = compressedSize;
		this.lastBits = lastBits;
	}
}

class HFCompressData {
	long CRC32Value;
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // to show compress progress
	byte lastBits;
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();

	public HFCompressData(InputStream sin, OutputStream sout, long sizeUncomp)
	{
		this.sin = sin;
		this.sout = sout;
		sizeUncompressed = sizeUncomp;
	}
}

class HFCallback
{
	public void compressPercent(int percent)
	{
		System.out.print("\r");
		System.out.printf("Progress %4d%%...", percent);
	}

	public void uncompressPercent(int percent)
	{
		System.out.print("\r");
		System.out.printf("Progress %4d%%...", percent);
	}
}
