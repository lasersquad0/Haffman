import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

	// this constructor used mostly in Compress methods when model is NOT adaptive (fixed model)
	public CompressData(InputStream sin, OutputStream sout, long uSize, ModelOrder0 m)
	{
		this(sin, sout, uSize);
		model = m;
	}

	// this constructor used mostly in Compress Huffman methods
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

	// this constructor used mostly in Uncompress methods
	public CompressData(InputStream sin, OutputStream sout, long cSize, long uSize, ModelOrder0 m)
	{
		this(sin, sout, cSize, uSize);
		model = m;
	}

	// this constructor used mostly in Uncompress methods
	public CompressData(InputStream sin, OutputStream sout, long cSize, long uSize, HFTree t)
	{
		this(sin, sout, cSize, uSize);
		tree = t;
	}
}

class HFCallback
{
	static HashMap<String, Integer> callers = new HashMap<>();
	StringBuilder sb = new StringBuilder();
	public synchronized void start()
	{
		callers.put(Thread.currentThread().getName(), 0);
		//System.out.println("START Uncompressing " + Thread.currentThread().getName());
	}

	public synchronized void heartBeat(int percent)
	{
		callers.put(Thread.currentThread().getName(), percent);

		sb.setLength(0);
		sb.append("Progress:");
		for (Map.Entry<String, Integer> entry : callers.entrySet())
		{
			sb.append("[");
			sb.append(entry.getKey());
			sb.append(":");
			sb.append(entry.getValue());
			sb.append("%]");
		}

		System.out.print("\r");
		System.out.print(sb);
		//System.out.printf("Progress %4d%%...", percent);
	}

	public synchronized void finish()
	{
		//callers.remove(Thread.currentThread().getName());

		String spaces = " ";
		spaces = spaces.repeat(sb.length());

		System.out.print("\r");
		System.out.print(spaces);
		System.out.print("\r\r");
	}
}
