import java.io.IOException;
import java.util.logging.Logger;
import java.util.zip.CRC32;

public class RangeUncompressor
{
	public enum Strategy { HIGHBYTE, RANGEBOTTOM }
	private final static Logger logger = Logger.getLogger("HFLogger");
	RangeUncompressData uData;
	CRC32 crc = new CRC32();
	long decodedBytes = 0;
	long compressedBytesCounter = 0;
	final int SHOW_PROGRESS_AFTER = 1_000_000; // display progress only if file size is larger then this
	private long low; //, high;
	private long range;
	private long nextChar = 0;
	//final long UPPER = (long)Math.pow(2, 32);//40) - 1; // set all bits to '1' in binary representation. Slightly more than 1_000_000_000_000L
	final int CODEBITS = 32;
	final int HIGHBYTE = CODEBITS - 8;
	final long TOP = 1L << CODEBITS;
	final long BOTTOM = 1 << (CODEBITS/2);
	final long MASK2 = 0x00000000FFFFFFFFL;
	private Strategy strategy;

	RangeUncompressor(Strategy strategy)
	{
		this.strategy = strategy;
	}

	RangeUncompressor()
	{
		this(Strategy.HIGHBYTE);
	}

	public void uncompress(UncompressData uuData) throws IOException
	{
		this.uData = (RangeUncompressData) uuData;
		decodedBytes = 0;

		if(uData.sizeCompressed == 0) // for support of zero-length files
			return;

		low = 0;
		range = TOP - 1; // сразу растягиваем на максимальный range
		long totalFreq = uData.cumFreq[uData.cumFreq.length - 1];

		logger.finer(String.format("low=%X high=%X, range=%X", low, low + range, range));

		if(uData.sizeCompressed > SHOW_PROGRESS_AFTER) uData.cb.start();

		long threshold = 0;
		long delta = uData.sizeCompressed/100;

		long c1 = uData.sin.read();
		long c2 = uData.sin.read();
		long c3 = uData.sin.read();
		long c4 = uData.sin.read();
		nextChar = (c1 << 24) | (c2 << 16) | c3 << 8 | c4;

		compressedBytesCounter = 4;

		while(decodedBytes < uData.sizeUncompressed)
		{
			if((uData.sizeCompressed > SHOW_PROGRESS_AFTER) && (compressedBytesCounter > threshold))
			{
				threshold +=delta;
				uData.cb.heartBeat((int)(100*threshold/uData.sizeCompressed));
			}

			long t = ((nextChar - low + 1) * totalFreq - 1) / (range + 1);
			//logger.fine(String.format("t = 0x%X", uData.symbols[j-1]));

			int j = 0;
			assert t <= totalFreq;
			while (/*(j < uData.cumFreq.length) &&*/ (uData.cumFreq[j] <= t)) j++;

			uData.sout.write(uData.symbols[j-1]);
			decodedBytes++;

			logger.finer(String.format("uncompress output byte: 0x%X, count: %,d", uData.symbols[j-1], decodedBytes));

			long left = uData.cumFreq[j - 1];
			long right = uData.cumFreq[j];

			//long r = range/totalFreq;
			low = low + ((range+1) * left) / totalFreq;
			range = ((range+1) * (right - left)) / totalFreq - 1;

			//long range = high - low;
			//high = low + (range * right) / totalFreq;
			//low = low + (range * left) / totalFreq;

			logger.finer(String.format("before scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low + range, range));

			switch (strategy)
			{
				case HIGHBYTE -> scale();
				case RANGEBOTTOM -> scale2();
			}

			logger.finer(String.format("after  scale sym=%X, low=%X high=%X, range=%X", nextChar, low, low+range, range));
		}

		uData.sout.flush();

		if(uData.sizeCompressed > SHOW_PROGRESS_AFTER) uData.cb.finish();

		uData.CRC32Value = crc.getValue();
	}

	private void scale() throws IOException
	{
		//long range = high - low;
		while( (((low ^ (low + range)) >>> HIGHBYTE) == 0) ) // use highbyte(low)==highbyte(high) strategy
		{
			if(compressedBytesCounter < uData.sizeCompressed) // не читаем лишние байты из потока так как там может быть следующий поток
			{
				int ch = uData.sin.read();
				if (ch == -1) break;
				compressedBytesCounter++;
				nextChar = ((nextChar << 8) | ch) & MASK2;
				low = (low << 8) & MASK2;
				range = range << 8;
				assert (range & MASK2) == range;
			}
			else break;
		}
		//high = low + range;
	}

	private void scale2() throws IOException
	{
		while(range <= BOTTOM) // using range<BOTTOM strategy
		{
			if(compressedBytesCounter < uData.sizeCompressed) // не читаем лишние байты из потока так как там может быть следующий поток
			{
				int ch = uData.sin.read();
				if (ch == -1) break;
				compressedBytesCounter++;
				nextChar = ((nextChar << 8) | ch) & MASK2;
				low = (low << 8) & MASK2;
				range = range << 8;
				assert (range & MASK2) == range;
			}
			else break;
		}
	}
}
