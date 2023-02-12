import java.io.InputStream;
import java.io.OutputStream;

public class HFUncompressData extends UncompressData
{
	byte lastBits;

	public HFUncompressData(InputStream sin, OutputStream sout, long cSize, long ucSize, byte lastBits)
	{
		super(sin, sout, cSize, ucSize);
		this.lastBits = lastBits;
	}
}

class HFCompressData extends CompressData
{
	byte lastBits;

	public HFCompressData(InputStream sin, OutputStream sout, long sizeUncomp)
	{
		super(sin, sout, sizeUncomp);
	}
}

class RangeCompressData extends CompressData
{
	long[] cumFreq;
	int[] symbol_to_freq_index;

	public RangeCompressData(InputStream sin, OutputStream sout, long sizeUncomp, long[] freq, int[] index)
	{
		super(sin, sout, sizeUncomp);
		cumFreq = freq;
		symbol_to_freq_index = index;
	}
}

class RangeUncompressData extends UncompressData
{
	long[] cumFreq;
	int[] symbols;

	public RangeUncompressData(InputStream sin, OutputStream sout, long cSize, long ucSize, long[] freq, int[] sym)
	{
		super(sin, sout, cSize, ucSize);
		cumFreq = freq;
		symbols = sym;
	}

}
