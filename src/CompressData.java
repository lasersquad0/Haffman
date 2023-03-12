import java.io.InputStream;
import java.io.OutputStream;

class CompressData {
	long sizeCompressed; // to know where to stop decoding
	long sizeUncompressed; // to show compress progress
	long CRC32Value;
	InputStream sin;
	OutputStream sout;
	HFCallback cb = new HFCallback();
	HFTree tree;
	ModelOrder0 model;

	// this constructor used in Compress methods mostly
	public CompressData(InputStream sin, OutputStream sout, long uSize)
	{
		this.sin = sin;
		this.sout = sout;
		sizeUncompressed = uSize;
	}

	// this constructor used in Uncompress methods mostly
	public CompressData(InputStream sin, OutputStream sout, long uSize, ModelOrder0 m)
	{
		this(sin, sout, uSize);
		model = m;
	}

	public CompressData(InputStream sin, OutputStream sout, long uSize, HFTree t)
	{
		this(sin, sout, uSize);
		tree = t;
	}

	// this constructor used in Uncompress methods mostly
	public CompressData(InputStream sin, OutputStream sout, long cSize, long uSize)
	{
		this(sin, sout, uSize);
		sizeCompressed = cSize;
	}

	public CompressData(InputStream sin, OutputStream sout, long cSize, long uSize, ModelOrder0 m)
	{
		this(sin, sout, cSize, uSize);
		model = m;
	}

	public CompressData(InputStream sin, OutputStream sout, long cSize, long uSize, HFTree t)
	{
		this(sin, sout, cSize, uSize);
		tree = t;
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
		System.out.print("\r                 \r\r");
	}
}
