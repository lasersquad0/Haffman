import java.io.InputStream;
import java.io.OutputStream;

public class UncompressData {
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // may be to compare with original size
	long CRC32Value;
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();

	public UncompressData(InputStream sin, OutputStream sout, long cSize, long ucSize)
	{
		this.sin = sin;
		this.sout = sout;
		sizeCompressed = cSize;
		sizeUncompressed = ucSize;
	}
}

class CompressData {
	long CRC32Value;
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // to show compress progress
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();

	public CompressData(InputStream sin, OutputStream sout, long uncompressedSize)
	{
		this.sin = sin;
		this.sout = sout;
		sizeUncompressed = uncompressedSize;
	}
}

class HFCallback
{
	public void start()
	{

	}

	public void heartBeat(int percent)
	{
		System.out.print("\r");
		System.out.printf("Progress %4d%%...", percent);
	}

	public void finish()
	{
		System.out.print("\r                 \r");
	}
}
